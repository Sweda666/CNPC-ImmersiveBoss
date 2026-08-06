package sweda.cnpc_immersiveboss.hitbox;

import net.minecraft.world.phys.Vec3;

/**
 * A parsed hitbox definition from a .geo.json bone with hitbox prefix.
 *
 * Bone naming convention:
 *   h[a][d][b|s]_name
 *     h  = hitbox marker
 *     a  = has appearance (render visible)
 *     d  = detectable (attackable / interactable)
 *     b  = blocking (physical collision)   vs   s  = sensor (no collision)
 *
 * Examples: hb_body, hs_head, hab_sword, has_wing, hdb_core, hadb_shield
 */
public class GeoHitboxDef {
    /** Unique identifer for this cube (e.g. "hadb_upbody" or "hadb_upbody__1" for extras). */
    public final String boneName;
    /** Original bone name for GeckoLib lookups (e.g. "hadb_upbody"). Same as boneName when only one cube. */
    public final String geoBoneName;
    public final boolean isPhysical;   // has collision (b suffix)
    public final boolean render;       // visible (a flag in prefix)
    public final boolean detectable;   // attackable/interactable (d flag in prefix)
    public final Vec3 origin;          // cube origin in model space (Blockbench units)
    public final Vec3 size;            // cube size in model space (Blockbench units)
    /** Cube-local pivot (the point the cube rotates around). Null if no rotation. */
    public final Vec3 cubePivot;
    /** Cube-local rotation in degrees [rx, ry, rz]. Null if no rotation. */
    public final Vec3 cubeRotation;
    /** Cached center point — computed once since origin/size are immutable. */
    private volatile Vec3 cachedCenter;
    /** Bone's static pivot position in model space (Blockbench units), accumulated through parent hierarchy. */
    public final Vec3 staticPivot;

    public GeoHitboxDef(String boneName, String geoBoneName, boolean isPhysical, boolean render, boolean detectable,
                        Vec3 origin, Vec3 size, Vec3 cubePivot, Vec3 cubeRotation, Vec3 staticPivot) {
        this.boneName = boneName;
        this.geoBoneName = geoBoneName;
        this.isPhysical = isPhysical;
        this.render = render;
        this.detectable = detectable;
        this.origin = origin;
        this.size = size;
        this.cubePivot = cubePivot;
        this.cubeRotation = cubeRotation;
        this.staticPivot = staticPivot;
    }

    /** Backward-compat: geoBoneName defaults to boneName (single-cube bones). */
    public GeoHitboxDef(String boneName, boolean isPhysical, boolean render, boolean detectable,
                        Vec3 origin, Vec3 size, Vec3 cubePivot, Vec3 cubeRotation, Vec3 staticPivot) {
        this(boneName, boneName, isPhysical, render, detectable, origin, size, cubePivot, cubeRotation, staticPivot);
    }

    /** Strips the multi-cube suffix from a unique bone name, returning the base GeoBone name. */
    public static String baseBoneName(String name) {
        int idx = name.indexOf("__");
        return idx > 0 ? name.substring(0, idx) : name;
    }

    /** Center point of the cube in model space, accounting for cube rotation (cached). */
    public Vec3 center() {
        Vec3 c = cachedCenter;
        if (c == null) {
            Vec3 simpleCenter = origin.add(size.scale(0.5));
            if (cubeRotation != null && cubePivot != null) {
                // Rotate the center around the cube pivot
                Vec3 rel = simpleCenter.subtract(cubePivot);
                double rx = Math.toRadians(cubeRotation.x);
                double ry = Math.toRadians(cubeRotation.y);
                double rz = Math.toRadians(cubeRotation.z);
                // Apply Z rotation first, then Y, then X (Blockbench order)
                Vec3 rotated = rotateZ(rel, rz);
                rotated = rotateY(rotated, ry);
                rotated = rotateX(rotated, rx);
                cachedCenter = c = cubePivot.add(rotated);
            } else {
                cachedCenter = c = simpleCenter;
            }
        }
        return c;
    }

    private static Vec3 rotateX(Vec3 v, double angle) {
        double c = Math.cos(angle), s = Math.sin(angle);
        return new Vec3(v.x, v.y * c - v.z * s, v.y * s + v.z * c);
    }
    private static Vec3 rotateY(Vec3 v, double angle) {
        double c = Math.cos(angle), s = Math.sin(angle);
        return new Vec3(v.x * c + v.z * s, v.y, -v.x * s + v.z * c);
    }
    private static Vec3 rotateZ(Vec3 v, double angle) {
        double c = Math.cos(angle), s = Math.sin(angle);
        return new Vec3(v.x * c - v.y * s, v.x * s + v.y * c, v.z);
    }

    // ── Bone name classification helpers ──────────────────────────────

    /**
     * Parse a hitbox bone name and return its type flags.
     * Expected format: h[a][d][b|s]_rest
     * Returns null if the bone is not a valid hitbox bone.
     */
    public static Type classify(String boneName) {
        if (boneName == null || boneName.length() < 3) return null;
        if (boneName.charAt(0) != 'h') return null;

        int u = boneName.indexOf('_');
        if (u <= 1 || u > 5) return null; // prefix between "h" and "_" is 1-4 chars

        String prefix = boneName.substring(1, u);
        boolean render = prefix.indexOf('a') >= 0;
        boolean detectable = prefix.indexOf('d') >= 0;

        char suffix = boneName.charAt(u - 1);
        boolean physical;
        if (suffix == 'b') physical = true;
        else if (suffix == 's') physical = false;
        else return null; // invalid suffix

        // Validate prefix contains only allowed chars (a, d, b, s)
        for (int i = 0; i < prefix.length() - 1; i++) {
            char c = prefix.charAt(i);
            if (c != 'a' && c != 'd') return null;
        }

        return new Type(physical, render, detectable);
    }

    /** Quick check: does the bone name indicate a physical (collision) hitbox? */
    public static boolean isPhysicalBone(String boneName) {
        int u = boneName.indexOf('_');
        return u > 1 && u <= 5 && boneName.charAt(0) == 'h' && boneName.charAt(u - 1) == 'b';
    }

    /** Quick check: does the bone name indicate a detectable (attackable) hitbox? */
    public static boolean isDetectableBone(String boneName) {
        int u = boneName.indexOf('_');
        if (u <= 1 || u > 5 || boneName.charAt(0) != 'h') return false;
        return boneName.substring(1, u).indexOf('d') >= 0;
    }

    /** Immutable type flags returned by classify(). */
    public static final class Type {
        public final boolean physical;
        public final boolean render;
        public final boolean detectable;

        Type(boolean physical, boolean render, boolean detectable) {
            this.physical = physical;
            this.render = render;
            this.detectable = detectable;
        }
    }
}
