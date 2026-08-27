package sweda.cnpc_immersiveboss.mixin.compat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import sweda.cnpc_immersiveboss.api.IOBBHolder;
import sweda.cnpc_immersiveboss.compat.ironsspellbooks.IronsSpellbooksCompat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Pseudo
@Mixin(targets = "io.redspace.ironsspellbooks.entity.spells.ChainLightning", remap = false)
public abstract class MixinIronsChainLightning {
    @Unique
    private final Map<Integer, String> cnpc_immersiveboss$hitboxes = new HashMap<>();

    @Redirect(method = {"tick()V", "m_8119_()V"},
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"),
        remap = false, require = 0)
    private List<Entity> cnpc_immersiveboss$mappedChainTargets(
            Level level, Entity excluded, AABB box, Predicate<? super Entity> predicate) {
        return cnpc_immersiveboss$mergeChainTargets(level, excluded, box, predicate);
    }

    @Redirect(method = {"tick()V", "m_8119_()V"},
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;m_6249_(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"),
        remap = false, require = 0)
    private List<Entity> cnpc_immersiveboss$srgChainTargets(
            Level level, Entity excluded, AABB box, Predicate<? super Entity> predicate) {
        return cnpc_immersiveboss$mergeChainTargets(level, excluded, box, predicate);
    }

    private List<Entity> cnpc_immersiveboss$mergeChainTargets(
            Level level, Entity excluded, AABB box, Predicate<? super Entity> predicate) {
        cnpc_immersiveboss$hitboxes.clear();
        List<Entity> nativeTargets = level.getEntities(excluded, box, predicate);
        if (!(level instanceof ServerLevel serverLevel)) return nativeTargets;
        return IronsSpellbooksCompat.mergeChainTargets(serverLevel, excluded,
            box, predicate, nativeTargets, cnpc_immersiveboss$hitboxes);
    }

    @Redirect(method = "lambda$tick$1",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;distanceToSqr(Lnet/minecraft/world/entity/Entity;)D"),
        remap = false, require = 0)
    private double cnpc_immersiveboss$mappedChainDistance(Entity target, Entity origin) {
        return IronsSpellbooksCompat.distanceToAttackableSqr(target, origin);
    }

    @Redirect(method = "lambda$tick$1",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;m_20280_(Lnet/minecraft/world/entity/Entity;)D"),
        remap = false, require = 0)
    private double cnpc_immersiveboss$srgChainDistance(Entity target, Entity origin) {
        return IronsSpellbooksCompat.distanceToAttackableSqr(target, origin);
    }

    @Redirect(method = "lambda$tick$0",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;distanceToSqr(Lnet/minecraft/world/entity/Entity;)D"),
        remap = false, require = 0)
    private static double cnpc_immersiveboss$mappedChainSortDistance(
            Entity target, Entity origin) {
        return IronsSpellbooksCompat.distanceToAttackableSqr(target, origin);
    }

    @Redirect(method = "lambda$tick$0",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;m_20280_(Lnet/minecraft/world/entity/Entity;)D"),
        remap = false, require = 0)
    private static double cnpc_immersiveboss$srgChainSortDistance(
            Entity target, Entity origin) {
        return IronsSpellbooksCompat.distanceToAttackableSqr(target, origin);
    }

    @ModifyArg(method = "lambda$tick$1",
        at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/entity/spells/ChainLightning;doHurt(Lnet/minecraft/world/entity/Entity;)V"),
        index = 0, remap = false, require = 0)
    private Entity cnpc_immersiveboss$attributeChainHit(Entity target) {
        String hitbox = cnpc_immersiveboss$hitboxes.get(target.getId());
        if (hitbox != null && target instanceof IOBBHolder holder) {
            holder.cnpc_immersiveboss$setLastHitboxName(hitbox);
        }
        return target;
    }
}
