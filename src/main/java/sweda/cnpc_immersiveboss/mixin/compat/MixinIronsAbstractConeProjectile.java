package sweda.cnpc_immersiveboss.mixin.compat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sweda.cnpc_immersiveboss.api.IHitboxDamageContext;
import sweda.cnpc_immersiveboss.api.IOBBHolder;
import sweda.cnpc_immersiveboss.compat.ironsspellbooks.IronsSpellbooksCompat;
import sweda.cnpc_immersiveboss.event.ProjectileOBBListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Pseudo
@Mixin(targets = "io.redspace.ironsspellbooks.entity.spells.AbstractConeProjectile",
    remap = false)
public abstract class MixinIronsAbstractConeProjectile implements IHitboxDamageContext {
    @Unique
    private final Map<Integer, String> cnpc_immersiveboss$hitboxes = new HashMap<>();
    @Unique
    private String cnpc_immersiveboss$activeHitboxName;
    @Unique
    private boolean cnpc_immersiveboss$hasDamageEventResult;
    @Unique
    private boolean cnpc_immersiveboss$damageEventResult;

    @Inject(method = "getSubEntityCollisions", at = @At("RETURN"),
        cancellable = true, remap = false, require = 0)
    private void cnpc_immersiveboss$findObbTargets(CallbackInfoReturnable<Set<Entity>> cir) {
        cnpc_immersiveboss$hitboxes.clear();
        cir.setReturnValue(IronsSpellbooksCompat.mergeConeTargets(
            (Projectile) (Object) this, cir.getReturnValue(), cnpc_immersiveboss$hitboxes));
    }

    @ModifyArg(method = {"tick()V", "m_8119_()V"},
        at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/entity/spells/AbstractConeProjectile;onHitEntity(Lnet/minecraft/world/phys/EntityHitResult;)V"),
        index = 0, remap = false, require = 0)
    private EntityHitResult cnpc_immersiveboss$beginMappedHit(EntityHitResult hit) {
        return cnpc_immersiveboss$beginHit(hit);
    }

    @ModifyArg(method = {"tick()V", "m_8119_()V"},
        at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/entity/spells/AbstractConeProjectile;m_5790_(Lnet/minecraft/world/phys/EntityHitResult;)V"),
        index = 0, remap = false, require = 0)
    private EntityHitResult cnpc_immersiveboss$beginSrgHit(EntityHitResult hit) {
        return cnpc_immersiveboss$beginHit(hit);
    }

    @Unique
    private EntityHitResult cnpc_immersiveboss$beginHit(EntityHitResult hit) {
        Entity target = hit.getEntity();
        String hitbox = cnpc_immersiveboss$hitboxes.get(target.getId());
        cnpc_immersiveboss$activeHitboxName = hitbox;
        cnpc_immersiveboss$hasDamageEventResult = false;
        if (hitbox != null && target instanceof IOBBHolder holder) {
            holder.cnpc_immersiveboss$setLastHitboxName(hitbox);
            ProjectileOBBListener.recordNativeProjectileDamage(
                (Projectile) (Object) this, target.getId());
        }
        return hit;
    }

    @Inject(method = {"tick()V", "m_8119_()V"}, at = @At("RETURN"),
        remap = false, require = 0)
    private void cnpc_immersiveboss$clearHit(CallbackInfo ci) {
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
