package sweda.cnpc_immersiveboss.hitbox;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.entity.EntityNPCInterface;
import org.slf4j.Logger;
import sweda.cnpc_immersiveboss.api.HitboxDamageCallback;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

/** Server-side collision damage windows activated through {@code ImmersiveBossAPI}. */
public final class HitboxDamageManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, Map<String, DamageWindow>> ACTIVE = new HashMap<>();

    private HitboxDamageManager() {}

    public static boolean activate(EntityNPCInterface npc, String hitboxName,
                                   int startDelayTicks, int durationTicks, float damage,
                                   int repeatCount, int repeatIntervalTicks, int maxTargets,
                                   HitboxDamageCallback callback) {
        if (npc == null || npc.level().isClientSide || npc.isRemoved() || npc.isDeadOrDying()) {
            return false;
        }
        String normalizedName = normalizeHitboxName(hitboxName);
        if (normalizedName == null || durationTicks <= 0 || !Float.isFinite(damage) || damage <= 0) {
            return false;
        }

        long activatesAt = npc.level().getGameTime() + Math.max(0L, (long) startDelayTicks);
        long expiresAt = activatesAt + (long) durationTicks;
        DamageWindow window = new DamageWindow(
            activatesAt, expiresAt, damage, repeatCount,
            Math.max(0, repeatIntervalTicks), maxTargets, callback
        );
        ACTIVE.computeIfAbsent(npc.getUUID(), ignored -> new HashMap<>())
            .put(normalizedName, window);
        return true;
    }

    public static void onCollision(EntityNPCInterface npc, String hitboxName, Entity other) {
        if (!(other instanceof LivingEntity target) || !target.isAlive()) return;

        Map<String, DamageWindow> windows = ACTIVE.get(npc.getUUID());
        if (windows == null) return;

        String normalizedName = normalizeHitboxName(hitboxName);
        if (normalizedName == null) return;
        DamageWindow window = windows.get(normalizedName);
        if (window == null) return;

        long gameTime = npc.level().getGameTime();
        if (gameTime < window.activatesAt) return;
        if (gameTime >= window.expiresAt) {
            windows.remove(normalizedName);
            removeNpcEntryIfEmpty(npc.getUUID(), windows);
            return;
        }

        UUID targetId = target.getUUID();
        TargetDamageState state = window.targets.get(targetId);
        if (state == null) {
            if (window.maxTargets > 0 && window.targets.size() >= window.maxTargets) return;
        } else {
            if (window.repeatCount > 0 && state.hitCount >= window.repeatCount) return;
            long elapsed = gameTime - state.lastDamageTick;
            if (elapsed <= 0 || elapsed < window.repeatIntervalTicks) return;
        }

        Long lastAttemptTick = window.lastAttemptTicks.get(targetId);
        if (lastAttemptTick != null && lastAttemptTick == gameTime) return;
        window.lastAttemptTicks.put(targetId, gameTime);
        if (!target.hurt(npc.damageSources().mobAttack(npc), window.damage)) return;

        if (state == null) {
            state = new TargetDamageState();
            window.targets.put(targetId, state);
        }
        state.hitCount++;
        state.lastDamageTick = gameTime;
        invokeCallback(window, npc, target, normalizedName);
    }

    public static void tick(EntityNPCInterface npc) {
        UUID npcId = npc.getUUID();
        if (npc.isRemoved() || npc.isDeadOrDying()) {
            ACTIVE.remove(npcId);
            return;
        }

        Map<String, DamageWindow> windows = ACTIVE.get(npcId);
        if (windows == null) return;
        long gameTime = npc.level().getGameTime();
        windows.values().removeIf(window -> gameTime >= window.expiresAt);
        removeNpcEntryIfEmpty(npcId, windows);
    }

    /** Cancels one active damage window, including all of its per-target state. */
    public static boolean cancel(EntityNPCInterface npc, String hitboxName) {
        if (!canAccess(npc)) return false;
        String normalizedName = normalizeHitboxName(hitboxName);
        if (normalizedName == null) return false;

        Map<String, DamageWindow> windows = getActiveWindows(npc);
        if (windows == null) return false;
        boolean removed = windows.remove(normalizedName) != null;
        removeNpcEntryIfEmpty(npc.getUUID(), windows);
        return removed;
    }

    /** Cancels every active damage window for this NPC and returns the count. */
    public static int cancelAll(EntityNPCInterface npc) {
        if (!canAccess(npc)) return 0;
        Map<String, DamageWindow> windows = getActiveWindows(npc);
        if (windows == null) return 0;
        int count = windows.size();
        ACTIVE.remove(npc.getUUID());
        return count;
    }

    public static boolean isActive(EntityNPCInterface npc, String hitboxName) {
        return getActiveWindow(npc, hitboxName) != null;
    }

    /** Returns zero when the requested window is absent or already expired. */
    public static int getRemainingTicks(EntityNPCInterface npc, String hitboxName) {
        DamageWindow window = getActiveWindow(npc, hitboxName);
        if (window == null) return 0;
        long remaining = window.expiresAt - npc.level().getGameTime();
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, remaining));
    }

    /** Returns active base bone names in deterministic order. */
    public static String[] getActiveHitboxNames(EntityNPCInterface npc) {
        if (!canAccess(npc)) return new String[0];
        Map<String, DamageWindow> windows = getActiveWindows(npc);
        if (windows == null) return new String[0];
        return new TreeSet<>(windows.keySet()).toArray(String[]::new);
    }

    public static void clear(EntityNPCInterface npc) {
        if (npc != null && !npc.level().isClientSide) ACTIVE.remove(npc.getUUID());
    }

    private static DamageWindow getActiveWindow(EntityNPCInterface npc, String hitboxName) {
        if (!canAccess(npc)) return null;
        String normalizedName = normalizeHitboxName(hitboxName);
        if (normalizedName == null) return null;
        Map<String, DamageWindow> windows = getActiveWindows(npc);
        return windows != null ? windows.get(normalizedName) : null;
    }

    private static Map<String, DamageWindow> getActiveWindows(EntityNPCInterface npc) {
        UUID npcId = npc.getUUID();
        Map<String, DamageWindow> windows = ACTIVE.get(npcId);
        if (windows == null) return null;
        long gameTime = npc.level().getGameTime();
        windows.values().removeIf(window -> gameTime >= window.expiresAt);
        removeNpcEntryIfEmpty(npcId, windows);
        return windows.isEmpty() ? null : windows;
    }

    private static boolean canAccess(EntityNPCInterface npc) {
        return npc != null && !npc.level().isClientSide && !npc.isRemoved()
            && !npc.isDeadOrDying();
    }

    private static String normalizeHitboxName(String hitboxName) {
        if (hitboxName == null) return null;
        String trimmed = hitboxName.trim();
        if (trimmed.isEmpty()) return null;
        String baseName = GeoHitboxDef.baseBoneName(trimmed);
        return GeoHitboxDef.classify(baseName) != null ? baseName : null;
    }

    private static void removeNpcEntryIfEmpty(UUID npcId, Map<String, DamageWindow> windows) {
        if (windows.isEmpty()) ACTIVE.remove(npcId);
    }

    private static void invokeCallback(DamageWindow window, EntityNPCInterface npc,
                                       LivingEntity target, String hitboxName) {
        if (window.callback == null) return;
        try {
            NpcAPI api = NpcAPI.Instance();
            window.callback.onDamage(npc.wrappedNPC,
                api != null ? api.getIEntity(target) : null);
        } catch (Exception exception) {
            LOGGER.error("Damage-window callback failed for NPC {} hitbox {}",
                npc.getUUID(), hitboxName, exception);
        }
    }

    private static final class DamageWindow {
        private final long activatesAt;
        private final long expiresAt;
        private final float damage;
        private final int repeatCount;
        private final int repeatIntervalTicks;
        private final int maxTargets;
        private final HitboxDamageCallback callback;
        private final Map<UUID, TargetDamageState> targets = new LinkedHashMap<>();
        private final Map<UUID, Long> lastAttemptTicks = new HashMap<>();

        private DamageWindow(long activatesAt, long expiresAt, float damage,
                             int repeatCount, int repeatIntervalTicks, int maxTargets,
                             HitboxDamageCallback callback) {
            this.activatesAt = activatesAt;
            this.expiresAt = expiresAt;
            this.damage = damage;
            this.repeatCount = repeatCount;
            this.repeatIntervalTicks = repeatIntervalTicks;
            this.maxTargets = maxTargets;
            this.callback = callback;
        }
    }

    private static final class TargetDamageState {
        private int hitCount;
        private long lastDamageTick;
    }
}
