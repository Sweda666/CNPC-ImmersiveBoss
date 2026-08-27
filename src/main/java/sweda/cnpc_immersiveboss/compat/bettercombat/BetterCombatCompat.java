package sweda.cnpc_immersiveboss.compat.bettercombat;

import com.mojang.logging.LogUtils;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import sweda.cnpc_immersiveboss.api.IOBBHolder;
import sweda.cnpc_immersiveboss.compat.ObbCompat;
import sweda.cnpc_immersiveboss.hitbox.OBB;

import java.lang.reflect.Method;

/** Server-authoritative Better Combat attack-volume validation. */
public final class BetterCombatCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean reflectionWarningLogged;
    private static final ThreadLocal<AttackContext> ATTACK_CONTEXT = new ThreadLocal<>();

    private BetterCombatCompat() {
    }

    public static void beginAttack(Object attributes, Object attack) {
        ATTACK_CONTEXT.set(new AttackContext(attributes, attack));
    }

    public static void endAttack() {
        ATTACK_CONTEXT.remove();
    }

    public static void attack(ServerPlayer attacker, Entity target) {
        AttackContext context = ATTACK_CONTEXT.get();
        if (context == null) {
            attacker.attack(target);
            return;
        }
        attack(attacker, target, context.attributes, context.attack);
    }

    public static void attack(ServerPlayer attacker, Entity target,
                              Object attributes, Object attack) {
        if (!ObbCompat.hasAnimatedObbs(target)) {
            attacker.attack(target);
            return;
        }

        try {
            String hitbox = resolveHitbox(attacker, target, attributes, attack);
            if (hitbox == null) return;

            IOBBHolder holder = (IOBBHolder) target;
            holder.cnpc_immersiveboss$setLastHitboxName(hitbox);
            attacker.attack(target);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            // A Better Combat bytecode change should not disable otherwise valid attacks.
            if (!reflectionWarningLogged) {
                reflectionWarningLogged = true;
                LOGGER.warn("Better Combat OBB validation could not inspect the attack; "
                    + "falling back to the native target", exception);
            }
            attacker.attack(target);
        }
    }

    /** Covers Better Combat's optional vanilla attack-packet path. */
    public static void handleInteract(ServerGamePacketListenerImpl listener,
                                      ServerboundInteractPacket packet) {
        AttackContext context = ATTACK_CONTEXT.get();
        ServerPlayer attacker = listener.player;
        Entity target = packet.getTarget(attacker.serverLevel());
        if (context == null || target == null || !ObbCompat.hasAnimatedObbs(target)) {
            listener.handleInteract(packet);
            return;
        }

        try {
            String hitbox = resolveHitbox(attacker, target,
                context.attributes, context.attack);
            if (hitbox == null) return;
            ((IOBBHolder) target).cnpc_immersiveboss$setLastHitboxName(hitbox);
            listener.handleInteract(packet);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logReflectionFallback(exception);
            listener.handleInteract(packet);
        }
    }

    private static String resolveHitbox(ServerPlayer attacker, Entity target,
                                        Object attributes, Object attack)
        throws ReflectiveOperationException {
        double range = invokeDouble(attributes, "attackRange");
        double angle = invokeDouble(attack, "angle");
        Object shape = attack.getClass().getMethod("hitbox").invoke(attack);
        OBB attackObb = createAttackObb(attacker, range, angle, shape.toString());
        Vec3 origin = attackOrigin(attacker);
        String hitbox = ObbCompat.findIntersection(target, attackObb, origin);
        // Better Combat's client target finder can retain the entity under
        // the crosshair even when its rotated attack volume misses a thin
        // animated OBB. Preserve that native aim behavior with the same
        // reach-limited ray used by vanilla melee.
        if (hitbox == null) {
            Vec3 eye = attacker.getEyePosition(1.0f);
            Vec3 rayEnd = eye.add(attacker.getViewVector(1.0f).scale(range));
            ObbCompat.RayHit rayHit = ObbCompat.findRayIntersection(
                target, eye, rayEnd, 0.0);
            hitbox = rayHit != null ? rayHit.hitboxName : null;
        }
        return hitbox != null && passesRadialFilter(attacker, target, range, angle)
            ? hitbox : null;
    }

    private static void logReflectionFallback(Exception exception) {
        if (reflectionWarningLogged) return;
        reflectionWarningLogged = true;
        LOGGER.warn("Better Combat OBB validation could not inspect the attack; "
            + "falling back to the native target", exception);
    }

    public static OBB createAttackObb(Entity attacker, double range,
                                      double angle, String shapeName) {
        boolean upsideDown = angle > 180.0;
        Vec3 dimensions = switch (shapeName) {
            case "FORWARD_BOX" -> new Vec3(range * 0.5, range * 0.5, range);
            case "VERTICAL_PLANE" -> new Vec3(range / 3.0, range * 2.0,
                range * (upsideDown ? 2.0 : 1.0));
            case "HORIZONTAL_PLANE" -> new Vec3(range * 2.0, range / 3.0,
                range * (upsideDown ? 2.0 : 1.0));
            default -> throw new IllegalArgumentException("Unknown Better Combat hitbox " + shapeName);
        };

        Vec3 axisZ = Vec3.directionFromRotation(attacker.getXRot(), attacker.getYRot()).normalize();
        Vec3 axisY = Vec3.directionFromRotation(attacker.getXRot() + 90.0F,
            attacker.getYRot()).reverse().normalize();
        Vec3 axisX = axisZ.cross(axisY).normalize();
        Vec3 center = attackOrigin(attacker);
        if (!upsideDown) center = center.add(axisZ.scale(dimensions.z * 0.5));

        return new OBB(center, dimensions.scale(0.5), axisX, axisY, axisZ);
    }

    public static Vec3 attackOrigin(Entity attacker) {
        // Matches Better Combat's TargetFinder.getInitialTracingPoint(): the
        // tracing origin is below the eye, scaled with the player model.
        double scale = attacker instanceof Player player ? player.getScale() : 1.0;
        return attacker.getEyePosition().subtract(0,
            attacker.getBbHeight() * 0.15 * scale, 0);
    }

    public static boolean passesRadialFilter(Entity attacker, Entity target,
                                             double range, double angle) {
        Vec3 origin = attackOrigin(attacker);
        Vec3 closest = ObbCompat.closestAttackablePoint(target, origin).subtract(origin);
        if (closest.lengthSqr() > range * range) return false;
        if (angle == 0.0) return true;

        Vec3 orientation = Vec3.directionFromRotation(attacker.getXRot(), attacker.getYRot());
        Vec3 center = target.getBoundingBox().getCenter().subtract(origin);
        double halfAngle = Math.max(0.0, Math.min(360.0, angle)) * 0.5;
        return angleBetweenDegrees(closest, orientation) <= halfAngle
            || angleBetweenDegrees(center, orientation) <= halfAngle;
    }

    private static double angleBetweenDegrees(Vec3 first, Vec3 second) {
        double denominator = Math.sqrt(first.lengthSqr() * second.lengthSqr());
        if (denominator < 1.0E-10) return 0.0;
        double cosine = Math.max(-1.0, Math.min(1.0, first.dot(second) / denominator));
        return Math.toDegrees(Math.acos(cosine));
    }

    private static double invokeDouble(Object owner, String methodName)
        throws ReflectiveOperationException {
        Method method = owner.getClass().getMethod(methodName);
        return ((Number) method.invoke(owner)).doubleValue();
    }

    private record AttackContext(Object attributes, Object attack) {
    }
}
