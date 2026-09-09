package sweda.cnpc_immersiveboss.api;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.entity.EntityNPCInterface;
import sweda.cnpc_immersiveboss.entity.NpcTurnSpeedManager;
import sweda.cnpc_immersiveboss.hitbox.GeoHitboxDef;
import sweda.cnpc_immersiveboss.hitbox.HitboxDamageManager;
import sweda.cnpc_immersiveboss.hitbox.ServerHitboxData;
import sweda.cnpc_immersiveboss.throwing.ThrowManager;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
 *       npc, "hs_sword", 0, 20, 4.0, 3, 10, 1,
 *       function(attacker, target) { attacker.say("Hit " + target.getName()); }
 *   );
 * </pre>
 * This waits 0 ticks, activates {@code hs_sword} for 20 ticks, deals 4 damage,
 * can damage each target up to 3 times at intervals of 10 ticks, and admits only
 * the first target. The callback runs after every successful damage application.
 * Non-positive repeat and target limits mean unlimited.
 */
public final class ImmersiveBossAPI {

    public static final int DEFAULT_HITBOX_DAMAGE_DURATION_TICKS = 20;
    public static final int DEFAULT_HITBOX_DAMAGE_START_DELAY_TICKS = 0;
    public static final float DEFAULT_HITBOX_COLLISION_DAMAGE = 1.0F;
    public static final int DEFAULT_HITBOX_DAMAGE_REPEAT_COUNT = 1;
    public static final int DEFAULT_HITBOX_DAMAGE_REPEAT_INTERVAL_TICKS = 10;
    public static final int DEFAULT_HITBOX_DAMAGE_MAX_TARGETS = 0;

    private ImmersiveBossAPI() {}

    /**
     * Starts a scripted throw animation against a player. The animation must
     * be present in the NPC's GeckoLib model; a {@code victim_root} hierarchy
     * created by the Blockbench tool is rendered with the target player's skin.
     * During the animation, {@code camera_root} drives first-person view;
     * {@code camera_second_person} and {@code camera_third_person} drive the
     * front and back third-person views. Missing external views are derived
     * from the first-person camera at runtime.
     *
     * @param npcWrapper attacking NPC wrapper
     * @param targetWrapper player entity wrapper to throw
     * @param animation GeckoLib animation name
     * @param durationTicks animation duration in ticks
     * @return true when the throw was accepted on the server
     */
    public static boolean startThrow(ICustomNpc npcWrapper, IEntity targetWrapper,
                                     String animation, int durationTicks) {
        return startThrow(npcWrapper, targetWrapper, animation, durationTicks, null);
    }

    public static boolean startThrow(ICustomNpc npcWrapper, IEntity targetWrapper,
                                     String animation, int durationTicks, String struggleMode) {
        return startThrow(npcWrapper, targetWrapper, animation, durationTicks, struggleMode, null);
    }

    /** Full script form with return-to-start toggle before struggle settings. */
    public static boolean startThrow(ICustomNpc npcWrapper, IEntity targetWrapper,
                                     String animation, int durationTicks, boolean returnToStart,
                                     String struggleMode, Integer difficulty,
                                     ThrowCallback onEscape, ThrowCallback onFinish) {
        return startThrow(npcWrapper, targetWrapper, animation, durationTicks, struggleMode,
            difficulty, returnToStart, onEscape, onFinish);
    }

    public static boolean startThrow(ICustomNpc npcWrapper, IEntity targetWrapper,
                                     String animation, int durationTicks, String struggleMode,
                                     Integer difficulty) {
        return startThrow(npcWrapper, targetWrapper, animation, durationTicks,
            struggleMode, difficulty, null, null);
    }

    public static boolean startThrow(ICustomNpc npcWrapper, IEntity targetWrapper,
                                     String animation, int durationTicks, String struggleMode,
                                     Integer difficulty, ThrowCallback onEscape) {
        return startThrow(npcWrapper, targetWrapper, animation, durationTicks,
            struggleMode, difficulty, onEscape, null);
    }

    /**
     * Optional mode: null/empty/none (disabled), ad, space, or shift.
     * Difficulty defaults to 5 when null; it must otherwise be positive.
     * An AD cycle is A then D or D then A; single-key modes count fresh presses.
     * Callbacks receive (npc, player), after state restoration, at most once.
     * Only successful escape calls onEscape; only expiration calls onFinish.
     * Cancellation, replacement, death, disconnect and dimension changes call neither.
     */
    public static boolean startThrow(ICustomNpc npcWrapper, IEntity targetWrapper,
                                     String animation, int durationTicks, String struggleMode,
                                     Integer difficulty, ThrowCallback onEscape, ThrowCallback onFinish) {
        return startThrow(npcWrapper, targetWrapper, animation, durationTicks, struggleMode,
            difficulty, false, onEscape, onFinish);
    }

    public static boolean startThrow(ICustomNpc npcWrapper, IEntity targetWrapper,
                                     String animation, int durationTicks, String struggleMode,
                                     Integer difficulty, boolean returnToStart,
                                     ThrowCallback onEscape, ThrowCallback onFinish) {
        EntityNPCInterface npc = getNpc(npcWrapper);
        Entity target = targetWrapper != null ? targetWrapper.getMCEntity() : null;
        return npc != null && ThrowManager.start(npc, target, animation, durationTicks,
            struggleMode, difficulty, returnToStart, onEscape, onFinish);
    }

    /** Numeric form: 0 none, 1 AD, 2 space, 3 shift. */
    public static boolean startThrow(ICustomNpc npcWrapper, IEntity targetWrapper,
                                     String animation, int durationTicks, int struggleMode,
                                     int difficulty) {
        return startThrow(npcWrapper, targetWrapper, animation, durationTicks,
            struggleMode, difficulty, null, null);
    }

    public static boolean startThrow(ICustomNpc npcWrapper, IEntity targetWrapper,
                                     String animation, int durationTicks, int struggleMode) {
        return startThrow(npcWrapper, targetWrapper, animation, durationTicks, struggleMode, 5);
    }

    public static boolean startThrow(ICustomNpc npcWrapper, IEntity targetWrapper,
                                     String animation, int durationTicks, int struggleMode,
                                     int difficulty, boolean returnToStart) {
        return startThrow(npcWrapper, targetWrapper, animation, durationTicks, struggleMode,
            difficulty, returnToStart, null, null);
    }

    /** Numeric form with callbacks: 0 none, 1 AD, 2 space, 3 shift. */
    public static boolean startThrow(ICustomNpc npcWrapper, IEntity targetWrapper,
                                     String animation, int durationTicks, int struggleMode,
                                     int difficulty, ThrowCallback onEscape, ThrowCallback onFinish) {
        return startThrow(npcWrapper, targetWrapper, animation, durationTicks, struggleMode,
            difficulty, false, onEscape, onFinish);
    }

    public static boolean startThrow(ICustomNpc npcWrapper, IEntity targetWrapper,
                                     String animation, int durationTicks, int struggleMode,
                                     int difficulty, boolean returnToStart,
                                     ThrowCallback onEscape, ThrowCallback onFinish) {
        EntityNPCInterface npc = getNpc(npcWrapper);
        Entity target = targetWrapper != null ? targetWrapper.getMCEntity() : null;
        return npc != null && ThrowManager.start(npc, target, animation, durationTicks,
            struggleMode, difficulty, returnToStart, onEscape, onFinish);
    }

    /** Stops the active throw for a target player and restores its state. */
    public static boolean stopThrow(IEntity targetWrapper) {
        Entity target = targetWrapper != null ? targetWrapper.getMCEntity() : null;
        return ThrowManager.stop(target);
    }

    /** Returns whether the target player is currently controlled by a throw. */
    public static boolean isThrowActive(IEntity targetWrapper) {
        Entity target = targetWrapper != null ? targetWrapper.getMCEntity() : null;
        return ThrowManager.isActive(target);
    }

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

    /** Returns every hitbox base bone name defined by the NPC's current model. */
    public static String[] getHitboxNames(ICustomNpc wrapper) {
        return getHitboxNames(wrapper, HitboxFilter.ALL);
    }

    /** Returns hitboxes with physical collision ({@code b} suffix). */
    public static String[] getPhysicalHitboxNames(ICustomNpc wrapper) {
        return getHitboxNames(wrapper, HitboxFilter.PHYSICAL);
    }

    /** Returns attackable/detectable hitboxes (the {@code d} flag). */
    public static String[] getDetectableHitboxNames(ICustomNpc wrapper) {
        return getHitboxNames(wrapper, HitboxFilter.DETECTABLE);
    }

    /** Returns non-physical sensor hitboxes ({@code s} suffix). */
    public static String[] getSensorHitboxNames(ICustomNpc wrapper) {
        return getHitboxNames(wrapper, HitboxFilter.SENSOR);
    }

    /** Returns hitboxes whose geometry is visible (the {@code a} flag). */
    public static String[] getVisibleHitboxNames(ICustomNpc wrapper) {
        return getHitboxNames(wrapper, HitboxFilter.VISIBLE);
    }

    /** Checks a base name or multi-cube internal name against the current model. */
    public static boolean hasHitbox(ICustomNpc wrapper, String hitboxName) {
        String normalizedName = normalizeHitboxName(hitboxName);
        if (normalizedName == null) return false;
        for (String name : getHitboxNames(wrapper)) {
            if (name.equals(normalizedName)) return true;
        }
        return false;
    }

    /**
     * Activates collision damage after the required delay, using all other defaults.
     * <pre>
     *   ImmersiveBossAPI.activateHitboxDamage(npc, "hs_sword", 0);
     * </pre>
     */
    public static boolean activateHitboxDamage(ICustomNpc wrapper, String hitboxName,
                                               int startDelayTicks) {
        return activateHitboxDamage(wrapper, hitboxName, startDelayTicks,
            DEFAULT_HITBOX_DAMAGE_DURATION_TICKS);
    }

    /** Activates collision damage after a custom delay and runs a callback after each hit. */
    public static boolean activateHitboxDamage(ICustomNpc wrapper, String hitboxName,
                                               int startDelayTicks,
                                               HitboxDamageCallback callback) {
        return activateHitboxDamage(wrapper, hitboxName, startDelayTicks,
            DEFAULT_HITBOX_DAMAGE_DURATION_TICKS, callback);
    }

    /** Activates collision damage with a custom start delay and duration in ticks. */
    public static boolean activateHitboxDamage(ICustomNpc wrapper, String hitboxName,
                                               int startDelayTicks, int durationTicks) {
        return activateHitboxDamage(wrapper, hitboxName, startDelayTicks, durationTicks,
            DEFAULT_HITBOX_COLLISION_DAMAGE);
    }

    public static boolean activateHitboxDamage(ICustomNpc wrapper, String hitboxName,
                                               int startDelayTicks, int durationTicks,
                                               HitboxDamageCallback callback) {
        return activateHitboxDamage(wrapper, hitboxName, startDelayTicks, durationTicks,
            DEFAULT_HITBOX_COLLISION_DAMAGE, callback);
    }

    /** Activates collision damage with a custom delay, duration, and damage amount. */
    public static boolean activateHitboxDamage(ICustomNpc wrapper, String hitboxName,
                                               int startDelayTicks, int durationTicks,
                                               float damage) {
        return activateHitboxDamage(wrapper, hitboxName, startDelayTicks, durationTicks, damage,
            DEFAULT_HITBOX_DAMAGE_REPEAT_COUNT);
    }

    public static boolean activateHitboxDamage(ICustomNpc wrapper, String hitboxName,
                                               int startDelayTicks, int durationTicks,
                                               float damage, HitboxDamageCallback callback) {
        return activateHitboxDamage(wrapper, hitboxName, startDelayTicks, durationTicks, damage,
            DEFAULT_HITBOX_DAMAGE_REPEAT_COUNT, callback);
    }

    /** Activates collision damage with a per-target repeat limit. Non-positive means unlimited. */
    public static boolean activateHitboxDamage(ICustomNpc wrapper, String hitboxName,
                                               int startDelayTicks, int durationTicks,
                                               float damage, int repeatCount) {
        return activateHitboxDamage(wrapper, hitboxName, startDelayTicks, durationTicks,
            damage, repeatCount,
            DEFAULT_HITBOX_DAMAGE_REPEAT_INTERVAL_TICKS);
    }

    public static boolean activateHitboxDamage(ICustomNpc wrapper, String hitboxName,
                                               int startDelayTicks, int durationTicks,
                                               float damage, int repeatCount,
                                               HitboxDamageCallback callback) {
        return activateHitboxDamage(wrapper, hitboxName, startDelayTicks, durationTicks,
            damage, repeatCount, DEFAULT_HITBOX_DAMAGE_REPEAT_INTERVAL_TICKS, callback);
    }

    /** Activates collision damage with a repeat interval in ticks. */
    public static boolean activateHitboxDamage(ICustomNpc wrapper, String hitboxName,
                                               int startDelayTicks, int durationTicks, float damage,
                                               int repeatCount, int repeatIntervalTicks) {
        return activateHitboxDamage(wrapper, hitboxName, startDelayTicks, durationTicks,
            damage, repeatCount,
            repeatIntervalTicks, DEFAULT_HITBOX_DAMAGE_MAX_TARGETS);
    }

    public static boolean activateHitboxDamage(ICustomNpc wrapper, String hitboxName,
                                               int startDelayTicks, int durationTicks, float damage,
                                               int repeatCount, int repeatIntervalTicks,
                                               HitboxDamageCallback callback) {
        return activateHitboxDamage(wrapper, hitboxName, startDelayTicks, durationTicks,
            damage, repeatCount, repeatIntervalTicks, DEFAULT_HITBOX_DAMAGE_MAX_TARGETS,
            callback);
    }

    /**
     * Activates collision damage for one hitbox. A non-positive repeat count or target
     * limit means unlimited. Re-activating the same hitbox starts a fresh damage window.
     *
     * @return true when the damage window was created; false for invalid input or client-side use
     */
    public static boolean activateHitboxDamage(ICustomNpc wrapper, String hitboxName,
                                               int startDelayTicks, int durationTicks, float damage,
                                               int repeatCount, int repeatIntervalTicks,
                                               int maxTargets) {
        return activateHitboxDamage(wrapper, hitboxName, startDelayTicks, durationTicks,
            damage, repeatCount, repeatIntervalTicks, maxTargets, null);
    }

    /**
     * Complete collision-damage window form. The callback receives CNPC wrappers for
     * the attacking NPC and damaged target after every successful damage application.
     */
    public static boolean activateHitboxDamage(ICustomNpc wrapper, String hitboxName,
                                               int startDelayTicks, int durationTicks, float damage,
                                               int repeatCount, int repeatIntervalTicks,
                                               int maxTargets, HitboxDamageCallback callback) {
        if (wrapper == null) return false;
        Entity entity = wrapper.getMCEntity();
        if (!(entity instanceof EntityNPCInterface npc)) return false;
        return HitboxDamageManager.activate(npc, hitboxName, startDelayTicks, durationTicks,
            damage, repeatCount, repeatIntervalTicks, maxTargets, callback);
    }

    /** Interrupts one damage window. Returns false if it was not active. */
    public static boolean cancelHitboxDamageWindow(ICustomNpc wrapper, String hitboxName) {
        EntityNPCInterface npc = getNpc(wrapper);
        return npc != null && HitboxDamageManager.cancel(npc, hitboxName);
    }

    /** Interrupts every active damage window and returns the number canceled. */
    public static int cancelAllHitboxDamageWindows(ICustomNpc wrapper) {
        EntityNPCInterface npc = getNpc(wrapper);
        return npc != null ? HitboxDamageManager.cancelAll(npc) : 0;
    }

    public static boolean isHitboxDamageWindowActive(ICustomNpc wrapper, String hitboxName) {
        EntityNPCInterface npc = getNpc(wrapper);
        return npc != null && HitboxDamageManager.isActive(npc, hitboxName);
    }

    /** Returns zero when the damage window is absent or expired. */
    public static int getHitboxDamageWindowRemainingTicks(ICustomNpc wrapper, String hitboxName) {
        EntityNPCInterface npc = getNpc(wrapper);
        return npc != null ? HitboxDamageManager.getRemainingTicks(npc, hitboxName) : 0;
    }

    /** Returns all active damage-window base bone names in deterministic order. */
    public static String[] getActiveHitboxDamageWindows(ICustomNpc wrapper) {
        EntityNPCInterface npc = getNpc(wrapper);
        return npc != null ? HitboxDamageManager.getActiveHitboxNames(npc) : new String[0];
    }

    /** Sets the maximum horizontal turn in degrees per tick. Zero freezes turning. */
    public static boolean setTurnSpeedLimit(ICustomNpc wrapper, float degreesPerTick) {
        return NpcTurnSpeedManager.setLimit(wrapper, degreesPerTick);
    }

    /** Returns {@code -1} when this NPC has no turn-speed limit. */
    public static float getTurnSpeedLimit(ICustomNpc wrapper) {
        return NpcTurnSpeedManager.getLimit(wrapper);
    }

    public static boolean hasTurnSpeedLimit(ICustomNpc wrapper) {
        return NpcTurnSpeedManager.hasLimit(wrapper);
    }

    public static boolean setTurnSpeedLimitEnabled(ICustomNpc wrapper, boolean enabled) {
        return NpcTurnSpeedManager.setEnabled(wrapper, enabled);
    }

    /** Removes the turn-speed limit and any queued gradual rotation target. */
    public static boolean clearTurnSpeedLimit(ICustomNpc wrapper) {
        return NpcTurnSpeedManager.clearLimit(wrapper);
    }

    /** Returns the navigation speed floor as a value from zero to one. */
    public static float getMinimumNavigationSpeedScale(ICustomNpc wrapper) {
        return NpcTurnSpeedManager.getMinimumNavigationSpeedScale(wrapper);
    }

    /** Sets the navigation speed floor as a value from zero to one. */
    public static boolean setMinimumNavigationSpeedScale(ICustomNpc wrapper, float scale) {
        return NpcTurnSpeedManager.setMinimumNavigationSpeedScale(wrapper, scale);
    }

    /** Immediately aligns entity, body, and head yaw, bypassing the active limit. */
    public static void setRotationImmediate(ICustomNpc wrapper, float rotation) {
        NpcTurnSpeedManager.setRotationImmediate(wrapper, rotation);
    }

    private static String[] getHitboxNames(ICustomNpc wrapper, HitboxFilter filter) {
        EntityNPCInterface npc = getNpc(wrapper);
        if (npc == null) return new String[0];

        Map<String, GeoHitboxDef.Type> hitboxes = new TreeMap<>();
        List<GeoHitboxDef> defs = npc.level().isClientSide
            ? null
            : ServerHitboxData.getOrLoadForNpc(npc);
        if (defs != null) {
            for (GeoHitboxDef def : defs) {
                addHitboxType(hitboxes, def.boneName);
            }
        } else if (npc instanceof IOBBHolder holder) {
            // Live OBBs are a fallback for client-side calls or unavailable defs.
            for (String name : holder.cnpc_immersiveboss$getBoneOBBs().keySet()) {
                addHitboxType(hitboxes, name);
            }
        }

        return hitboxes.entrySet().stream()
            .filter(entry -> filter.matches(entry.getValue()))
            .map(Map.Entry::getKey)
            .toArray(String[]::new);
    }

    private static void addHitboxType(Map<String, GeoHitboxDef.Type> hitboxes, String name) {
        String baseName = GeoHitboxDef.baseBoneName(name);
        GeoHitboxDef.Type type = GeoHitboxDef.classify(baseName);
        if (type != null) hitboxes.putIfAbsent(baseName, type);
    }

    private static EntityNPCInterface getNpc(ICustomNpc wrapper) {
        if (wrapper == null) return null;
        Entity entity = wrapper.getMCEntity();
        return entity instanceof EntityNPCInterface npc ? npc : null;
    }

    private static String normalizeHitboxName(String hitboxName) {
        if (hitboxName == null) return null;
        String trimmed = hitboxName.trim();
        if (trimmed.isEmpty()) return null;
        String baseName = GeoHitboxDef.baseBoneName(trimmed);
        return GeoHitboxDef.classify(baseName) != null ? baseName : null;
    }

    private enum HitboxFilter {
        ALL, PHYSICAL, DETECTABLE, SENSOR, VISIBLE;

        private boolean matches(GeoHitboxDef.Type type) {
            return switch (this) {
                case ALL -> true;
                case PHYSICAL -> type.physical;
                case DETECTABLE -> type.detectable;
                case SENSOR -> !type.physical;
                case VISIBLE -> type.render;
            };
        }
    }
}
