package sweda.cnpc_immersiveboss.mixin;

import com.goodbird.cnpcgeckoaddon.data.CustomModelData;
import com.goodbird.cnpcgeckoaddon.mixin.IDataDisplay;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import noppes.npcs.api.event.NpcEvent;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sweda.cnpc_immersiveboss.api.IMixinNpcDamagedEvent;
import sweda.cnpc_immersiveboss.api.IOBBHolder;
import sweda.cnpc_immersiveboss.event.NpcUpdateListener;
import sweda.cnpc_immersiveboss.hitbox.ClientHitboxData;
import sweda.cnpc_immersiveboss.hitbox.GeoHitboxDef;
import sweda.cnpc_immersiveboss.hitbox.GeoHitboxParser;
import sweda.cnpc_immersiveboss.hitbox.OBB;
import sweda.cnpc_immersiveboss.hitbox.OBBPhysics;
import sweda.cnpc_immersiveboss.hitbox.ServerHitboxData;

import java.util.*;

@Mixin(EntityNPCInterface.class)
public abstract class MixinEntityNPCInterface implements IOBBHolder {

    @Unique
    private final Map<String, OBB> cnpc_multihitbox$boneOBBs = new HashMap<>();

    @Unique
    private String cnpc_multihitbox$lastHitboxName = null;

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

    /** Clean up all per-entity hitbox state when the entity is removed. */
    @Inject(method = "remove", at = @At("HEAD"), remap = false)
    private void cnpc_multihitbox$onRemove(net.minecraft.world.entity.Entity.RemovalReason reason, CallbackInfo ci) {
        EntityNPCInterface self = (EntityNPCInterface) (Object) this;
        cnpc_multihitbox$boneOBBs.clear();
        int id = self.getId();
        if (self.level().isClientSide) {
            // Client caches — classes in these branches are only touched client-side,
            // so referencing client-only classes here is dedicated-server safe.
            ClientHitboxData.remove(id);
            sweda.cnpc_immersiveboss.client.bossbar.ClientBossBarData.remove(id);
        } else {
            ServerHitboxData.remove(id);
            NpcUpdateListener.onEntityRemoved(id);
        }
    }

    /** Static hitbox fallback: only when no animated OBBs from client. */
    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void cnpc_multihitbox$onTick(CallbackInfo ci) {
        EntityNPCInterface self = (EntityNPCInterface) (Object) this;

        // Dead NPC: drop all OBB collision data immediately — hitbox raycasts,
        // wireframes and entity collision vanish with the death (AABB stays default).
        if (self.isDeadOrDying()) {
            cnpc_multihitbox$boneOBBs.clear();
            return;
        }

        // If animated OBBs exist, don't touch the AABB
        if (!cnpc_multihitbox$boneOBBs.isEmpty()) return;

        // Current model path — cached defs must be validated against it so that
        // switching the NPC model invalidates stale hitbox definitions.
        ResourceLocation modelRL = null;
        if (self instanceof EntityCustomNpc) {
            DataDisplay display = self.display;
            if (display instanceof IDataDisplay idDisplay && idDisplay.hasCustomModel()) {
                CustomModelData modelData = idDisplay.getCustomModelData();
                String modelPath = modelData.getModel();
                if (modelPath != null && !modelPath.isEmpty()) {
                    modelRL = ResourceLocation.tryParse(modelPath);
                }
            }
        }
        if (modelRL == null) return;

        List<GeoHitboxDef> defs = self.level().isClientSide
            ? ClientHitboxData.getForModel(self.getId(), modelRL)
            : ServerHitboxData.getForModel(self.getId(), modelRL);
        if (defs == null || defs.isEmpty()) {
            defs = parseHitboxDefs(modelRL, self);
            // Defensive: parse can theoretically yield null under cache races.
            if (defs == null || defs.isEmpty()) return;
            if (self.level().isClientSide) {
                ClientHitboxData.put(self.getId(), modelRL, defs);
            } else {
                ServerHitboxData.put(self.getId(), modelRL, defs);
            }
        }

        float size = self.display.getSize();
        float yawRad = (float) Math.toRadians(self.yBodyRot);
        double bbToWorld = size / 80.0;

        AABB fallback = OBBPhysics.staticFallbackAABB(self, defs, bbToWorld, yawRad);
        if (fallback != null) {
            self.setBoundingBox(fallback);
        }
    }

    @Unique
    private static List<GeoHitboxDef> parseHitboxDefs(ResourceLocation modelRL, EntityNPCInterface npc) {
        try {
            MinecraftServer server = npc.level().getServer();
            if (server == null) return new ArrayList<>();
            ResourceManager rm = server.getResourceManager();
            return GeoHitboxParser.parse(modelRL, rm);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Runs at HEAD of {@code EntityNPCInterface.hurt()} — BEFORE CNPC dispatches
     * {@code NpcEvent.DamagedEvent} to scripts. Detects which OBB hitbox was struck.
     * <p>
     * For projectiles: {@link sweda.cnpc_immersiveboss.event.ProjectileOBBListener}
     * already set {@code lastHitboxName} — this method skips the melee raycast.
     * For melee / direct attacks: raycasts the attacker's look ray against OBBs.
     */
    @Inject(method = "hurt", at = @At("HEAD"), remap = false)
    private void cnpc_multihitbox$detectHitboxHead(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        EntityNPCInterface self = (EntityNPCInterface) (Object) this;

        // Projectile / prior damage: already set by ProjectileOBBListener or ImmersiveBossAPI
        if (cnpc_multihitbox$lastHitboxName != null) return;

        Map<String, OBB> obbs = cnpc_multihitbox$boneOBBs;
        if (obbs.isEmpty()) return;

        // Melee / direct attack: raycast attacker's look ray against attackable OBBs.
        // Only bones with b suffix (physical) or d flag (detectable) are attackable.
        // Sensor-only bones (hs_/has_ — no d, no b) are skipped.
        Entity attacker = source.getEntity();
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
    @ModifyArg(method = "hurt",
               at = @At(value = "INVOKE",
                        target = "Lnoppes/npcs/EventHooks;onNPCDamaged(Lnoppes/npcs/entity/EntityNPCInterface;Lnoppes/npcs/api/event/NpcEvent$DamagedEvent;)Z"),
               index = 1,
               remap = false)
    private NpcEvent.DamagedEvent cnpc_multihitbox$injectHitboxName(NpcEvent.DamagedEvent event) {
        String hitboxName = cnpc_multihitbox$lastHitboxName;
        if (hitboxName != null) {
            ((IMixinNpcDamagedEvent) event).setHitboxName(hitboxName);
        }
        return event;
    }
}
