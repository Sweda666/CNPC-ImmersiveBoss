package sweda.cnpc_immersiveboss.mixin;

import net.minecraft.nbt.CompoundTag;
import noppes.npcs.entity.data.DataAI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sweda.cnpc_immersiveboss.api.IMixinDataAI;
import sweda.cnpc_immersiveboss.entity.NpcTurnSpeedManager;

@Mixin(value = DataAI.class, remap = false)
public class MixinDataAI implements IMixinDataAI {
    @Unique
    private boolean cnpc_immersiveboss$rotationLimitEnabled;

    @Unique
    private float cnpc_immersiveboss$rotationSpeed =
        NpcTurnSpeedManager.DEFAULT_TURN_SPEED;

    @Unique
    private float cnpc_immersiveboss$minimumNavigationSpeedScale =
        NpcTurnSpeedManager.DEFAULT_MINIMUM_NAVIGATION_SPEED_SCALE;

    @Override
    public boolean cnpc_immersiveboss$isRotationLimitEnabled() {
        return cnpc_immersiveboss$rotationLimitEnabled;
    }

    @Override
    public void cnpc_immersiveboss$setRotationLimitEnabled(boolean enabled) {
        cnpc_immersiveboss$rotationLimitEnabled = enabled;
    }

    @Override
    public float cnpc_immersiveboss$getRotationSpeed() {
        return cnpc_immersiveboss$rotationSpeed;
    }

    @Override
    public void cnpc_immersiveboss$setRotationSpeed(float degreesPerTick) {
        cnpc_immersiveboss$rotationSpeed = NpcTurnSpeedManager.sanitizeTurnSpeed(
            degreesPerTick);
    }

    @Override
    public float cnpc_immersiveboss$getMinimumNavigationSpeedScale() {
        return cnpc_immersiveboss$minimumNavigationSpeedScale;
    }

    @Override
    public void cnpc_immersiveboss$setMinimumNavigationSpeedScale(float scale) {
        cnpc_immersiveboss$minimumNavigationSpeedScale =
            NpcTurnSpeedManager.sanitizeMinimumNavigationSpeedScale(scale);
    }

    @Inject(method = "save", at = @At("RETURN"), remap = false, require = 0)
    private void cnpc_immersiveboss$saveRotationSettings(CompoundTag tag,
                                                         CallbackInfoReturnable<CompoundTag> cir) {
        tag.putBoolean(NpcTurnSpeedManager.ROTATION_LIMIT_ENABLED_NBT_KEY,
            cnpc_immersiveboss$rotationLimitEnabled);
        tag.putFloat(NpcTurnSpeedManager.ROTATION_SPEED_NBT_KEY,
            cnpc_immersiveboss$rotationSpeed);
        tag.putFloat(NpcTurnSpeedManager.MINIMUM_NAVIGATION_SPEED_SCALE_NBT_KEY,
            cnpc_immersiveboss$minimumNavigationSpeedScale);
    }

    @Inject(method = "readToNBT", at = @At("RETURN"), remap = false, require = 0)
    private void cnpc_immersiveboss$readRotationSettings(CompoundTag tag, CallbackInfo ci) {
        cnpc_immersiveboss$rotationLimitEnabled = tag.getBoolean(
            NpcTurnSpeedManager.ROTATION_LIMIT_ENABLED_NBT_KEY);
        cnpc_immersiveboss$rotationSpeed = tag.contains(
            NpcTurnSpeedManager.ROTATION_SPEED_NBT_KEY)
            ? NpcTurnSpeedManager.sanitizeTurnSpeed(tag.getFloat(
                NpcTurnSpeedManager.ROTATION_SPEED_NBT_KEY))
            : NpcTurnSpeedManager.DEFAULT_TURN_SPEED;
        cnpc_immersiveboss$minimumNavigationSpeedScale = tag.contains(
            NpcTurnSpeedManager.MINIMUM_NAVIGATION_SPEED_SCALE_NBT_KEY)
            ? NpcTurnSpeedManager.sanitizeMinimumNavigationSpeedScale(tag.getFloat(
                NpcTurnSpeedManager.MINIMUM_NAVIGATION_SPEED_SCALE_NBT_KEY))
            : NpcTurnSpeedManager.DEFAULT_MINIMUM_NAVIGATION_SPEED_SCALE;
    }
}
