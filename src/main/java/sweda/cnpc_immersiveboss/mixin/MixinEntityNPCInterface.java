package sweda.cnpc_immersiveboss.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import noppes.npcs.EventHooks;
import noppes.npcs.api.event.NpcEvent;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sweda.cnpc_immersiveboss.api.IMixinNpcDamagedEvent;
import sweda.cnpc_immersiveboss.api.IHitboxDamageContext;
import sweda.cnpc_immersiveboss.api.INpcTurnState;
import sweda.cnpc_immersiveboss.api.IOBBHolder;
import sweda.cnpc_immersiveboss.compat.epicfight.EpicFightCompat;
import sweda.cnpc_immersiveboss.entity.NpcTurnSpeedManager;
import sweda.cnpc_immersiveboss.hitbox.GeoHitboxDef;
import sweda.cnpc_immersiveboss.hitbox.OBB;
import sweda.cnpc_immersiveboss.hitbox.OBBPhysics;

import java.util.*;

@Mixin(value = EntityNPCInterface.class, remap = false)
public abstract class MixinEntityNPCInterface implements IOBBHolder, INpcTurnState {

    @Unique
    private final Map<String, OBB> cnpc_multihitbox$boneOBBs = new HashMap<>();

    @Unique
    private String cnpc_multihitbox$lastHitboxName = null;

    @Unique
    private IHitboxDamageContext cnpc_multihitbox$activeDamageContext = null;

    @Unique
    private boolean cnpc_immersiveboss$turnStateInitialized;

    @Unique
    private float cnpc_immersiveboss$limitedEntityYaw;

    @Unique
    private float cnpc_immersiveboss$limitedBodyYaw;

    @Unique
    private float cnpc_immersiveboss$limitedHeadYaw;

    @Unique
    private int cnpc_immersiveboss$rearTurnDirection;

    @Unique
    private long cnpc_immersiveboss$navigationTurnTick = Long.MIN_VALUE;

    @Unique
    private float cnpc_immersiveboss$navigationTargetYaw;

    @Unique
    private float cnpc_immersiveboss$navigationSpeedScale = 1.0F;

    @Unique
    private long cnpc_immersiveboss$lastTurnLimitTick = Long.MIN_VALUE;

    @Unique
    private boolean cnpc_immersiveboss$hasRequestedRotation;

    @Unique
    private float cnpc_immersiveboss$requestedRotation;

    @Override
    public Map<String, OBB> cnpc_immersiveboss$getBoneOBBs() {
        return cnpc_multihitbox$boneOBBs;
    }

    @Override
    public void cnpc_immersiveboss$setBoneOBBs(Map<String, OBB> obbs) {
        cnpc_multihitbox$boneOBBs.clear();
        cnpc_multihitbox$boneOBBs.putAll(obbs);
    }

    @Override
    public void cnpc_immersiveboss$setLastHitboxName(String name) {
        cnpc_multihitbox$lastHitboxName = name;
    }

    @Override
    public String cnpc_immersiveboss$getLastHitboxName() {
        return cnpc_multihitbox$lastHitboxName;
    }

    @Override
    public boolean cnpc_immersiveboss$isTurnStateInitialized() {
        return cnpc_immersiveboss$turnStateInitialized;
    }

    @Override
    public void cnpc_immersiveboss$initializeTurnState(float entityYaw, float bodyYaw,
                                                       float headYaw) {
        cnpc_immersiveboss$limitedEntityYaw = entityYaw;
        cnpc_immersiveboss$limitedBodyYaw = bodyYaw;
        cnpc_immersiveboss$limitedHeadYaw = headYaw;
        cnpc_immersiveboss$turnStateInitialized = true;
    }

    @Override
    public float cnpc_immersiveboss$getLimitedEntityYaw() {
        return cnpc_immersiveboss$limitedEntityYaw;
    }

    @Override
    public float cnpc_immersiveboss$getLimitedBodyYaw() {
        return cnpc_immersiveboss$limitedBodyYaw;
    }

    @Override
    public float cnpc_immersiveboss$getLimitedHeadYaw() {
        return cnpc_immersiveboss$limitedHeadYaw;
    }

    @Override
    public void cnpc_immersiveboss$setLimitedYaws(float entityYaw, float bodyYaw,
                                                  float headYaw) {
        cnpc_immersiveboss$limitedEntityYaw = entityYaw;
        cnpc_immersiveboss$limitedBodyYaw = bodyYaw;
        cnpc_immersiveboss$limitedHeadYaw = headYaw;
    }

    @Override
    public int cnpc_immersiveboss$getRearTurnDirection() {
        return cnpc_immersiveboss$rearTurnDirection;
    }

    @Override
    public void cnpc_immersiveboss$setRearTurnDirection(int direction) {
        cnpc_immersiveboss$rearTurnDirection = Integer.compare(direction, 0);
    }

    @Override
    public long cnpc_immersiveboss$getNavigationTurnTick() {
        return cnpc_immersiveboss$navigationTurnTick;
    }

    @Override
    public float cnpc_immersiveboss$getNavigationTargetYaw() {
        return cnpc_immersiveboss$navigationTargetYaw;
    }

    @Override
    public float cnpc_immersiveboss$getNavigationSpeedScale() {
        return cnpc_immersiveboss$navigationSpeedScale;
    }

    @Override
    public void cnpc_immersiveboss$recordNavigationTurn(long gameTime, float targetYaw,
                                                         float speedScale) {
        cnpc_immersiveboss$navigationTurnTick = gameTime;
        cnpc_immersiveboss$navigationTargetYaw = targetYaw;
        cnpc_immersiveboss$navigationSpeedScale = speedScale;
    }

    @Override
    public long cnpc_immersiveboss$getLastTurnLimitTick() {
        return cnpc_immersiveboss$lastTurnLimitTick;
    }

    @Override
    public void cnpc_immersiveboss$setLastTurnLimitTick(long gameTime) {
        cnpc_immersiveboss$lastTurnLimitTick = gameTime;
    }

    @Override
    public boolean cnpc_immersiveboss$hasRequestedRotation() {
        return cnpc_immersiveboss$hasRequestedRotation;
    }

    @Override
    public float cnpc_immersiveboss$getRequestedRotation() {
        return cnpc_immersiveboss$requestedRotation;
    }

    @Override
    public void cnpc_immersiveboss$setRequestedRotation(float rotation) {
        cnpc_immersiveboss$requestedRotation = rotation;
        cnpc_immersiveboss$hasRequestedRotation = true;
    }

    @Override
    public void cnpc_immersiveboss$clearRequestedRotation() {
        cnpc_immersiveboss$hasRequestedRotation = false;
    }

    @Override
    public void cnpc_immersiveboss$clearTurnState() {
        cnpc_immersiveboss$turnStateInitialized = false;
        cnpc_immersiveboss$rearTurnDirection = 0;
        cnpc_immersiveboss$navigationTurnTick = Long.MIN_VALUE;
        cnpc_immersiveboss$navigationSpeedScale = 1.0F;
        cnpc_immersiveboss$lastTurnLimitTick = Long.MIN_VALUE;
        cnpc_immersiveboss$hasRequestedRotation = false;
    }

    /**
     * Clean up all per-entity hitbox state when the entity is removed.
     * Optional (require=0): builds without a resolvable EntityNPCInterface.remove
     * skip this; NpcHitboxTickHandler (Forge EntityTickEvent) covers cleanup for
     * those builds.
     */
    @Inject(method = "remove", at = @At("HEAD"), remap = false, require = 0)
    private void cnpc_multihitbox$onRemove(net.minecraft.world.entity.Entity.RemovalReason reason, CallbackInfo ci) {
        sweda.cnpc_immersiveboss.event.NpcHitboxTickHandler.clearEntityState((EntityNPCInterface) (Object) this);
    }

    /**
     * Static hitbox fallback + removal/death cleanup.
     * Optional (require=0): builds where tick() is not resolvable skip this
     * injector; the Forge EntityTickEvent path in NpcHitboxTickHandler runs the
     * identical logic (both paths are idempotent).
     */
    @Inject(method = "tick", at = @At("HEAD"), remap = false, require = 0)
    private void cnpc_immersiveboss$prepareTurnLimit(CallbackInfo ci) {
        NpcTurnSpeedManager.prepare((EntityNPCInterface) (Object) this);
    }

    @Inject(method = "tick", at = @At("TAIL"), remap = false, require = 0)
    private void cnpc_multihitbox$onTick(CallbackInfo ci) {
        EntityNPCInterface self = (EntityNPCInterface) (Object) this;
        NpcTurnSpeedManager.apply(self);
        sweda.cnpc_immersiveboss.event.NpcHitboxTickHandler.onNpcTick(self);
    }

    /**
     * Runs at HEAD of {@code EntityNPCInterface.hurt()} — BEFORE CNPC dispatches
     * {@code NpcEvent.DamagedEvent} to scripts. Detects which OBB hitbox was struck.
     * <p>
     * For projectiles: {@link sweda.cnpc_immersiveboss.event.ProjectileOBBListener}
     * already set {@code lastHitboxName} — this method skips the melee raycast.
     * For melee / direct attacks: raycasts the attacker's look ray against OBBs.
     */
    @Inject(method = "hurt", at = @At("HEAD"), remap = false, require = 0)
    private void cnpc_multihitbox$detectHitboxHead(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        EntityNPCInterface self = (EntityNPCInterface) (Object) this;

        cnpc_multihitbox$activeDamageContext = null;
        Entity attacker = source.getEntity();
        String epicFightHitbox = EpicFightCompat.consumeHitbox(attacker, self);
        if (epicFightHitbox != null) {
            cnpc_multihitbox$lastHitboxName = epicFightHitbox;
            return;
        }

        Entity directEntity = source.getDirectEntity();
        if (directEntity instanceof IHitboxDamageContext context) {
            String contextHitbox = context.cnpc_immersiveboss$getActiveHitboxName();
            if (contextHitbox != null) {
                cnpc_multihitbox$lastHitboxName = contextHitbox;
                cnpc_multihitbox$activeDamageContext = context;
                return;
            }
        }

        // Projectile / prior damage: already set by ProjectileOBBListener or ImmersiveBossAPI
        if (cnpc_multihitbox$lastHitboxName != null) return;

        Map<String, OBB> obbs = cnpc_multihitbox$boneOBBs;
        if (obbs.isEmpty()) return;

        // Melee / direct attack: raycast attacker's look ray against attackable OBBs.
        // Only bones with b suffix (physical) or d flag (detectable) are attackable.
        // Sensor-only bones (hs_/has_ — no d, no b) are skipped.
        if (!(attacker instanceof LivingEntity livingAttacker)) return;

        Vec3 eyePos = livingAttacker.getEyePosition(1.0f);
        Vec3 lookVec = livingAttacker.getViewVector(1.0f);
        double reach = (attacker instanceof Player) ? 6.0 : 3.0;
        Vec3 rayEnd = eyePos.add(lookVec.x * reach, lookVec.y * reach, lookVec.z * reach);

        Vec3 pos = self.position();
        String hitPhysicalBone = null;
        String hitDetectableBone = null;
        double closestPhysical = Double.MAX_VALUE;
        double closestDetectable = Double.MAX_VALUE;

        for (Map.Entry<String, OBB> entry : obbs.entrySet()) {
            String name = entry.getKey();
            boolean isPhysical = GeoHitboxDef.isPhysicalBone(name);
            if (!isPhysical && !GeoHitboxDef.isDetectableBone(name)) continue;
            OBB rel = entry.getValue();
            OBB worldOBB = new OBB(
                rel.center.add(pos), rel.halfExtents,
                rel.axisX, rel.axisY, rel.axisZ
            );
            double t = OBBPhysics.intersectRay(worldOBB, eyePos, rayEnd);
            if (t < 0) continue;
            if (isPhysical && t < closestPhysical) {
                closestPhysical = t;
                hitPhysicalBone = name;
            } else if (!isPhysical && t < closestDetectable) {
                closestDetectable = t;
                hitDetectableBone = name;
            }
        }

        String raw = (hitPhysicalBone != null) ? hitPhysicalBone : hitDetectableBone;
        if (raw == null) return; // ray missed every OBB — leave hitboxName null
        cnpc_multihitbox$lastHitboxName = GeoHitboxDef.baseBoneName(raw);
    }

    /**
     * Intercepts the {@code NpcEvent.DamagedEvent} argument passed to
     * {@code EventHooks.onNPCDamaged()} and injects the hitbox name
     * from {@link IOBBHolder} into the event BEFORE the CNPC script runs.
     * <p>
     * This makes {@code e.hitboxName} available in CNPC's {@code damaged(e)} handler.
     */
    @Redirect(method = "hurt",
               at = @At(value = "INVOKE",
                         target = "Lnoppes/npcs/EventHooks;onNPCDamaged(Lnoppes/npcs/entity/EntityNPCInterface;Lnoppes/npcs/api/event/NpcEvent$DamagedEvent;)Z"),
               remap = false,
               require = 0)
    private boolean cnpc_multihitbox$dispatchDamagedEvent(EntityNPCInterface npc,
                                                           NpcEvent.DamagedEvent event) {
        String hitboxName = cnpc_multihitbox$lastHitboxName;
        if (hitboxName != null) {
            ((IMixinNpcDamagedEvent) event).setHitboxName(hitboxName);
        }

        IHitboxDamageContext context = cnpc_multihitbox$activeDamageContext;
        if (context != null && context.cnpc_immersiveboss$hasDamageEventResult()) {
            return context.cnpc_immersiveboss$getDamageEventResult();
        }

        boolean result = EventHooks.onNPCDamaged(npc, event);
        if (context != null) {
            context.cnpc_immersiveboss$setDamageEventResult(result);
        }
        return result;
    }

    @Inject(method = "hurt", at = @At("RETURN"), remap = false, require = 0)
    private void cnpc_multihitbox$clearDamageContext(DamageSource source, float amount,
                                                      CallbackInfoReturnable<Boolean> cir) {
        cnpc_multihitbox$activeDamageContext = null;
    }
}
