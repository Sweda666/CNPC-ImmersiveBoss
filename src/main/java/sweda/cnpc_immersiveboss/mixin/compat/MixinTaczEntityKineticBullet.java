package sweda.cnpc_immersiveboss.mixin.compat;

import com.tacz.guns.entity.EntityKineticBullet.EntityResult;
import com.tacz.guns.util.TacHitResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sweda.cnpc_immersiveboss.api.IHitboxDamageContext;
import sweda.cnpc_immersiveboss.api.IOBBHolder;
import sweda.cnpc_immersiveboss.compat.tacz.TaczProjectileCompat;
import sweda.cnpc_immersiveboss.event.ProjectileOBBListener;
import sweda.cnpc_immersiveboss.event.ProjectileOBBListener.ProjectileHit;

import java.util.List;

@Mixin(targets = "com.tacz.guns.entity.EntityKineticBullet", remap = false)
public abstract class MixinTaczEntityKineticBullet implements IHitboxDamageContext {
    @Unique
    private String cnpc_immersiveboss$activeHitboxName;

    @Unique
    private boolean cnpc_immersiveboss$hasDamageEventResult;

    @Unique
    private boolean cnpc_immersiveboss$damageEventResult;

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

    @Redirect(method = "onBulletTick",
        at = @At(value = "INVOKE",
            target = "Lcom/tacz/guns/util/EntityUtil;findEntityOnPath(Lnet/minecraft/world/entity/projectile/Projectile;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Lcom/tacz/guns/entity/EntityKineticBullet$EntityResult;"),
        remap = false,
        require = 0)
    private EntityResult cnpc_immersiveboss$findEntityOnPath(
            Projectile projectile, Vec3 rayStart, Vec3 rayEnd) {
        return TaczProjectileCompat.findEntityOnPath(projectile, rayStart, rayEnd);
    }

    @Redirect(method = "onBulletTick",
        at = @At(value = "INVOKE",
            target = "Lcom/tacz/guns/util/EntityUtil;findEntitiesOnPath(Lnet/minecraft/world/entity/projectile/Projectile;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Ljava/util/List;"),
        remap = false,
        require = 0)
    private List<EntityResult> cnpc_immersiveboss$findEntitiesOnPath(
            Projectile projectile, Vec3 rayStart, Vec3 rayEnd) {
        return TaczProjectileCompat.findEntitiesOnPath(projectile, rayStart, rayEnd);
    }

    @Inject(method = "onHitEntity", at = @At("HEAD"), remap = false, require = 0)
    private void cnpc_immersiveboss$recordHitbox(TacHitResult result, Vec3 rayStart,
                                                  Vec3 rayEnd, CallbackInfo ci) {
        Entity target = result.getEntity();
        ProjectileHit hit = TaczProjectileCompat.findHit(target, rayStart, rayEnd);
        if (hit == null || !(target instanceof IOBBHolder holder)) return;

        cnpc_immersiveboss$activeHitboxName = hit.hitboxName;
        cnpc_immersiveboss$hasDamageEventResult = false;
        holder.cnpc_immersiveboss$setLastHitboxName(hit.hitboxName);
        ProjectileOBBListener.recordNativeProjectileDamage(
            (Projectile) (Object) this, target.getId());
    }

    @Inject(method = "onHitEntity", at = @At("RETURN"), remap = false, require = 0)
    private void cnpc_immersiveboss$clearHitbox(TacHitResult result, Vec3 rayStart,
                                                 Vec3 rayEnd, CallbackInfo ci) {
        Entity target = result.getEntity();
        if (target instanceof IOBBHolder holder) {
            holder.cnpc_immersiveboss$setLastHitboxName(null);
        }
        cnpc_immersiveboss$activeHitboxName = null;
        cnpc_immersiveboss$hasDamageEventResult = false;
    }
}
