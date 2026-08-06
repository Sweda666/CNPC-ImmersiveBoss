package sweda.cnpc_immersiveboss.hitbox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Server-side storage for hitbox definitions synced from client.
 * Key: entityId of the NPC (EntityNPCInterface.getId()).
 */
public final class ServerHitboxData {

    private static final Map<Integer, List<GeoHitboxDef>> DATA = new HashMap<>();

    private ServerHitboxData() {}

    public static void put(int entityId, List<GeoHitboxDef> defs) {
        DATA.put(entityId, defs);
    }

    public static List<GeoHitboxDef> get(int entityId) {
        return DATA.get(entityId);
    }

    public static void remove(int entityId) {
        DATA.remove(entityId);
    }
}
