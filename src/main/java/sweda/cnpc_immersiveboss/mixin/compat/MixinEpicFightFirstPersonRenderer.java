package sweda.cnpc_immersiveboss.mixin.compat;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sweda.cnpc_immersiveboss.client.ThrowClientState;

/** Suppresses Epic Fight's replacement first-person arm renderer during throws. */
@Mixin(targets = "yesman.epicfight.client.renderer.FirstPersonRenderer", remap = false)
@Pseudo
public abstract class MixinEpicFightFirstPersonRenderer {
    @Inject(method = "render(Lnet/minecraft/client/player/LocalPlayer;Lyesman/epicfight/client/world/capabilites/entitypatch/player/LocalPlayerPatch;Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;Lnet/minecraft/client/renderer/MultiBufferSource;Lcom/mojang/blaze3d/vertex/PoseStack;IF)V",
        at = @At("HEAD"), cancellable = true, require = 0)
    private void cnpc_immersiveboss$hideThrowerArms(CallbackInfo ci) {
        LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null && ThrowClientState.isActiveForTarget(player.getId())) {
            ci.cancel();
        }
    }
}
