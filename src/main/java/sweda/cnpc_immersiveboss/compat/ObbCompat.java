package sweda.cnpc_immersiveboss.compat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import sweda.cnpc_immersiveboss.api.IOBBHolder;
import sweda.cnpc_immersiveboss.hitbox.GeoHitboxDef;
import sweda.cnpc_immersiveboss.hitbox.OBB;
import sweda.cnpc_immersiveboss.hitbox.OBBPhysics;

import java.util.Map;
import java.util.function.Predicate;

/** Shared attackable-OBB queries used by optional combat integrations. */
public final class ObbCompat {
    private ObbCompat() {
    }

    public static boolean hasAnimatedObbs(Entity entity) {
        return entity instanceof IOBBHolder holder
            && !holder.cnpc_immersiveboss$getBoneOBBs().isEmpty();
    }

    public static String findIntersection(Entity entity, OBB volume, Vec3 reference) {
        return find(entity, reference, worldObb -> OBBPhysics.intersects(worldObb, volume));
    }

    public static String findIntersection(Entity entity, AABB volume, Vec3 reference) {
        return find(entity, reference, worldObb -> OBBPhysics.intersects(worldObb, volume));
    }

    public static String findSphereIntersection(Entity entity, Vec3 center,
                                                 double radius, Vec3 reference) {
        double radiusSqr = radius * radius;
        return find(entity, reference,
            worldObb -> OBBPhysics.distanceToSqr(worldObb, center) <= radiusSqr);
    }

    public static String findNearest(Entity entity, Vec3 reference) {
        return find(entity, reference, worldObb -> true);
    }

    public static RayHit findRayIntersection(Entity entity, Vec3 rayStart,
                                             Vec3 rayEnd, double inflation) {
        if (!(entity instanceof IOBBHolder holder)) return null;

        RayHit physical = null;
        RayHit detectable = null;
        for (Map.Entry<String, OBB> entry : holder.cnpc_immersiveboss$getBoneOBBs().entrySet()) {
            String name = entry.getKey();
            boolean isPhysical = GeoHitboxDef.isPhysicalBone(name);
            if (!isPhysical && !GeoHitboxDef.isDetectableBone(name)) continue;

            OBB worldObb = toWorld(entity, entry.getValue());
            if (inflation > 0) {
                worldObb = new OBB(worldObb.center,
                    worldObb.halfExtents.add(inflation, inflation, inflation),
                    worldObb.axisX, worldObb.axisY, worldObb.axisZ);
            }
            double distance = OBBPhysics.intersectRay(worldObb, rayStart, rayEnd);
            if (distance < 0) continue;
            RayHit hit = new RayHit(GeoHitboxDef.baseBoneName(name),
                pointAlongRay(rayStart, rayEnd, distance), distance);
            if (isPhysical && (physical == null || distance < physical.distance)) {
                physical = hit;
            } else if (!isPhysical && (detectable == null || distance < detectable.distance)) {
                detectable = hit;
            }
        }
        return physical != null ? physical : detectable;
    }

    public static Vec3 closestPoint(OBB obb, Vec3 point) {
        Vec3 offset = point.subtract(obb.center);
        double x = clamp(offset.dot(obb.axisX), -obb.halfExtents.x, obb.halfExtents.x);
        double y = clamp(offset.dot(obb.axisY), -obb.halfExtents.y, obb.halfExtents.y);
        double z = clamp(offset.dot(obb.axisZ), -obb.halfExtents.z, obb.halfExtents.z);
        return obb.center
            .add(obb.axisX.scale(x))
            .add(obb.axisY.scale(y))
            .add(obb.axisZ.scale(z));
    }

    public static Vec3 closestAttackablePoint(Entity entity, Vec3 point) {
        if (!(entity instanceof IOBBHolder holder)) return entity.getBoundingBox().getCenter();

        Vec3 closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (Map.Entry<String, OBB> entry : holder.cnpc_immersiveboss$getBoneOBBs().entrySet()) {
            if (!isAttackable(entry.getKey())) continue;
            Vec3 candidate = closestPoint(toWorld(entity, entry.getValue()), point);
            double distance = point.distanceToSqr(candidate);
            if (distance < closestDistance) {
                closest = candidate;
                closestDistance = distance;
            }
        }
        return closest != null ? closest : entity.getBoundingBox().getCenter();
    }

    private static String find(Entity entity, Vec3 reference, Predicate<OBB> intersects) {
        if (!(entity instanceof IOBBHolder holder)) return null;

        String physical = null;
        String detectable = null;
        double physicalDistance = Double.MAX_VALUE;
        double detectableDistance = Double.MAX_VALUE;

        for (Map.Entry<String, OBB> entry : holder.cnpc_immersiveboss$getBoneOBBs().entrySet()) {
            String name = entry.getKey();
            boolean isPhysical = GeoHitboxDef.isPhysicalBone(name);
            if (!isPhysical && !GeoHitboxDef.isDetectableBone(name)) continue;

            OBB worldObb = toWorld(entity, entry.getValue());
            if (!intersects.test(worldObb)) continue;
            double distance = reference.distanceToSqr(closestPoint(worldObb, reference));
            if (isPhysical && distance < physicalDistance) {
                physical = name;
                physicalDistance = distance;
            } else if (!isPhysical && distance < detectableDistance) {
                detectable = name;
                detectableDistance = distance;
            }
        }

        String selected = physical != null ? physical : detectable;
        return selected != null ? GeoHitboxDef.baseBoneName(selected) : null;
    }

    private static boolean isAttackable(String name) {
        return GeoHitboxDef.isPhysicalBone(name) || GeoHitboxDef.isDetectableBone(name);
    }

    private static OBB toWorld(Entity entity, OBB relative) {
        return new OBB(relative.center.add(entity.position()), relative.halfExtents,
            relative.axisX, relative.axisY, relative.axisZ);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Vec3 pointAlongRay(Vec3 start, Vec3 end, double distance) {
        double length = start.distanceTo(end);
        if (length < 1.0E-10) return start;
        return start.lerp(end, Math.max(0.0, Math.min(1.0, distance / length)));
    }

    public static final class RayHit {
        public final String hitboxName;
        public final Vec3 hitPoint;
        public final double distance;

        private RayHit(String hitboxName, Vec3 hitPoint, double distance) {
            this.hitboxName = hitboxName;
            this.hitPoint = hitPoint;
            this.distance = distance;
        }
    }
}
