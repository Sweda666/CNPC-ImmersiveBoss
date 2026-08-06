package sweda.cnpc_immersiveboss.hitbox;

import com.mojang.logging.LogUtils;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side storage for per-bone OBBs synced from client each frame.
 * Receives animation-aware OBBs and computes the merged entity AABB.
 */
public final class BoneWorldData {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** entityId -> boneName -> OBB */
    private static final Map<Integer, Map<String, OBB>> DATA = new ConcurrentHashMap<>();

    private BoneWorldData() {}

    /** Replace all bone OBBs for an entity. */
    public static void updateOBB(int entityId, Map<String, OBB> boneObbs) {
        if (boneObbs.isEmpty()) {
            DATA.remove(entityId);
        } else {
            DATA.put(entityId, new HashMap<>(boneObbs));
            LOGGER.debug("[OBB-Server] Stored {} OBBs for entity {}", boneObbs.size(), entityId);
        }
    }

    /** Get all per-bone OBBs for an entity (may be empty, never null). */
    public static Map<String, OBB> getOBBs(int entityId) {
        Map<String, OBB> map = DATA.get(entityId);
        return map != null ? map : Collections.emptyMap();
    }

    /**
     * Get all per-bone OBBs converted to world-space at the given entity position.
     * Returns empty list if no data is available.
     */
    public static List<OBB> getWorldOBBs(int entityId, Vec3 entityPos) {
        Map<String, OBB> map = DATA.get(entityId);
        if (map == null || map.isEmpty()) return Collections.emptyList();

        List<OBB> worldObbs = new ArrayList<>(map.size());
        for (OBB rel : map.values()) {
            worldObbs.add(new OBB(
                rel.center.add(entityPos),
                rel.halfExtents,
                rel.axisX, rel.axisY, rel.axisZ
            ));
        }
        return worldObbs;
    }

    /**
     * Compute a merged AABB that encloses all per-bone OBBs at the given entity position.
     * OBBs are stored entity-relative; this method positions them at {@code entityPos}.
     * Returns null if no data is available.
     */
    public static AABB getMerged(int entityId, Vec3 entityPos) {
        Map<String, OBB> map = DATA.get(entityId);
        if (map == null || map.isEmpty()) return null;

        List<OBB> worldObbs = getWorldOBBs(entityId, entityPos);
        return worldObbs.isEmpty() ? null : OBBPhysics.enclosingAABB(worldObbs);
    }

    public static void remove(int entityId) {
        DATA.remove(entityId);
    }

    public static void clear() {
        DATA.clear();
    }
}
