package sweda.cnpc_immersiveboss.mixin;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sweda.cnpc_immersiveboss.client.ThrowClientState;

/** Prevents the real victim from being drawn alongside the Gecko puppet. */
@Mixin(EntityRenderDispatcher.class)
public abstract class MixinEntityRenderDispatcher {
    @Inject(
        method = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void cnpc_immersiveboss$hideThrowVictim(
            Entity entity, double x, double y, double z, float yRot, float partialTick,
            com.mojang.blaze3d.vertex.PoseStack poseStack,
            net.minecraft.client.renderer.MultiBufferSource bufferSource,
            int packedLight, CallbackInfo ci) {
        if (entity != null && ThrowClientState.isActiveForTarget(entity.getId())) {
            ci.cancel();
        }
    }
}
