package sweda.cnpc_immersiveboss.compat.bettercombat;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import sweda.cnpc_immersiveboss.compat.ObbCompat;
import sweda.cnpc_immersiveboss.hitbox.OBB;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Client-only augmentation of Better Combat's final target result. */
public final class BetterCombatClientCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean reflectionWarningLogged;

    private BetterCombatClientCompat() {
    }

    @SuppressWarnings("unchecked")
    public static void augmentTargets(Player player, Entity targetUnderCursor,
                                      Object attack, Object targetResult) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || targetResult == null) return;

        try {
            Field entitiesField = targetResult.getClass().getField("entities");
            Field obbField = targetResult.getClass().getField("obb");
            List<Entity> nativeTargets = (List<Entity>) entitiesField.get(targetResult);
            OBB attackObb = readObb(obbField.get(targetResult));
            double angle = ((Number) attack.getClass().getMethod("angle").invoke(attack)).doubleValue();
            double range = attackRange(attack, attackObb);
            Vec3 origin = BetterCombatCompat.attackOrigin(player);

            Map<Integer, Entity> merged = new LinkedHashMap<>();
            for (Entity target : nativeTargets) {
                if (!ObbCompat.hasAnimatedObbs(target)) {
                    merged.put(target.getId(), target);
                    continue;
                }
                if (ObbCompat.findIntersection(target, attackObb, origin) == null
                    || !BetterCombatCompat.passesRadialFilter(player, target, range, angle)
                    || (!allowsAttackingThroughWalls()
                        && !hasLineOfSight(level, player, target, origin))) continue;
                merged.put(target.getId(), target);
            }

            for (Entity candidate : level.entitiesForRendering()) {
                if (merged.containsKey(candidate.getId())
                    || !ObbCompat.hasAnimatedObbs(candidate)
                    || !isNativeCandidate(player, targetUnderCursor, candidate)) continue;
                String hitbox = ObbCompat.findIntersection(candidate, attackObb, origin);
                if (hitbox == null
                    || !BetterCombatCompat.passesRadialFilter(player, candidate, range, angle)) continue;
                if (!allowsAttackingThroughWalls()
                    && !hasLineOfSight(level, player, candidate, origin)) continue;
                merged.put(candidate.getId(), candidate);
            }

            // TargetFinder's CollisionFilter may remove the crosshair target
            // before this augmentation runs. Re-add it when the OBB ray says
            // the player is genuinely aiming at an attackable bone.
            if (targetUnderCursor != null && !merged.containsKey(targetUnderCursor.getId())
                && ObbCompat.hasAnimatedObbs(targetUnderCursor)
                && isNativeCandidate(player, null, targetUnderCursor)) {
                Vec3 rayEnd = player.getEyePosition(1.0f)
                    .add(player.getViewVector(1.0f).scale(range));
                if (ObbCompat.findRayIntersection(targetUnderCursor,
                    player.getEyePosition(1.0f), rayEnd, 0.0) != null
                    && BetterCombatCompat.passesRadialFilter(player, targetUnderCursor, range, angle)
                    && (allowsAttackingThroughWalls()
                        || hasLineOfSight(level, player, targetUnderCursor, origin))) {
                    merged.put(targetUnderCursor.getId(), targetUnderCursor);
                }
            }

            entitiesField.set(targetResult, new ArrayList<>(merged.values()));
        } catch (ReflectiveOperationException | RuntimeException exception) {
            if (!reflectionWarningLogged) {
                reflectionWarningLogged = true;
                LOGGER.warn("Better Combat client target result changed; OBB targets were not added",
                    exception);
            }
        }
    }

    private static OBB readObb(Object betterCombatObb) throws ReflectiveOperationException {
        Class<?> type = betterCombatObb.getClass();
        Vec3 center = (Vec3) type.getField("center").get(betterCombatObb);
        Vec3 extent = (Vec3) type.getField("extent").get(betterCombatObb);
        Vec3 axisX = (Vec3) type.getField("axisX").get(betterCombatObb);
        Vec3 axisY = (Vec3) type.getField("axisY").get(betterCombatObb);
        Vec3 axisZ = (Vec3) type.getField("axisZ").get(betterCombatObb);
        return new OBB(center, extent, axisX, axisY, axisZ);
    }

    private static double attackRange(Object attack, OBB attackObb)
        throws ReflectiveOperationException {
        Object shape = attack.getClass().getMethod("hitbox").invoke(attack);
        return switch (shape.toString()) {
            case "FORWARD_BOX" -> attackObb.halfExtents.z * 2.0;
            case "VERTICAL_PLANE" -> attackObb.halfExtents.y;
            case "HORIZONTAL_PLANE" -> attackObb.halfExtents.x;
            default -> throw new IllegalArgumentException("Unknown Better Combat hitbox " + shape);
        };
    }

    private static boolean allowsAttackingThroughWalls() {
        try {
            Class<?> betterCombat = Class.forName("net.bettercombat.BetterCombat");
            Object config = betterCombat.getField("config").get(null);
            return config != null && config.getClass().getField("allow_attacking_thru_walls")
                .getBoolean(config);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private static boolean isNativeCandidate(Player player, Entity targetUnderCursor,
                                             Entity candidate) throws ReflectiveOperationException {
        if (candidate == player || candidate == targetUnderCursor || !candidate.isAlive()
            || candidate.isSpectator() || !candidate.isPickable()) return false;

        Class<?> helper = Class.forName("net.bettercombat.logic.TargetHelper");
        if (candidate.equals(player.getVehicle())) {
            Method attackableMount = helper.getMethod("isAttackableMount", Entity.class);
            if (!(Boolean) attackableMount.invoke(null, candidate)) return false;
        }
        Method relationMethod = helper.getMethod("getRelation", Player.class, Entity.class);
        Object relation = relationMethod.invoke(null, player, candidate);
        return relation instanceof Enum<?> enumValue && enumValue.name().equals("HOSTILE");
    }

    private static boolean hasLineOfSight(ClientLevel level, Player player, Entity target,
                                          Vec3 origin) {
        Vec3 closest = ObbCompat.closestAttackablePoint(target, origin);
        return clearRay(level, player, origin, closest)
            || clearRay(level, player, origin, target.getBoundingBox().getCenter());
    }

    private static boolean clearRay(ClientLevel level, Player player, Vec3 start, Vec3 end) {
        BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE, player));
        return hit.getType() != HitResult.Type.BLOCK;
    }
}
