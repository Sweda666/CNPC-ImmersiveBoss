package sweda.cnpc_immersiveboss.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sweda.cnpc_immersiveboss.api.IOBBHolder;
import sweda.cnpc_immersiveboss.hitbox.GeoHitboxDef;
import sweda.cnpc_immersiveboss.hitbox.OBB;
import sweda.cnpc_immersiveboss.hitbox.OBBPhysics;
import sweda.cnpc_immersiveboss.throwing.ThrowManager;

import java.util.Map;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class MixinServerGamePacketListenerImpl {
    /**
     * ThrowManager teleports the victim every server tick.  Client movement
     * packets sent during that window contain the pre-throw position and are
     * rejected by vanilla's moved-too-quickly check before the next tick can
     * correct them.  Ignore those packets while the throw is authoritative;
     * normal movement validation resumes as soon as the throw ends.
     */
    @Inject(method = "handleMovePlayer", at = @At("HEAD"), cancellable = true,
        remap = true, require = 0)
    private void cnpc_immersiveboss$ignoreThrowMovement(ServerboundMovePlayerPacket packet,
                                                         CallbackInfo ci) {
        ServerGamePacketListenerImpl listener = (ServerGamePacketListenerImpl) (Object) this;
        if (ThrowManager.isActive(listener.player)) ci.cancel();
    }

    @Redirect(
        method = "handleInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;canReachRaw(Lnet/minecraft/world/entity/Entity;D)Z",
            remap = false
        )
    )
    private boolean cnpc_multihitbox$canReachObb(ServerPlayer player, Entity target,
                                                 double padding) {
        if (player.canReachRaw(target, padding)) return true;
        if (!(target instanceof IOBBHolder holder)) return false;

        Map<String, OBB> obbs = holder.cnpc_immersiveboss$getBoneOBBs();
        if (obbs.isEmpty()) return false;

        double reach = player.getAttributeValue(ForgeMod.ENTITY_REACH.get()) + padding;
        if (reach <= 0) return false;
        double reachSqr = reach * reach;
        Vec3 eyePosition = player.getEyePosition();
        Vec3 entityPosition = target.position();

        for (Map.Entry<String, OBB> entry : obbs.entrySet()) {
            if (!GeoHitboxDef.isDetectableBone(entry.getKey())) continue;
            OBB relative = entry.getValue();
            OBB world = new OBB(
                relative.center.add(entityPosition), relative.halfExtents,
                relative.axisX, relative.axisY, relative.axisZ
            );
            if (OBBPhysics.distanceToSqr(world, eyePosition) < reachSqr) return true;
        }
        return false;
    }
}
