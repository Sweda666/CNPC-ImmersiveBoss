package sweda.cnpc_immersiveboss.hitbox;

import com.goodbird.cnpcgeckoaddon.data.CustomModelData;
import com.goodbird.cnpcgeckoaddon.mixin.IDataDisplay;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataDisplay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Server-side storage for hitbox definitions synced from client.
 * Key: entityId of the NPC (EntityNPCInterface.getId()).
 * Entries are tied to the model they were parsed from — a model change
 * invalidates the cached defs so they get re-synced / re-parsed.
 */
public final class ServerHitboxData {

    private static final Map<Integer, ResourceLocation> MODELS = new HashMap<>();
    private static final Map<Integer, List<GeoHitboxDef>> DATA = new HashMap<>();

    private ServerHitboxData() {}

    public static void put(int entityId, ResourceLocation model, List<GeoHitboxDef> defs) {
        DATA.put(entityId, defs);
        MODELS.put(entityId, model);
    }

    /** Returns cached defs only if they belong to the given model; null otherwise. */
    public static List<GeoHitboxDef> getForModel(int entityId, ResourceLocation model) {
        if (!DATA.containsKey(entityId)) return null;
        ResourceLocation stored = MODELS.get(entityId);
        if (Objects.equals(stored, model)) {
            return DATA.get(entityId);
        }
        return null;
    }

    /**
     * Returns definitions for the NPC's current custom model, parsing the
     * server-side resource when the client has not synced definitions yet.
     * A non-null empty list means the current model has no hitbox bones.
     */
    public static List<GeoHitboxDef> getOrLoadForNpc(EntityNPCInterface npc) {
        if (npc == null || npc.level().isClientSide || !(npc instanceof EntityCustomNpc)) {
            return null;
        }

        ResourceLocation model = getCurrentModel(npc);
        if (model == null) return null;

        List<GeoHitboxDef> defs = getForModel(npc.getId(), model);
        if (defs != null) return defs;

        MinecraftServer server = npc.level().getServer();
        if (server == null) return null;
        try {
            defs = GeoHitboxParser.parse(model, server.getResourceManager());
        } catch (Exception ignored) {
            defs = new ArrayList<>();
        }
        if (defs == null) defs = new ArrayList<>();
        put(npc.getId(), model, defs);
        return defs;
    }

    private static ResourceLocation getCurrentModel(EntityNPCInterface npc) {
        DataDisplay display = npc.display;
        if (!(display instanceof IDataDisplay dataDisplay) || !dataDisplay.hasCustomModel()) {
            return null;
        }
        CustomModelData modelData = dataDisplay.getCustomModelData();
        if (modelData == null) return null;
        String modelPath = modelData.getModel();
        return modelPath == null || modelPath.isEmpty()
            ? null
            : ResourceLocation.tryParse(modelPath);
    }

    public static void remove(int entityId) {
        DATA.remove(entityId);
        MODELS.remove(entityId);
    }
}
