package sweda.cnpc_immersiveboss.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import sweda.cnpc_immersiveboss.client.StruggleClientState;
import java.util.function.Supplier;

public record SyncStruggleProgressPacket(int sequence, int cycles) {
    public static void encode(SyncStruggleProgressPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.sequence);
        buf.writeVarInt(packet.cycles);
    }

    public static SyncStruggleProgressPacket decode(FriendlyByteBuf buf) {
        return new SyncStruggleProgressPacket(buf.readInt(), buf.readVarInt());
    }

    public static void handle(SyncStruggleProgressPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> StruggleClientState.progress(packet.sequence, packet.cycles));
        context.get().setPacketHandled(true);
    }
}
