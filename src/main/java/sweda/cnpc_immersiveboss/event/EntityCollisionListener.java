package sweda.cnpc_immersiveboss.event;

import com.mojang.logging.LogUtils;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import noppes.npcs.api.wrapper.WrapperNpcAPI;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.entity.EntityNPCInterface;
import org.slf4j.Logger;
import sweda.cnpc_immersiveboss.api.HitboxCollideEvent;
import sweda.cnpc_immersiveboss.api.IOBBHolder;
import sweda.cnpc_immersiveboss.hitbox.GeoHitboxDef;
import sweda.cnpc_immersiveboss.hitbox.OBB;
import sweda.cnpc_immersiveboss.hitbox.OBBPhysics;

import java.util.*;

/**
 * Tick-level OBB entity collision detection following HitboxAPI's pattern.
 * Runs on ServerTickEvent.END — detects OBB overlaps between NPCs and other entities,
 * applies mutual push forces, and fires CNPC script hook (HitboxCollideEvent).
 */
public class EntityCollisionListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static int tickCounter = 0;
    /** Track collide events per tick to avoid duplicate firings per entity pair.
     *  Uses two longs packed as a key instead of String concatenation. */
    private static final Set<Long> firedPairs = new HashSet<>();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        tickCounter++;
        boolean logThisTick = (tickCounter % 100 == 0);
        firedPairs.clear();

        for (net.minecraft.server.level.ServerLevel level :
                event.getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof EntityNPCInterface npc)) continue;
                // Per-tick hitbox maintenance — fallback for CustomNPCs builds
                // where EntityNPCInterface.tick()/remove() are not mixin-injectable
                // (idempotent alongside the mixin path).
                NpcHitboxTickHandler.onNpcTick(npc);
                if (!(entity instanceof IOBBHolder holderA)) continue;

                Map<String, OBB> obbsA = holderA.cnpc_immersiveboss$getBoneOBBs();
                if (obbsA.isEmpty()) continue;

                Vec3 posA = npc.position();
                // Sensors participate in overlap events; only physical boxes apply push forces.
                String[] boneNamesA = new String[obbsA.size()];
                boolean[] physicalA = new boolean[obbsA.size()];
                List<OBB> worldObbsA = new ArrayList<>(obbsA.size());
                int bi = 0;
                for (Map.Entry<String, OBB> entry : obbsA.entrySet()) {
                    OBB rel = entry.getValue();
                    worldObbsA.add(new OBB(
                        rel.center.add(posA), rel.halfExtents,
                        rel.axisX, rel.axisY, rel.axisZ
                    ));
                    boneNamesA[bi] = entry.getKey();
                    physicalA[bi++] = GeoHitboxDef.isPhysicalBone(entry.getKey());
                }

                AABB broad = OBBPhysics.enclosingAABB(worldObbsA).inflate(0.5);

                List<Entity> nearby = level.getEntities(entity, broad,
                    e -> e != entity && !e.noPhysics && !e.isPassengerOfSameVehicle(entity));

                int collisions = 0;
                for (Entity other : nearby) {
                    if (other instanceof IOBBHolder holderB) {
                        Map<String, OBB> obbsB = holderB.cnpc_immersiveboss$getBoneOBBs();
                        if (obbsB.isEmpty()) continue;
                        Vec3 posB = other.position();
                        String[] boneNamesB = new String[obbsB.size()];
                        boolean[] physicalB = new boolean[obbsB.size()];
                        List<OBB> worldObbsB = new ArrayList<>(obbsB.size());
                        int bj = 0;
                        for (Map.Entry<String, OBB> entry : obbsB.entrySet()) {
                            OBB rel = entry.getValue();
                            worldObbsB.add(new OBB(
                                rel.center.add(posB), rel.halfExtents,
                                rel.axisX, rel.axisY, rel.axisZ
                            ));
                            boneNamesB[bj] = entry.getKey();
                            physicalB[bj++] = GeoHitboxDef.isPhysicalBone(entry.getKey());
                        }

                        boolean pushed = false;
                        for (int ia = 0; ia < worldObbsA.size(); ia++) {
                            OBB obbA = worldObbsA.get(ia);
                            for (int ib = 0; ib < worldObbsB.size(); ib++) {
                                if (OBBPhysics.intersects(obbA, worldObbsB.get(ib))) {
                                    if (!pushed && physicalA[ia] && physicalB[ib]) {
                                        pushMutual(entity, other, obbA);
                                        pushed = true;
                                    }
                                    fireCollideHook(npc, other, boneNamesA[ia], boneNamesB[ib]);
                                    collisions++;
                                }
                            }
                        }
                    } else {
                        AABB otherBB = other.getBoundingBox();
                        boolean pushed = false;
                        for (int ia = 0; ia < worldObbsA.size(); ia++) {
                            if (OBBPhysics.intersects(worldObbsA.get(ia), otherBB)) {
                                if (!pushed && physicalA[ia]) {
                                    pushMutual(entity, other, worldObbsA.get(ia));
                                    pushed = true;
                                }
                                fireCollideHook(npc, other, boneNamesA[ia], "AABB");
                                collisions++;
                            }
                        }
                    }
                }

                if (logThisTick && collisions > 0) {
                    LOGGER.info("[OBB-Collision] NPC {} {} OBBs, {} entity collisions",
                        entity.getId(), worldObbsA.size(), collisions);
                }
            }
        }
    }

    private static void fireCollideHook(EntityNPCInterface npc, Entity other,
                                         String hitboxAName, String hitboxBName) {
        // Packed key to avoid string concatenation in hot path
        long pairKey = ((long) npc.getId() << 32) | ((long) other.getId() & 0xFFFF_FFFFL);
        pairKey = pairKey * 31 + hitboxAName.hashCode();
        pairKey = pairKey * 31 + hitboxBName.hashCode();
        if (!firedPairs.add(pairKey)) return;

        if (!npc.script.isEnabled() || npc.script.getScripts().isEmpty()) return;

        HitboxCollideEvent collideEvent = new HitboxCollideEvent(
            npc.wrappedNPC, other, hitboxAName, hitboxBName);

        npc.script.runScript(EnumScriptType.COLLIDE, collideEvent);
        WrapperNpcAPI.EVENT_BUS.post(collideEvent);
    }

    /** Push both entities apart from OBB center, syncing velocity to clients for players. */
    private static void pushMutual(Entity self, Entity other, OBB obb) {
        double dx = other.getX() - obb.center.x;
        double dz = other.getZ() - obb.center.z;
        double maxDist = Math.max(Math.abs(dx), Math.abs(dz));
        if (maxDist < 0.01) {
            dx = self.level().getRandom().nextDouble() - 0.5;
            dz = self.level().getRandom().nextDouble() - 0.5;
            maxDist = Math.max(Math.abs(dx), Math.abs(dz));
        }
        if (maxDist < 0.01) return;

        maxDist = Math.sqrt(maxDist);
        dx /= maxDist;
        dz /= maxDist;
        double force = Math.min(1.0 / maxDist, 1.0) * 0.05;

        // Push other AWAY from OBB center
        other.setDeltaMovement(other.getDeltaMovement().add(dx * force, 0, dz * force));
        // Push self AWAY from other
        self.setDeltaMovement(self.getDeltaMovement().add(-dx * force, 0, -dz * force));

        // Sync velocity to client for players (required for setDeltaMovement to take effect)
        if (other instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
        }
        if (self instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
        }
    }
}
