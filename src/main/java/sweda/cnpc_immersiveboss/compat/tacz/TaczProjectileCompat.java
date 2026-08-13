package sweda.cnpc_immersiveboss.compat.tacz;

import com.tacz.guns.entity.EntityKineticBullet.EntityResult;
import com.tacz.guns.util.EntityUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import noppes.npcs.entity.EntityNPCInterface;
import sweda.cnpc_immersiveboss.api.IOBBHolder;
import sweda.cnpc_immersiveboss.event.ProjectileOBBListener;
import sweda.cnpc_immersiveboss.event.ProjectileOBBListener.ProjectileHit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** TACZ-native entity raycast with animated OBB targets merged into its result set. */
public final class TaczProjectileCompat {
    private TaczProjectileCompat() {
    }

    public static EntityResult findEntityOnPath(Projectile projectile, Vec3 rayStart, Vec3 rayEnd) {
        EntityResult closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (EntityResult result : findEntitiesOnPath(projectile, rayStart, rayEnd)) {
            double distance = rayStart.distanceToSqr(result.getHitPos());
            if (distance < closestDistance) {
                closest = result;
                closestDistance = distance;
            }
        }
        return closest;
    }

    public static List<EntityResult> findEntitiesOnPath(Projectile projectile,
                                                         Vec3 rayStart, Vec3 rayEnd) {
        Map<Integer, EntityResult> results = new HashMap<>();

        // Preserve TACZ results for ordinary entities. OBB-backed NPCs are
        // replaced with the precise OBB result so their vanilla AABB is not authoritative.
        for (EntityResult nativeResult : EntityUtil.findEntitiesOnPath(projectile, rayStart, rayEnd)) {
            Entity target = nativeResult.getEntity();
            if (hasAnimatedObbs(target)) {
                addObbResult(results, target, rayStart, rayEnd);
            } else {
                results.put(target.getId(), nativeResult);
            }
        }

        // TACZ's own broad phase only sees the entity AABB. Scan OBB-backed NPCs
        // separately so boxes extending beyond that AABB remain hittable.
        if (projectile.level() instanceof ServerLevel serverLevel) {
            for (Entity target : serverLevel.getAllEntities()) {
                if (!isValidObbTarget(projectile, target) || results.containsKey(target.getId())) continue;
                addObbResult(results, target, rayStart, rayEnd);
            }
        }

        return new ArrayList<>(results.values());
    }

    public static ProjectileHit findHit(Entity target, Vec3 rayStart, Vec3 rayEnd) {
        if (!(target instanceof EntityNPCInterface npc) || !(target instanceof IOBBHolder holder)) {
            return null;
        }
        if (holder.cnpc_immersiveboss$getBoneOBBs().isEmpty()) return null;
        return ProjectileOBBListener.raycastHitboxes(
            holder.cnpc_immersiveboss$getBoneOBBs(), npc.position(), rayStart, rayEnd);
    }

    private static void addObbResult(Map<Integer, EntityResult> results, Entity target,
                                     Vec3 rayStart, Vec3 rayEnd) {
        ProjectileHit hit = findHit(target, rayStart, rayEnd);
        if (hit == null) return;
        results.put(target.getId(), new EntityResult(target, hit.hitPoint,
            isHeadshot(target, hit.hitPoint)));
    }

    private static boolean hasAnimatedObbs(Entity target) {
        return target instanceof IOBBHolder holder
            && !holder.cnpc_immersiveboss$getBoneOBBs().isEmpty();
    }

    private static boolean isValidObbTarget(Projectile projectile, Entity target) {
        Entity owner = projectile.getOwner();
        return target instanceof EntityNPCInterface
            && hasAnimatedObbs(target)
            && target.isAlive()
            && target.isPickable()
            && !target.isSpectator()
            && target != owner
            && (owner == null || !target.isPassengerOfSameVehicle(owner));
    }

    private static boolean isHeadshot(Entity target, Vec3 hitPoint) {
        double relativeY = hitPoint.y - target.getY();
        double eyeHeight = target.getEyeHeight();
        return relativeY > eyeHeight - 0.25 && relativeY < eyeHeight + 0.25;
    }
}
