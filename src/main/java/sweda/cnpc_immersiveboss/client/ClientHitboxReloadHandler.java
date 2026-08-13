package sweda.cnpc_immersiveboss.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import sweda.cnpc_immersiveboss.Cnpc_immersiveboss;
import sweda.cnpc_immersiveboss.api.IOBBHolder;
import sweda.cnpc_immersiveboss.hitbox.ClientHitboxData;
import sweda.cnpc_immersiveboss.hitbox.GeoHitboxParser;
import sweda.cnpc_immersiveboss.network.NetworkHandler;
import sweda.cnpc_immersiveboss.network.packet.SyncHitboxPacket;
import sweda.cnpc_immersiveboss.network.packet.SyncOBBPacket;

import java.util.List;
import java.util.Map;

/** Invalidates hitbox state once when the active client resource set changes. */
@Mod.EventBusSubscriber(
    modid = Cnpc_immersiveboss.MODID,
    bus = Mod.EventBusSubscriber.Bus.MOD,
    value = Dist.CLIENT
)
public final class ClientHitboxReloadHandler {

    private ClientHitboxReloadHandler() {}

    @SubscribeEvent
    public static void registerReloadListener(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) resourceManager ->
            Minecraft.getInstance().execute(ClientHitboxReloadHandler::onResourcesReloaded));
    }

    private static void onResourcesReloaded() {
        Minecraft minecraft = Minecraft.getInstance();
        Map<Integer, ResourceLocation> previousModels = ClientHitboxData.snapshotModels();
        GeoHitboxParser.clearCache();
        ClientHitboxData.clearAll();

        if (minecraft.level == null) return;
        boolean canSync = minecraft.getConnection() != null && NetworkHandler.INSTANCE != null;
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof IOBBHolder holder)) continue;
            holder.cnpc_immersiveboss$setBoneOBBs(Map.of());
            if (!canSync) continue;

            int entityId = entity.getId();
            ResourceLocation previousModel = previousModels.get(entityId);
            if (previousModel != null) {
                NetworkHandler.sendToServer(new SyncHitboxPacket(
                    entityId, previousModel.toString(), List.of()));
            }
            NetworkHandler.sendToServer(new SyncOBBPacket(entityId, Map.of()));
        }
    }
}
