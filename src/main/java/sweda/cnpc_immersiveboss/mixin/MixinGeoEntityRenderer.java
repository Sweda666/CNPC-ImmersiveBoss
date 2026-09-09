package sweda.cnpc_immersiveboss.mixin;

import com.goodbird.cnpcgeckoaddon.client.renderer.RenderCustomModel;
import com.goodbird.cnpcgeckoaddon.entity.EntityCustomModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import sweda.cnpc_immersiveboss.client.OBBRenderCapture;
import sweda.cnpc_immersiveboss.client.ThrowClientState;

@Mixin(value = GeoEntityRenderer.class, remap = false)
public abstract class MixinGeoEntityRenderer {
    @Inject(method = "renderRecursively", at = @At("HEAD"), remap = false, require = 0)
    private void cnpc_immersiveboss$beginRender(
            PoseStack poseStack, Entity animatable, GeoBone bone, RenderType renderType,
            MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
            float partialTick, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha, CallbackInfo callbackInfo) {
        ThrowClientState.beginRender(bufferSource, renderType);
    }

    @Inject(method = "renderRecursively", at = @At("RETURN"), remap = false, require = 0)
    private void cnpc_immersiveboss$endRender(
            PoseStack poseStack, Entity animatable, GeoBone bone, RenderType renderType,
            MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
            float partialTick, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha, CallbackInfo callbackInfo) {
        ThrowClientState.endRender();
    }

    @Inject(
        method = "renderRecursively",
        at = @At(
            value = "INVOKE",
            target = "Lsoftware/bernie/geckolib/renderer/GeoEntityRenderer;renderCubesOfBone(Lcom/mojang/blaze3d/vertex/PoseStack;Lsoftware/bernie/geckolib/cache/object/GeoBone;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V",
            shift = At.Shift.BEFORE
        ),
        remap = false,
        require = 0
    )
    private void cnpc_multihitbox$captureBone(
            PoseStack poseStack, Entity animatable, GeoBone bone, RenderType renderType,
            MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
            float partialTick, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha, CallbackInfo callbackInfo) {
        if (isReRender) return;
        if (!((Object) this instanceof RenderCustomModel)) return;
        if (!(animatable instanceof EntityCustomModel customModel)) return;
        Matrix4f matrix = new Matrix4f(poseStack.last().pose());
        OBBRenderCapture.captureBone(customModel, bone, matrix);
    }

    @Inject(
        method = "renderRecursively",
        at = @At(
            value = "INVOKE",
            target = "Lsoftware/bernie/geckolib/renderer/GeoEntityRenderer;renderChildBones(Lcom/mojang/blaze3d/vertex/PoseStack;Lsoftware/bernie/geckolib/core/animatable/GeoAnimatable;Lsoftware/bernie/geckolib/cache/object/GeoBone;Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/MultiBufferSource;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZFIIFFFF)V",
            shift = At.Shift.BEFORE
        ),
        remap = false,
        require = 0
    )
    private void cnpc_immersiveboss$captureCameraBone(
            PoseStack poseStack, Entity animatable, GeoBone bone, RenderType renderType,
            MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
            float partialTick, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha, CallbackInfo callbackInfo) {
        if (isReRender) return;
        if (animatable instanceof EntityCustomModel customModel) {
            ThrowClientState.captureCamera(customModel, bone,
                new Matrix4f(poseStack.last().pose()), partialTick);
            ThrowClientState.renderEquipment(customModel, bone, poseStack, bufferSource,
                packedLight, partialTick);
            ThrowClientState.restoreOriginalRender();
        }
    }

    @org.spongepowered.asm.mixin.injection.Redirect(
        method = "renderRecursively",
        at = @At(
            value = "INVOKE",
            target = "Lsoftware/bernie/geckolib/renderer/GeoEntityRenderer;renderCubesOfBone(Lcom/mojang/blaze3d/vertex/PoseStack;Lsoftware/bernie/geckolib/cache/object/GeoBone;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"
        ),
        remap = false,
        require = 0
    )
    private void cnpc_immersiveboss$renderVictimSkin(
            GeoEntityRenderer<?> renderer, PoseStack poseStack, GeoBone bone,
            VertexConsumer buffer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        VertexConsumer selected = buffer;
        if (renderer.getAnimatable() instanceof EntityCustomModel customModel) {
            selected = ThrowClientState.victimConsumer(customModel, bone, buffer);
        }
        renderer.renderCubesOfBone(poseStack, bone, selected, packedLight, packedOverlay,
            red, green, blue, alpha);
        // A skin RenderType can share BufferSource's active builder with the
        // NPC RenderType. Restore the NPC batch before GeckoLib renders this
        // bone's layers and recursively visits its children.
        if (selected != buffer) {
            ThrowClientState.restoreOriginalRender();
        }
    }

}
