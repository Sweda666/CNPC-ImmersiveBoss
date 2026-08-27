package sweda.cnpc_immersiveboss.mixin.compat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sweda.cnpc_immersiveboss.compat.bettercombat.BetterCombatCompat;

@Pseudo
@Mixin(targets = "net.bettercombat.network.ServerNetwork", remap = false)
public abstract class MixinBetterCombatServerNetwork {
    @Inject(method = "lambda$initializeHandlers$5", at = @At("HEAD"),
        remap = false, require = 0)
    private static void cnpc_immersiveboss$beginAttackContext(
            ServerPlayer player, @Coerce Object request, @Coerce Object attributes,
            @Coerce Object attack, @Coerce Object hand, ServerLevel level,
            boolean vanillaPacket, ServerGamePacketListenerImpl listener, CallbackInfo ci) {
        BetterCombatCompat.beginAttack(attributes, attack);
    }

    @Inject(method = "lambda$initializeHandlers$5", at = @At("RETURN"),
        remap = false, require = 0)
    private static void cnpc_immersiveboss$endAttackContext(
            ServerPlayer player, @Coerce Object request, @Coerce Object attributes,
            @Coerce Object attack, @Coerce Object hand, ServerLevel level,
            boolean vanillaPacket, ServerGamePacketListenerImpl listener, CallbackInfo ci) {
        BetterCombatCompat.endAttack();
    }

    @Redirect(method = "lambda$initializeHandlers$5",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;attack(Lnet/minecraft/world/entity/Entity;)V"),
        remap = false, require = 0)
    private static void cnpc_immersiveboss$validateMappedAttack(
            ServerPlayer attacker, Entity target) {
        BetterCombatCompat.attack(attacker, target);
    }

    @Redirect(method = "lambda$initializeHandlers$5",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;m_5706_(Lnet/minecraft/world/entity/Entity;)V"),
        remap = false, require = 0)
    private static void cnpc_immersiveboss$validateSrgAttack(
            ServerPlayer attacker, Entity target) {
        BetterCombatCompat.attack(attacker, target);
    }

    @Redirect(method = "lambda$initializeHandlers$5",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;handleInteract(Lnet/minecraft/network/protocol/game/ServerboundInteractPacket;)V"),
        remap = false, require = 0)
    private static void cnpc_immersiveboss$validateMappedPacketAttack(
            ServerGamePacketListenerImpl listener, ServerboundInteractPacket packet) {
        BetterCombatCompat.handleInteract(listener, packet);
    }

    @Redirect(method = "lambda$initializeHandlers$5",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;m_6946_(Lnet/minecraft/network/protocol/game/ServerboundInteractPacket;)V"),
        remap = false, require = 0)
    private static void cnpc_immersiveboss$validateSrgPacketAttack(
            ServerGamePacketListenerImpl listener, ServerboundInteractPacket packet) {
        BetterCombatCompat.handleInteract(listener, packet);
    }
}
