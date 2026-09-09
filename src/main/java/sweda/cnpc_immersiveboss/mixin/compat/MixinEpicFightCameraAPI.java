package sweda.cnpc_immersiveboss.mixin.compat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import sweda.cnpc_immersiveboss.compat.epicfight.EpicFightClientCompat;

import java.util.function.Predicate;

@Pseudo
@Mixin(targets = "yesman.epicfight.api.client.camera.EpicFightCameraAPI", remap = false)
public abstract class MixinEpicFightCameraAPI {
    @Redirect(method = "postClientTick",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/ProjectileUtil;getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;"),
        remap = false, require = 0)
    private EntityHitResult cnpc_immersiveboss$mappedFindObbTarget(
            Entity viewer, Vec3 rayStart, Vec3 rayEnd, AABB searchArea,
            Predicate<Entity> focusable, double maxDistanceSqr) {
        return EpicFightClientCompat.findTarget(viewer, rayStart, rayEnd,
            searchArea, focusable, maxDistanceSqr);
    }

    @Redirect(method = "postClientTick",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/ProjectileUtil;m_37287_(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;"),
        remap = false, require = 0)
    private EntityHitResult cnpc_immersiveboss$srgFindObbTarget(
            Entity viewer, Vec3 rayStart, Vec3 rayEnd, AABB searchArea,
            Predicate<Entity> focusable, double maxDistanceSqr) {
        return EpicFightClientCompat.findTarget(viewer, rayStart, rayEnd,
            searchArea, focusable, maxDistanceSqr);
    }
}
