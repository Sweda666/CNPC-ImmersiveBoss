package sweda.cnpc_immersiveboss.mixin.compat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sweda.cnpc_immersiveboss.compat.bettercombat.BetterCombatClientCompat;

@Pseudo
@Mixin(targets = "net.bettercombat.client.collision.TargetFinder", remap = false)
public abstract class MixinBetterCombatTargetFinder {
    @Inject(method = "findAttackTargetResult", at = @At("RETURN"),
        remap = false, require = 0)
    private static void cnpc_immersiveboss$addObbTargets(
            Player player, Entity targetUnderCursor, @Coerce Object attack, double range,
            CallbackInfoReturnable<Object> cir) {
        BetterCombatClientCompat.augmentTargets(player, targetUnderCursor,
            attack, cir.getReturnValue());
    }
}
