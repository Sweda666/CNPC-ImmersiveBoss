package sweda.cnpc_immersiveboss.mixin;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import noppes.npcs.entity.EntityNPCInterface;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sweda.cnpc_immersiveboss.hitbox.BoneWorldData;
import sweda.cnpc_immersiveboss.hitbox.OBB;
import sweda.cnpc_immersiveboss.hitbox.OBBPhysics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Replaces vanilla AABB-based block collision with SAT-based OBB collision
 * for CustomNPCs entities that have per-bone hitbox data.
 *
 * Intercepts Entity.move() at HEAD, resolves collision via OBBPhysics.obbMove(),
 * and cancels vanilla processing so no AABB-based collision occurs.
 */
@Mixin(Entity.class)
public abstract class MixinEntityMove {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Shadow
    public abstract void checkInsideBlocks();

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void cnpc_multihitbox$onMove(MoverType type, Vec3 motion, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof EntityNPCInterface npc)) return;

        if (self.noPhysics) return;

        Map<String, OBB> relObbs = BoneWorldData.getOBBs(self.getId());
        if (relObbs.isEmpty()) return;

        // Convert entity-relative OBBs to world-space for obbMove()
        Vec3 pos = self.position();
        List<OBB> worldObbs = new ArrayList<>(relObbs.size());
        for (OBB rel : relObbs.values()) {
            worldObbs.add(new OBB(
                rel.center.add(pos),
                rel.halfExtents, rel.axisX, rel.axisY, rel.axisZ
            ));
        }

        // SAT-based block collision
        Vec3 resolved = OBBPhysics.obbMove(worldObbs, motion, self, npc.level());

        // Log when collision actually blocked movement
        double lost = motion.length() - resolved.length();
        if (lost > 0.01) {
            LOGGER.debug("[OBB-Move] NPC {} blocked: motion={} resolved={}",
                self.getId(), motion, resolved);
        }

        // Apply resolved position
        self.setPos(pos.x + resolved.x, pos.y + resolved.y, pos.z + resolved.z);

        // Set onGround when Y motion was cut short (hit the ground)
        if (resolved.y != motion.y && motion.y < 0.0) {
            self.setOnGround(true);
        }

        // Fluid/portal detection (uses entity AABB, not OBBs — acceptable)
        this.checkInsideBlocks();

        ci.cancel();
    }
}
