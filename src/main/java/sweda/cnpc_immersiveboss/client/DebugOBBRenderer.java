package sweda.cnpc_immersiveboss.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import noppes.npcs.entity.EntityNPCInterface;
import sweda.cnpc_immersiveboss.api.IOBBHolder;
import sweda.cnpc_immersiveboss.config.ClientConfig;
import sweda.cnpc_immersiveboss.hitbox.GeoHitboxDef;
import sweda.cnpc_immersiveboss.hitbox.OBB;
import sweda.cnpc_immersiveboss.hitbox.OBBPhysics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Debug renderer — draws property-colored wireframe outlines of active OBBs in the world.
 * Reads OBB data per-entity via IOBBHolder (NOT the old static BoneWorldData).
 * Hooked into RenderLevelStageEvent.AFTER_TRANSLUCENT_BLOCKS.
 */
public class DebugOBBRenderer {

    private static final int COLOR_PHYSICAL_DETECTABLE = 0xFFFFFF;
    private static final int COLOR_PHYSICAL = 0x4488FF;
    private static final int COLOR_DETECTABLE = 0xFFFF00;
    private static final int COLOR_SENSOR = 0x00FF00;
    private static final int COLOR_COLLIDING = 0xFF0000;
    private static final float LABEL_SCALE = 0.025F;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        // Only render when F3+B debug hitboxes are enabled
        if (!mc.getEntityRenderDispatcher().shouldRenderHitBoxes()) return;

        PoseStack poseStack = event.getPoseStack();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        List<DebugBox> debugBoxes = new ArrayList<>();
        Map<Entity, List<DebugBox>> boxesByEntity = new IdentityHashMap<>();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof EntityNPCInterface)) continue;
            if (!(entity instanceof IOBBHolder holder)) continue;

            Map<String, OBB> obbs = holder.cnpc_immersiveboss$getBoneOBBs();
            if (obbs.isEmpty()) continue;

            Vec3 pos = entity.position();
            List<DebugBox> entityBoxes = new ArrayList<>(obbs.size());
            for (Map.Entry<String, OBB> entry : obbs.entrySet()) {
                OBB rel = entry.getValue();
                OBB worldObb = new OBB(
                    rel.center.add(pos), rel.halfExtents,
                    rel.axisX, rel.axisY, rel.axisZ
                );
                DebugBox debugBox = new DebugBox(
                    entity, entry.getKey(), worldObb,
                    GeoHitboxDef.isPhysicalBone(entry.getKey()),
                    GeoHitboxDef.isDetectableBone(entry.getKey())
                );
                entityBoxes.add(debugBox);
                debugBoxes.add(debugBox);
            }
            boxesByEntity.put(entity, entityBoxes);
        }

        Set<DebugBox> collidingBoxes = findCollidingBoxes(mc, debugBoxes, boxesByEntity);

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        for (DebugBox debugBox : debugBoxes) {
            drawOBB(poseStack, bufferSource, debugBox.obb,
                debugColor(debugBox, collidingBoxes));
        }

        if (ClientConfig.SHOW_OBB_NAMES.get()) {
            Font font = mc.font;
            for (DebugBox debugBox : debugBoxes) {
                drawName(poseStack, bufferSource, mc, font, debugBox,
                    debugColor(debugBox, collidingBoxes));
            }
        }

        poseStack.popPose();
        bufferSource.endBatch();
    }

    private static Set<DebugBox> findCollidingBoxes(
            Minecraft mc, List<DebugBox> debugBoxes,
            Map<Entity, List<DebugBox>> boxesByEntity) {
        Set<DebugBox> result = Collections.newSetFromMap(new IdentityHashMap<>());
        if (mc.level == null) return result;

        for (DebugBox debugBox : debugBoxes) {
            AABB broad = OBBPhysics.enclosingAABB(debugBox.obb).inflate(0.5);
            List<Entity> nearby = mc.level.getEntities(debugBox.owner, broad,
                other -> !other.noPhysics
                    && !other.isPassengerOfSameVehicle(debugBox.owner));

            for (Entity other : nearby) {
                if (other instanceof IOBBHolder) {
                    List<DebugBox> otherBoxes = boxesByEntity.get(other);
                    if (otherBoxes == null || otherBoxes.isEmpty()) continue;

                    for (DebugBox otherBox : otherBoxes) {
                        if (OBBPhysics.intersects(debugBox.obb, otherBox.obb)) {
                            result.add(debugBox);
                            result.add(otherBox);
                        }
                    }
                } else if (OBBPhysics.intersects(debugBox.obb, other.getBoundingBox())) {
                    result.add(debugBox);
                }
            }
        }
        return result;
    }

    private static int colorFor(boolean physical, boolean detectable) {
        if (physical && detectable) return COLOR_PHYSICAL_DETECTABLE;
        if (physical) return COLOR_PHYSICAL;
        if (detectable) return COLOR_DETECTABLE;
        return COLOR_SENSOR;
    }

    private static int debugColor(DebugBox debugBox, Set<DebugBox> collidingBoxes) {
        return collidingBoxes.contains(debugBox)
            ? COLOR_COLLIDING
            : colorFor(debugBox.physical, debugBox.detectable);
    }

    private static void drawName(PoseStack poseStack, MultiBufferSource.BufferSource buffer,
                                 Minecraft mc, Font font, DebugBox debugBox, int color) {
        poseStack.pushPose();
        poseStack.translate(debugBox.obb.center.x, debugBox.obb.center.y, debugBox.obb.center.z);
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-LABEL_SCALE, -LABEL_SCALE, LABEL_SCALE);

        float x = -font.width(debugBox.name) / 2.0F;
        float y = -font.lineHeight / 2.0F;
        font.drawInBatch(
            debugBox.name, x, y, 0xFF000000 | color, false,
            poseStack.last().pose(), buffer, Font.DisplayMode.SEE_THROUGH,
            0, LightTexture.FULL_BRIGHT
        );
        poseStack.popPose();
    }

    private static void drawOBB(PoseStack poseStack, MultiBufferSource.BufferSource buffer,
                                OBB obb, int color) {
        int r = color >> 16 & 0xFF;
        int g = color >> 8 & 0xFF;
        int b = color & 0xFF;
        Vec3[] corners = getCorners(obb);
        VertexConsumer vc = buffer.getBuffer(RenderType.LINES);

        drawLine(poseStack, vc, corners[0], corners[1], r, g, b, 255);
        drawLine(poseStack, vc, corners[1], corners[2], r, g, b, 255);
        drawLine(poseStack, vc, corners[2], corners[3], r, g, b, 255);
        drawLine(poseStack, vc, corners[3], corners[0], r, g, b, 255);
        drawLine(poseStack, vc, corners[4], corners[5], r, g, b, 255);
        drawLine(poseStack, vc, corners[5], corners[6], r, g, b, 255);
        drawLine(poseStack, vc, corners[6], corners[7], r, g, b, 255);
        drawLine(poseStack, vc, corners[7], corners[4], r, g, b, 255);
        drawLine(poseStack, vc, corners[0], corners[4], r, g, b, 255);
        drawLine(poseStack, vc, corners[1], corners[5], r, g, b, 255);
        drawLine(poseStack, vc, corners[2], corners[6], r, g, b, 255);
        drawLine(poseStack, vc, corners[3], corners[7], r, g, b, 255);

        Vec3 c = obb.center;
        Vec3 ax = c.add(obb.axisX.scale(obb.halfExtents.x));
        Vec3 ay = c.add(obb.axisY.scale(obb.halfExtents.y));
        Vec3 az = c.add(obb.axisZ.scale(obb.halfExtents.z));
        drawLine(poseStack, vc, c, ax, 255, 100, 100, 128);
        drawLine(poseStack, vc, c, ay, 100, 255, 100, 128);
        drawLine(poseStack, vc, c, az, 100, 100, 255, 128);
    }

    private static Vec3[] getCorners(OBB obb) {
        Vec3 cx = obb.axisX.scale(obb.halfExtents.x);
        Vec3 cy = obb.axisY.scale(obb.halfExtents.y);
        Vec3 cz = obb.axisZ.scale(obb.halfExtents.z);
        return new Vec3[]{
            obb.center.subtract(cx).subtract(cy).subtract(cz),
            obb.center.add(cx).subtract(cy).subtract(cz),
            obb.center.add(cx).add(cy).subtract(cz),
            obb.center.subtract(cx).add(cy).subtract(cz),
            obb.center.subtract(cx).subtract(cy).add(cz),
            obb.center.add(cx).subtract(cy).add(cz),
            obb.center.add(cx).add(cy).add(cz),
            obb.center.subtract(cx).add(cy).add(cz),
        };
    }

    private static void drawLine(PoseStack poseStack, VertexConsumer vc,
                                  Vec3 from, Vec3 to, int r, int g, int b, int a) {
        vc.vertex(poseStack.last().pose(), (float) from.x, (float) from.y, (float) from.z)
            .color(r, g, b, a).normal(0, 1, 0).endVertex();
        vc.vertex(poseStack.last().pose(), (float) to.x, (float) to.y, (float) to.z)
            .color(r, g, b, a).normal(0, 1, 0).endVertex();
    }

    private static final class DebugBox {
        private final Entity owner;
        private final String name;
        private final OBB obb;
        private final boolean physical;
        private final boolean detectable;

        private DebugBox(Entity owner, String name, OBB obb,
                         boolean physical, boolean detectable) {
            this.owner = owner;
            this.name = name;
            this.obb = obb;
            this.physical = physical;
            this.detectable = detectable;
        }
    }
}
