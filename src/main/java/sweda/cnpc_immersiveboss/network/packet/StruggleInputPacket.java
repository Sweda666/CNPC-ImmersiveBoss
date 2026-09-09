package sweda.cnpc_immersiveboss.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import sweda.cnpc_immersiveboss.throwing.ThrowManager;
import java.util.function.Supplier;

/** Reports a key transition for the sender's current throw, never another player. */
public record StruggleInputPacket(int sequence, int key, boolean pressed) {
    public static void encode(StruggleInputPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.sequence);
        buf.writeByte(packet.key);
        buf.writeBoolean(packet.pressed);
    }

    public static StruggleInputPacket decode(FriendlyByteBuf buf) {
        return new StruggleInputPacket(buf.readInt(), buf.readUnsignedByte(), buf.readBoolean());
    }

    public static void handle(StruggleInputPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            if (ctx.getSender() != null) {
                ThrowManager.struggle(ctx.getSender(), packet.sequence, packet.key, packet.pressed);
            }
        });
        ctx.setPacketHandled(true);
    }
}
