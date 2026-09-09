package sweda.cnpc_immersiveboss.mixin.compat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sweda.cnpc_immersiveboss.compat.epicfight.EpicFightCompat;

@Pseudo
@Mixin(targets = "yesman.epicfight.api.animation.types.AttackAnimation", remap = false)
public abstract class MixinEpicFightAttackAnimation {
    @Inject(method = "attackTick", at = @At("HEAD"), remap = false, require = 0)
    private void cnpc_immersiveboss$beginObbAttackPass(
            @Coerce Object livingEntityPatch, @Coerce Object animationAccessor,
            CallbackInfo ci) {
        EpicFightCompat.beginAttackPass(livingEntityPatch);
    }

    @Inject(method = "attackTick", at = @At("RETURN"), remap = false, require = 0)
    private void cnpc_immersiveboss$endObbAttackPass(
            @Coerce Object livingEntityPatch, @Coerce Object animationAccessor,
            CallbackInfo ci) {
        EpicFightCompat.endAttackPass();
    }
}
