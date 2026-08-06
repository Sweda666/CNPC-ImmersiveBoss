package sweda.cnpc_immersiveboss.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import sweda.cnpc_immersiveboss.client.bossbar.ClientBossBarData;

import java.util.function.Supplier;

/**
 * 同步自定义 BossBar 数据的网络包
 */
public class SyncCustomBossBarPacket {

    private final int entityId;
    private final String textureUrl;
    private final int color;
    private final int bossBarMode;
    private final int xShift;
    private final double xScale;
    private final double yScale;
    private final int width;
    private final int height;
    private final boolean combatActive; // 服务端告知客户端此 NPC 正在战斗中

    public SyncCustomBossBarPacket(int entityId, String textureUrl, int color, int xShift, int bossBarMode,
                                   double xScale, double yScale, int width, int height, boolean combatActive) {
        this.entityId = entityId;
        this.textureUrl = textureUrl;
        this.color = color;
        this.xShift = xShift;
        this.bossBarMode = bossBarMode;
        this.xScale = xScale;
        this.yScale = yScale;
        this.width = width;
        this.height = height;
        this.combatActive = combatActive;
    }

    public static void encode(SyncCustomBossBarPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeUtf(msg.textureUrl != null ? msg.textureUrl : "");
        buf.writeInt(msg.color);
        buf.writeInt(msg.xShift);
        buf.writeByte(msg.bossBarMode);
        buf.writeDouble(msg.xScale);
        buf.writeDouble(msg.yScale);
        buf.writeInt(msg.width);
        buf.writeInt(msg.height);
        buf.writeBoolean(msg.combatActive);
    }

    public static SyncCustomBossBarPacket decode(FriendlyByteBuf buf) {
        return new SyncCustomBossBarPacket(
                buf.readInt(),
                buf.readUtf(),
                buf.readInt(),
                buf.readInt(),
                buf.readByte(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readInt(),
                buf.readInt(),
                buf.readBoolean()
        );
    }

    public static void handle(SyncCustomBossBarPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientBossBarData.updateBossBar(
                    msg.entityId,
                    msg.textureUrl,
                    msg.color,
                    msg.xShift,
                    msg.bossBarMode,
                    msg.xScale,
                    msg.yScale,
                    msg.width,
                    msg.height
            );
            if (msg.combatActive) {
                ClientBossBarData.markCombat(msg.entityId);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
