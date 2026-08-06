package sweda.cnpc_immersiveboss.network;

import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;
import sweda.cnpc_immersiveboss.network.packet.SyncCustomBossBarPacket;
import sweda.cnpc_immersiveboss.network.packet.SyncHitboxPacket;
import sweda.cnpc_immersiveboss.network.packet.SyncOBBPacket;

public class NetworkHandler {

    public static SimpleChannel INSTANCE;

    private static int packetId = 0;

    public static void register() {
        INSTANCE.messageBuilder(SyncCustomBossBarPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncCustomBossBarPacket::encode)
                .decoder(SyncCustomBossBarPacket::decode)
                .consumerMainThread(SyncCustomBossBarPacket::handle)
                .add();

        INSTANCE.messageBuilder(SyncHitboxPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SyncHitboxPacket::encode)
                .decoder(SyncHitboxPacket::decode)
                .consumerMainThread(SyncHitboxPacket::handle)
                .add();

        INSTANCE.messageBuilder(SyncOBBPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SyncOBBPacket::encode)
                .decoder(SyncOBBPacket::decode)
                .consumerMainThread(SyncOBBPacket::handle)
                .add();
    }

    public static void sendToPlayer(net.minecraft.server.level.ServerPlayer player, SyncCustomBossBarPacket packet) {
        INSTANCE.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void sendToServer(SyncHitboxPacket packet) {
        INSTANCE.sendToServer(packet);
    }

    public static void sendToServer(SyncOBBPacket packet) {
        INSTANCE.sendToServer(packet);
    }
}
