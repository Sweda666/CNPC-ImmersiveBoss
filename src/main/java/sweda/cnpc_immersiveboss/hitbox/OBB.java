package sweda.cnpc_immersiveboss.hitbox;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Oriented Bounding Box — a box with arbitrary position, rotation, and size.
 * Represents a hitbox bone's world-space collision volume,
 * following the bone's animation transform each frame.
 *
 * The half-extents are computed by transforming all 8 cube corners through the
 * world matrix, then projecting onto the orientation axes. This avoids issues
 * with non-uniform matrix column scaling (e.g. Y vs XZ scale differences in
 * GeckoLib's rendering pipeline).
 */
public final class OBB {

    public final Vec3 center;
    public final Vec3 halfExtents;
    public final Vec3 axisX, axisY, axisZ;

    public OBB(Vec3 center, Vec3 halfExtents, Vec3 axisX, Vec3 axisY, Vec3 axisZ) {
        this.center = center;
        this.halfExtents = halfExtents;
        this.axisX = axisX;
        this.axisY = axisY;
        this.axisZ = axisZ;
    }

    /**
     * Build an OBB from a GeckoLib world-space matrix and model-space cube geometry.
     * Transforms all 8 cube corners to world space, then fits the OBB to them.
     *
     * @param worldMatrix 4×4 bone world matrix (from GeoBone.getWorldSpaceMatrix())
     *                     GeckoLib 4.8.4 matrix is already in world coordinates.
     * @param originBB    cube origin in world-scaled units (pre-multiplied by bbToWorld)
     * @param sizeBB      cube size in world-scaled units
     */
    public static OBB fromWorldMatrix(Matrix4f worldMatrix, Vec3 originBB, Vec3 sizeBB) {
        if (worldMatrix == null) return null;

        // Extract 3×3 and normalize for orientation axes
        float m00 = worldMatrix.m00(), m01 = worldMatrix.m01(), m02 = worldMatrix.m02();
        float m10 = worldMatrix.m10(), m11 = worldMatrix.m11(), m12 = worldMatrix.m12();
        float m20 = worldMatrix.m20(), m21 = worldMatrix.m21(), m22 = worldMatrix.m22();

        Vec3 colX = new Vec3(m00, m10, m20);
        Vec3 colY = new Vec3(m01, m11, m21);
        Vec3 colZ = new Vec3(m02, m12, m22);

        double lenX = colX.length();
        double lenY = colY.length();
        double lenZ = colZ.length();
        if (lenX < 1e-10 || lenY < 1e-10 || lenZ < 1e-10) return null;

        Vec3 axisX = colX.scale(1.0 / lenX);
        Vec3 axisY = colY.scale(1.0 / lenY);
        Vec3 axisZ = colZ.scale(1.0 / lenZ);

        // Translation from matrix (JOML m03/m13/m23 = column 3 rows 0-2)
        float tx = worldMatrix.m03();
        float ty = worldMatrix.m13();
        float tz = worldMatrix.m23();

        // --- Transform all 8 cube corners to world space ---
        // Use the NORMALIZED (pure rotation) 3×3 so that explicit bbToWorld
        // scaling applied by the caller is not interfered with by any
        // embedded scale in the GeckoLib world matrix.
        double ox = originBB.x, oy = originBB.y, oz = originBB.z;
        double sx = sizeBB.x, sy = sizeBB.y, sz = sizeBB.z;

        double[][] corners = new double[8][3];

        for (int i = 0; i < 8; i++) {
            double cx = ox + ((i & 1) != 0 ? sx : 0);
            double cy = oy + ((i & 2) != 0 ? sy : 0);
            double cz = oz + ((i & 4) != 0 ? sz : 0);
            // Transform with normalized (rotation-only) 3×3 + world-space translation
            corners[i][0] = axisX.x * cx + axisY.x * cy + axisZ.x * cz + tx;
            corners[i][1] = axisX.y * cx + axisY.y * cy + axisZ.y * cz + ty;
            corners[i][2] = axisX.z * cx + axisY.z * cy + axisZ.z * cz + tz;
        }

        // Center = midpoint of the AABB enclosing all 8 corners
        double minX = corners[0][0], minY = corners[0][1], minZ = corners[0][2];
        double maxX = minX, maxY = minY, maxZ = minZ;
        for (int i = 1; i < 8; i++) {
            minX = Math.min(minX, corners[i][0]);
            minY = Math.min(minY, corners[i][1]);
            minZ = Math.min(minZ, corners[i][2]);
            maxX = Math.max(maxX, corners[i][0]);
            maxY = Math.max(maxY, corners[i][1]);
            maxZ = Math.max(maxZ, corners[i][2]);
        }
        Vec3 center = new Vec3((minX + maxX) * 0.5, (minY + maxY) * 0.5, (minZ + maxZ) * 0.5);

        // Half-extents: max absolute projection of each corner-relative-to-center onto each axis
        double hx = 0, hy = 0, hz = 0;
        for (int i = 0; i < 8; i++) {
            double dx = corners[i][0] - center.x;
            double dy = corners[i][1] - center.y;
            double dz = corners[i][2] - center.z;
            double projX = Math.abs(dx * axisX.x + dy * axisX.y + dz * axisX.z);
            double projY = Math.abs(dx * axisY.x + dy * axisY.y + dz * axisY.z);
            double projZ = Math.abs(dx * axisZ.x + dy * axisZ.y + dz * axisZ.z);
            if (projX > hx) hx = projX;
            if (projY > hy) hy = projY;
            if (projZ > hz) hz = projZ;
        }
        Vec3 halfExt = new Vec3(hx, hy, hz);

        return new OBB(center, halfExt, axisX, axisY, axisZ);
    }

    // toAABB() removed — use OBBPhysics.enclosingAABB() for broad-phase queries.
    // Collision uses pure SAT-based OBB math via OBBPhysics.intersects().
}
