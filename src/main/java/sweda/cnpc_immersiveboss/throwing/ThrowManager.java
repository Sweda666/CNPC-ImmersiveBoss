package sweda.cnpc_immersiveboss.throwing;

import com.goodbird.cnpcgeckoaddon.entity.EntityCustomModel;
import com.goodbird.cnpcgeckoaddon.network.NetworkWrapper;
import com.goodbird.cnpcgeckoaddon.network.PacketSyncAnimation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.api.NpcAPI;
import sweda.cnpc_immersiveboss.api.ThrowCallback;
import sweda.cnpc_immersiveboss.network.NetworkHandler;
import sweda.cnpc_immersiveboss.network.packet.SyncThrowPacket;
import sweda.cnpc_immersiveboss.network.packet.SyncStruggleProgressPacket;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;

/** Server-authoritative lifecycle for a scripted GeckoLib throw animation. */
public final class ThrowManager {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final Map<Integer, ThrowState> ACTIVE = new HashMap<>();
    private static int nextSequence;

    private ThrowManager() {
    }

    public static boolean start(EntityNPCInterface npc, Entity target,
                                String animation, int durationTicks) {
        return start(npc, target, animation, durationTicks, null, null, null, null);
    }

    public static boolean start(EntityNPCInterface npc, Entity target, String animation,
                                int durationTicks, String struggleMode, Integer difficulty,
                                ThrowCallback onEscape, ThrowCallback onFinish) {
        return start(npc, target, animation, durationTicks, struggleMode, difficulty, false,
            onEscape, onFinish);
    }

    public static boolean start(EntityNPCInterface npc, Entity target, String animation,
                                int durationTicks, String struggleMode, Integer difficulty,
                                boolean returnToStart, ThrowCallback onEscape, ThrowCallback onFinish) {
        if (npc == null || !(target instanceof ServerPlayer player)
            || animation == null || animation.trim().isEmpty()
            || durationTicks <= 0 || npc.level().isClientSide
            || target.level() != npc.level() || !npc.isAlive() || !target.isAlive()
            || player.isDeadOrDying() || player.getHealth() <= 0.0F) {
            return false;
        }

        EntityCustomModel model = customModel(npc);
        if (model == null) return false;

        StruggleProgress struggle = new StruggleProgress(StruggleMode.parse(struggleMode),
            difficulty == null ? StruggleProgress.DEFAULT_DIFFICULTY : difficulty);
        // One Gecko model can only animate one victim at a time.
        for (ThrowState active : ACTIVE.values()) {
            if (active.npc == npc && active.target != player) return false;
        }

        stop(player.getId());

        ThrowState state = new ThrowState(
            ++nextSequence, npc, player, animation.trim(), durationTicks,
            player.position(), player.getDeltaMovement(), player.isNoGravity(),
            player.getYRot(), player.getXRot(), struggle, returnToStart, onEscape, onFinish);
        ACTIVE.put(player.getId(), state);

        // Reuse the addon's existing animation sync path so every client sees
        // exactly the same NPC animation. Mark the attack as dealt to prevent
        // the addon's normal melee callback from adding an unwanted hit.
        model.startAttackAnimation(state.animation, 0.0F, player);
        model.attackAnimStartTick = npc.tickCount;
        model.attackDamageDealt = true;

        player.setNoGravity(true);
        player.setDeltaMovement(Vec3.ZERO);
        player.teleportTo(npc.getX(), npc.getY(), npc.getZ());
        // The packet keeps the original camera_root field for wire
        // compatibility. The client additionally resolves the standard
        // camera_second_person and camera_third_person bones when present.
        broadcast(state, SyncThrowPacket.start(
            state.sequence, npc.getId(), player.getId(), npc.level().getGameTime(),
            durationTicks, "camera_root", struggle.mode(), struggle.difficulty()));
        return true;
    }

    public static boolean start(EntityNPCInterface npc, Entity target, String animation,
                                int durationTicks, int struggleMode, int difficulty,
                                ThrowCallback onEscape, ThrowCallback onFinish) {
        return start(npc, target, animation, durationTicks, String.valueOf(struggleMode),
            difficulty, onEscape, onFinish);
    }

    public static boolean start(EntityNPCInterface npc, Entity target, String animation,
                                int durationTicks, int struggleMode, int difficulty,
                                boolean returnToStart, ThrowCallback onEscape,
                                ThrowCallback onFinish) {
        return start(npc, target, animation, durationTicks, String.valueOf(struggleMode),
            difficulty, returnToStart, onEscape, onFinish);
    }

    public static boolean stop(Entity target) {
        return target != null && stop(target.getId());
    }

    public static boolean stop(int targetId) {
        ThrowState state = ACTIVE.remove(targetId);
        if (state == null) return false;

        finish(state, null);
        return true;
    }

    public static boolean isActive(Entity target) {
        return target != null && ACTIVE.containsKey(target.getId());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ACTIVE.isEmpty()) return;

        // Callbacks may start/stop throws, including throws for other players.
        for (ThrowState state : new ArrayList<>(ACTIVE.values())) {
            if (ACTIVE.get(state.target.getId()) != state) continue;
            long elapsed = state.level.getGameTime() - state.startTick;
            boolean valid = isValid(state);
            if (!valid || elapsed >= state.durationTicks) {
                ACTIVE.remove(state.target.getId(), state);
                finish(state, valid ? state.onFinish : null);
                continue;
            }

            // The real player is hidden on the client while the Gecko puppet is
            // rendered. Keeping its server position on the NPC prevents motion
            // and fall damage from fighting the throw animation.
            state.target.setNoGravity(true);
            state.target.setDeltaMovement(Vec3.ZERO);
            state.target.teleportTo(state.npc.getX(), state.npc.getY(), state.npc.getZ());
            state.target.setYRot(state.npc.getYRot());
        }
    }

    public static void struggle(ServerPlayer player, int sequence, int key, boolean pressed) {
        ThrowState state = ACTIVE.get(player.getId());
        if (state == null || state.target != player || state.sequence != sequence) return;
        if (!isValid(state) || state.level.getGameTime() - state.startTick >= state.durationTicks) return;
        if (!state.struggle.input(key, pressed, state.level.getGameTime())) return;
        if (state.struggle.complete()) {
            ACTIVE.remove(player.getId(), state);
            finish(state, state.onEscape);
        } else {
            NetworkHandler.INSTANCE.sendTo(new SyncStruggleProgressPacket(sequence, state.struggle.cycles()),
                player.connection.connection, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
        }
    }

    private static boolean isValid(ThrowState state) {
        return state.npc.isAlive() && !state.npc.isRemoved() && state.target.isAlive()
            && !state.target.isRemoved() && !state.target.isDeadOrDying()
            && state.target.getHealth() > 0.0F
            && state.target.level() == state.level && state.npc.level() == state.level;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        stop(event.getEntity());
    }

    /** Stop immediately when lethal damage starts the player's death sequence. */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            stop(player);
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        stop(event.getEntity());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        for (ThrowState state : new ArrayList<>(ACTIVE.values())) stop(state.target);
    }

    private static void finish(ThrowState state, ThrowCallback callback) {
        EntityCustomModel model = customModel(state.npc);
        if (model != null) {
            model.resetAttackState();
            model.manualAnimName = null;
            clearClientAnimation(state.npc, model);
        }
        state.target.setNoGravity(state.previousNoGravity);
        state.target.fallDistance = 0;
        if (state.target.isAlive() && state.target.level() == state.level) {
            state.target.setDeltaMovement(state.previousMotion);
            if (state.returnToStart) {
                state.target.teleportTo(state.previousPosition.x, state.previousPosition.y,
                    state.previousPosition.z);
            }
            state.target.setYRot(state.previousYRot);
            state.target.setXRot(state.previousXRot);
        }
        broadcast(state, SyncThrowPacket.stop(
            state.sequence, state.npc.getId(), state.target.getId()));
        if (callback != null) {
            try {
                callback.onEnd(state.npc.wrappedNPC, NpcAPI.Instance().getIEntity(state.target));
            } catch (Exception exception) {
                LOGGER.error("Throw callback failed for NPC {} and player {}",
                    state.npc.getUUID(), state.target.getUUID(), exception);
            }
        }
    }

    private static void broadcast(ThrowState state, SyncThrowPacket packet) {
        NetworkHandler.sendToTrackingAndSelf(state.npc, packet);
        NetworkHandler.sendToPlayer(state.target, packet);
    }

    private static void clearClientAnimation(EntityNPCInterface npc, EntityCustomModel model) {
        if (npc.level().isClientSide || model.owner == null) return;
        NetworkWrapper.sendToAll(new PacketSyncAnimation(npc.getId(), null, false));
    }

    private static EntityCustomModel customModel(EntityNPCInterface npc) {
        if (!(npc instanceof EntityCustomNpc customNpc) || customNpc.modelData == null) {
            return null;
        }
        Entity entity = customNpc.modelData.getEntity(npc);
        return entity instanceof EntityCustomModel model ? model : null;
    }

    private static final class ThrowState {
        private final int sequence;
        private final EntityNPCInterface npc;
        private final ServerPlayer target;
        private final String animation;
        private final int durationTicks;
        private final long startTick;
        private final Vec3 previousPosition;
        private final Vec3 previousMotion;
        private final boolean previousNoGravity;
        private final float previousYRot;
        private final float previousXRot;
        private final ServerLevel level;
        private final StruggleProgress struggle;
        private final boolean returnToStart;
        private final ThrowCallback onEscape;
        private final ThrowCallback onFinish;

        private ThrowState(int sequence, EntityNPCInterface npc, ServerPlayer target,
                           String animation, int durationTicks, Vec3 previousPosition,
                           Vec3 previousMotion, boolean previousNoGravity,
                           float previousYRot, float previousXRot, StruggleProgress struggle,
                           boolean returnToStart, ThrowCallback onEscape, ThrowCallback onFinish) {
            this.sequence = sequence;
            this.npc = npc;
            this.target = target;
            this.animation = animation;
            this.durationTicks = durationTicks;
            this.startTick = npc.level().getGameTime();
            this.previousPosition = previousPosition;
            this.previousMotion = previousMotion;
            this.previousNoGravity = previousNoGravity;
            this.previousYRot = previousYRot;
            this.previousXRot = previousXRot;
            this.level = target.serverLevel();
            this.struggle = struggle;
            this.returnToStart = returnToStart;
            this.onEscape = onEscape;
            this.onFinish = onFinish;
        }
    }
}
