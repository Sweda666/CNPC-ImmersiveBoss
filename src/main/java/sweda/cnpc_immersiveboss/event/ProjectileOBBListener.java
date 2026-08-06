package sweda.cnpc_immersiveboss.event;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import noppes.npcs.entity.EntityNPCInterface;
import sweda.cnpc_immersiveboss.api.IOBBHolder;
import sweda.cnpc_immersiveboss.hitbox.GeoHitboxDef;
import sweda.cnpc_immersiveboss.hitbox.OBB;
import sweda.cnpc_immersiveboss.hitbox.OBBPhysics;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Intercepts projectile impacts on NPCs and re-checks the collision ray
 * against per-bone OBBs using SAT slab-method raycasting.
 *
 * Two-phase approach:
 * 1. {@code ProjectileImpactEvent} — catches hits within the NPC's default AABB
 *    (fires only when the vanilla system detects an AABB intersection).
 * 2. Tick-level background scan — catches hits in OBB-covered areas that
 *    extend beyond the default AABB (e.g. large sensor boxes). Without this,
 *    projectiles passing through the outer reaches of an OBB but outside the
 *    narrow default AABB would never trigger an impact event.
 */
public class ProjectileOBBListener {

    /** Tridents bounce back at this fraction of their incoming velocity. */
    private static final double TRIDENT_BOUNCE_FACTOR = 0.2;
    /** Ticks a trident keeps its "bounced" flag (re-hit guard) AND gets velocity damping. */
    private static final long BOUNCE_DAMP_TICKS = 12;
    /** Per-tick velocity multiplier while a bounced trident is in its damping window. */
    private static final double TRIDENT_DAMP_FACTOR = 0.85;
    /** Server-side: UUID → tick of last trident bounce. Prevents re-damage/re-bounce while the trident is still inside the boss. */
    private static final Map<UUID, Long> BOUNCED_TRIDENTS = new ConcurrentHashMap<>();
    /** World-spanning AABB for per-dimension trident lookups (1.20.1 has no AABB.INFINITE). */
    private static final AABB INFINITE_BOX = new AABB(
        Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
        Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    /**
     * Pierce-arrow damage dedup: arrow UUID → NPC ids this bolt already damaged.
     * Kept in sync across both damage paths (vanilla AABB hits and this scan's
     * OBB-only hits) so a piercing bolt damages each NPC exactly once.
     */
    private static final Map<UUID, PierceDamageEntry> PIERCE_DAMAGED = new ConcurrentHashMap<>();
    /** How long a pierce-dedup entry is kept after its last hit before being GC'd. */
    private static final long PIERCE_ENTRY_TTL_TICKS = 6000;

    private static final class PierceDamageEntry {
        final Set<Integer> npcIds = ConcurrentHashMap.newKeySet();
        long lastHitTick;
    }

    /**
     * Records a pierce hit on this NPC. Returns true if this bolt had NOT damaged
     * this NPC before (caller should deal damage); false if already damaged.
     */
    private static boolean recordPierceDamage(Projectile proj, int npcId, long tick) {
        PierceDamageEntry entry = PIERCE_DAMAGED.computeIfAbsent(proj.getUUID(), k -> new PierceDamageEntry());
        entry.lastHitTick = tick;
        return entry.npcIds.add(npcId);
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        HitResult hit = event.getRayTraceResult();
        if (!(hit instanceof EntityHitResult entityHit)) return;

        Entity target = entityHit.getEntity();
        if (!(target instanceof EntityNPCInterface npc)) return;

        Map<String, OBB> relObbs;
        if (target instanceof IOBBHolder holder) {
            relObbs = holder.cnpc_immersiveboss$getBoneOBBs();
        } else {
            return;
        }
        if (relObbs.isEmpty()) return;

        Projectile proj = event.getProjectile();
        Vec3 rayStart = new Vec3(proj.xo, proj.yo, proj.zo);
        Vec3 rayEnd = proj.position();
        Vec3 pos = npc.position();

        // Fast projectiles (crossbow bolts) can jump several blocks per tick —
        // subdivide the segment so thin OBBs are not skipped.
        String hitBone = testRaySegmented(relObbs, pos, rayStart, rayEnd);

        if (hitBone != null && target instanceof IOBBHolder h) {
            h.cnpc_immersiveboss$setLastHitboxName(hitBone);
        }

        // Piercing arrows: vanilla pierce logic applies damage on AABB hits. If this
        // bolt already damaged this NPC (via the tick scan covering OBB-only hits),
        // cancel so vanilla cannot double-damage it. Never cancelled on first hit —
        // cancelling skips hitEntity entirely, which would make the bolt pass through
        // the NPC without dealing any damage.
        if (proj instanceof AbstractArrow arrow && arrow.getPierceLevel() > 0) {
            if (!recordPierceDamage(proj, npc.getId(), proj.level().getGameTime())) {
                event.setCanceled(true);
                return;
            }
            return; // first hit — let vanilla pierce deal the damage
        }

        if (hitBone == null) {
            event.setCanceled(true);
            return;
        }

        if (proj instanceof ThrownTrident) {
            // The server drives the whole bounce. The client cancels too so its
            // vanilla hitEntity can't double-play sounds or fight the server motion.
            event.setCanceled(true);
            if (!proj.level().isClientSide) {
                List<OBB> worldObbs = new ArrayList<>(relObbs.size());
                for (OBB rel : relObbs.values()) {
                    worldObbs.add(new OBB(rel.center.add(pos), rel.halfExtents, rel.axisX, rel.axisY, rel.axisZ));
                }
                bounceTrident(npc, proj, hitBone, worldObbs);
            }
            return;
        }

        // Piercing arrows and everything else: leave to vanilla hit handling. Vanilla pierce
        // logic applies damage and keeps the arrow flying; the tick scan skips piercing arrows
        // so it cannot damage them twice or discard them.
    }

    /**
     * Tick-level background scan: catches projectiles that pass through
     * OBB-covered areas outside the NPC's default AABB.
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        long tick = event.getServer().getTickCount();

        // GC stale pierce-dedup entries (arrows that have since been removed).
        if (!PIERCE_DAMAGED.isEmpty()) {
            PIERCE_DAMAGED.entrySet().removeIf(e -> tick - e.getValue().lastHitTick > PIERCE_ENTRY_TTL_TICKS);
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
            // Trident bounce damping: while a bounced trident is still in its damping
            // window, bleed its speed every tick so it stops a short distance away
            // instead of flying off at full bounce speed.
            if (!BOUNCED_TRIDENTS.isEmpty()) {
                for (ThrownTrident t : level.getEntitiesOfClass(ThrownTrident.class, INFINITE_BOX)) {
                    Long bounceTick = BOUNCED_TRIDENTS.get(t.getUUID());
                    if (bounceTick == null) continue;
                    long since = tick - bounceTick;
                    if (since > BOUNCE_DAMP_TICKS) {
                        BOUNCED_TRIDENTS.remove(t.getUUID());
                        continue;
                    }
                    t.setDeltaMovement(t.getDeltaMovement().scale(TRIDENT_DAMP_FACTOR));
                }
            }

            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof EntityNPCInterface npc)) continue;
                if (!(entity instanceof IOBBHolder holder)) continue;

                Map<String, OBB> obbs = holder.cnpc_immersiveboss$getBoneOBBs();
                if (obbs.isEmpty()) continue;

                // Build world-space OBBs and scan for nearby projectiles
                Vec3 pos = npc.position();
                List<OBB> worldObbs = new ArrayList<>(obbs.size());
                for (Map.Entry<String, OBB> entry : obbs.entrySet()) {
                    OBB rel = entry.getValue();
                    worldObbs.add(new OBB(rel.center.add(pos), rel.halfExtents,
                        rel.axisX, rel.axisY, rel.axisZ));
                }
                AABB scanBox = OBBPhysics.enclosingAABB(worldObbs).inflate(1.5);

                List<Projectile> projectiles = level.getEntitiesOfClass(Projectile.class, scanBox,
                    p -> p.isAlive() && !(p.getOwner() == npc));
                for (Projectile proj : projectiles) {
                    Vec3 rayStart = new Vec3(proj.xo, proj.yo, proj.zo);
                    Vec3 rayEnd = proj.position();

                    String hitBone = testRaySegmented(obbs, pos, rayStart, rayEnd);
                    if (hitBone == null) continue;

                    if (proj instanceof ThrownTrident) {
                        // Never discard a trident — bounce it once (re-hit guard lives
                        // inside bounceTrident) and leave it alone while it flies back out.
                        bounceTrident(npc, proj, hitBone, worldObbs);
                        continue;
                    }

                    // Piercing arrows: vanilla pierce damages AABB hits; this scan covers
                    // OBB-only hits outside the default AABB (otherwise fast bolts could
                    // pass through the boss untouched). Shared dedup set ensures the same
                    // bolt damages each NPC exactly once across both paths.
                    if (proj instanceof AbstractArrow arrow && arrow.getPierceLevel() > 0) {
                        if (recordPierceDamage(proj, npc.getId(), tick)) {
                            applyProjectileDamage(npc, proj, hitBone);
                        } else if (npc instanceof IOBBHolder h) {
                            h.cnpc_immersiveboss$setLastHitboxName(hitBone);
                        }
                        continue;
                    }

                    applyProjectileDamage(npc, proj, hitBone);
                    proj.discard();
                    break;
                }
            }
        }
    }

    /**
     * Bounces a trident back out of the boss, mimicking vanilla's on-hit response
     * (immediate damage + TRIDENT_HIT sound) while keeping the mod's bounce-back
     * gameplay: the trident is snapped out of the hitboxes and flung back along its
     * incoming path at a readable speed — no slow crawl-through delay.
     */
    private static void bounceTrident(EntityNPCInterface npc, Projectile proj, String hitBone, List<OBB> worldObbs) {
        if (npc instanceof IOBBHolder h) {
            h.cnpc_immersiveboss$setLastHitboxName(hitBone);
        }

        // Re-hit guard: while the trident is flying back out, don't bounce it again.
        long tick = proj.level().getGameTime();
        Long lastBounce = BOUNCED_TRIDENTS.get(proj.getUUID());
        if (lastBounce != null && tick - lastBounce <= BOUNCE_DAMP_TICKS) return;
        BOUNCED_TRIDENTS.put(proj.getUUID(), tick);

        applyProjectileDamage(npc, proj, hitBone);

        Vec3 vel = proj.getDeltaMovement();
        double speed = vel.length();
        if (speed > 1e-6) {
            Vec3 dir = vel.normalize();
            // Snap the trident back out of the boss's hitboxes so the bounce reads
            // instantly instead of crawling through the body.
            Vec3 back = dir.scale(-0.5);
            AABB box = proj.getBoundingBox();
            for (int i = 0; i < 6 && !worldObbs.isEmpty(); i++) {
                boolean inside = false;
                for (OBB obb : worldObbs) {
                    if (OBBPhysics.intersects(obb, box)) { inside = true; break; }
                }
                if (!inside) break;
                box = box.move(back);
            }
            proj.setPos(box.getCenter());
            // Reverse and fling back at a readable fraction of the incoming speed.
            proj.setDeltaMovement(dir.scale(-speed * TRIDENT_BOUNCE_FACTOR));
            proj.hasImpulse = true; // push the new velocity to clients immediately
        } else {
            proj.setDeltaMovement(vel.multiply(-0.1, -0.1, -0.1));
        }

        proj.level().playSound(null, proj.getX(), proj.getY(), proj.getZ(),
            SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 1.0F, 1.0F);
        if (proj.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CRIT, proj.getX(), proj.getY() + 0.2, proj.getZ(),
                8, 0.15, 0.15, 0.15, 0.08);
        }
    }

    /** Records the hit bone for script attribution, then deals mod damage via {@code npc.hurt()}. */
    private static void applyProjectileDamage(EntityNPCInterface npc, Projectile proj, String hitBone) {
        if (npc instanceof IOBBHolder h) {
            h.cnpc_immersiveboss$setLastHitboxName(hitBone);
        }
        if (!(proj.level() instanceof ServerLevel level)) return;

        Entity owner = proj.getOwner();
        DamageSource ds;
        if (proj instanceof ThrownTrident trident) {
            ds = level.damageSources().trident(trident, owner != null ? owner : trident);
        } else if (proj instanceof AbstractArrow arrow) {
            ds = level.damageSources().arrow(arrow, owner != null ? owner : arrow);
        } else {
            ds = level.damageSources().thrown(proj, owner);
        }
        npc.hurt(ds, projectileDamage(proj));
    }

    /** Vanilla-ish base damage: tridents 8.0, arrows their base damage, other projectiles kinetic. */
    private static float projectileDamage(Projectile proj) {
        if (proj instanceof ThrownTrident) return 8.0f;
        if (proj instanceof AbstractArrow arrow) return (float) arrow.getBaseDamage();
        return (float) proj.getDeltaMovement().length() * 3.0f;
    }

    /**
     * Ray from rayStart to rayEnd, subdivided so fast projectiles (crossbow bolts)
     * do not skip thin OBBs between ticks. Returns the first hit bone name or null.
     */
    private static String testRaySegmented(Map<String, OBB> obbs, Vec3 entityPos,
                                           Vec3 rayStart, Vec3 rayEnd) {
        double dist = rayStart.distanceTo(rayEnd);
        int steps = Math.max(1, (int) Math.ceil(dist / 0.5));
        for (int i = 0; i < steps; i++) {
            double t0 = (double) i / steps;
            double t1 = (double) (i + 1) / steps;
            String hit = testAllOBBs(obbs, entityPos, rayStart.lerp(rayEnd, t0), rayStart.lerp(rayEnd, t1));
            if (hit != null) return hit;
        }
        return null;
    }

    /** Tests a ray against attackable OBBs (physical or detectable), preferring physical; returns hit bone name or null. */
    private static String testAllOBBs(Map<String, OBB> obbs, Vec3 entityPos,
                                       Vec3 rayStart, Vec3 rayEnd) {
        double closestPhysical = Double.MAX_VALUE;
        double closestDetectable = Double.MAX_VALUE;
        String hitPhysicalBone = null;
        String hitDetectableBone = null;

        for (Map.Entry<String, OBB> entry : obbs.entrySet()) {
            String name = entry.getKey();
            boolean isPhysical = GeoHitboxDef.isPhysicalBone(name);
            // Only attackable if physical (b suffix) or explicitly detectable (d flag).
            // Sensor-only bones (hs_/has_ — no d, no b) are skipped.
            if (!isPhysical && !GeoHitboxDef.isDetectableBone(name)) continue;

            OBB rel = entry.getValue();
            OBB worldOBB = new OBB(rel.center.add(entityPos), rel.halfExtents,
                rel.axisX, rel.axisY, rel.axisZ);
            double t = OBBPhysics.intersectRay(worldOBB, rayStart, rayEnd);
            if (t < 0) continue;
            if (isPhysical && t < closestPhysical) {
                closestPhysical = t;
                hitPhysicalBone = name;
            } else if (!isPhysical && t < closestDetectable) {
                closestDetectable = t;
                hitDetectableBone = name;
            }
        }

        if (closestPhysical < Double.MAX_VALUE) return GeoHitboxDef.baseBoneName(hitPhysicalBone);
        if (closestDetectable < Double.MAX_VALUE) return GeoHitboxDef.baseBoneName(hitDetectableBone);
        return null;
    }
}
