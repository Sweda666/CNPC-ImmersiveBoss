package sweda.cnpc_immersiveboss.hitbox;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.phys.Vec3;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Parses .geo.json files to extract collision box bone definitions.
 * Bones prefixed with "hb_" are physical hitboxes, "hs_" are sensors.
 */
public final class GeoHitboxParser {

    // Thread-safe: read from both the client render thread and the server tick thread.
    private static final Map<CacheKey, List<GeoHitboxDef>> CACHE = new ConcurrentHashMap<>();

    private GeoHitboxParser() {}

    /**
     * Parse a .geo.json and return all hb_ and hs_ bone definitions.
     * Tries ResourceManager first (covers mod assets + resource packs),
     * falls back to classpath (covers mod JAR on server).
     * Results are cached by model ResourceLocation.
     */
    public static List<GeoHitboxDef> parse(ResourceLocation geoJsonLocation, ResourceManager resourceManager) {
        CacheKey cacheKey = new CacheKey(geoJsonLocation, resourceManager);
        List<GeoHitboxDef> cached = CACHE.get(cacheKey);
        if (cached != null) return cached;

        List<GeoHitboxDef> result = new ArrayList<>();
        try {
            InputStream stream = null;

            // 1) Try ResourceManager (covers resource packs, mod assets on client)
            if (resourceManager != null) {
                Optional<Resource> optResource = resourceManager.getResource(geoJsonLocation);
                if (optResource.isPresent()) {
                    stream = optResource.get().open();
                }
            }

            // 2) Fall back to classpath
            if (stream == null) {
                String classpath = "assets/" + geoJsonLocation.getNamespace() + "/" + geoJsonLocation.getPath();
                stream = GeoHitboxParser.class.getClassLoader().getResourceAsStream(classpath);
            }

            if (stream == null) {
                CACHE.put(cacheKey, result);
                return result;
            }

            JsonObject root;
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                root = JsonParser.parseReader(reader).getAsJsonObject();
            }

            extractHitboxes(root, result);
        } catch (Exception e) {
            // Return whatever was parsed so far (or empty)
        }

        CACHE.put(cacheKey, result);
        return result;
    }

    /**
     * Get hitbox bone names for a model. Parses via ResourceManager first if not cached.
     */
    public static Set<String> getHitboxBoneNames(ResourceLocation geoJsonLocation, ResourceManager resourceManager) {
        CacheKey cacheKey = new CacheKey(geoJsonLocation, resourceManager);
        List<GeoHitboxDef> defs = CACHE.get(cacheKey);
        // Empty is a valid negative cache entry. Resource reload clears it.
        if (defs == null) defs = parse(geoJsonLocation, resourceManager);
        if (defs == null || defs.isEmpty()) return Collections.emptySet();
        Set<String> names = new HashSet<>();
        for (GeoHitboxDef def : defs) {
            names.add(def.boneName);
        }
        return names;
    }

    private static void extractHitboxes(JsonObject root, List<GeoHitboxDef> result) {
        JsonArray geometries = root.getAsJsonArray("minecraft:geometry");
        if (geometries == null || geometries.isEmpty()) return;
        JsonObject firstGeometry = geometries.get(0).getAsJsonObject();
        JsonArray bones = firstGeometry.getAsJsonArray("bones");
        if (bones == null) return;

        // Build a map of bone name → pivot for all bones (needed for static pivot lookup)
        Map<String, Vec3> pivotMap = new HashMap<>();
        for (JsonElement elem : bones) {
            JsonObject bone = elem.getAsJsonObject();
            String name = bone.has("name") ? bone.get("name").getAsString() : "";
            if (name.isEmpty()) continue;
            JsonArray pivotArr = bone.getAsJsonArray("pivot");
            if (pivotArr != null && pivotArr.size() >= 3) {
                pivotMap.put(name, new Vec3(pivotArr.get(0).getAsDouble(), pivotArr.get(1).getAsDouble(), pivotArr.get(2).getAsDouble()));
            }
        }

        // Extract hitbox bones
        for (JsonElement elem : bones) {
            JsonObject bone = elem.getAsJsonObject();
            String name = bone.has("name") ? bone.get("name").getAsString() : "";
            if (name.isEmpty()) continue;

            // Classify using the unified prefix parser (hb_, hs_, hab_, has_, hdb_, hds_, hadb_, hads_)
            GeoHitboxDef.Type type = GeoHitboxDef.classify(name);
            if (type == null) continue;

            JsonArray cubes = bone.getAsJsonArray("cubes");
            if (cubes == null || cubes.isEmpty()) continue;

            // Compute static model-space pivot — in Bedrock geometry format,
            // bone pivots are absolute model-space coordinates (not parent-relative).
            // GeckoLib internally computes the parent-relative offset when building
            // the bone hierarchy, but the .geo.json pivot field is absolute.
            Vec3 staticPivot = pivotMap.getOrDefault(name, Vec3.ZERO);

            // Process ALL cubes in this bone (not just the first).
            // Multi-cube bones get suffixed names: hadb_upbody, hadb_upbody__1, hadb_upbody__2, ...
            for (int ci = 0; ci < cubes.size(); ci++) {
                JsonObject cube = cubes.get(ci).getAsJsonObject();
                JsonArray originArr = cube.getAsJsonArray("origin");
                JsonArray sizeArr = cube.getAsJsonArray("size");
                if (originArr == null || sizeArr == null) continue;
                if (originArr.size() < 3 || sizeArr.size() < 3) continue;
                Vec3 origin = new Vec3(originArr.get(0).getAsDouble(), originArr.get(1).getAsDouble(), originArr.get(2).getAsDouble());
                Vec3 size = new Vec3(sizeArr.get(0).getAsDouble(), sizeArr.get(1).getAsDouble(), sizeArr.get(2).getAsDouble());

                Vec3 cubeRot = null, cubePvt = null;
                JsonArray rotArr = cube.getAsJsonArray("rotation");
                if (rotArr != null && rotArr.size() >= 3) {
                    cubeRot = new Vec3(rotArr.get(0).getAsDouble(), rotArr.get(1).getAsDouble(), rotArr.get(2).getAsDouble());
                }
                if (cubeRot != null) {
                    JsonArray pvtArr = cube.getAsJsonArray("pivot");
                    if (pvtArr != null && pvtArr.size() >= 3) {
                        cubePvt = new Vec3(pvtArr.get(0).getAsDouble(), pvtArr.get(1).getAsDouble(), pvtArr.get(2).getAsDouble());
                    }
                }

                String uniqueName = (ci == 0) ? name : name + "__" + ci;
                result.add(new GeoHitboxDef(uniqueName, name, type.physical, type.render, type.detectable,
                    origin, size, cubePvt, cubeRot, staticPivot));
            }
        }
    }

    /** Clear the parse cache (e.g. on resource reload). */
    public static void clearCache() {
        CACHE.clear();
    }

    /** Invalidate cache entry for a specific model. */
    public static void invalidate(ResourceLocation geoJsonLocation) {
        CACHE.keySet().removeIf(key -> key.location.equals(geoJsonLocation));
    }

    /** Cache entries are scoped to one resource-manager instance. */
    private static final class CacheKey {
        private final ResourceLocation location;
        private final ResourceManager resourceManager;

        private CacheKey(ResourceLocation location, ResourceManager resourceManager) {
            this.location = location;
            this.resourceManager = resourceManager;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof CacheKey key)) return false;
            return location.equals(key.location) && resourceManager == key.resourceManager;
        }

        @Override
        public int hashCode() {
            return 31 * location.hashCode() + System.identityHashCode(resourceManager);
        }
    }
}
