package sweda.cnpc_immersiveboss.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import sweda.cnpc_immersiveboss.client.ThrowClientState;
import sweda.cnpc_immersiveboss.throwing.StruggleMode;

import java.util.function.Supplier;

/** Synchronizes the client-side throw puppet and camera timeline. */
public final class SyncThrowPacket {
    private final int sequence;
    private final int npcId;
    private final int targetId;
    private final long startTick;
    private final int durationTicks;
    private final String cameraBone;
    private final boolean active;
    private final StruggleMode struggleMode;
    private final int difficulty;

    private SyncThrowPacket(int sequence, int npcId, int targetId, long startTick,
                            int durationTicks, String cameraBone, boolean active,
                            StruggleMode struggleMode, int difficulty) {
        this.sequence = sequence;
        this.npcId = npcId;
        this.targetId = targetId;
        this.startTick = startTick;
        this.durationTicks = durationTicks;
        this.cameraBone = cameraBone;
        this.active = active;
        this.struggleMode = struggleMode;
        this.difficulty = difficulty;
    }

    public static SyncThrowPacket start(int sequence, int npcId, int targetId,
                                        long startTick, int durationTicks,
                                        String cameraBone, StruggleMode struggleMode, int difficulty) {
        return new SyncThrowPacket(sequence, npcId, targetId, startTick,
            durationTicks, cameraBone, true, struggleMode, difficulty);
    }

    public static SyncThrowPacket stop(int sequence, int npcId, int targetId) {
        return new SyncThrowPacket(sequence, npcId, targetId, 0, 0,
            "camera_root", false, StruggleMode.NONE, 5);
    }

    public static void encode(SyncThrowPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.sequence);
        buf.writeInt(packet.npcId);
        buf.writeInt(packet.targetId);
        buf.writeLong(packet.startTick);
        buf.writeInt(packet.durationTicks);
        buf.writeUtf(packet.cameraBone, 64);
        buf.writeBoolean(packet.active);
        buf.writeEnum(packet.struggleMode);
        buf.writeVarInt(packet.difficulty);
    }

    public static SyncThrowPacket decode(FriendlyByteBuf buf) {
        return new SyncThrowPacket(buf.readInt(), buf.readInt(), buf.readInt(),
            buf.readLong(), buf.readInt(), buf.readUtf(64), buf.readBoolean(),
            buf.readEnum(StruggleMode.class), buf.readVarInt());
    }

    public static void handle(SyncThrowPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (packet.active) {
                ThrowClientState.start(packet.sequence, packet.npcId, packet.targetId,
                    packet.startTick, packet.durationTicks, packet.cameraBone,
                    packet.struggleMode, packet.difficulty);
            } else {
                ThrowClientState.stop(packet.sequence, packet.targetId);
            }
        });
        context.get().setPacketHandled(true);
    }
}
