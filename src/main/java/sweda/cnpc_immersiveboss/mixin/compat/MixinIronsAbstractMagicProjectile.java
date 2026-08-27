package sweda.cnpc_immersiveboss.mixin.compat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sweda.cnpc_immersiveboss.api.IHitboxDamageContext;
import sweda.cnpc_immersiveboss.api.IOBBHolder;
import sweda.cnpc_immersiveboss.compat.ironsspellbooks.IronsSpellbooksCompat;
import sweda.cnpc_immersiveboss.event.ProjectileOBBListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Pseudo
@Mixin(targets = "io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile",
    remap = false)
public abstract class MixinIronsAbstractMagicProjectile implements IHitboxDamageContext {
    @Unique
    private final Map<Integer, String> cnpc_immersiveboss$hitboxes = new HashMap<>();
    @Unique
    private String cnpc_immersiveboss$activeHitboxName;
    @Unique
    private boolean cnpc_immersiveboss$hasDamageEventResult;
    @Unique
    private boolean cnpc_immersiveboss$damageEventResult;

    @Inject(method = "raycastForEntitiesAlongPath", at = @At("RETURN"),
        cancellable = true, remap = false, require = 0)
    private void cnpc_immersiveboss$raycastObbs(
            Vec3 rayEnd, Vec3 rayStart, CallbackInfoReturnable<List<HitResult>> cir) {
        cnpc_immersiveboss$hitboxes.clear();
        List<HitResult> merged = IronsSpellbooksCompat.mergeProjectileHits(
            (Projectile) (Object) this, rayEnd, rayStart,
            IronsSpellbooksCompat.hitDetectionInflation(this),
            cir.getReturnValue(), cnpc_immersiveboss$hitboxes);
        cir.setReturnValue(merged);
    }

    @Inject(method = {
        "onHit(Lnet/minecraft/world/phys/HitResult;)V",
        "m_6532_(Lnet/minecraft/world/phys/HitResult;)V"
    }, at = @At("HEAD"), remap = false, require = 0)
    private void cnpc_immersiveboss$beginNativeHit(HitResult result, CallbackInfo ci) {
        if (!(result instanceof EntityHitResult entityHit)) return;
        Entity target = entityHit.getEntity();
        String hitbox = cnpc_immersiveboss$hitboxes.get(target.getId());
        if (hitbox == null || !(target instanceof IOBBHolder holder)) return;

        cnpc_immersiveboss$activeHitboxName = hitbox;
        cnpc_immersiveboss$hasDamageEventResult = false;
        holder.cnpc_immersiveboss$setLastHitboxName(hitbox);
        ProjectileOBBListener.recordNativeProjectileDamage(
            (Projectile) (Object) this, target.getId());
    }

    @Inject(method = {
        "onHit(Lnet/minecraft/world/phys/HitResult;)V",
        "m_6532_(Lnet/minecraft/world/phys/HitResult;)V"
    }, at = @At("RETURN"), remap = false, require = 0)
    private void cnpc_immersiveboss$endNativeHit(HitResult result, CallbackInfo ci) {
        cnpc_immersiveboss$activeHitboxName = null;
        cnpc_immersiveboss$hasDamageEventResult = false;
    }

    @Override
    public String cnpc_immersiveboss$getActiveHitboxName() {
        return cnpc_immersiveboss$activeHitboxName;
    }

    @Override
    public boolean cnpc_immersiveboss$hasDamageEventResult() {
        return cnpc_immersiveboss$hasDamageEventResult;
    }

    @Override
    public boolean cnpc_immersiveboss$getDamageEventResult() {
        return cnpc_immersiveboss$damageEventResult;
    }

    @Override
    public void cnpc_immersiveboss$setDamageEventResult(boolean result) {
        cnpc_immersiveboss$damageEventResult = result;
        cnpc_immersiveboss$hasDamageEventResult = true;
    }
}
