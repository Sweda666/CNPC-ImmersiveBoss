package sweda.cnpc_immersiveboss.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import software.bernie.geckolib.cache.object.GeoBone;

/**
 * Exposes GeoBone.parent (private) so we can walk the bone hierarchy
 * to accumulate model-space positions correctly.
 */
@Mixin(GeoBone.class)
public interface GeoBoneAccessor {
    @Accessor(value = "parent", remap = false)
    GeoBone getParentBone();
}
