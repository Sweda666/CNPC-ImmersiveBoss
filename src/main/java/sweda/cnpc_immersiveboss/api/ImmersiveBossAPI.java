package sweda.cnpc_immersiveboss.api;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.entity.EntityNPCInterface;

/**
 * Static utility API for CNPC scripts to interact with ImmersiveBoss features.
 *
 * <h3>Getting the struck hitbox</h3>
 * The simplest way is {@code e.hitboxName} in the {@code damaged(e)} handler:
 * <pre>
 *   function damaged(e) {
 *       if (e.hitboxName == "hb_head") { npc.say("Headshot!"); }
 *       if (e.hitboxName == "hadb_upbody") { npc.say("Body hit!"); }
 *   }
 * </pre>
 * For advanced use, this API provides {@link #damageHitbox} to apply damage
 * to a specific hitbox and fire {@link HitboxDamagedEvent}.
 * <pre>
 *   var ImmersiveBossAPI = Java.type("sweda.cnpc_immersiveboss.api.ImmersiveBossAPI");
 *   ImmersiveBossAPI.damageHitbox(npc, 10.0, "hb_head");
 * </pre>
 */
public final class ImmersiveBossAPI {

    private ImmersiveBossAPI() {}

    /**
     * Applies damage to an NPC and records which hitbox was struck.
     * Fires {@link HitboxDamagedEvent} to CNPC scripts, allowing per-hitbox
     * damage logic in the damaged() handler.
     *
     * @param wrapper     the NPC wrapper (from CNPC script's 'npc' variable)
     * @param amount      damage amount
     * @param hitboxName  the bone name of the hitbox (e.g. "hb_head", "hadb_upbody")
     * @return true if damage was applied, false if canceled or NPC is dead
     */
    public static boolean damageHitbox(ICustomNpc wrapper, float amount, String hitboxName) {
        return damageHitbox(wrapper, null, amount, hitboxName);
    }

    /**
     * Applies damage with a source entity.
     * @param wrapper     the NPC to damage
     * @param source      the attacking NPC (can be null)
     * @param amount      damage amount
     * @param hitboxName  hitbox bone name
     * @return true if damage was applied
     */
    public static boolean damageHitbox(ICustomNpc wrapper, ICustomNpc source, float amount, String hitboxName) {
        if (wrapper == null || amount <= 0 || wrapper.getHealth() <= 0) return false;

        Entity entity = wrapper.getMCEntity();
        if (!(entity instanceof EntityNPCInterface npc)) return false;

        // Record which hitbox was struck
        if (entity instanceof IOBBHolder holder) {
            holder.cnpc_immersiveboss$setLastHitboxName(hitboxName);
        }

        // Fire the hitbox-aware damage event
        DamageSource ds = entity.level().damageSources().generic();
        Entity sourceEntity = source != null ? source.getMCEntity() : null;
        HitboxDamagedEvent event = new HitboxDamagedEvent(wrapper, sourceEntity, ds, amount, source, hitboxName);
        npc.script.runScript(EnumScriptType.DAMAGED, event);

        if (event.isCanceled()) return false;

        // Apply damage
        float newHealth = Math.max(0, wrapper.getHealth() - event.damage);
        wrapper.setHealth((int) newHealth);
        return true;
    }

    /**
     * Convenience: gets the last hitbox name struck on this NPC.
     * Returns null if no hitbox damage has been recorded.
     */
    public static String getLastHitboxName(ICustomNpc wrapper) {
        if (wrapper == null) return null;
        Entity entity = wrapper.getMCEntity();
        if (entity instanceof IOBBHolder holder) {
            return holder.cnpc_immersiveboss$getLastHitboxName();
        }
        return null;
    }

    /**
     * Clears the recorded hitbox name. Call this in damaged() after reading hitboxName
     * to prevent stale values from persisting to the next damage event.
     */
    public static void clearLastHitboxName(ICustomNpc wrapper) {
        if (wrapper == null) return;
        Entity entity = wrapper.getMCEntity();
        if (entity instanceof IOBBHolder holder) {
            holder.cnpc_immersiveboss$setLastHitboxName(null);
        }
    }
}
