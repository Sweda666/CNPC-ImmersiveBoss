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
    /** Models already synced to the server (client → server SyncHitboxPacket state). */
    private static final Map<Integer, ResourceLocation> SYNCED_MODELS = new HashMap<>();

    private ClientHitboxData() {}

    /**
     * Model-change-aware sync gate: returns true (and records the model) when the
     * given entity's model has not been synced to the server yet — callers should
     * then send a fresh SyncHitboxPacket.
     */
    public static boolean shouldSyncModel(int entityId, ResourceLocation model) {
        if (model == null) return false;
        ResourceLocation prev = SYNCED_MODELS.get(entityId);
        if (prev != null && prev.equals(model)) return false;
        SYNCED_MODELS.put(entityId, model);
        return true;
    }

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
        SYNCED_MODELS.remove(entityId);
    }
}
