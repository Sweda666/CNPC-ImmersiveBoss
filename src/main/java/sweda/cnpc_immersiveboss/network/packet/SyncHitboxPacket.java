package sweda.cnpc_immersiveboss.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import sweda.cnpc_immersiveboss.hitbox.GeoHitboxDef;
import sweda.cnpc_immersiveboss.hitbox.ServerHitboxData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Syncs parsed hitbox definitions from client to server.
 * Sent once per NPC when its GeckoLib model is first rendered.
 * Direction: PLAY_TO_SERVER.
 */
public class SyncHitboxPacket {

    private final int entityId;
    private final List<GeoHitboxDef> hitboxes;

    public SyncHitboxPacket(int entityId, List<GeoHitboxDef> hitboxes) {
        this.entityId = entityId;
        this.hitboxes = hitboxes;
    }

    public static void encode(SyncHitboxPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeInt(msg.hitboxes.size());
        for (GeoHitboxDef def : msg.hitboxes) {
            buf.writeUtf(def.boneName);
            buf.writeBoolean(def.isPhysical);
            buf.writeDouble(def.origin.x);
            buf.writeDouble(def.origin.y);
            buf.writeDouble(def.origin.z);
            buf.writeDouble(def.size.x);
            buf.writeDouble(def.size.y);
            buf.writeDouble(def.size.z);
            buf.writeDouble(def.staticPivot.x);
            buf.writeDouble(def.staticPivot.y);
            buf.writeDouble(def.staticPivot.z);
        }
    }

    public static SyncHitboxPacket decode(FriendlyByteBuf buf) {
        int entityId = buf.readInt();
        int count = buf.readInt();
        List<GeoHitboxDef> hitboxes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String name = buf.readUtf();
            buf.readBoolean(); // legacy isPhysical — now derived from bone name
            Vec3 origin = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            Vec3 size = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            Vec3 staticPivot = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            // Derive all flags from bone name prefix
            GeoHitboxDef.Type type = GeoHitboxDef.classify(name);
            if (type != null) {
                hitboxes.add(new GeoHitboxDef(name, type.physical, type.render, type.detectable, origin, size, null, null, staticPivot));
            }
        }
        return new SyncHitboxPacket(entityId, hitboxes);
    }

    public static void handle(SyncHitboxPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerHitboxData.put(msg.entityId, msg.hitboxes);
        });
        ctx.get().setPacketHandled(true);
    }
}
