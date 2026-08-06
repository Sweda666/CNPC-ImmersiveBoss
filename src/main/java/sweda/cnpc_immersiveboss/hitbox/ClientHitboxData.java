package sweda.cnpc_immersiveboss.hitbox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side storage for hitbox definitions.
 * Populated by the render mixin, read by the tick mixin.
 */
public final class ClientHitboxData {

    private static final Map<Integer, List<GeoHitboxDef>> DATA = new HashMap<>();

    private ClientHitboxData() {}

    public static void put(int entityId, List<GeoHitboxDef> defs) {
        DATA.put(entityId, defs);
    }

    public static List<GeoHitboxDef> get(int entityId) {
        return DATA.get(entityId);
    }
}
