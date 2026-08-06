package sweda.cnpc_immersiveboss.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import noppes.npcs.entity.EntityNPCInterface;
import sweda.cnpc_immersiveboss.api.IOBBHolder;
import sweda.cnpc_immersiveboss.hitbox.OBB;

import java.util.*;

/**
 * Debug renderer — draws red wireframe outlines of active OBBs in the world.
 * Reads OBB data per-entity via IOBBHolder (NOT the old static BoneWorldData).
 * Hooked into RenderLevelStageEvent.AFTER_TRANSLUCENT_BLOCKS.
 */
public class DebugOBBRenderer {

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

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof EntityNPCInterface npc)) continue;
            if (!(entity instanceof IOBBHolder holder)) continue;

            Map<String, OBB> obbs = holder.cnpc_immersiveboss$getBoneOBBs();
            if (obbs.isEmpty()) continue;

            // Convert entity-relative OBBs to world-space
            Vec3 pos = entity.position();
            List<OBB> worldObbs = new ArrayList<>(obbs.size());
            for (OBB rel : obbs.values()) {
                worldObbs.add(new OBB(
                    rel.center.add(pos), rel.halfExtents,
                    rel.axisX, rel.axisY, rel.axisZ
                ));
            }
            drawOBBs(poseStack, bufferSource, worldObbs, 0xff, 0x44, 0x44);
        }

        poseStack.popPose();
        bufferSource.endBatch();
    }

    private static void drawOBBs(PoseStack poseStack, MultiBufferSource.BufferSource buffer,
                                 List<OBB> obbs, int r, int g, int b) {
        for (OBB obb : obbs) {
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
}
