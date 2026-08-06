package sweda.cnpc_immersiveboss.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import noppes.npcs.entity.EntityNPCInterface;
import sweda.cnpc_immersiveboss.api.IOBBHolder;
import sweda.cnpc_immersiveboss.hitbox.OBB;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Syncs per-bone OBBs from client to server each render frame.
 * Direction: PLAY_TO_SERVER.
 *
 * Writes OBB data directly to the entity's IOBBHolder (per-entity storage)
 * instead of the old static BoneWorldData map.
 */
public class SyncOBBPacket {

    private final int entityId;
    private final Map<String, OBB> boneObbs;

    public SyncOBBPacket(int entityId, Map<String, OBB> boneObbs) {
        this.entityId = entityId;
        this.boneObbs = boneObbs;
    }

    public static void encode(SyncOBBPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeInt(msg.boneObbs.size());
        for (Map.Entry<String, OBB> entry : msg.boneObbs.entrySet()) {
            OBB obb = entry.getValue();
            buf.writeUtf(entry.getKey());
            buf.writeDouble(obb.center.x);
            buf.writeDouble(obb.center.y);
            buf.writeDouble(obb.center.z);
            buf.writeDouble(obb.halfExtents.x);
            buf.writeDouble(obb.halfExtents.y);
            buf.writeDouble(obb.halfExtents.z);
            buf.writeFloat((float) obb.axisX.x);
            buf.writeFloat((float) obb.axisX.y);
            buf.writeFloat((float) obb.axisX.z);
            buf.writeFloat((float) obb.axisY.x);
            buf.writeFloat((float) obb.axisY.y);
            buf.writeFloat((float) obb.axisY.z);
            buf.writeFloat((float) obb.axisZ.x);
            buf.writeFloat((float) obb.axisZ.y);
            buf.writeFloat((float) obb.axisZ.z);
        }
    }

    public static SyncOBBPacket decode(FriendlyByteBuf buf) {
        int entityId = buf.readInt();
        int count = buf.readInt();
        Map<String, OBB> obbs = new HashMap<>(count);
        for (int i = 0; i < count; i++) {
            String name = buf.readUtf();
            Vec3 center = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            Vec3 halfExt = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            Vec3 ax = new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat());
            Vec3 ay = new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat());
            Vec3 az = new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat());
            obbs.put(name, new OBB(center, halfExt, ax, ay, az));
        }
        return new SyncOBBPacket(entityId, obbs);
    }

    public static void handle(SyncOBBPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Entity entity = ctx.get().getSender().level().getEntity(msg.entityId);
            if (entity instanceof IOBBHolder holder) {
                holder.cnpc_immersiveboss$setBoneOBBs(msg.boneObbs);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
