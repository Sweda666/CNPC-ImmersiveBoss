package sweda.cnpc_immersiveboss.mixin.compat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import sweda.cnpc_immersiveboss.compat.epicfight.EpicFightCompat;

import java.util.List;
import java.util.function.Predicate;

@Pseudo
@Mixin(targets = "yesman.epicfight.api.collider.Collider", remap = false)
public abstract class MixinEpicFightCollider {
    @Redirect(method = "getCollideEntities",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"),
        remap = false, require = 0)
    private List<Entity> cnpc_immersiveboss$mappedFindObbTargets(
            Level level, Entity excluded, AABB area,
            Predicate<? super Entity> predicate) {
        return EpicFightCompat.getEntitiesIncludingObbs(
            level, excluded, area, predicate);
    }

    @Redirect(method = "getCollideEntities",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;m_6249_(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"),
        remap = false, require = 0)
    private List<Entity> cnpc_immersiveboss$srgFindObbTargets(
            Level level, Entity excluded, AABB area,
            Predicate<? super Entity> predicate) {
        return EpicFightCompat.getEntitiesIncludingObbs(
            level, excluded, area, predicate);
    }
}
