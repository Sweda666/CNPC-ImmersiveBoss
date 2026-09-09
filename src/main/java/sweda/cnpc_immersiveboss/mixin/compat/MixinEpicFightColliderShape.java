package sweda.cnpc_immersiveboss.mixin.compat;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sweda.cnpc_immersiveboss.compat.epicfight.EpicFightCompat;

@Pseudo
@Mixin(targets = {
    "yesman.epicfight.api.collider.OBBCollider",
    "yesman.epicfight.api.collider.LineCollider",
    "yesman.epicfight.api.collider.PlaneCollider"
}, remap = false)
public abstract class MixinEpicFightColliderShape {
    @Inject(method = "isCollide(Lnet/minecraft/world/entity/Entity;)Z",
        at = @At("HEAD"), cancellable = true,
        remap = false, require = 0)
    private void cnpc_immersiveboss$testAnimatedObbs(
            Entity target, CallbackInfoReturnable<Boolean> cir) {
        Boolean collides = EpicFightCompat.testObbCollision(this, target);
        if (collides != null) cir.setReturnValue(collides);
    }
}
