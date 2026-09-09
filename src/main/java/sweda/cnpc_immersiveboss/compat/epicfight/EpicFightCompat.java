package sweda.cnpc_immersiveboss.compat.epicfight;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import sweda.cnpc_immersiveboss.api.IOBBHolder;
import sweda.cnpc_immersiveboss.compat.ObbCompat;
import sweda.cnpc_immersiveboss.hitbox.GeoHitboxDef;
import sweda.cnpc_immersiveboss.hitbox.OBB;
import sweda.cnpc_immersiveboss.hitbox.OBBPhysics;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Server-side adapters for Epic Fight's transformed attack colliders. */
public final class EpicFightCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ThreadLocal<AttackPass> ATTACK_PASS = new ThreadLocal<>();
    private static boolean reflectionWarningLogged;

    private EpicFightCompat() {
    }

    public static void beginAttackPass(Object livingEntityPatch) {
        try {
            Method getOriginal = livingEntityPatch.getClass().getMethod("getOriginal");
            Object original = getOriginal.invoke(livingEntityPatch);
            ATTACK_PASS.set(original instanceof Entity entity ? new AttackPass(entity) : null);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            ATTACK_PASS.remove();
            logReflectionFailure(exception);
        }
    }

    public static void endAttackPass() {
        ATTACK_PASS.remove();
    }

    /** Expands Epic Fight's broad phase so OBBs outside their owner AABB are tested. */
    public static List<Entity> getEntitiesIncludingObbs(
            Level level, Entity excluded, AABB area,
            Predicate<? super Entity> predicate) {
        List<Entity> nativeTargets = level.getEntities(excluded, area, predicate);
        AttackPass attackPass = ATTACK_PASS.get();
        if (attackPass == null || attackPass.attacker != excluded
            || !(level instanceof ServerLevel serverLevel)) {
            return nativeTargets;
        }

        Map<Integer, Entity> merged = new LinkedHashMap<>();
        for (Entity target : nativeTargets) merged.put(target.getId(), target);
        for (Entity candidate : serverLevel.getAllEntities()) {
            if (candidate == excluded || merged.containsKey(candidate.getId())
                || !ObbCompat.hasAnimatedObbs(candidate)) continue;
            if (predicate.test(candidate)) {
                merged.put(candidate.getId(), candidate);
            }
        }
        return new ArrayList<>(merged.values());
    }

    /** Replaces an entity AABB test with the transformed collider-vs-OBB test. */
    public static Boolean testObbCollision(Object collider, Entity target) {
        AttackPass attackPass = ATTACK_PASS.get();
        if (attackPass == null || !(attackPass.attacker.level() instanceof ServerLevel)
            || !ObbCompat.hasAnimatedObbs(target)) return null;

        try {
            List<CollisionShape> shapes = readShapes(collider);
            if (shapes.isEmpty()) return null;
            String hitbox = findHitbox(target, shapes, attackPass.attacker.position());
            if (hitbox != null) {
                attackPass.hitboxes.putIfAbsent(target.getId(), hitbox);
            }
            return hitbox != null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logReflectionFailure(exception);
            return null;
        }
    }

    /** Consumes the bone selected by the active Epic Fight attack pass. */
    public static String consumeHitbox(Entity attacker, Entity target) {
        AttackPass attackPass = ATTACK_PASS.get();
        if (attackPass == null || attacker == null || target == null
            || attackPass.attacker != attacker) return null;
        return attackPass.hitboxes.remove(target.getId());
    }

    private static String findHitbox(Entity entity, List<CollisionShape> shapes,
                                     Vec3 reference) {
        if (!(entity instanceof IOBBHolder holder)) return null;

        String physical = null;
        String detectable = null;
        double physicalDistance = Double.MAX_VALUE;
        double detectableDistance = Double.MAX_VALUE;
        for (Map.Entry<String, OBB> entry : holder.cnpc_immersiveboss$getBoneOBBs().entrySet()) {
            String name = entry.getKey();
            boolean isPhysical = GeoHitboxDef.isPhysicalBone(name);
            if (!isPhysical && !GeoHitboxDef.isDetectableBone(name)) continue;

            OBB relative = entry.getValue();
            OBB world = new OBB(relative.center.add(entity.position()), relative.halfExtents,
                relative.axisX, relative.axisY, relative.axisZ);
            boolean intersects = false;
            for (CollisionShape shape : shapes) {
                if (shape.intersects(world)) {
                    intersects = true;
                    break;
                }
            }
            if (!intersects) continue;

            double distance = reference.distanceToSqr(ObbCompat.closestPoint(world, reference));
            if (isPhysical && distance < physicalDistance) {
                physical = name;
                physicalDistance = distance;
            } else if (!isPhysical && distance < detectableDistance) {
                detectable = name;
                detectableDistance = distance;
            }
        }

        String selected = physical != null ? physical : detectable;
        return selected != null ? GeoHitboxDef.baseBoneName(selected) : null;
    }

    private static List<CollisionShape> readShapes(Object collider)
        throws ReflectiveOperationException {
        Field verticesField = findField(collider.getClass(), "rotatedVertices", false);
        Field normalsField = findField(collider.getClass(), "rotatedNormals", false);
        if (verticesField != null && normalsField != null) {
            Vec3 center = toWorld((Vec3) readRequired(collider, "worldCenter"));
            Vec3[] vertices = toWorld((Vec3[]) read(verticesField, collider));
            Vec3[] normals = toWorld((Vec3[]) read(normalsField, collider));
            // rotatedVertices already include Epic Fight's animation scale.
            // Do not gate this exact shape with the unscaled outerAABB.
            return List.of(createObbShape(center, vertices, normals));
        }

        Field lineField = findField(collider.getClass(), "worldVec", false);
        if (lineField != null) {
            Vec3 start = toWorld((Vec3) readRequired(collider, "worldCenter"));
            Vec3 direction = toWorld((Vec3) read(lineField, collider));
            Vec3 end = start.add(direction);
            return List.of(obb -> OBBPhysics.intersectRay(obb, start, end) >= 0.0);
        }

        Field planeField = findField(collider.getClass(), "worldPos", false);
        if (planeField != null) {
            Vec3 center = toWorld((Vec3) readRequired(collider, "worldCenter"));
            Vec3[] planeVectors = toWorld((Vec3[]) read(planeField, collider));
            if (planeVectors.length < 2) return List.of();
            Vec3 normal = planeVectors[0].cross(planeVectors[1]);
            return List.of(withBroadPhase(collider,
                obb -> intersectsPlane(obb, center, normal)));
        }

        return List.of();
    }

    private static CollisionShape withBroadPhase(Object collider, CollisionShape shape)
        throws ReflectiveOperationException {
        Field boundsField = findField(collider.getClass(), "outerAABB", false);
        if (boundsField == null || !(read(boundsField, collider) instanceof AABB bounds)) {
            return shape;
        }
        Vec3 center = toWorld((Vec3) readRequired(collider, "worldCenter"));
        AABB worldBounds = bounds.move(center);
        return obb -> OBBPhysics.intersects(obb, worldBounds) && shape.intersects(obb);
    }

    private static CollisionShape createObbShape(Vec3 center, Vec3[] vertices,
                                                  Vec3[] normals) {
        if (vertices.length > 0 && normals.length >= 3) {
            Vec3 axisX = normals[0].normalize();
            Vec3 axisY = normals[1].normalize();
            Vec3 axisZ = normals[2].normalize();
            Vec3 extents = new Vec3(maxProjection(vertices, axisX),
                maxProjection(vertices, axisY), maxProjection(vertices, axisZ));
            OBB attackObb = new OBB(center, extents, axisX, axisY, axisZ);
            return target -> OBBPhysics.intersects(attackObb, target);
        }

        return target -> overlapsOnFaceNormals(center, vertices, normals, target);
    }

    private static boolean overlapsOnFaceNormals(Vec3 center, Vec3[] vertices,
                                                  Vec3[] normals, OBB target) {
        List<Vec3> axes = new ArrayList<>(normals.length + 3);
        for (Vec3 normal : normals) axes.add(normal);
        axes.add(target.axisX);
        axes.add(target.axisY);
        axes.add(target.axisZ);

        Vec3 offset = target.center.subtract(center);
        for (Vec3 axis : axes) {
            if (axis == null || axis.lengthSqr() < 1.0E-20) continue;
            double attackRadius = maxProjection(vertices, axis);
            double targetRadius = Math.abs(target.halfExtents.x * target.axisX.dot(axis))
                + Math.abs(target.halfExtents.y * target.axisY.dot(axis))
                + Math.abs(target.halfExtents.z * target.axisZ.dot(axis));
            if (Math.abs(offset.dot(axis)) > attackRadius + targetRadius) return false;
        }
        return true;
    }

    private static boolean intersectsPlane(OBB obb, Vec3 point, Vec3 normal) {
        if (normal.lengthSqr() < 1.0E-20) return false;
        double radius = Math.abs(obb.halfExtents.x * obb.axisX.dot(normal))
            + Math.abs(obb.halfExtents.y * obb.axisY.dot(normal))
            + Math.abs(obb.halfExtents.z * obb.axisZ.dot(normal));
        return Math.abs(obb.center.subtract(point).dot(normal)) <= radius;
    }

    private static double maxProjection(Vec3[] vertices, Vec3 axis) {
        double maximum = 0.0;
        for (Vec3 vertex : vertices) {
            if (vertex != null) maximum = Math.max(maximum, Math.abs(vertex.dot(axis)));
        }
        return maximum;
    }

    /** Epic Fight mirrors X/Z in its internal collider coordinate system. */
    private static Vec3 toWorld(Vec3 vector) {
        return new Vec3(-vector.x, vector.y, -vector.z);
    }

    private static Vec3[] toWorld(Vec3[] vectors) {
        Vec3[] converted = new Vec3[vectors.length];
        for (int i = 0; i < vectors.length; i++) {
            converted[i] = vectors[i] != null ? toWorld(vectors[i]) : null;
        }
        return converted;
    }

    private static Object readRequired(Object owner, String name)
        throws ReflectiveOperationException {
        Field field = findField(owner.getClass(), name, true);
        return read(field, owner);
    }

    private static Object read(Field field, Object owner) throws IllegalAccessException {
        field.setAccessible(true);
        return field.get(owner);
    }

    private static Field findField(Class<?> type, String name, boolean required)
        throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        if (required) throw new NoSuchFieldException(type.getName() + "." + name);
        return null;
    }

    private static void logReflectionFailure(Exception exception) {
        if (reflectionWarningLogged) return;
        reflectionWarningLogged = true;
        LOGGER.warn("Epic Fight collider layout changed; OBB attack compatibility was skipped",
            exception);
    }

    private interface CollisionShape {
        boolean intersects(OBB obb);
    }

    private static final class AttackPass {
        private final Entity attacker;
        private final Map<Integer, String> hitboxes = new LinkedHashMap<>();

        private AttackPass(Entity attacker) {
            this.attacker = attacker;
        }
    }
}
