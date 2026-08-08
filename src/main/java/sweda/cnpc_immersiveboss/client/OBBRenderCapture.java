package sweda.cnpc_immersiveboss.client;

import com.goodbird.cnpcgeckoaddon.entity.EntityCustomModel;
import com.mojang.logging.LogUtils;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import sweda.cnpc_immersiveboss.hitbox.GeoHitboxDef;
import sweda.cnpc_immersiveboss.hitbox.OBB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class OBBRenderCapture {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ThreadLocal<Capture> ACTIVE = new ThreadLocal<>();
    private static final Set<String> LOGGED_ERRORS = ConcurrentHashMap.newKeySet();

    private OBBRenderCapture() {}

    public static void begin(EntityCustomModel animatable, Matrix4f renderBase,
                             List<GeoHitboxDef> definitions) {
        Map<String, List<GeoHitboxDef>> definitionsByBone = new HashMap<>();
        for (GeoHitboxDef definition : definitions) {
            definitionsByBone.computeIfAbsent(definition.geoBoneName, ignored -> new ArrayList<>())
                .add(definition);
        }

        Matrix4f inverseRenderBase = new Matrix4f(renderBase).invert();
        if (!inverseRenderBase.isFinite()) {
            ACTIVE.remove();
            return;
        }
        ACTIVE.set(new Capture(animatable, inverseRenderBase, definitionsByBone));
    }

    public static void captureBone(EntityCustomModel animatable, GeoBone bone, Matrix4f renderedBoneMatrix) {
        Capture capture = ACTIVE.get();
        if (capture == null || capture.animatable != animatable) return;

        List<GeoHitboxDef> definitions = capture.definitionsByBone.get(bone.getName());
        if (definitions == null || definitions.isEmpty()) return;

        Matrix4f entityRelativeBone = new Matrix4f(capture.inverseRenderBase).mul(renderedBoneMatrix);
        for (GeoHitboxDef definition : definitions) {
            try {
                int cubeIndex = cubeIndex(definition);
                if (cubeIndex < 0 || cubeIndex >= bone.getCubes().size()) continue;
                GeoCube cube = bone.getCubes().get(cubeIndex);
                Matrix4f cubeMatrix = applyCubeTransform(entityRelativeBone, cube);
                OBB obb = OBB.fromGeckoCube(cubeMatrix, cube);
                if (obb != null) capture.obbs.put(definition.boneName, obb);
            } catch (RuntimeException exception) {
                if (LOGGED_ERRORS.add(definition.boneName)) {
                    LOGGER.error("[OBB] Failed to capture cube {}: {}",
                        definition.boneName, exception.toString());
                }
            }
        }
    }

    public static Map<String, OBB> finish(EntityCustomModel animatable) {
        Capture capture = ACTIVE.get();
        ACTIVE.remove();
        if (capture == null || capture.animatable != animatable) return Map.of();
        return capture.obbs;
    }

    public static void discard() {
        ACTIVE.remove();
    }

    private static Matrix4f applyCubeTransform(Matrix4f boneMatrix, GeoCube cube) {
        return new Matrix4f(boneMatrix)
            .translate((float) (cube.pivot().x / 16.0),
                (float) (cube.pivot().y / 16.0), (float) (cube.pivot().z / 16.0))
            .rotateZ((float) cube.rotation().z)
            .rotateY((float) cube.rotation().y)
            .rotateX((float) cube.rotation().x)
            .translate((float) (-cube.pivot().x / 16.0),
                (float) (-cube.pivot().y / 16.0), (float) (-cube.pivot().z / 16.0));
    }

    private static int cubeIndex(GeoHitboxDef definition) {
        if (definition.boneName.equals(definition.geoBoneName)) return 0;
        String prefix = definition.geoBoneName + "__";
        if (!definition.boneName.startsWith(prefix)) return -1;
        return Integer.parseInt(definition.boneName.substring(prefix.length()));
    }

    private static final class Capture {
        private final EntityCustomModel animatable;
        private final Matrix4f inverseRenderBase;
        private final Map<String, List<GeoHitboxDef>> definitionsByBone;
        private final Map<String, OBB> obbs = new HashMap<>();

        private Capture(EntityCustomModel animatable, Matrix4f inverseRenderBase,
                        Map<String, List<GeoHitboxDef>> definitionsByBone) {
            this.animatable = animatable;
            this.inverseRenderBase = inverseRenderBase;
            this.definitionsByBone = definitionsByBone;
        }
    }
}
