package sweda.cnpc_immersiveboss.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sweda.cnpc_immersiveboss.hitbox.BoneWorldData;
import sweda.cnpc_immersiveboss.hitbox.OBB;
import sweda.cnpc_immersiveboss.hitbox.OBBPhysics;

import java.util.List;
import java.util.Map;

/**
 * Replaces vanilla AABB-based entity-vs-entity pushing with SAT-based OBB collision
 * for CustomNPCs that have per-bone hitbox data.
 *
 * Intercepts Entity.push(Entity) at HEAD, checks OBB overlap via SAT,
 * and pushes using OBB center as reference point instead of entity AABB center.
 */
@Mixin(Entity.class)
public abstract class MixinEntityPush {

    @Inject(method = "push", at = @At("HEAD"), cancellable = true)
    private void cnpc_multihitbox$onPush(Entity other, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof EntityNPCInterface)) return;

        Map<String, OBB> relObbs = BoneWorldData.getOBBs(self.getId());
        if (relObbs.isEmpty()) return;

        // Same pre-checks as vanilla
        if (self.isPassengerOfSameVehicle(other)) return;
        if (self.noPhysics || other.noPhysics) return;

        // Convert OBBs to world-space
        Vec3 pos = self.position();
        List<OBB> worldObbs = BoneWorldData.getWorldOBBs(self.getId(), pos);
        if (worldObbs.isEmpty()) return;

        // Find the first OBB that overlaps with the other entity's AABB
        AABB otherBB = other.getBoundingBox();
        OBB hitOBB = null;
        for (OBB obb : worldObbs) {
            if (OBBPhysics.intersects(obb, otherBB)) {
                hitOBB = obb;
                break;
            }
        }
        if (hitOBB == null) return;

        // Compute push direction from OBB center → other entity
        double dx = other.getX() - hitOBB.center.x;
        double dz = other.getZ() - hitOBB.center.z;
        double maxDist = Math.max(Math.abs(dx), Math.abs(dz));
        if (maxDist < 0.01) {
            // Avoid zero vector: random nudge
            dx = self.level().getRandom().nextDouble() - 0.5;
            dz = self.level().getRandom().nextDouble() - 0.5;
            maxDist = Math.max(Math.abs(dx), Math.abs(dz));
        }
        if (maxDist < 0.01) {
            ci.cancel();
            return;
        }

        maxDist = Math.sqrt(maxDist);
        dx /= maxDist;
        dz /= maxDist;
        double inv = Math.min(1.0 / maxDist, 1.0);
        dx *= inv * 0.05;
        dz *= inv * 0.05;

        // Vehicle sign flip (same logic as vanilla Entity.push)
        if (!self.isVehicle() || !self.hasPassenger(other)) {
            dx = -dx;
            dz = -dz;
        }
        if (!other.isVehicle() || !other.hasPassenger(self)) {
            other.setDeltaMovement(other.getDeltaMovement().add(dx, 0, dz));
        }

        ci.cancel();
    }
}
