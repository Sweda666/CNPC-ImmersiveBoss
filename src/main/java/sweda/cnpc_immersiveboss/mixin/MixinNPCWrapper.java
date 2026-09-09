package sweda.cnpc_immersiveboss.mixin;

import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.wrapper.NPCWrapper;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sweda.cnpc_immersiveboss.api.HitboxDamageCallback;
import sweda.cnpc_immersiveboss.api.IHitboxDamageNpc;
import sweda.cnpc_immersiveboss.api.ImmersiveBossAPI;
import sweda.cnpc_immersiveboss.api.INpcTurnControl;
import sweda.cnpc_immersiveboss.api.IThrowNpc;
import sweda.cnpc_immersiveboss.entity.NpcTurnSpeedManager;

/** Adds ImmersiveBoss script shortcuts directly to {@code e.npc}. */
@Mixin(value = NPCWrapper.class, remap = false)
public abstract class MixinNPCWrapper implements IHitboxDamageNpc, INpcTurnControl, IThrowNpc {

    @Unique
    private boolean cnpc_immersiveboss$restoreRotationAfterSet;

    @Unique
    private float cnpc_immersiveboss$rotationBeforeSet;

    @Unique
    private float cnpc_immersiveboss$bodyRotationBeforeSet;

    @Unique
    private float cnpc_immersiveboss$headRotationBeforeSet;

    @Unique
    private ICustomNpc cnpc_immersiveboss$self() {
        return (ICustomNpc) (Object) this;
    }

    @Inject(method = "setRotation", at = @At("HEAD"), remap = false, require = 0)
    private void cnpc_immersiveboss$deferLimitedRotation(float rotation, CallbackInfo ci) {
        ICustomNpc wrapper = cnpc_immersiveboss$self();
        if (!(wrapper.getMCEntity() instanceof EntityNPCInterface npc)
            || !NpcTurnSpeedManager.shouldDeferRotation(npc, rotation)) {
            cnpc_immersiveboss$restoreRotationAfterSet = false;
            return;
        }

        cnpc_immersiveboss$rotationBeforeSet = npc.getYRot();
        cnpc_immersiveboss$bodyRotationBeforeSet = npc.yBodyRot;
        cnpc_immersiveboss$headRotationBeforeSet = npc.yHeadRot;
        cnpc_immersiveboss$restoreRotationAfterSet = true;
    }

    @Inject(method = "setRotation", at = @At("RETURN"), remap = false, require = 0)
    private void cnpc_immersiveboss$restoreLimitedRotation(float rotation, CallbackInfo ci) {
        if (!cnpc_immersiveboss$restoreRotationAfterSet) return;
        cnpc_immersiveboss$restoreRotationAfterSet = false;

        ICustomNpc wrapper = cnpc_immersiveboss$self();
        if (!(wrapper.getMCEntity() instanceof EntityNPCInterface npc)) return;
        npc.setYRot(cnpc_immersiveboss$rotationBeforeSet);
        npc.yBodyRot = cnpc_immersiveboss$bodyRotationBeforeSet;
        npc.yHeadRot = cnpc_immersiveboss$headRotationBeforeSet;
    }

    @Override
    public boolean activateHitboxDamage(String hitboxName, int startDelayTicks) {
        return ImmersiveBossAPI.activateHitboxDamage(
            cnpc_immersiveboss$self(), hitboxName, startDelayTicks);
    }

    @Override
    public boolean activateHitboxDamage(String hitboxName, int startDelayTicks,
                                        HitboxDamageCallback callback) {
        return ImmersiveBossAPI.activateHitboxDamage(
            cnpc_immersiveboss$self(), hitboxName, startDelayTicks, callback);
    }

    @Override
    public boolean activateHitboxDamage(String hitboxName, int startDelayTicks,
                                        int durationTicks) {
        return ImmersiveBossAPI.activateHitboxDamage(
            cnpc_immersiveboss$self(), hitboxName, startDelayTicks, durationTicks);
    }

    @Override
    public boolean activateHitboxDamage(String hitboxName, int startDelayTicks,
                                        int durationTicks, HitboxDamageCallback callback) {
        return ImmersiveBossAPI.activateHitboxDamage(
            cnpc_immersiveboss$self(), hitboxName, startDelayTicks, durationTicks, callback);
    }

    @Override
    public boolean activateHitboxDamage(String hitboxName, int startDelayTicks,
                                        int durationTicks, float damage) {
        return ImmersiveBossAPI.activateHitboxDamage(
            cnpc_immersiveboss$self(), hitboxName, startDelayTicks, durationTicks, damage);
    }

    @Override
    public boolean activateHitboxDamage(String hitboxName, int startDelayTicks,
                                        int durationTicks, float damage,
                                        HitboxDamageCallback callback) {
        return ImmersiveBossAPI.activateHitboxDamage(
            cnpc_immersiveboss$self(), hitboxName, startDelayTicks, durationTicks,
            damage, callback);
    }

    @Override
    public boolean activateHitboxDamage(String hitboxName, int startDelayTicks,
                                        int durationTicks, float damage, int repeatCount) {
        return ImmersiveBossAPI.activateHitboxDamage(
            cnpc_immersiveboss$self(), hitboxName, startDelayTicks, durationTicks,
            damage, repeatCount);
    }

    @Override
    public boolean activateHitboxDamage(String hitboxName, int startDelayTicks,
                                        int durationTicks, float damage, int repeatCount,
                                        HitboxDamageCallback callback) {
        return ImmersiveBossAPI.activateHitboxDamage(
            cnpc_immersiveboss$self(), hitboxName, startDelayTicks, durationTicks,
            damage, repeatCount, callback);
    }

    @Override
    public boolean activateHitboxDamage(String hitboxName, int startDelayTicks,
                                        int durationTicks, float damage, int repeatCount,
                                        int repeatIntervalTicks) {
        return ImmersiveBossAPI.activateHitboxDamage(
            cnpc_immersiveboss$self(), hitboxName, startDelayTicks, durationTicks,
            damage, repeatCount, repeatIntervalTicks);
    }

    @Override
    public boolean activateHitboxDamage(String hitboxName, int startDelayTicks,
                                        int durationTicks, float damage, int repeatCount,
                                        int repeatIntervalTicks,
                                        HitboxDamageCallback callback) {
        return ImmersiveBossAPI.activateHitboxDamage(
            cnpc_immersiveboss$self(), hitboxName, startDelayTicks, durationTicks,
            damage, repeatCount, repeatIntervalTicks, callback);
    }

    @Override
    public boolean activateHitboxDamage(String hitboxName, int startDelayTicks,
                                        int durationTicks, float damage, int repeatCount,
                                        int repeatIntervalTicks, int maxTargets) {
        return ImmersiveBossAPI.activateHitboxDamage(
            cnpc_immersiveboss$self(), hitboxName, startDelayTicks, durationTicks,
            damage, repeatCount, repeatIntervalTicks, maxTargets);
    }

    @Override
    public boolean activateHitboxDamage(String hitboxName, int startDelayTicks,
                                        int durationTicks, float damage, int repeatCount,
                                        int repeatIntervalTicks, int maxTargets,
                                        HitboxDamageCallback callback) {
        return ImmersiveBossAPI.activateHitboxDamage(
            cnpc_immersiveboss$self(), hitboxName, startDelayTicks, durationTicks,
            damage, repeatCount, repeatIntervalTicks, maxTargets, callback);
    }

    @Override
    public boolean cancelHitboxDamageWindow(String hitboxName) {
        return ImmersiveBossAPI.cancelHitboxDamageWindow(
            cnpc_immersiveboss$self(), hitboxName);
    }

    @Override
    public int cancelAllHitboxDamageWindows() {
        return ImmersiveBossAPI.cancelAllHitboxDamageWindows(cnpc_immersiveboss$self());
    }

    @Override
    public boolean isHitboxDamageWindowActive(String hitboxName) {
        return ImmersiveBossAPI.isHitboxDamageWindowActive(
            cnpc_immersiveboss$self(), hitboxName);
    }

    @Override
    public int getHitboxDamageWindowRemainingTicks(String hitboxName) {
        return ImmersiveBossAPI.getHitboxDamageWindowRemainingTicks(
            cnpc_immersiveboss$self(), hitboxName);
    }

    @Override
    public String[] getActiveHitboxDamageWindows() {
        return ImmersiveBossAPI.getActiveHitboxDamageWindows(cnpc_immersiveboss$self());
    }

    @Override
    public boolean setTurnSpeedLimit(float degreesPerTick) {
        return ImmersiveBossAPI.setTurnSpeedLimit(
            cnpc_immersiveboss$self(), degreesPerTick);
    }

    @Override
    public float getTurnSpeedLimit() {
        return ImmersiveBossAPI.getTurnSpeedLimit(cnpc_immersiveboss$self());
    }

    @Override
    public boolean hasTurnSpeedLimit() {
        return ImmersiveBossAPI.hasTurnSpeedLimit(cnpc_immersiveboss$self());
    }

    @Override
    public boolean setTurnSpeedLimitEnabled(boolean enabled) {
        return ImmersiveBossAPI.setTurnSpeedLimitEnabled(
            cnpc_immersiveboss$self(), enabled);
    }

    @Override
    public boolean clearTurnSpeedLimit() {
        return ImmersiveBossAPI.clearTurnSpeedLimit(cnpc_immersiveboss$self());
    }

    @Override
    public float getMinimumNavigationSpeedScale() {
        return ImmersiveBossAPI.getMinimumNavigationSpeedScale(
            cnpc_immersiveboss$self());
    }

    @Override
    public boolean setMinimumNavigationSpeedScale(float scale) {
        return ImmersiveBossAPI.setMinimumNavigationSpeedScale(
            cnpc_immersiveboss$self(), scale);
    }

    @Override
    public void setRotationImmediate(float rotation) {
        ImmersiveBossAPI.setRotationImmediate(cnpc_immersiveboss$self(), rotation);
    }

    @Override
    public boolean startThrow(noppes.npcs.api.entity.IEntity target, String animation,
                              int durationTicks) {
        return ImmersiveBossAPI.startThrow(cnpc_immersiveboss$self(), target,
            animation, durationTicks);
    }

    @Override
    public boolean startThrow(noppes.npcs.api.entity.IEntity target, String animation,
                              int durationTicks, int struggleMode) {
        return ImmersiveBossAPI.startThrow(cnpc_immersiveboss$self(), target, animation,
            durationTicks, struggleMode);
    }

    @Override
    public boolean startThrow(noppes.npcs.api.entity.IEntity target, String animation,
                              int durationTicks, int struggleMode, int difficulty) {
        return ImmersiveBossAPI.startThrow(cnpc_immersiveboss$self(), target, animation,
            durationTicks, struggleMode, difficulty);
    }

    @Override
    public boolean startThrow(noppes.npcs.api.entity.IEntity target, String animation,
                              int durationTicks, int struggleMode, int difficulty,
                              boolean returnToStart) {
        return ImmersiveBossAPI.startThrow(cnpc_immersiveboss$self(), target, animation,
            durationTicks, struggleMode, difficulty, returnToStart);
    }

    @Override
    public boolean startThrow(noppes.npcs.api.entity.IEntity target, String animation,
                              int durationTicks, boolean returnToStart, int struggleMode,
                              int difficulty) {
        return ImmersiveBossAPI.startThrow(cnpc_immersiveboss$self(), target, animation,
            durationTicks, struggleMode, difficulty, returnToStart);
    }

    @Override
    public boolean stopThrow(noppes.npcs.api.entity.IEntity target) {
        return ImmersiveBossAPI.stopThrow(target);
    }

    @Override
    public boolean startThrow(noppes.npcs.api.entity.IEntity target, String animation,
                              int durationTicks, String struggleMode) {
        return ImmersiveBossAPI.startThrow(cnpc_immersiveboss$self(), target,
            animation, durationTicks, struggleMode);
    }

    @Override
    public boolean startThrow(noppes.npcs.api.entity.IEntity target, String animation,
                              int durationTicks, String struggleMode, Integer difficulty) {
        return ImmersiveBossAPI.startThrow(cnpc_immersiveboss$self(), target,
            animation, durationTicks, struggleMode, difficulty);
    }

    @Override
    public boolean startThrow(noppes.npcs.api.entity.IEntity target, String animation,
                              int durationTicks, String struggleMode, Integer difficulty,
                              sweda.cnpc_immersiveboss.api.ThrowCallback onEscape) {
        return ImmersiveBossAPI.startThrow(cnpc_immersiveboss$self(), target,
            animation, durationTicks, struggleMode, difficulty, onEscape);
    }

    @Override
    public boolean startThrow(noppes.npcs.api.entity.IEntity target, String animation,
                              int durationTicks, String struggleMode, Integer difficulty,
                              sweda.cnpc_immersiveboss.api.ThrowCallback onEscape,
                              sweda.cnpc_immersiveboss.api.ThrowCallback onFinish) {
        return ImmersiveBossAPI.startThrow(cnpc_immersiveboss$self(), target,
            animation, durationTicks, struggleMode, difficulty, onEscape, onFinish);
    }

    @Override
    public boolean startThrow(noppes.npcs.api.entity.IEntity target, String animation,
                              int durationTicks, String struggleMode, Integer difficulty,
                              boolean returnToCamera,
                              sweda.cnpc_immersiveboss.api.ThrowCallback onEscape,
                              sweda.cnpc_immersiveboss.api.ThrowCallback onFinish) {
        return ImmersiveBossAPI.startThrow(cnpc_immersiveboss$self(), target,
            animation, durationTicks, struggleMode, difficulty, returnToCamera,
            onEscape, onFinish);
    }

    @Override
    public boolean isThrowActive(noppes.npcs.api.entity.IEntity target) {
        return ImmersiveBossAPI.isThrowActive(target);
    }
}
