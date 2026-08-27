package sweda.cnpc_immersiveboss.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import sweda.cnpc_immersiveboss.api.IOBBHolder;
import sweda.cnpc_immersiveboss.hitbox.GeoHitboxDef;
import sweda.cnpc_immersiveboss.hitbox.OBB;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Short-lived client-side association between a damage event and its hit OBB. */
public final class DamageParticleData {
    private static final long TTL_NANOS = 750_000_000L;
    private static final Map<Integer, Hit> HITS = new HashMap<>();

    private DamageParticleData() {}

    public static void record(int entityId, String hitboxName) {
        if (hitboxName == null || hitboxName.isEmpty()) return;
        HITS.put(entityId, new Hit(hitboxName, System.nanoTime() + TTL_NANOS));
    }

    /** Resolves the current animated OBB center for a damage particle target. */
    public static Vec3 findCenter(Entity entity) {
        if (entity == null) return null;
        Hit hit = getLiveHit(entity.getId());
        if (hit == null || !(entity instanceof IOBBHolder holder)) return null;

        for (Map.Entry<String, OBB> entry : holder.cnpc_immersiveboss$getBoneOBBs().entrySet()) {
            if (entry.getKey().equals(hit.hitboxName)
                || GeoHitboxDef.baseBoneName(entry.getKey()).equals(hit.hitboxName)) {
                return entry.getValue().center.add(entity.position());
            }
        }
        return null;
    }

    /** Finds the recently damaged NPC whose vanilla particle position was supplied. */
    public static Vec3 findCenterNear(ClientLevel level, double x, double y, double z) {
        if (level == null) return null;
        pruneExpired();
        Vec3 original = new Vec3(x, y, z);
        double closestDistance = 4.0D;
        Vec3 closestCenter = null;
        for (Integer entityId : HITS.keySet().toArray(Integer[]::new)) {
            Entity entity = level.getEntity(entityId);
            if (entity == null) continue;
            Vec3 vanilla = new Vec3(entity.getX(), entity.getY(0.5D), entity.getZ());
            double distance = vanilla.distanceToSqr(original);
            if (distance <= closestDistance) {
                Vec3 center = findCenter(entity);
                if (center != null) {
                    closestDistance = distance;
                    closestCenter = center;
                }
            }
        }
        return closestCenter;
    }

    private static Hit getLiveHit(int entityId) {
        Hit hit = HITS.get(entityId);
        if (hit == null) return null;
        if (hit.expiresAt < System.nanoTime()) {
            HITS.remove(entityId);
            return null;
        }
        return hit;
    }

    private static void pruneExpired() {
        long now = System.nanoTime();
        Iterator<Map.Entry<Integer, Hit>> iterator = HITS.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().expiresAt < now) iterator.remove();
        }
    }

    private record Hit(String hitboxName, long expiresAt) {}
}
