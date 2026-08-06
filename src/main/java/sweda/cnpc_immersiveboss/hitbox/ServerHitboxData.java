package sweda.cnpc_immersiveboss.hitbox;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        ResourceLocation stored = MODELS.get(entityId);
        if (stored == null || model == null || stored.equals(model)) {
            return DATA.get(entityId);
        }
        return null;
    }

    public static void remove(int entityId) {
        DATA.remove(entityId);
        MODELS.remove(entityId);
    }
}
