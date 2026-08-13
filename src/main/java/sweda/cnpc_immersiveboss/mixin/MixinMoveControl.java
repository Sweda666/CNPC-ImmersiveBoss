package sweda.cnpc_immersiveboss.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import sweda.cnpc_immersiveboss.entity.NpcTurnSpeedManager;

/** Couples limited NPC yaw to vanilla navigation before its forward travel step. */
@Mixin(MoveControl.class)
public abstract class MixinMoveControl {
    @Redirect(method = "tick",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Mob;setYRot(F)V"))
    private void cnpc_immersiveboss$limitNavigationYaw(Mob movingMob, float targetYaw) {
        if (movingMob instanceof EntityNPCInterface npc) {
            movingMob.setYRot(NpcTurnSpeedManager.limitNavigationYaw(npc, targetYaw));
        } else {
            movingMob.setYRot(targetYaw);
        }
    }

    @Redirect(method = "tick",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Mob;setSpeed(F)V"))
    private void cnpc_immersiveboss$limitNavigationSpeed(Mob movingMob, float speed) {
        if (movingMob instanceof EntityNPCInterface npc) {
            movingMob.setSpeed(NpcTurnSpeedManager.limitNavigationSpeed(npc, speed));
        } else {
            movingMob.setSpeed(speed);
        }
    }
}
