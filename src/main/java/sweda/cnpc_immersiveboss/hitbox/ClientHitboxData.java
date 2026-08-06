package sweda.cnpc_immersiveboss.hitbox;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side storage for hitbox definitions.
 * Populated by the render mixin, read by the tick mixin.
 * Entries are tied to the model they were parsed from — a model change
 * invalidates the cached defs so they get re-parsed / re-synced.
 */
public final class ClientHitboxData {

    private static final Map<Integer, ResourceLocation> MODELS = new HashMap<>();
    private static final Map<Integer, List<GeoHitboxDef>> DATA = new HashMap<>();

    private ClientHitboxData() {}

    public static void put(int entityId, ResourceLocation model, List<GeoHitboxDef> defs) {
        DATA.put(entityId, defs);
        MODELS.put(entityId, model);
    }

    /** Unchecked lookup (render path — data was just put for the current model). */
    public static List<GeoHitboxDef> get(int entityId) {
        return DATA.get(entityId);
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
