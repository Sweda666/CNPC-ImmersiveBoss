package sweda.cnpc_immersiveboss.compat.epicfight;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import sweda.cnpc_immersiveboss.compat.ObbCompat;

import java.util.function.Predicate;

/** Client-only OBB raycast for Epic Fight's independent camera target picker. */
public final class EpicFightClientCompat {
    private EpicFightClientCompat() {
    }

    public static EntityHitResult findTarget(Entity viewer, Vec3 rayStart,
                                             Vec3 rayEnd, AABB searchArea,
                                             Predicate<Entity> focusable,
                                             double maxDistanceSqr) {
        EntityHitResult bestResult = ProjectileUtil.getEntityHitResult(
            viewer, rayStart, rayEnd, searchArea,
            candidate -> !ObbCompat.hasAnimatedObbs(candidate) && focusable.test(candidate),
            maxDistanceSqr);
        double bestDistance = bestResult != null
            ? bestResult.getLocation().distanceToSqr(rayStart)
            : maxDistanceSqr;

        if (!(viewer.level() instanceof ClientLevel level)) return bestResult;
        for (Entity candidate : level.entitiesForRendering()) {
            if (candidate == viewer || !ObbCompat.hasAnimatedObbs(candidate)
                || !focusable.test(candidate)) continue;

            ObbCompat.RayHit hit = ObbCompat.findRayIntersection(
                candidate, rayStart, rayEnd, 0.0);
            if (hit == null) continue;
            double distance = hit.hitPoint.distanceToSqr(rayStart);
            if (distance <= maxDistanceSqr && distance < bestDistance) {
                bestDistance = distance;
                bestResult = new EntityHitResult(candidate, hit.hitPoint);
            }
        }
        return bestResult;
    }
}
