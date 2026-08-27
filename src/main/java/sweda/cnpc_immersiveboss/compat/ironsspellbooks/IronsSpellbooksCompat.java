package sweda.cnpc_immersiveboss.compat.ironsspellbooks;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import sweda.cnpc_immersiveboss.api.IOBBHolder;
import sweda.cnpc_immersiveboss.compat.ObbCompat;
import sweda.cnpc_immersiveboss.compat.ObbCompat.RayHit;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** OBB collision adapters for Iron's Spellbooks native attack paths. */
public final class IronsSpellbooksCompat {
    private IronsSpellbooksCompat() {
    }

    /** Reads the optional native inflation method without hard-linking the method in a mixin shadow. */
    public static double hitDetectionInflation(Object projectile) {
        try {
            return ((Number) projectile.getClass().getMethod("getHitDetectionInflation")
                .invoke(projectile)).doubleValue();
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return 0.0;
        }
    }

    /**
     * Applies the OBB raycast to RaycastBuilder while keeping all third-party
     * member lookups optional across Iron's Spellbooks builds.
     */
    @SuppressWarnings("unchecked")
    public static HitResult mergeBuilderHit(Object builder, HitResult nativeResult) {
        try {
            Class<?> type = builder.getClass();
            LevelFields fields = new LevelFields(
                (Level) readField(type, builder, "level", Level.class),
                (Entity) readField(type, builder, "originEntity", Entity.class),
                (Vec3) readField(type, builder, "start", Vec3.class),
                (Vec3) readField(type, builder, "end", Vec3.class),
                (Boolean) readField(type, builder, "checkForBlocks", boolean.class),
                ((Number) readField(type, builder, "bbInflation", float.class)).floatValue(),
                (Predicate<? super Entity>) readField(type, builder, "filter", Predicate.class));
            if (!(fields.level instanceof ServerLevel serverLevel)
                || fields.start == null || fields.end == null) return nativeResult;
            return mergeBuilderHit(serverLevel, fields.originEntity, fields.start, fields.end,
                fields.checkForBlocks, fields.bbInflation, fields.filter, nativeResult);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return nativeResult;
        }
    }

    private static Object readField(Class<?> type, Object owner, String name, Class<?> expected)
        throws ReflectiveOperationException {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        Object value = field.get(owner);
        if (value == null && expected.isPrimitive()) {
            throw new IllegalStateException("Missing RaycastBuilder field " + name);
        }
        return value;
    }

    private record LevelFields(Level level, Entity originEntity,
                               Vec3 start, Vec3 end, boolean checkForBlocks,
                               float bbInflation, Predicate<? super Entity> filter) {
    }

    public static List<HitResult> mergeProjectileHits(
            Projectile projectile, Vec3 rayEnd, Vec3 rayStart, double inflation,
            List<HitResult> nativeHits, Map<Integer, String> hitboxes) {
        if (!(projectile.level() instanceof ServerLevel level)) return nativeHits;

        List<HitResult> merged = new ArrayList<>();
        Set<Integer> nativeIds = new HashSet<>();
        for (HitResult result : nativeHits) {
            if (result instanceof EntityHitResult entityHit
                && ObbCompat.hasAnimatedObbs(entityHit.getEntity())) {
                nativeIds.add(entityHit.getEntity().getId());
                continue;
            }
            merged.add(result);
            if (result instanceof EntityHitResult entityHit) {
                nativeIds.add(entityHit.getEntity().getId());
            }
        }

        for (Entity candidate : level.getAllEntities()) {
            if (!(candidate instanceof LivingEntity) || !isValidTarget(projectile, candidate)) continue;
            if (nativeIds.contains(candidate.getId()) && !ObbCompat.hasAnimatedObbs(candidate)) continue;
            if (!ObbCompat.hasAnimatedObbs(candidate)) continue;

            RayHit hit = ObbCompat.findRayIntersection(candidate, rayStart, rayEnd, inflation);
            if (hit == null) continue;
            merged.add(new EntityHitResult(candidate, hit.hitPoint));
            hitboxes.put(candidate.getId(), hit.hitboxName);
        }
        merged.sort(Comparator.comparingDouble(result -> result.getLocation().distanceToSqr(rayStart)));
        return merged;
    }

    public static Set<Entity> mergeConeTargets(Projectile cone, Set<Entity> nativeTargets,
                                                Map<Integer, String> hitboxes) {
        if (!(cone.level() instanceof ServerLevel level)) return nativeTargets;

        Map<Integer, Entity> merged = new LinkedHashMap<>();
        for (Entity target : nativeTargets) {
            if (!ObbCompat.hasAnimatedObbs(target)) merged.put(target.getId(), target);
        }

        net.minecraftforge.entity.PartEntity<?>[] parts = cone.getParts();
        if (parts == null || parts.length == 0) return new LinkedHashSet<>(merged.values());
        Vec3 reference = cone.position();
        for (Entity candidate : level.getAllEntities()) {
            if (!(candidate instanceof LivingEntity) || !isValidTarget(cone, candidate)
                || !ObbCompat.hasAnimatedObbs(candidate)) continue;

            String hitbox = null;
            for (net.minecraftforge.entity.PartEntity<?> part : parts) {
                hitbox = ObbCompat.findIntersection(candidate, part.getBoundingBox(), reference);
                if (hitbox != null) break;
            }
            if (hitbox == null || !hasLineOfSight(cone, candidate)) continue;
            merged.put(candidate.getId(), candidate);
            hitboxes.put(candidate.getId(), hitbox);
        }
        return new LinkedHashSet<>(merged.values());
    }

    public static <T extends LivingEntity> List<T> mergeAoeTargets(
            Projectile aoe, Class<T> entityClass, AABB queryBox, List<T> nativeTargets,
            Map<Integer, String> hitboxes) {
        if (!(aoe.level() instanceof ServerLevel level)) return nativeTargets;

        Map<Integer, T> merged = new LinkedHashMap<>();
        for (T target : nativeTargets) {
            if (!ObbCompat.hasAnimatedObbs(target)) merged.put(target.getId(), target);
        }

        for (Entity candidate : level.getAllEntities()) {
            if (!entityClass.isInstance(candidate) || !isValidTarget(aoe, candidate)
                || !ObbCompat.hasAnimatedObbs(candidate)) continue;
            T living = entityClass.cast(candidate);
            String hitbox = ObbCompat.findIntersection(living, queryBox, aoe.position());
            if (hitbox == null) continue;
            merged.put(living.getId(), living);
            hitboxes.put(living.getId(), hitbox);
        }
        return new ArrayList<>(merged.values());
    }

    public static double distanceToAoeSqr(LivingEntity target, Entity aoe) {
        if (!ObbCompat.hasAnimatedObbs(target)) return target.distanceToSqr(aoe);
        Vec3 closest = ObbCompat.closestAttackablePoint(target, aoe.position());
        return closest.distanceToSqr(aoe.position());
    }

    public static HitResult mergeBuilderHit(ServerLevel level, Entity origin,
                                            Vec3 rayStart, Vec3 rayEnd,
                                            boolean checkForBlocks, float inflation,
                                            Predicate<? super Entity> filter,
                                            HitResult nativeResult) {
        Vec3 clippedEnd = rayEnd;
        HitResult terminalResult;
        if (checkForBlocks) {
            BlockHitResult blockHit = level.clip(new ClipContext(rayStart, rayEnd,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, origin));
            if (blockHit.getType() == HitResult.Type.BLOCK) clippedEnd = blockHit.getLocation();
            terminalResult = blockHit;
        } else {
            terminalResult = BlockHitResult.miss(rayEnd, Direction.UP,
                BlockPos.containing(rayEnd));
        }

        Entity selected = null;
        Vec3 selectedPoint = null;
        double selectedDistance = Double.MAX_VALUE;
        for (Entity candidate : level.getAllEntities()) {
            if (candidate == origin || !ObbCompat.hasAnimatedObbs(candidate)
                || (filter != null && !filter.test(candidate))) continue;
            RayHit hit = ObbCompat.findRayIntersection(candidate, rayStart, clippedEnd, inflation);
            if (hit != null && hit.distance < selectedDistance) {
                selected = candidate;
                selectedPoint = hit.hitPoint;
                selectedDistance = hit.distance;
            }
        }

        HitResult validatedNative = nativeResult;
        if (nativeResult instanceof EntityHitResult entityHit
            && ObbCompat.hasAnimatedObbs(entityHit.getEntity())) {
            validatedNative = findNativeNonObbHit(level, origin, rayStart, clippedEnd,
                inflation, filter);
            if (validatedNative == null) validatedNative = terminalResult;
        }

        double nativeDistance = validatedNative != null
            ? validatedNative.getLocation().distanceToSqr(rayStart) : Double.MAX_VALUE;
        return selectedPoint != null && selectedPoint.distanceToSqr(rayStart) < nativeDistance
            ? new EntityHitResult(selected, selectedPoint) : validatedNative;
    }

    private static HitResult findNativeNonObbHit(ServerLevel level, Entity origin,
                                                  Vec3 start, Vec3 end, float inflation,
                                                  Predicate<? super Entity> filter) {
        AABB queryBox = origin.getBoundingBox().expandTowards(end.subtract(start));
        Entity selected = null;
        Vec3 selectedPoint = null;
        double selectedDistance = Double.MAX_VALUE;
        for (Entity candidate : level.getEntities(origin, queryBox, entity ->
            !ObbCompat.hasAnimatedObbs(entity) && (filter == null || filter.test(entity)))) {
            Vec3 point = findNativeIntersection(candidate, start, end, inflation);
            if (point == null) continue;
            double distance = point.distanceToSqr(start);
            if (distance < selectedDistance) {
                selected = candidate;
                selectedPoint = point;
                selectedDistance = distance;
            }
        }
        return selected != null ? new EntityHitResult(selected, selectedPoint) : null;
    }

    private static Vec3 findNativeIntersection(Entity candidate, Vec3 start,
                                                Vec3 end, float inflation) {
        if (candidate.isMultipartEntity()) {
            net.minecraftforge.entity.PartEntity<?>[] parts = candidate.getParts();
            if (parts != null) {
                for (net.minecraftforge.entity.PartEntity<?> part : parts) {
                    if (part == null) continue;
                    Vec3 point = part.getBoundingBox().inflate(inflation)
                        .clip(start, end).orElse(null);
                    if (point != null) return point;
                }
            }
            return null;
        }
        return candidate.getBoundingBox().inflate(inflation).clip(start, end).orElse(null);
    }

    public static List<Entity> mergeChainTargets(ServerLevel level, Entity excluded,
                                                  AABB queryBox,
                                                  Predicate<? super Entity> predicate,
                                                  List<Entity> nativeTargets,
                                                  Map<Integer, String> hitboxes) {
        Map<Integer, Entity> merged = new LinkedHashMap<>();
        for (Entity target : nativeTargets) {
            if (!ObbCompat.hasAnimatedObbs(target)) merged.put(target.getId(), target);
        }
        Vec3 reference = queryBox.getCenter();
        for (Entity candidate : level.getAllEntities()) {
            if (candidate == excluded || merged.containsKey(candidate.getId())
                || !ObbCompat.hasAnimatedObbs(candidate)
                || (predicate != null && !predicate.test(candidate))) continue;
            String hitbox = ObbCompat.findIntersection(candidate, queryBox, reference);
            if (hitbox != null) {
                merged.put(candidate.getId(), candidate);
                hitboxes.put(candidate.getId(), hitbox);
            }
        }
        return new ArrayList<>(merged.values());
    }

    public static double distanceToAttackableSqr(Entity target, Entity origin) {
        if (!ObbCompat.hasAnimatedObbs(target)) return target.distanceToSqr(origin);
        return ObbCompat.closestAttackablePoint(target, origin.position())
            .distanceToSqr(origin.position());
    }

    public static void attributeSpellDamage(Entity target, DamageSource source) {
        if (!(target instanceof IOBBHolder holder)
            || holder.cnpc_immersiveboss$getLastHitboxName() != null
            || !ObbCompat.hasAnimatedObbs(target)) return;

        Entity direct = source.getDirectEntity();
        if (direct instanceof Projectile projectile) {
            RayHit sweptHit = ObbCompat.findRayIntersection(target,
                new Vec3(projectile.xo, projectile.yo, projectile.zo),
                projectile.position(), 0.0);
            if (sweptHit != null) {
                holder.cnpc_immersiveboss$setLastHitboxName(sweptHit.hitboxName);
                return;
            }
        }

        Vec3 reference = direct != null ? direct.position()
            : source.getEntity() != null ? source.getEntity().position()
            : target.position();
        holder.cnpc_immersiveboss$setLastHitboxName(ObbCompat.findNearest(target, reference));
    }

    private static boolean isValidTarget(Projectile projectile, Entity target) {
        Entity owner = projectile.getOwner();
        return target != projectile && target != owner && target.isAlive()
            && !target.isSpectator() && target.isPickable()
            && (owner == null || (!owner.isAlliedTo(target) && !target.isAlliedTo(owner)))
            && (owner == null || !target.isPassengerOfSameVehicle(owner));
    }

    private static boolean hasLineOfSight(Projectile source, Entity target) {
        Vec3 start = source.position();
        Vec3 end = ObbCompat.closestAttackablePoint(target, start);
        BlockHitResult hit = source.level().clip(new ClipContext(start, end,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, source));
        return hit.getType() != HitResult.Type.BLOCK;
    }
}
