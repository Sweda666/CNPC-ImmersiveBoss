package sweda.cnpc_immersiveboss.hitbox;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;

/**
 * A strict oriented box in entity-relative coordinates.
 *
 * Dynamic instances use GeckoLib's animated transform while keeping all three
 * axes orthogonal and retaining the raw Blockbench cube dimensions.
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

    public static OBB fromGeckoCube(Matrix4f cubeMatrix, GeoCube cube) {
        if (cubeMatrix == null || cube == null) return null;

        LocalBounds bounds = localBounds(cube);
        if (bounds == null) return null;

        Vec3 rawX = transformDirection(cubeMatrix, 1, 0, 0);
        Vec3 rawY = transformDirection(cubeMatrix, 0, 1, 0);
        Vec3 rawZ = transformDirection(cubeMatrix, 0, 0, 1);
        double scaleX = rawX.length();
        double scaleY = rawY.length();
        double scaleZ = rawZ.length();
        if (!isUsableLength(scaleX)) return null;
        if (!isUsableLength(scaleY)) return null;
        if (!isUsableLength(scaleZ)) return null;
        Vec3 axisX = rawX.scale(1.0 / scaleX);

        // Keep a rigid OBB even when nested non-uniform scales introduce shear.
        Vec3 orthogonalY = rawY.subtract(axisX.scale(rawY.dot(axisX)));
        double orthogonalYLength = orthogonalY.length();
        if (!isUsableLength(orthogonalYLength)) return null;
        Vec3 axisY = orthogonalY.scale(1.0 / orthogonalYLength);

        Vec3 axisZ = axisX.cross(axisY);
        double axisZLength = axisZ.length();
        if (!isUsableLength(axisZLength)) return null;
        axisZ = axisZ.scale(1.0 / axisZLength);
        if (axisZ.dot(rawZ) < 0) axisZ = axisZ.scale(-1);

        Vector3f transformedCenter = cubeMatrix.transformPosition(new Vector3f(
            (float) bounds.center.x, (float) bounds.center.y, (float) bounds.center.z
        ));
        Vec3 center = new Vec3(
            transformedCenter.x, transformedCenter.y, transformedCenter.z
        );
        Vec3 halfExtents = new Vec3(
            bounds.halfExtents.x * scaleX,
            bounds.halfExtents.y * scaleY,
            bounds.halfExtents.z * scaleZ
        );
        if (!allFinite(center.x, center.y, center.z,
            halfExtents.x, halfExtents.y, halfExtents.z)) return null;

        return new OBB(center, halfExtents, axisX, axisY, axisZ);
    }

    private static LocalBounds localBounds(GeoCube cube) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        for (GeoQuad quad : cube.quads()) {
            if (quad == null) continue;
            for (GeoVertex vertex : quad.vertices()) {
                Vector3f position = vertex.position();
                minX = Math.min(minX, position.x);
                minY = Math.min(minY, position.y);
                minZ = Math.min(minZ, position.z);
                maxX = Math.max(maxX, position.x);
                maxY = Math.max(maxY, position.y);
                maxZ = Math.max(maxZ, position.z);
            }
        }
        if (!allFinite(minX, minY, minZ, maxX, maxY, maxZ)) return null;

        return new LocalBounds(
            new Vec3((minX + maxX) * 0.5, (minY + maxY) * 0.5, (minZ + maxZ) * 0.5),
            new Vec3((maxX - minX) * 0.5, (maxY - minY) * 0.5, (maxZ - minZ) * 0.5)
        );
    }

    private static Vec3 transformDirection(Matrix4f matrix, float x, float y, float z) {
        Vector3f transformed = matrix.transformDirection(new Vector3f(x, y, z));
        return new Vec3(transformed.x, transformed.y, transformed.z);
    }

    private static boolean isUsableLength(double length) {
        return Double.isFinite(length) && length >= 1e-10;
    }

    private static boolean allFinite(double... values) {
        for (double value : values) {
            if (!Double.isFinite(value)) return false;
        }
        return true;
    }

    private record LocalBounds(Vec3 center, Vec3 halfExtents) {}
}
