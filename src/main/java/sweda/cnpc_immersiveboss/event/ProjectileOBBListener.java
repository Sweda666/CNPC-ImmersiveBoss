package sweda.cnpc_immersiveboss.event;

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
    private static final double TRIDENT_BOUNCE_FACTOR = 0.1;
    /** Ticks a trident keeps its "bounced" flag, preventing re-hits while flying back out. */
    private static final long BOUNCE_FLAG_TICKS = 100;
    /** Server-side: UUID → tick of last trident bounce. Prevents re-damage/re-bounce while the trident is still inside the boss. */
    private static final Map<UUID, Long> BOUNCED_TRIDENTS = new ConcurrentHashMap<>();

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

        // Piercing arrows: vanilla pierce logic applies damage AND keeps the arrow
        // flying. Never cancel — cancelling skips hitEntity entirely, which makes
        // the bolt visually pass through the NPC without dealing any damage.
        if (proj instanceof AbstractArrow arrow && arrow.getPierceLevel() > 0) {
            return;
        }

        if (hitBone == null) {
            event.setCanceled(true);
            return;
        }

        if (proj instanceof ThrownTrident && !proj.level().isClientSide) {
            // Vanilla would zero the trident's velocity and leave it hanging inside the boss,
            // where the tick scan below used to discard it. Instead: damage the hitbox and
            // bounce the trident back. Cancelling also prevents vanilla from double-damaging.
            event.setCanceled(true);
            long tick = proj.level().getGameTime();
            Long lastBounce = BOUNCED_TRIDENTS.get(proj.getUUID());
            if (lastBounce == null || tick - lastBounce > BOUNCE_FLAG_TICKS) {
                BOUNCED_TRIDENTS.put(proj.getUUID(), tick);
                applyProjectileDamage(npc, proj, hitBone);
                proj.setDeltaMovement(proj.getDeltaMovement()
                    .multiply(-TRIDENT_BOUNCE_FACTOR, -TRIDENT_BOUNCE_FACTOR, -TRIDENT_BOUNCE_FACTOR));
                proj.level().playSound(null, proj.getX(), proj.getY(), proj.getZ(),
                    SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 1.0F, 1.0F);
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
        if (!BOUNCED_TRIDENTS.isEmpty()) {
            BOUNCED_TRIDENTS.entrySet().removeIf(e -> tick - e.getValue() > BOUNCE_FLAG_TICKS);
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
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
                    // Piercing arrows: vanilla pierce logic already applied damage and keeps them
                    // flying — do not let the scan damage them twice or discard them.
                    if (proj instanceof AbstractArrow arrow && arrow.getPierceLevel() > 0
                        && !(proj instanceof ThrownTrident)) {
                        continue;
                    }

                    Vec3 rayStart = new Vec3(proj.xo, proj.yo, proj.zo);
                    Vec3 rayEnd = proj.position();

                    String hitBone = testRaySegmented(obbs, pos, rayStart, rayEnd);
                    if (hitBone == null) continue;

                    if (proj instanceof ThrownTrident) {
                        // Never discard a trident. Bounce it once (if it has not bounced already)
                        // and leave it alone while it flies back out of the boss.
                        Long lastBounce = BOUNCED_TRIDENTS.get(proj.getUUID());
                        if (lastBounce == null || tick - lastBounce > BOUNCE_FLAG_TICKS) {
                            BOUNCED_TRIDENTS.put(proj.getUUID(), tick);
                            applyProjectileDamage(npc, proj, hitBone);
                            proj.setDeltaMovement(proj.getDeltaMovement()
                                .multiply(-TRIDENT_BOUNCE_FACTOR, -TRIDENT_BOUNCE_FACTOR, -TRIDENT_BOUNCE_FACTOR));
                            proj.level().playSound(null, proj.getX(), proj.getY(), proj.getZ(),
                                SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 1.0F, 1.0F);
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
