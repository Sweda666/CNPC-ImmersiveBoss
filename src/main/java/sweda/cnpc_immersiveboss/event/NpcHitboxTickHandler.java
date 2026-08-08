package sweda.cnpc_immersiveboss.event;

import com.goodbird.cnpcgeckoaddon.data.CustomModelData;
import com.goodbird.cnpcgeckoaddon.mixin.IDataDisplay;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.AABB;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataDisplay;
import sweda.cnpc_immersiveboss.api.IOBBHolder;
import sweda.cnpc_immersiveboss.hitbox.ClientHitboxData;
import sweda.cnpc_immersiveboss.hitbox.GeoHitboxDef;
import sweda.cnpc_immersiveboss.hitbox.GeoHitboxParser;
import sweda.cnpc_immersiveboss.hitbox.OBBPhysics;
import sweda.cnpc_immersiveboss.hitbox.ServerHitboxData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Per-tick hitbox maintenance for NPCs, shared by the mixin injector
 * ({@code MixinEntityNPCInterface.tick}) and a server-side fallback call from
 * {@link EntityCollisionListener} (Forge 1.20.1 has no EntityTickEvent). The
 * fallback keeps cleanup + static-AABB behavior working on CustomNPCs builds
 * where {@code EntityNPCInterface} has no resolvable {@code tick()}/{@code remove()}
 * for mixin injection (both paths are idempotent, so running them together is
 * harmless).
 */
public class NpcHitboxTickHandler {

    /** Full per-tick hitbox maintenance (idempotent). */
    public static void onNpcTick(EntityNPCInterface self) {
        if (!(self instanceof IOBBHolder holder)) return;

        // Entity fully removed: purge every per-entity cache.
        if (self.isRemoved()) {
            clearEntityState(self);
            return;
        }
        // Dead NPC: drop all OBB collision data immediately — hitbox raycasts,
        // wireframes and entity collision vanish with the death (AABB stays default).
        if (self.isDeadOrDying()) {
            holder.cnpc_immersiveboss$setBoneOBBs(Collections.emptyMap());
            return;
        }
        // If animated OBBs exist, don't touch the AABB.
        if (!holder.cnpc_immersiveboss$getBoneOBBs().isEmpty()) return;

        // Static fallback: compute AABB from GeoHitboxDef, validated against the
        // current model so switching the NPC model invalidates stale defs.
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
        // 与 MixinRenderCustomModel 一致：GeckoLib 渲染 yaw = 180 − yBodyRot（模型前向 −Z）
        float yawRad = (float) Math.toRadians(180.0 - self.yBodyRot);
        double bbToWorld = size / 80.0;

        AABB fallback = OBBPhysics.staticFallbackAABB(self, defs, bbToWorld, yawRad);
        if (fallback != null) {
            self.setBoundingBox(fallback);
        }
    }

    /** Purges all per-entity hitbox state (OBBs + static caches). */
    public static void clearEntityState(EntityNPCInterface self) {
        if (self instanceof IOBBHolder holder) {
            holder.cnpc_immersiveboss$setBoneOBBs(Collections.emptyMap());
        }
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

    private static List<GeoHitboxDef> parseHitboxDefs(ResourceLocation modelRL, EntityNPCInterface npc) {
        try {
            MinecraftServer server = npc.level().getServer();
            if (server == null) return new ArrayList<>();
            return GeoHitboxParser.parse(modelRL, server.getResourceManager());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
