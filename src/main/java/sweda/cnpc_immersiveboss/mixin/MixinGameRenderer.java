package sweda.cnpc_immersiveboss.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sweda.cnpc_immersiveboss.api.IOBBHolder;
import sweda.cnpc_immersiveboss.hitbox.GeoHitboxDef;
import sweda.cnpc_immersiveboss.hitbox.OBB;
import sweda.cnpc_immersiveboss.hitbox.OBBPhysics;

import java.util.Map;

/**
 * Augments entity picking with OBB-precise raycasting.
 *
 * After vanilla determines the crosshair entity via AABB raycasting,
 * this injector additionally tests the player's look ray against
 * per-bone OBBs of nearby NPCs. If an OBB hit is closer than the
 * vanilla entity hit, the crosshair target is overridden.
 *
 * This enables attacking and interacting with NPCs through their
 * GeckoLib-model OBBs (bones prefixed with hdb_/hds_/hadb_/hads_).
 */
@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {

    @Inject(method = "pick", at = @At("TAIL"))
    private void cnpc_multihitbox$obbEntityPick(float partialTick, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Entity camera = mc.getCameraEntity();
        if (camera == null) return;

        // Reconstruct player look ray
        Vec3 eyePos = camera.getEyePosition(partialTick);
        Vec3 viewVec = camera.getViewVector(partialTick);
        double reach = mc.player.getAttributeValue(ForgeMod.ENTITY_REACH.get());
        if (reach <= 0) reach = 4.5; // fallback
        Vec3 rayEnd = eyePos.add(viewVec.x * reach, viewVec.y * reach, viewVec.z * reach);

        // Currently targeted entity (vanilla result) and its distance
        Entity vanillaTarget = mc.crosshairPickEntity;
        double bestDist = Double.MAX_VALUE;
        if (vanillaTarget != null) {
            if (mc.hitResult instanceof EntityHitResult entityHit
                && entityHit.getEntity() == vanillaTarget) {
                bestDist = entityHit.getLocation().distanceTo(eyePos);
            } else {
                bestDist = Math.sqrt(vanillaTarget.distanceToSqr(camera));
            }
        }

        Entity bestEntity = vanillaTarget;
        Vec3 bestHitPos = null;

        // Iterate renderable entities for NPCs with detectable OBBs
        double reachSq = reach * reach * 2.25; // broad-phase: skip entities beyond 1.5× reach
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof IOBBHolder holder)) continue;

            // Broad-phase: skip entities clearly beyond reach
            if (entity.distanceToSqr(camera) > reachSq) continue;

            Map<String, OBB> boneObbs = holder.cnpc_immersiveboss$getBoneOBBs();
            if (boneObbs.isEmpty()) continue;

            Vec3 pos = entity.position();
            double minT = Double.MAX_VALUE;
            Vec3 hitPos = null;

            for (Map.Entry<String, OBB> entry : boneObbs.entrySet()) {
                // Only check detectable bones (hdb_/hds_/hadb_/hads_)
                if (!GeoHitboxDef.isDetectableBone(entry.getKey())) continue;

                OBB rel = entry.getValue();
                OBB worldOBB = new OBB(
                    rel.center.add(pos), rel.halfExtents,
                    rel.axisX, rel.axisY, rel.axisZ
                );

                double t = OBBPhysics.intersectRay(worldOBB, eyePos, rayEnd);
                if (t >= 0 && t < minT) {
                    minT = t;
                    hitPos = eyePos.add(viewVec.scale(t));
                }
            }

            if (minT < bestDist && hitPos != null) {
                bestDist = minT;
                bestEntity = entity;
                bestHitPos = hitPos;
            }
        }

        // Override crosshair target if OBB hit is closer
        if (bestEntity != vanillaTarget && bestEntity != null && bestHitPos != null) {
            mc.crosshairPickEntity = bestEntity;

            // Also update hitResult if the OBB hit is closer than any block hit
            if (mc.hitResult == null || mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
                mc.hitResult = new EntityHitResult(bestEntity, bestHitPos);
            } else {
                double blockDist = mc.hitResult.getLocation().distanceToSqr(eyePos);
                double entityDist = bestHitPos.distanceToSqr(eyePos);
                if (entityDist < blockDist) {
                    mc.hitResult = new EntityHitResult(bestEntity, bestHitPos);
                }
            }
        }
    }
}
