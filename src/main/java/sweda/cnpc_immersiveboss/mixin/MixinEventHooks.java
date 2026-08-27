package sweda.cnpc_immersiveboss.mixin;

import noppes.npcs.EventHooks;
import noppes.npcs.api.event.NpcEvent;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sweda.cnpc_immersiveboss.api.IMixinNpcDamagedEvent;
import sweda.cnpc_immersiveboss.api.IOBBHolder;

/**
 * Applies the recorded OBB name at the CNPC event boundary. This is a
 * fallback for CustomNPCs builds whose {@code EntityNPCInterface.hurt()}
 * bytecode does not expose a stable redirect site.
 */
@Mixin(value = EventHooks.class, remap = false)
public abstract class MixinEventHooks {
    @Inject(method = "onNPCDamaged", at = @At("HEAD"), remap = false, require = 0)
    private static void cnpc_immersiveboss$setDamagedHitbox(
            EntityNPCInterface npc, NpcEvent.DamagedEvent event,
            CallbackInfoReturnable<Boolean> cir) {
        if (!(npc instanceof IOBBHolder holder)
            || !(event instanceof IMixinNpcDamagedEvent hitboxEvent)) return;
        String hitbox = holder.cnpc_immersiveboss$getLastHitboxName();
        if (hitbox != null) hitboxEvent.setHitboxName(hitbox);
    }
}
