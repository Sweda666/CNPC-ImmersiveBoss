package sweda.cnpc_immersiveboss.mixin.compat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sweda.cnpc_immersiveboss.api.IHitboxDamageContext;
import sweda.cnpc_immersiveboss.api.IOBBHolder;
import sweda.cnpc_immersiveboss.compat.ironsspellbooks.IronsSpellbooksCompat;
import sweda.cnpc_immersiveboss.event.ProjectileOBBListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Pseudo
@Mixin(targets = "io.redspace.ironsspellbooks.entity.spells.AoeEntity", remap = false)
public abstract class MixinIronsAoeEntity implements IHitboxDamageContext {
    @Unique
    private final Map<Integer, String> cnpc_immersiveboss$hitboxes = new HashMap<>();
    @Unique
    private String cnpc_immersiveboss$activeHitboxName;
    @Unique
    private boolean cnpc_immersiveboss$hasDamageEventResult;
    @Unique
    private boolean cnpc_immersiveboss$damageEventResult;

    @Redirect(method = "checkHits",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"),
        remap = false, require = 0)
    private <T extends LivingEntity> List<T> cnpc_immersiveboss$mappedAoeTargets(
            Level level, Class<T> entityClass, AABB box) {
        return cnpc_immersiveboss$mergeAoeTargets(level, entityClass, box);
    }

    @Redirect(method = "checkHits",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;m_45976_(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"),
        remap = false, require = 0)
    private <T extends LivingEntity> List<T> cnpc_immersiveboss$srgAoeTargets(
            Level level, Class<T> entityClass, AABB box) {
        return cnpc_immersiveboss$mergeAoeTargets(level, entityClass, box);
    }

    @Unique
    private <T extends LivingEntity> List<T> cnpc_immersiveboss$mergeAoeTargets(
            Level level, Class<T> entityClass, AABB box) {
        cnpc_immersiveboss$hitboxes.clear();
        List<T> nativeTargets = level.getEntitiesOfClass(entityClass, box);
        return IronsSpellbooksCompat.mergeAoeTargets((Projectile) (Object) this,
            entityClass, box, nativeTargets, cnpc_immersiveboss$hitboxes);
    }

    @Redirect(method = "checkHits",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;distanceToSqr(Lnet/minecraft/world/entity/Entity;)D"),
        remap = false, require = 0)
    private double cnpc_immersiveboss$mappedObbDistance(LivingEntity target, Entity aoe) {
        return IronsSpellbooksCompat.distanceToAoeSqr(target, aoe);
    }

    @Redirect(method = "checkHits",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;m_20280_(Lnet/minecraft/world/entity/Entity;)D"),
        remap = false, require = 0)
    private double cnpc_immersiveboss$srgObbDistance(LivingEntity target, Entity aoe) {
        return IronsSpellbooksCompat.distanceToAoeSqr(target, aoe);
    }

    @ModifyArg(method = "checkHits",
        at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/entity/spells/AoeEntity;applyEffect(Lnet/minecraft/world/entity/LivingEntity;)V"),
        index = 0, remap = false, require = 0)
    private LivingEntity cnpc_immersiveboss$beginEffect(LivingEntity target) {
        String hitbox = cnpc_immersiveboss$hitboxes.get(target.getId());
        cnpc_immersiveboss$activeHitboxName = hitbox;
        cnpc_immersiveboss$hasDamageEventResult = false;
        if (hitbox != null && target instanceof IOBBHolder holder) {
            holder.cnpc_immersiveboss$setLastHitboxName(hitbox);
            ProjectileOBBListener.recordNativeProjectileDamage(
                (Projectile) (Object) this, target.getId());
        }
        return target;
    }

    @Inject(method = "checkHits", at = @At("RETURN"), remap = false, require = 0)
    private void cnpc_immersiveboss$clearEffect(CallbackInfo ci) {
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
