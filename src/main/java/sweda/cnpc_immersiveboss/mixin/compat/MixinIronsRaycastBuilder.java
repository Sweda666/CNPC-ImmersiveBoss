package sweda.cnpc_immersiveboss.mixin.compat;

import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sweda.cnpc_immersiveboss.compat.ironsspellbooks.IronsSpellbooksCompat;

@Pseudo
@Mixin(targets = "io.redspace.ironsspellbooks.api.util.RaycastBuilder", remap = false)
public abstract class MixinIronsRaycastBuilder {
    @Inject(method = "performRaycast", at = @At("RETURN"),
        cancellable = true, remap = false, require = 0)
    private void cnpc_immersiveboss$raycastObbs(CallbackInfoReturnable<HitResult> cir) {
        cir.setReturnValue(IronsSpellbooksCompat.mergeBuilderHit(this, cir.getReturnValue()));
    }
}
