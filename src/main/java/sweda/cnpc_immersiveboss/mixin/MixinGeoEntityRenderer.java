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

@Mixin(value = GeoEntityRenderer.class, remap = false)
public abstract class MixinGeoEntityRenderer {
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
        if (!((Object) this instanceof RenderCustomModel)) return;
        if (!(animatable instanceof EntityCustomModel customModel)) return;
        OBBRenderCapture.captureBone(customModel, bone, new Matrix4f(poseStack.last().pose()));
    }
}
