package sweda.cnpc_immersiveboss.entity;

import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import sweda.cnpc_immersiveboss.api.IMixinDataAI;
import sweda.cnpc_immersiveboss.api.INpcTurnState;

/** Applies a persistent, per-NPC horizontal turn-speed limit after AI controls run. */
public final class NpcTurnSpeedManager {
    public static final float NO_TURN_SPEED_LIMIT = -1.0F;
    public static final float DEFAULT_TURN_SPEED = 10.0F;
    public static final float MAX_TURN_SPEED = 180.0F;
    public static final float DEFAULT_MINIMUM_NAVIGATION_SPEED_SCALE = 0.2F;
    public static final String ROTATION_LIMIT_ENABLED_NBT_KEY =
        "ImmersiveBossRotationLimitEnabled";
    public static final String ROTATION_SPEED_NBT_KEY =
        "ImmersiveBossRotationSpeed";
    public static final String MINIMUM_NAVIGATION_SPEED_SCALE_NBT_KEY =
        "ImmersiveBossMinimumNavigationSpeedScale";

    private static final String LEGACY_TURN_SPEED_LIMIT_KEY =
        "cnpc_immersiveboss.turn_speed_limit";
    private static final float ARRIVAL_EPSILON = 0.001F;
    private static final float REAR_LOCK_ENTER_DEGREES = 170.0F;
    private static final float REAR_LOCK_EXIT_DEGREES = 135.0F;
    private static final ThreadLocal<Boolean> IMMEDIATE_ROTATION =
        ThreadLocal.withInitial(() -> false);

    private NpcTurnSpeedManager() {}

    public static boolean setLimit(ICustomNpc wrapper, float degreesPerTick) {
        EntityNPCInterface npc = getNpc(wrapper);
        if (!canConfigure(npc) || !isValidTurnSpeed(degreesPerTick)) {
            return false;
        }

        IMixinDataAI settings = getSettings(npc);
        if (settings == null) return false;
        settings.cnpc_immersiveboss$setRotationSpeed(degreesPerTick);
        settings.cnpc_immersiveboss$setRotationLimitEnabled(true);
        npc.getPersistentData().remove(LEGACY_TURN_SPEED_LIMIT_KEY);
        if (npc instanceof INpcTurnState state
            && !state.cnpc_immersiveboss$isTurnStateInitialized()) {
            initializeState(npc);
        }
        return true;
    }

    public static float getLimit(ICustomNpc wrapper) {
        return getLimit(getNpc(wrapper));
    }

    public static boolean hasLimit(ICustomNpc wrapper) {
        return getLimit(wrapper) >= 0.0F;
    }

    public static boolean setEnabled(ICustomNpc wrapper, boolean enabled) {
        EntityNPCInterface npc = getNpc(wrapper);
        if (!canConfigure(npc)) return false;
        IMixinDataAI settings = getSettings(npc);
        if (settings == null) return false;

        migrateLegacySettings(npc, settings);
        settings.cnpc_immersiveboss$setRotationLimitEnabled(enabled);
        if (!enabled && npc instanceof INpcTurnState state) {
            state.cnpc_immersiveboss$clearTurnState();
        }
        return true;
    }

    public static float getMinimumNavigationSpeedScale(ICustomNpc wrapper) {
        EntityNPCInterface npc = getNpc(wrapper);
        IMixinDataAI settings = getSettings(npc);
        if (settings == null) return DEFAULT_MINIMUM_NAVIGATION_SPEED_SCALE;
        migrateLegacySettings(npc, settings);
        return settings.cnpc_immersiveboss$getMinimumNavigationSpeedScale();
    }

    public static boolean setMinimumNavigationSpeedScale(ICustomNpc wrapper, float scale) {
        EntityNPCInterface npc = getNpc(wrapper);
        if (!canConfigure(npc) || !isValidMinimumNavigationSpeedScale(scale)) return false;
        IMixinDataAI settings = getSettings(npc);
        if (settings == null) return false;
        settings.cnpc_immersiveboss$setMinimumNavigationSpeedScale(scale);
        return true;
    }

    public static boolean clearLimit(ICustomNpc wrapper) {
        EntityNPCInterface npc = getNpc(wrapper);
        if (!canConfigure(npc)) return false;

        IMixinDataAI settings = getSettings(npc);
        if (settings == null) return false;
        migrateLegacySettings(npc, settings);
        boolean existed = settings.cnpc_immersiveboss$isRotationLimitEnabled();
        settings.cnpc_immersiveboss$setRotationLimitEnabled(false);
        npc.getPersistentData().remove(LEGACY_TURN_SPEED_LIMIT_KEY);
        if (npc instanceof INpcTurnState state) state.cnpc_immersiveboss$clearTurnState();
        return existed;
    }

    /** Queues an ordinary NPCWrapper.setRotation call as a gradual target. */
    public static boolean shouldDeferRotation(EntityNPCInterface npc, float rotation) {
        if (isImmediateRotation() || !canConfigure(npc) || !Float.isFinite(rotation)
            || getLimit(npc) < 0.0F || !(npc instanceof INpcTurnState state)) {
            return false;
        }

        if (!state.cnpc_immersiveboss$isTurnStateInitialized()) initializeState(npc);
        state.cnpc_immersiveboss$setRequestedRotation(Mth.wrapDegrees(rotation));
        return true;
    }

    public static void setRotationImmediate(ICustomNpc wrapper, float rotation) {
        EntityNPCInterface npc = getNpc(wrapper);
        if (!canConfigure(npc) || !Float.isFinite(rotation)) return;

        float normalized = Mth.wrapDegrees(rotation);
        IMMEDIATE_ROTATION.set(true);
        try {
            wrapper.setRotation(rotation);
        } finally {
            IMMEDIATE_ROTATION.remove();
        }

        npc.setYRot(normalized);
        npc.yBodyRot = normalized;
        npc.yHeadRot = normalized;
        npc.yRotO = normalized;
        npc.yBodyRotO = normalized;
        npc.yHeadRotO = normalized;
        if (npc instanceof INpcTurnState state) {
            state.cnpc_immersiveboss$clearTurnState();
            state.cnpc_immersiveboss$initializeTurnState(normalized, normalized, normalized);
        }
    }

    /** Captures the current allowed orientation before this tick's AI updates it. */
    public static void prepare(EntityNPCInterface npc) {
        if (npc == null || npc.level().isClientSide || npc.isRemoved()
            || getLimit(npc) < 0.0F || !(npc instanceof INpcTurnState state)
            || state.cnpc_immersiveboss$isTurnStateInitialized()) {
            return;
        }
        initializeState(npc);
    }

    /** Limits MoveControl's yaw before travel so navigation follows the allowed heading. */
    public static float limitNavigationYaw(EntityNPCInterface npc, float navigationTarget) {
        if (npc == null || npc.level().isClientSide || npc.isRemoved()
            || !Float.isFinite(navigationTarget) || !(npc instanceof INpcTurnState state)) {
            return navigationTarget;
        }

        float limit = getLimit(npc);
        if (limit < 0.0F) return navigationTarget;
        if (!state.cnpc_immersiveboss$isTurnStateInitialized()) initializeState(npc);

        float target = state.cnpc_immersiveboss$hasRequestedRotation()
            ? state.cnpc_immersiveboss$getRequestedRotation() : navigationTarget;
        int turnDirection = updateRearTurnDirection(
            state, state.cnpc_immersiveboss$getLimitedEntityYaw(), target);
        float entityYaw = approachDegrees(
            state.cnpc_immersiveboss$getLimitedEntityYaw(), target, limit, turnDirection);
        float remaining = Math.abs(stableDifference(entityYaw, target, turnDirection));
        float speedScale = navigationSpeedScale(
            remaining, getMinimumNavigationSpeedScale(npc));

        state.cnpc_immersiveboss$setLimitedYaws(
            entityYaw,
            state.cnpc_immersiveboss$getLimitedBodyYaw(),
            state.cnpc_immersiveboss$getLimitedHeadYaw());
        state.cnpc_immersiveboss$recordNavigationTurn(
            npc.level().getGameTime(), target, speedScale);
        return entityYaw;
    }

    /** Scales only MoveControl's requested speed; velocity, knockback, and pushes are untouched. */
    public static float limitNavigationSpeed(EntityNPCInterface npc, float requestedSpeed) {
        if (npc == null || !(npc instanceof INpcTurnState state)
            || getLimit(npc) < 0.0F
            || state.cnpc_immersiveboss$getNavigationTurnTick()
                != npc.level().getGameTime()) {
            return requestedSpeed;
        }
        return requestedSpeed * state.cnpc_immersiveboss$getNavigationSpeedScale();
    }

    /** Idempotent per game tick; safe from both the entity mixin and Forge fallback. */
    public static void apply(EntityNPCInterface npc) {
        if (npc == null || npc.level().isClientSide || npc.isRemoved()
            || !(npc instanceof INpcTurnState state)) {
            return;
        }

        float limit = getLimit(npc);
        if (limit < 0.0F) {
            state.cnpc_immersiveboss$clearTurnState();
            return;
        }

        long gameTime = npc.level().getGameTime();
        if (state.cnpc_immersiveboss$getLastTurnLimitTick() == gameTime) return;
        state.cnpc_immersiveboss$setLastTurnLimitTick(gameTime);

        if (!state.cnpc_immersiveboss$isTurnStateInitialized()) {
            initializeState(npc);
            return;
        }

        boolean requested = state.cnpc_immersiveboss$hasRequestedRotation();
        boolean navigationTurn = state.cnpc_immersiveboss$getNavigationTurnTick() == gameTime;
        float targetEntity = requested
            ? state.cnpc_immersiveboss$getRequestedRotation()
            : navigationTurn ? state.cnpc_immersiveboss$getNavigationTargetYaw() : npc.getYRot();
        float targetBody = requested
            ? state.cnpc_immersiveboss$getRequestedRotation()
            : navigationTurn ? state.cnpc_immersiveboss$getNavigationTargetYaw() : npc.yBodyRot;
        float targetHead = requested
            ? state.cnpc_immersiveboss$getRequestedRotation() : npc.yHeadRot;

        int turnDirection = updateRearTurnDirection(
            state, state.cnpc_immersiveboss$getLimitedEntityYaw(), targetEntity);
        float entityYaw = navigationTurn
            ? state.cnpc_immersiveboss$getLimitedEntityYaw()
            : approachDegrees(state.cnpc_immersiveboss$getLimitedEntityYaw(),
                targetEntity, limit, turnDirection);
        float bodyYaw = approachDegrees(state.cnpc_immersiveboss$getLimitedBodyYaw(),
            targetBody, limit, turnDirection);
        float headYaw = approachDegrees(state.cnpc_immersiveboss$getLimitedHeadYaw(),
            targetHead, limit, turnDirection);

        npc.setYRot(entityYaw);
        npc.yBodyRot = bodyYaw;
        npc.yHeadRot = headYaw;
        state.cnpc_immersiveboss$setLimitedYaws(entityYaw, bodyYaw, headYaw);

        if (requested && arrived(entityYaw, targetEntity)
            && arrived(bodyYaw, targetBody) && arrived(headYaw, targetHead)) {
            state.cnpc_immersiveboss$clearRequestedRotation();
        }
    }

    private static float approachDegrees(float current, float target, float limit,
                                         int preferredDirection) {
        float difference = stableDifference(current, target, preferredDirection);
        return Mth.wrapDegrees(current + Mth.clamp(difference, -limit, limit));
    }

    private static float stableDifference(float current, float target, int preferredDirection) {
        float difference = Mth.wrapDegrees(target - current);
        if (preferredDirection > 0 && difference < 0.0F
            && Math.abs(difference) >= REAR_LOCK_EXIT_DEGREES) {
            return difference + 360.0F;
        }
        if (preferredDirection < 0 && difference > 0.0F
            && Math.abs(difference) >= REAR_LOCK_EXIT_DEGREES) {
            return difference - 360.0F;
        }
        return difference;
    }

    private static int updateRearTurnDirection(INpcTurnState state, float current,
                                                float target) {
        float difference = Mth.wrapDegrees(target - current);
        float absolute = Math.abs(difference);
        int direction = state.cnpc_immersiveboss$getRearTurnDirection();

        if (direction == 0 && absolute >= REAR_LOCK_ENTER_DEGREES) {
            direction = difference > 0.0F ? 1 : -1;
            state.cnpc_immersiveboss$setRearTurnDirection(direction);
        } else if (direction != 0 && absolute < REAR_LOCK_EXIT_DEGREES) {
            direction = 0;
            state.cnpc_immersiveboss$setRearTurnDirection(0);
        }
        return direction;
    }

    private static float navigationSpeedScale(float remainingDegrees,
                                              float minimumSpeedScale) {
        float alignment = Mth.cos(remainingDegrees * Mth.DEG_TO_RAD);
        return Mth.lerp(Math.max(0.0F, alignment),
            minimumSpeedScale, 1.0F);
    }

    private static boolean arrived(float current, float target) {
        return Math.abs(Mth.wrapDegrees(target - current)) <= ARRIVAL_EPSILON;
    }

    private static void initializeState(EntityNPCInterface npc) {
        if (npc instanceof INpcTurnState state) {
            state.cnpc_immersiveboss$initializeTurnState(
                npc.getYRot(), npc.yBodyRot, npc.yHeadRot);
        }
    }

    private static float getLimit(EntityNPCInterface npc) {
        if (!canConfigure(npc)) return NO_TURN_SPEED_LIMIT;
        IMixinDataAI settings = getSettings(npc);
        if (settings == null) return NO_TURN_SPEED_LIMIT;
        migrateLegacySettings(npc, settings);
        return settings.cnpc_immersiveboss$isRotationLimitEnabled()
            ? settings.cnpc_immersiveboss$getRotationSpeed()
            : NO_TURN_SPEED_LIMIT;
    }

    private static float getMinimumNavigationSpeedScale(EntityNPCInterface npc) {
        IMixinDataAI settings = getSettings(npc);
        return settings != null
            ? settings.cnpc_immersiveboss$getMinimumNavigationSpeedScale()
            : DEFAULT_MINIMUM_NAVIGATION_SPEED_SCALE;
    }

    private static IMixinDataAI getSettings(EntityNPCInterface npc) {
        return npc != null && npc.ais instanceof IMixinDataAI settings ? settings : null;
    }

    private static void migrateLegacySettings(EntityNPCInterface npc,
                                              IMixinDataAI settings) {
        if (npc == null || npc.level().isClientSide
            || !npc.getPersistentData().contains(
                LEGACY_TURN_SPEED_LIMIT_KEY, Tag.TAG_ANY_NUMERIC)) {
            return;
        }

        float legacyLimit = npc.getPersistentData().getFloat(
            LEGACY_TURN_SPEED_LIMIT_KEY);
        if (isValidTurnSpeed(legacyLimit)) {
            settings.cnpc_immersiveboss$setRotationSpeed(legacyLimit);
            settings.cnpc_immersiveboss$setRotationLimitEnabled(true);
        }
        npc.getPersistentData().remove(LEGACY_TURN_SPEED_LIMIT_KEY);
    }

    public static float sanitizeTurnSpeed(float degreesPerTick) {
        return isValidTurnSpeed(degreesPerTick)
            ? degreesPerTick : DEFAULT_TURN_SPEED;
    }

    public static float sanitizeMinimumNavigationSpeedScale(float scale) {
        return isValidMinimumNavigationSpeedScale(scale)
            ? scale : DEFAULT_MINIMUM_NAVIGATION_SPEED_SCALE;
    }

    private static boolean isValidTurnSpeed(float degreesPerTick) {
        return Float.isFinite(degreesPerTick)
            && degreesPerTick >= 0.0F && degreesPerTick <= MAX_TURN_SPEED;
    }

    private static boolean isValidMinimumNavigationSpeedScale(float scale) {
        return Float.isFinite(scale) && scale >= 0.0F && scale <= 1.0F;
    }

    private static EntityNPCInterface getNpc(ICustomNpc wrapper) {
        if (wrapper == null) return null;
        return wrapper.getMCEntity() instanceof EntityNPCInterface npc ? npc : null;
    }

    private static boolean canConfigure(EntityNPCInterface npc) {
        return npc != null && !npc.level().isClientSide && !npc.isRemoved();
    }

    private static boolean isImmediateRotation() {
        return Boolean.TRUE.equals(IMMEDIATE_ROTATION.get());
    }
}
