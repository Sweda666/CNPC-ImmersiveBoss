package sweda.cnpc_immersiveboss.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.event.NpcEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sweda.cnpc_immersiveboss.api.IMixinNpcInteractEvent;
import sweda.cnpc_immersiveboss.api.IOBBHolder;
import sweda.cnpc_immersiveboss.hitbox.GeoHitboxDef;
import sweda.cnpc_immersiveboss.hitbox.OBB;
import sweda.cnpc_immersiveboss.hitbox.OBBPhysics;

import java.util.Map;

/**
 * Adds {@code hitboxName} to the CNPC interact event. The hit is resolved on
 * the server from the interacting player's view ray, before the script runs.
 */
@Mixin(value = NpcEvent.InteractEvent.class, remap = false)
public abstract class MixinNpcInteractEvent implements IMixinNpcInteractEvent {

    @Unique
    private String cnpc_immersiveboss$hitboxName;

    @Inject(method = "<init>", at = @At("TAIL"), remap = false, require = 0)
    private void cnpc_immersiveboss$detectHitbox(ICustomNpc npc, Player player, CallbackInfo ci) {
        if (npc == null || player == null) return;

        Entity entity = npc.getMCEntity();
        if (!(entity instanceof IOBBHolder holder)) return;

        Map<String, OBB> obbs = holder.cnpc_immersiveboss$getBoneOBBs();
        if (obbs.isEmpty()) return;

        double reach = player.getEntityReach();
        if (reach <= 0) return;

        Vec3 eyePosition = player.getEyePosition();
        Vec3 rayEnd = eyePosition.add(player.getViewVector(1.0F).scale(reach));
        Vec3 entityPosition = entity.position();
        String closestHitbox = null;
        double closestDistance = Double.MAX_VALUE;

        for (Map.Entry<String, OBB> entry : obbs.entrySet()) {
            String name = entry.getKey();
            if (!GeoHitboxDef.isDetectableBone(name)) continue;

            OBB relative = entry.getValue();
            OBB world = new OBB(
                relative.center.add(entityPosition), relative.halfExtents,
                relative.axisX, relative.axisY, relative.axisZ
            );
            double distance = OBBPhysics.intersectRay(world, eyePosition, rayEnd);
            if (distance >= 0 && distance < closestDistance) {
                closestDistance = distance;
                closestHitbox = name;
            }
        }

        if (closestHitbox != null) {
            cnpc_immersiveboss$hitboxName = GeoHitboxDef.baseBoneName(closestHitbox);
        }
    }

    @Override
    public String getHitboxName() {
        return cnpc_immersiveboss$hitboxName;
    }

    @Override
    public void setHitboxName(String name) {
        cnpc_immersiveboss$hitboxName = name;
    }
}
