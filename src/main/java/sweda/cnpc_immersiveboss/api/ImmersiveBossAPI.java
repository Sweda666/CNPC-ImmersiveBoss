package sweda.cnpc_immersiveboss.api;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.entity.EntityNPCInterface;
import sweda.cnpc_immersiveboss.hitbox.HitboxDamageManager;

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
 *
 * <h3>Activating collision damage</h3>
 * Optional arguments can be omitted from the end. The complete form is:
 * <pre>
 *   ImmersiveBossAPI.activateHitboxDamage(
 *       npc, "hs_sword", 20, 4.0, 3, 10, 1
 *   );
 * </pre>
 * This activates {@code hs_sword} for 20 ticks, deals 4 damage, can damage each
 * target up to 3 times at intervals of 10 ticks, and admits only the first target.
 * Non-positive repeat and target limits mean unlimited.
 */
public final class ImmersiveBossAPI {

    public static final int DEFAULT_HITBOX_DAMAGE_DURATION_TICKS = 20;
    public static final float DEFAULT_HITBOX_COLLISION_DAMAGE = 1.0F;
    public static final int DEFAULT_HITBOX_DAMAGE_REPEAT_COUNT = 1;
    public static final int DEFAULT_HITBOX_DAMAGE_REPEAT_INTERVAL_TICKS = 10;
    public static final int DEFAULT_HITBOX_DAMAGE_MAX_TARGETS = 0;

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

    /**
     * Activates collision damage for one hitbox using all default options.
     * <pre>
     *   ImmersiveBossAPI.activateHitboxDamage(npc, "hs_sword");
     * </pre>
     */
    public static boolean activateHitboxDamage(ICustomNpc wrapper, String hitboxName) {
        return activateHitboxDamage(wrapper, hitboxName,
            DEFAULT_HITBOX_DAMAGE_DURATION_TICKS);
    }

    /** Activates collision damage with a custom duration in ticks. */
    public static boolean activateHitboxDamage(ICustomNpc wrapper, String hitboxName,
                                               int durationTicks) {
        return activateHitboxDamage(wrapper, hitboxName, durationTicks,
            DEFAULT_HITBOX_COLLISION_DAMAGE);
    }

    /** Activates collision damage with a custom duration and damage amount. */
    public static boolean activateHitboxDamage(ICustomNpc wrapper, String hitboxName,
                                               int durationTicks, float damage) {
        return activateHitboxDamage(wrapper, hitboxName, durationTicks, damage,
            DEFAULT_HITBOX_DAMAGE_REPEAT_COUNT);
    }

    /** Activates collision damage with a per-target repeat limit. Non-positive means unlimited. */
    public static boolean activateHitboxDamage(ICustomNpc wrapper, String hitboxName,
                                               int durationTicks, float damage,
                                               int repeatCount) {
        return activateHitboxDamage(wrapper, hitboxName, durationTicks, damage, repeatCount,
            DEFAULT_HITBOX_DAMAGE_REPEAT_INTERVAL_TICKS);
    }

    /** Activates collision damage with a repeat interval in ticks. */
    public static boolean activateHitboxDamage(ICustomNpc wrapper, String hitboxName,
                                               int durationTicks, float damage,
                                               int repeatCount, int repeatIntervalTicks) {
        return activateHitboxDamage(wrapper, hitboxName, durationTicks, damage, repeatCount,
            repeatIntervalTicks, DEFAULT_HITBOX_DAMAGE_MAX_TARGETS);
    }

    /**
     * Activates collision damage for one hitbox. A non-positive repeat count or target
     * limit means unlimited. Re-activating the same hitbox starts a fresh damage window.
     *
     * @return true when the damage window was created; false for invalid input or client-side use
     */
    public static boolean activateHitboxDamage(ICustomNpc wrapper, String hitboxName,
                                               int durationTicks, float damage,
                                               int repeatCount, int repeatIntervalTicks,
                                               int maxTargets) {
        if (wrapper == null) return false;
        Entity entity = wrapper.getMCEntity();
        if (!(entity instanceof EntityNPCInterface npc)) return false;
        return HitboxDamageManager.activate(npc, hitboxName, durationTicks, damage,
            repeatCount, repeatIntervalTicks, maxTargets);
    }
}
