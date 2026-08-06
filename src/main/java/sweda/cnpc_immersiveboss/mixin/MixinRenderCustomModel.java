package sweda.cnpc_immersiveboss.mixin;

import com.goodbird.cnpcgeckoaddon.client.renderer.RenderCustomModel;
import com.goodbird.cnpcgeckoaddon.entity.EntityCustomModel;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import sweda.cnpc_immersiveboss.api.IOBBHolder;
import sweda.cnpc_immersiveboss.hitbox.GeoHitboxDef;
import sweda.cnpc_immersiveboss.hitbox.GeoHitboxParser;
import sweda.cnpc_immersiveboss.hitbox.OBB;
import sweda.cnpc_immersiveboss.hitbox.ClientHitboxData;
import sweda.cnpc_immersiveboss.network.NetworkHandler;
import sweda.cnpc_immersiveboss.network.packet.SyncOBBPacket;
import sweda.cnpc_immersiveboss.network.packet.SyncHitboxPacket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Mixin(RenderCustomModel.class)
public abstract class MixinRenderCustomModel {

    @Unique
    private static final Logger LOGGER = LogUtils.getLogger();

    @Unique
    private static final Map<Integer, ResourceLocation> syncedModels = new HashMap<>();

    /** Removes sync state when the NPC is removed — prevents stale model tracking on entity-ID reuse. */
    public static void onEntityRemoved(int npcId) {
        syncedModels.remove(npcId);
    }

    @Unique
    private static int diagFrameCount = 0;
    @Unique
    private static final int DIAG_INTERVAL = 30;

    @Inject(method = "defaultRender", at = @At("HEAD"), remap = false)
    private void cnpc_multihitbox$hideHitboxBones(
            com.mojang.blaze3d.vertex.PoseStack poseStack, EntityCustomModel animatable,
            net.minecraft.client.renderer.MultiBufferSource bufferSource,
            net.minecraft.client.renderer.RenderType renderType,
            com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer,
            float yaw, float partialTick, int packedLight, CallbackInfo ci) {

        if (animatable.modelResLoc == null) return;

        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        List<GeoHitboxDef> hitboxDefs = GeoHitboxParser.parse(animatable.modelResLoc, rm);
        if (hitboxDefs.isEmpty()) {
            GeoHitboxParser.invalidate(animatable.modelResLoc);
            hitboxDefs = GeoHitboxParser.parse(animatable.modelResLoc, rm);
        }
        if (hitboxDefs.isEmpty()) return;

        if (animatable.owner != null) {
            int npcId = animatable.owner.getId();
            ClientHitboxData.put(npcId, animatable.modelResLoc, hitboxDefs);
            // Re-sync whenever the model changes (not just once per entity) — a model
            // switch must also invalidate the server-side fallback defs.
            ResourceLocation prevModel = syncedModels.get(npcId);
            if (prevModel == null || !prevModel.equals(animatable.modelResLoc)) {
                syncedModels.put(npcId, animatable.modelResLoc);
                NetworkHandler.sendToServer(new SyncHitboxPacket(npcId, animatable.modelResLoc.toString(), hitboxDefs));
            }
        }

        GeoModel<EntityCustomModel> model = ((RenderCustomModel) (Object) this).getGeoModel();
        if (model == null) return;
        BakedGeoModel bakedModel = model.getBakedModel(animatable.modelResLoc);
        if (bakedModel == null) return;

        for (GeoHitboxDef def : hitboxDefs) {
            Optional<GeoBone> boneOpt = bakedModel.getBone(def.geoBoneName);
            boneOpt.ifPresent(b -> {
                b.setHidden(!def.render);
                b.setTrackingMatrices(true);
            });
        }
    }

    @Inject(method = "defaultRender", at = @At("TAIL"), remap = false)
    private void cnpc_multihitbox$sendOBB(
            com.mojang.blaze3d.vertex.PoseStack poseStack, EntityCustomModel animatable,
            net.minecraft.client.renderer.MultiBufferSource bufferSource,
            net.minecraft.client.renderer.RenderType renderType,
            com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer,
            float yaw, float partialTick, int packedLight, CallbackInfo ci) {

        if (animatable.owner == null || animatable.modelResLoc == null) return;

        int npcId = animatable.owner.getId();
        List<GeoHitboxDef> hitboxDefs = ClientHitboxData.get(npcId);
        if (hitboxDefs == null || hitboxDefs.isEmpty()) return;

        GeoModel<EntityCustomModel> model = ((RenderCustomModel) (Object) this).getGeoModel();
        if (model == null) return;
        BakedGeoModel bakedModel = model.getBakedModel(animatable.modelResLoc);
        if (bakedModel == null) return;

        Map<String, OBB> boneObbs = new HashMap<>(hitboxDefs.size());
        Vec3 entityPos = animatable.owner.position();
        double bbToWorld = animatable.owner.display.getSize() / 80.0;
        float yawRad = (float)Math.toRadians(animatable.owner.yBodyRot);
        double cosYaw = Math.cos(yawRad), sinYaw = Math.sin(yawRad);

        diagFrameCount++;
        boolean doLog = (diagFrameCount % DIAG_INTERVAL == 0 && npcId < 50);

        for (GeoHitboxDef def : hitboxDefs) {
            Optional<GeoBone> boneOpt = bakedModel.getBone(def.geoBoneName);
            if (boneOpt.isEmpty()) continue;
            GeoBone bone = boneOpt.get();

            // --- Animated pivot: getWorldPosition() is the only source with correct hierarchy position ---
            org.joml.Vector3d wp = bone.getWorldPosition();
            if (wp == null) continue;
            double pivX = wp.x - entityPos.x;
            double pivY = wp.y - entityPos.y;
            double pivZ = wp.z - entityPos.z;

            // --- Bone hierarchy rotation (entity-local: no world yaw). Translation always 0. ---
            org.joml.Matrix4f mm = bone.getModelSpaceMatrix();
            if (mm == null) continue;
            float m00 = mm.m00(), m10 = mm.m10(), m20 = mm.m20();
            float m01 = mm.m01(), m11 = mm.m11(), m21 = mm.m21();
            float m02 = mm.m02(), m12 = mm.m12(), m22 = mm.m22();
            double lenX = Math.sqrt(m00*m00 + m10*m10 + m20*m20);
            double lenY = Math.sqrt(m01*m01 + m11*m11 + m21*m21);
            double lenZ = Math.sqrt(m02*m02 + m12*m12 + m22*m22);
            if (lenX < 1e-10 || lenY < 1e-10 || lenZ < 1e-10) continue;

            // --- Cube-to-static-pivot offset (static, in MC world units via bbToWorld) ---
            // Blockbench convention: front = -Z (north).  Entity-local space (yaw=0): front = +Z (south).
            // The model-space Z offset is negated to map Blockbench coords to entity-local coords.
            Vec3 dc = def.center();
            double lx = (dc.x - def.staticPivot.x) * bbToWorld;
            double ly = (dc.y - def.staticPivot.y) * bbToWorld;
            double lz = (def.staticPivot.z - dc.z) * bbToWorld;

            // --- Rotate offset by MODEL-space rotation (no entity yaw) ---
            double rlx = (m00/lenX)*lx + (m01/lenY)*ly + (m02/lenZ)*lz;
            double rly = (m10/lenX)*lx + (m11/lenY)*ly + (m12/lenZ)*lz;
            double rlz = (m20/lenX)*lx + (m21/lenY)*ly + (m22/lenZ)*lz;

            // --- Apply entity yaw to model-space rotated offset → world space ---
            // pivot is already entity-relative world space, so add world-space rotated offset
            double rx = pivX + (rlx * cosYaw - rlz * sinYaw);
            double ry = pivY + rly;
            double rz = pivZ + (rlx * sinYaw + rlz * cosYaw);

            // --- World-space OBB axes = model-space rotation × entity yaw ---
            double nx = m00/lenX, ny = m10/lenX, nz = m20/lenX;
            double ny_x = m01/lenY, ny_y = m11/lenY, ny_z = m21/lenY;
            double nz_x = m02/lenZ, nz_y = m12/lenZ, nz_z = m22/lenZ;
            Vec3 ax = new Vec3(nx*cosYaw - nz*sinYaw, ny, nx*sinYaw + nz*cosYaw);
            Vec3 ay = new Vec3(ny_x*cosYaw - ny_z*sinYaw, ny_y, ny_x*sinYaw + ny_z*cosYaw);
            Vec3 az = new Vec3(nz_x*cosYaw - nz_z*sinYaw, nz_y, nz_x*sinYaw + nz_z*cosYaw);

            // --- Apply cube-local rotation (Blockbench order: Z → Y → X) ---
            if (def.cubeRotation != null) {
                double crx = Math.toRadians(def.cubeRotation.x);
                double cry = Math.toRadians(def.cubeRotation.y);
                double crz = Math.toRadians(def.cubeRotation.z);
                ax = rotateAround(ax, az, crz); // Z first
                ay = rotateAround(ay, az, crz);
                ax = rotateAround(ax, ay, cry); // Y second
                az = rotateAround(az, ay, cry);
                ay = rotateAround(ay, ax, crx); // X last
                az = rotateAround(az, ax, crx);
            }

            double hx = def.size.x * 0.5 * bbToWorld;
            double hy = def.size.y * 0.5 * bbToWorld;
            double hz = def.size.z * 0.5 * bbToWorld;

            if (doLog) {
                LOGGER.info(String.format("[OBB-DIAG] %s ePvt(%.2f,%.2f,%.2f) off(%.2f,%.2f,%.2f)",
                    GeoHitboxDef.baseBoneName(def.boneName), pivX, pivY, pivZ, lx, ly, lz));
                LOGGER.info(String.format("[OBB-DIAG]   e-rel(%.3f,%.3f,%.3f) hExt(%.3f,%.3f,%.3f) r(%.1f,%.1f,%.1f) p(%.1f,%.1f,%.1f)",
                    rx, ry, rz, hx, hy, hz, bone.getRotX(), bone.getRotY(), bone.getRotZ(),
                    bone.getPosX(), bone.getPosY(), bone.getPosZ()));
            }

            boneObbs.put(def.boneName, new OBB(
                new Vec3(rx, ry, rz), new Vec3(hx, hy, hz), ax, ay, az));
        }

        if (!boneObbs.isEmpty()) {
            if (animatable.owner instanceof IOBBHolder holder) {
                holder.cnpc_immersiveboss$setBoneOBBs(boneObbs);
            }
            NetworkHandler.sendToServer(new SyncOBBPacket(npcId, boneObbs));
        }
    }

    @Unique
    private static Vec3 rotateAround(Vec3 v, Vec3 axis, double angleRad) {
        if (Math.abs(angleRad) < 1e-10) return v;
        double c = Math.cos(angleRad), s = Math.sin(angleRad);
        double dot = v.x * axis.x + v.y * axis.y + v.z * axis.z;
        // Rodrigues' rotation: v' = v*cosθ + (k×v)*sinθ + k*(k·v)*(1-cosθ)
        return new Vec3(
            v.x * c + (axis.y * v.z - axis.z * v.y) * s + axis.x * dot * (1 - c),
            v.y * c + (axis.z * v.x - axis.x * v.z) * s + axis.y * dot * (1 - c),
            v.z * c + (axis.x * v.y - axis.y * v.x) * s + axis.z * dot * (1 - c)
        );
    }
}
