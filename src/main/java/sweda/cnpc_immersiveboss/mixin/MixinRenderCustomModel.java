package sweda.cnpc_immersiveboss.mixin;

import com.goodbird.cnpcgeckoaddon.client.renderer.RenderCustomModel;
import com.goodbird.cnpcgeckoaddon.entity.EntityCustomModel;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import sweda.cnpc_immersiveboss.api.IOBBHolder;
import sweda.cnpc_immersiveboss.client.OBBRenderCapture;
import sweda.cnpc_immersiveboss.client.ThrowClientState;
import sweda.cnpc_immersiveboss.hitbox.ClientHitboxData;
import sweda.cnpc_immersiveboss.hitbox.GeoHitboxDef;
import sweda.cnpc_immersiveboss.hitbox.GeoHitboxParser;
import sweda.cnpc_immersiveboss.hitbox.OBB;
import sweda.cnpc_immersiveboss.network.NetworkHandler;
import sweda.cnpc_immersiveboss.network.packet.SyncHitboxPacket;
import sweda.cnpc_immersiveboss.network.packet.SyncOBBPacket;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mixin(value = RenderCustomModel.class, remap = false)
public abstract class MixinRenderCustomModel {
    @Inject(method = "defaultRender", at = @At("HEAD"), remap = false, require = 0)
    private void cnpc_multihitbox$hideHitboxBones(
            com.mojang.blaze3d.vertex.PoseStack poseStack, EntityCustomModel animatable,
            net.minecraft.client.renderer.MultiBufferSource bufferSource,
            net.minecraft.client.renderer.RenderType renderType,
            com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer,
            float yaw, float partialTick, int packedLight, CallbackInfo ci) {
        OBBRenderCapture.discard();
        ThrowClientState.beginModelRender(animatable,
            new org.joml.Matrix4f(poseStack.last().pose()));

        if (animatable.modelResLoc == null) return;

        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        List<GeoHitboxDef> hitboxDefs = GeoHitboxParser.parse(animatable.modelResLoc, resourceManager);

        if (animatable.owner != null) {
            int npcId = animatable.owner.getId();
            ClientHitboxData.put(npcId, animatable.modelResLoc, hitboxDefs);
            if (ClientHitboxData.shouldSyncModel(npcId, animatable.modelResLoc)) {
                NetworkHandler.sendToServer(new SyncHitboxPacket(
                    npcId, animatable.modelResLoc.toString(), hitboxDefs));
            }
        }
        GeoModel<EntityCustomModel> model = ((RenderCustomModel) (Object) this).getGeoModel();
        if (model == null) return;
        BakedGeoModel bakedModel;
        try {
            bakedModel = model.getBakedModel(animatable.modelResLoc);
        } catch (RuntimeException ignored) {
            // A removed or malformed model must not throw once per NPC per frame.
            cnpc_multihitbox$clearObbs(animatable);
            return;
        }
        if (bakedModel == null) return;

        boolean throwActive = animatable.owner != null
            && ThrowClientState.isActiveForNpc(animatable.owner.getId());
        bakedModel.getBone("victim_root").ifPresent(root -> {
            root.setHidden(!throwActive);
            root.setChildrenHidden(!throwActive);
        });

        if (hitboxDefs.isEmpty()) {
            cnpc_multihitbox$clearObbs(animatable);
            return;
        }

        for (GeoHitboxDef def : hitboxDefs) {
            Optional<GeoBone> bone = bakedModel.getBone(def.geoBoneName);
            bone.ifPresent(value -> value.setHidden(!def.render));
        }

        if (animatable.owner != null) {
            OBBRenderCapture.begin(animatable, poseStack.last().pose(), hitboxDefs);
        }
    }

    @Inject(method = "defaultRender", at = @At("TAIL"), remap = false, require = 0)
    private void cnpc_multihitbox$sendOBB(
            com.mojang.blaze3d.vertex.PoseStack poseStack, EntityCustomModel animatable,
            net.minecraft.client.renderer.MultiBufferSource bufferSource,
            net.minecraft.client.renderer.RenderType renderType,
            com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer,
            float yaw, float partialTick, int packedLight, CallbackInfo ci) {

        ThrowClientState.endModelRender(animatable);

        if (animatable.owner == null || animatable.modelResLoc == null) return;

        int npcId = animatable.owner.getId();
        Map<String, OBB> boneObbs = OBBRenderCapture.finish(animatable);

        if (boneObbs.isEmpty()) {
            cnpc_multihitbox$clearObbs(animatable);
            return;
        }
        if (animatable.owner instanceof IOBBHolder holder) {
            holder.cnpc_immersiveboss$setBoneOBBs(boneObbs);
        }
        ClientHitboxData.markObbsActive(npcId);
        NetworkHandler.sendToServer(new SyncOBBPacket(npcId, boneObbs));
    }

    @Unique
    private static void cnpc_multihitbox$clearObbs(EntityCustomModel animatable) {
        if (animatable.owner == null) return;
        int npcId = animatable.owner.getId();
        if (animatable.owner instanceof IOBBHolder holder) {
            holder.cnpc_immersiveboss$setBoneOBBs(Map.of());
        }
        if (ClientHitboxData.shouldSyncEmptyObbs(npcId)) {
            NetworkHandler.sendToServer(new SyncOBBPacket(npcId, Map.of()));
        }
    }
}
