package sweda.cnpc_immersiveboss.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import sweda.cnpc_immersiveboss.client.DamageParticleData;

import java.util.function.Supplier;

/** Sends the server-confirmed OBB hit to clients before vanilla damage effects. */
public final class SyncDamageParticlePacket {
    private final int entityId;
    private final String hitboxName;

    public SyncDamageParticlePacket(int entityId, String hitboxName) {
        this.entityId = entityId;
        this.hitboxName = hitboxName;
    }

    public static void encode(SyncDamageParticlePacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeUtf(msg.hitboxName == null ? "" : msg.hitboxName);
    }

    public static SyncDamageParticlePacket decode(FriendlyByteBuf buf) {
        return new SyncDamageParticlePacket(buf.readInt(), buf.readUtf());
    }

    public static void handle(SyncDamageParticlePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DamageParticleData.record(msg.entityId, msg.hitboxName));
        ctx.get().setPacketHandled(true);
    }
}
