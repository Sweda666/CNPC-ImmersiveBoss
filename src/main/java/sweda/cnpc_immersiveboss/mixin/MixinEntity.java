package sweda.cnpc_immersiveboss.mixin;

import net.minecraft.world.entity.Entity;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
}
