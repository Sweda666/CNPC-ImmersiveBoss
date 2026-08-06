package sweda.cnpc_immersiveboss.hitbox;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import noppes.npcs.entity.EntityNPCInterface;

import java.util.Collection;
import java.util.List;

/**
 * Pure OBB collision detection using Separating Axis Theorem (SAT).
 *
 * Key methods:
 *   intersects(OBB, AABB)  — OBB vs block collision (SAT)
 *   intersects(OBB, OBB)   — OBB vs OBB collision  (SAT, 15 axes)
 *   intersectRay(OBB, ...) — OBB vs ray slab-method
 *   enclosingAABB(...)     — broad-phase / vanilla compatibility AABB
 *   staticFallbackAABB(...)— static AABB from GeoHitboxDef (no animation)
 */
public final class OBBPhysics {

    // Cached world-axis unit vectors — avoid allocating per SAT test
    private static final Vec3 WORLD_X = new Vec3(1, 0, 0);
    private static final Vec3 WORLD_Y = new Vec3(0, 1, 0);
    private static final Vec3 WORLD_Z = new Vec3(0, 0, 1);

    private OBBPhysics() {}

    // ── SAT collision tests ────────────────────────────────────────────

    /** OBB vs world-axis AABB intersection (SAT, 15 potential separating axes). */
    public static boolean intersects(OBB obb, AABB aabb) {
        Vec3 cA = obb.center;
        Vec3 hA = obb.halfExtents;
        Vec3 ax = obb.axisX, ay = obb.axisY, az = obb.axisZ;

        Vec3 cB = aabb.getCenter();
        Vec3 hB = new Vec3(
            (aabb.maxX - aabb.minX) * 0.5,
            (aabb.maxY - aabb.minY) * 0.5,
            (aabb.maxZ - aabb.minZ) * 0.5
        );

        // Axes to test: OBB-axes (3) + world-axes (3) + cross(obb × world) (9) = 15
        return satTest15(cA, hA, ax, ay, az, cB, hB, ax, ay, az, true);
    }

    /** OBB vs OBB intersection (SAT, 15 potential separating axes). */
    public static boolean intersects(OBB a, OBB b) {
        return satTest15(
            a.center, a.halfExtents, a.axisX, a.axisY, a.axisZ,
            b.center, b.halfExtents, b.axisX, b.axisY, b.axisZ,
            false
        );
    }

    /**
     * Full 15-axis SAT between OBB A and OBB B.
     * @param bIsAABB if true, B uses world axes regardless of _bX/Y/Z
     */
    private static boolean satTest15(
        Vec3 cA, Vec3 hA, Vec3 aX, Vec3 aY, Vec3 aZ,
        Vec3 cB, Vec3 hB, Vec3 _bX, Vec3 _bY, Vec3 _bZ,
        boolean bIsAABB
    ) {
        Vec3 bX = bIsAABB ? WORLD_X : _bX;
        Vec3 bY = bIsAABB ? WORLD_Y : _bY;
        Vec3 bZ = bIsAABB ? WORLD_Z : _bZ;

        Vec3[] aAxes = {aX, aY, aZ};
        Vec3[] bAxes = {bX, bY, bZ};

        // Test A's 3 face normals
        for (Vec3 axis : aAxes) {
            if (!overlap(axis, cA, hA, aX, aY, aZ, cB, hB, bX, bY, bZ))
                return false;
        }
        // Test B's 3 face normals
        for (Vec3 axis : bAxes) {
            if (!overlap(axis, cA, hA, aX, aY, aZ, cB, hB, bX, bY, bZ))
                return false;
        }
        // Test 9 cross-product axes: A axes × B axes
        for (Vec3 aAxis : aAxes) {
            for (Vec3 bAxis : bAxes) {
                Vec3 cross = aAxis.cross(bAxis);
                if (cross.lengthSqr() < 1e-10) continue;
                Vec3 axis = cross.normalize();
                if (!overlap(axis, cA, hA, aX, aY, aZ, cB, hB, bX, bY, bZ))
                    return false;
            }
        }
        return true;
    }

    /** Project both OBBs onto a single axis, return true if intervals overlap. */
    private static boolean overlap(
        Vec3 L,
        Vec3 cA, Vec3 hA, Vec3 aX, Vec3 aY, Vec3 aZ,
        Vec3 cB, Vec3 hB, Vec3 bX, Vec3 bY, Vec3 bZ
    ) {
        double rA = Math.abs(hA.x * dot(L, aX))
                  + Math.abs(hA.y * dot(L, aY))
                  + Math.abs(hA.z * dot(L, aZ));
        double rB = Math.abs(hB.x * dot(L, bX))
                  + Math.abs(hB.y * dot(L, bY))
                  + Math.abs(hB.z * dot(L, bZ));
        double dist = Math.abs(dot(L, cB.subtract(cA)));
        return dist <= rA + rB;
    }

    // ── Raycasting ──────────────────────────────────────────────────────

    /** Pre-allocated reusable array for slab1D to avoid per-call allocation. */
    private static final double[] SLAB_TMP = new double[2];

    /**
     * Ray vs OBB intersection (slab method in OBB-local space).
     * Returns distance along ray to hit point, or -1 if no hit.
     */
    public static double intersectRay(OBB obb, Vec3 rayStart, Vec3 rayEnd) {
        double dx = rayStart.x - obb.center.x;
        double dy = rayStart.y - obb.center.y;
        double dz = rayStart.z - obb.center.z;
        double lsx = dx * obb.axisX.x + dy * obb.axisX.y + dz * obb.axisX.z;
        double lsy = dx * obb.axisY.x + dy * obb.axisY.y + dz * obb.axisY.z;
        double lsz = dx * obb.axisZ.x + dy * obb.axisZ.y + dz * obb.axisZ.z;

        dx = rayEnd.x - obb.center.x;
        dy = rayEnd.y - obb.center.y;
        dz = rayEnd.z - obb.center.z;
        double lex = dx * obb.axisX.x + dy * obb.axisX.y + dz * obb.axisX.z;
        double ley = dx * obb.axisY.x + dy * obb.axisY.y + dz * obb.axisY.z;
        double lez = dx * obb.axisZ.x + dy * obb.axisZ.y + dz * obb.axisZ.z;

        double dirx = lex - lsx, diry = ley - lsy, dirz = lez - lsz;
        double len = Math.sqrt(dirx * dirx + diry * diry + dirz * dirz);
        if (len < 1e-10) return -1;

        double hx = obb.halfExtents.x, hy = obb.halfExtents.y, hz = obb.halfExtents.z;

        if (!slab1D(dirx, lsx, -hx, hx, SLAB_TMP)) return -1;
        double tmin = SLAB_TMP[0], tmax = SLAB_TMP[1];

        if (!slab1D(diry, lsy, -hy, hy, SLAB_TMP)) return -1;
        tmin = Math.max(tmin, SLAB_TMP[0]); tmax = Math.min(tmax, SLAB_TMP[1]);

        if (!slab1D(dirz, lsz, -hz, hz, SLAB_TMP)) return -1;
        tmin = Math.max(tmin, SLAB_TMP[0]); tmax = Math.min(tmax, SLAB_TMP[1]);

        return (tmin <= tmax && tmin >= 0 && tmin <= len) ? tmin : -1;
    }

    /** Writes {t1, t2} into out[] for the intersection of a 1D ray with an interval. Returns false if miss. */
    private static boolean slab1D(double dir, double start, double lo, double hi, double[] out) {
        if (Math.abs(dir) < 1e-10) {
            if (start >= lo && start <= hi) {
                out[0] = 0; out[1] = Double.MAX_VALUE;
                return true;
            }
            return false;
        }
        double inv = 1.0 / dir;
        double t1 = (lo - start) * inv;
        double t2 = (hi - start) * inv;
        out[0] = Math.min(t1, t2);
        out[1] = Math.max(t1, t2);
        return true;
    }

    // ── Broad-phase / vanilla compat AABB utilities ────────────────────

    /** Conservative (tightest) axis-aligned bounding box enclosing one OBB. */
    public static AABB enclosingAABB(OBB obb) {
        double ex = Math.abs(obb.halfExtents.x * obb.axisX.x)
                  + Math.abs(obb.halfExtents.y * obb.axisY.x)
                  + Math.abs(obb.halfExtents.z * obb.axisZ.x);
        double ey = Math.abs(obb.halfExtents.x * obb.axisX.y)
                  + Math.abs(obb.halfExtents.y * obb.axisY.y)
                  + Math.abs(obb.halfExtents.z * obb.axisZ.y);
        double ez = Math.abs(obb.halfExtents.x * obb.axisX.z)
                  + Math.abs(obb.halfExtents.y * obb.axisY.z)
                  + Math.abs(obb.halfExtents.z * obb.axisZ.z);
        return new AABB(
            obb.center.x - ex, obb.center.y - ey, obb.center.z - ez,
            obb.center.x + ex, obb.center.y + ey, obb.center.z + ez
        );
    }

    /** Merge multiple OBBs into one conservative AABB. */
    public static AABB enclosingAABB(Collection<OBB> obbs) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;

        for (OBB obb : obbs) {
            AABB aabb = enclosingAABB(obb);
            minX = Math.min(minX, aabb.minX); maxX = Math.max(maxX, aabb.maxX);
            minY = Math.min(minY, aabb.minY); maxY = Math.max(maxY, aabb.maxY);
            minZ = Math.min(minZ, aabb.minZ); maxZ = Math.max(maxZ, aabb.maxZ);
        }

        return minX == Double.MAX_VALUE
            ? new AABB(0, 0, 0, 0, 0, 0)
            : new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Static AABB from GeoHitboxDef list (no animation data).
     * Used as fallback when no OBB sync from client is available.
     */
    public static AABB staticFallbackAABB(
        EntityNPCInterface npc, List<GeoHitboxDef> defs, double bbToWorld, float yawRad
    ) {
        double cos = Math.cos(yawRad), sin = Math.sin(yawRad);
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;

        for (GeoHitboxDef def : defs) {
            Vec3 center = def.center().scale(bbToWorld);
            double hw = def.size.x * bbToWorld / 2.0;
            double hh = def.size.y * bbToWorld / 2.0;
            double hd = def.size.z * bbToWorld / 2.0;

            double cx = npc.getX() + center.x * cos - center.z * sin;
            double cy = npc.getY() + center.y;
            double cz = npc.getZ() + center.x * sin + center.z * cos;

            double ex = Math.abs(hw * cos) + Math.abs(hd * sin);
            double ez = Math.abs(hw * sin) + Math.abs(hd * cos);

            minX = Math.min(minX, cx - ex); maxX = Math.max(maxX, cx + ex);
            minY = Math.min(minY, cy - hh); maxY = Math.max(maxY, cy + hh);
            minZ = Math.min(minZ, cz - ez); maxZ = Math.max(maxZ, cz + ez);
        }

        if (minX == Double.MAX_VALUE) return null;
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    // ── Misc helpers ──────────────────────────────────────────────────

    private static double dot(Vec3 a, Vec3 b) { return a.x*b.x + a.y*b.y + a.z*b.z; }
}
