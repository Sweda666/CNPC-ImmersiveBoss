package sweda.cnpc_immersiveboss.mixin;

import net.minecraft.nbt.CompoundTag;
import noppes.npcs.Resistances;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sweda.cnpc_immersiveboss.api.IPushResistance;

@Mixin(value = Resistances.class, remap = false)
public class MixinResistances implements IPushResistance {
    @Unique
    private static final String cnpc_immersiveboss$PUSH_RESISTANCE_NBT_KEY = "PushResistance";

    @Unique
    private float cnpc_immersiveboss$pushResistance;

    @Override
    public float cnpc_immersiveboss$getPushResistance() {
        return cnpc_immersiveboss$pushResistance;
    }

    @Override
    public void cnpc_immersiveboss$setPushResistance(float value) {
        cnpc_immersiveboss$pushResistance = sanitize(value);
    }

    @Inject(method = "save", at = @At("RETURN"), remap = false, require = 0)
    private void cnpc_immersiveboss$savePushResistance(
        CallbackInfoReturnable<CompoundTag> cir) {
        cir.getReturnValue().putFloat(cnpc_immersiveboss$PUSH_RESISTANCE_NBT_KEY,
            cnpc_immersiveboss$pushResistance);
    }

    @Inject(method = "readToNBT", at = @At("RETURN"), remap = false, require = 0)
    private void cnpc_immersiveboss$readPushResistance(CompoundTag tag, CallbackInfo ci) {
        cnpc_immersiveboss$pushResistance = tag.contains(cnpc_immersiveboss$PUSH_RESISTANCE_NBT_KEY)
            ? sanitize(tag.getFloat(cnpc_immersiveboss$PUSH_RESISTANCE_NBT_KEY))
            : 0.0F;
    }

    @Unique
    private static float sanitize(float value) {
        if (!Float.isFinite(value)) return 0.0F;
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
