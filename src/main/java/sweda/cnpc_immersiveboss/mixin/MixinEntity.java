package sweda.cnpc_immersiveboss.mixin;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sweda.cnpc_immersiveboss.api.IPushResistance;

/**
 * Entity-level mixin — NO AABB manipulation.
 * Only exists as a placeholder for future OBB-based entity picking (raycasting).
 * DO NOT call setBoundingBox() here — the AABB is managed by vanilla/CustomNPCs.
 */
@Mixin(Entity.class)
public abstract class MixinEntity {

    @Inject(method = "refreshDimensions", at = @At("TAIL"))
    private void cnpc_multihitbox$onRefreshDimensions(CallbackInfo ci) {
        // Intentionally empty — AABB must stay at vanilla default.
        // OBB data is accessed per-entity via IOBBHolder.
    }
    @Redirect(method = "push(Lnet/minecraft/world/entity/Entity;)V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;push(DDD)V"))
    private void cnpc_immersiveboss$applyPushResistance(Entity pushed,
                                                         double x, double y, double z) {
        float resistance = 0.0F;
        if (pushed instanceof EntityNPCInterface npc
            && npc.stats != null
            && npc.stats.resistances instanceof IPushResistance pushResistance) {
            resistance = Mth.clamp(
                pushResistance.cnpc_immersiveboss$getPushResistance(), 0.0F, 1.0F);
        }
        double scale = 1.0 - resistance;
        pushed.push(x * scale, y * scale, z * scale);
    }
}
