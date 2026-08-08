package sweda.cnpc_immersiveboss.hitbox;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import noppes.npcs.entity.EntityNPCInterface;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Server-side collision damage windows activated through {@code ImmersiveBossAPI}. */
public final class HitboxDamageManager {
    private static final Map<UUID, Map<String, DamageWindow>> ACTIVE = new HashMap<>();

    private HitboxDamageManager() {}

    public static boolean activate(EntityNPCInterface npc, String hitboxName,
                                   int durationTicks, float damage, int repeatCount,
                                   int repeatIntervalTicks, int maxTargets) {
        if (npc == null || npc.level().isClientSide || npc.isRemoved() || npc.isDeadOrDying()) {
            return false;
        }
        String normalizedName = normalizeHitboxName(hitboxName);
        if (normalizedName == null || durationTicks <= 0 || !Float.isFinite(damage) || damage <= 0) {
            return false;
        }

        long expiresAt = npc.level().getGameTime() + (long) durationTicks;
        DamageWindow window = new DamageWindow(
            expiresAt, damage, repeatCount, Math.max(0, repeatIntervalTicks), maxTargets
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

    public static void clear(EntityNPCInterface npc) {
        if (npc != null && !npc.level().isClientSide) ACTIVE.remove(npc.getUUID());
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

    private static final class DamageWindow {
        private final long expiresAt;
        private final float damage;
        private final int repeatCount;
        private final int repeatIntervalTicks;
        private final int maxTargets;
        private final Map<UUID, TargetDamageState> targets = new LinkedHashMap<>();
        private final Map<UUID, Long> lastAttemptTicks = new HashMap<>();

        private DamageWindow(long expiresAt, float damage, int repeatCount,
                             int repeatIntervalTicks, int maxTargets) {
            this.expiresAt = expiresAt;
            this.damage = damage;
            this.repeatCount = repeatCount;
            this.repeatIntervalTicks = repeatIntervalTicks;
            this.maxTargets = maxTargets;
        }
    }

    private static final class TargetDamageState {
        private int hitCount;
        private long lastDamageTick;
    }
}
