package sweda.cnpc_immersiveboss.network;

import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;
import sweda.cnpc_immersiveboss.network.packet.SyncCustomBossBarPacket;
import sweda.cnpc_immersiveboss.network.packet.SyncDamageParticlePacket;
import sweda.cnpc_immersiveboss.network.packet.SyncHitboxPacket;
import sweda.cnpc_immersiveboss.network.packet.SyncOBBPacket;
import sweda.cnpc_immersiveboss.network.packet.SyncThrowPacket;
import sweda.cnpc_immersiveboss.network.packet.StruggleInputPacket;
import sweda.cnpc_immersiveboss.network.packet.SyncStruggleProgressPacket;

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

        // Keep the pre-existing packet IDs stable; this new packet is appended.
        INSTANCE.messageBuilder(SyncDamageParticlePacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncDamageParticlePacket::encode)
                .decoder(SyncDamageParticlePacket::decode)
                .consumerMainThread(SyncDamageParticlePacket::handle)
                .add();

        INSTANCE.messageBuilder(SyncThrowPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncThrowPacket::encode)
                .decoder(SyncThrowPacket::decode)
                .consumerMainThread(SyncThrowPacket::handle)
                .add();

        INSTANCE.messageBuilder(StruggleInputPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(StruggleInputPacket::encode)
                .decoder(StruggleInputPacket::decode)
                .consumerMainThread(StruggleInputPacket::handle)
                .add();

        INSTANCE.messageBuilder(SyncStruggleProgressPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncStruggleProgressPacket::encode)
                .decoder(SyncStruggleProgressPacket::decode)
                .consumerMainThread(SyncStruggleProgressPacket::handle)
                .add();
    }

    public static void sendToPlayer(net.minecraft.server.level.ServerPlayer player, SyncCustomBossBarPacket packet) {
        INSTANCE.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void sendToPlayer(net.minecraft.server.level.ServerPlayer player, SyncThrowPacket packet) {
        INSTANCE.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void sendToTrackingAndSelf(net.minecraft.world.entity.Entity entity,
                                             SyncThrowPacket packet) {
        INSTANCE.send(net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
    }

    public static void sendToTrackingAndSelf(net.minecraft.world.entity.Entity entity,
                                             SyncDamageParticlePacket packet) {
        INSTANCE.send(net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
    }

    public static void sendToServer(SyncHitboxPacket packet) {
        INSTANCE.sendToServer(packet);
    }

    public static void sendToServer(SyncOBBPacket packet) {
        INSTANCE.sendToServer(packet);
    }
}
