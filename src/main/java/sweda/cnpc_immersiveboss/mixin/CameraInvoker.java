package sweda.cnpc_immersiveboss.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Accesses Camera's protected transform methods from the camera mixin. */
@Mixin(Camera.class)
public interface CameraInvoker {
    @Invoker("setPosition")
    void cnpc_immersiveboss$setPosition(Vec3 position);

    @Invoker("setRotation")
    void cnpc_immersiveboss$setRotation(float yRot, float xRot);
}
