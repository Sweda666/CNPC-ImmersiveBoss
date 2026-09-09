package sweda.cnpc_immersiveboss.mixin;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sweda.cnpc_immersiveboss.client.ThrowClientState;

/** Applies the throw camera after every vanilla/compatibility angle update. */
@Mixin(Camera.class)
public abstract class MixinCamera {
    @Inject(method = {"setAnglesInternal", "m_90572_"}, at = @At("TAIL"),
        remap = false, require = 0)
    private void cnpc_immersiveboss$applyThrowCamera(float yaw, float pitch, CallbackInfo ci) {
        ThrowClientState.applyCamera((Camera) (Object) this);
    }
}
