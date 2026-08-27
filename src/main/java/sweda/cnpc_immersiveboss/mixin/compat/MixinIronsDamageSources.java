package sweda.cnpc_immersiveboss.mixin.compat;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sweda.cnpc_immersiveboss.compat.ironsspellbooks.IronsSpellbooksCompat;

@Pseudo
@Mixin(targets = "io.redspace.ironsspellbooks.damage.DamageSources", remap = false)
public abstract class MixinIronsDamageSources {
    private static final String SPELL_DAMAGE_SOURCE =
        "io.redspace.ironsspellbooks.damage.SpellDamageSource";

    @Inject(method = "applyDamage", at = @At("HEAD"), remap = false, require = 0)
    private static void cnpc_immersiveboss$attributeSpellHit(
            Entity target, float amount, DamageSource source,
            CallbackInfoReturnable<Boolean> cir) {
        if (source != null && source.getClass().getName().equals(SPELL_DAMAGE_SOURCE)) {
            IronsSpellbooksCompat.attributeSpellDamage(target, source);
        }
    }
}
