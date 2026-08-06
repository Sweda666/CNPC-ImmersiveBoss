package sweda.cnpc_immersiveboss.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import noppes.npcs.entity.EntityNPCInterface;
import sweda.cnpc_immersiveboss.api.IOBBHolder;

/**
 * Cleans up {@code lastHitboxName} after each damage event to prevent
 * stale values leaking into the next {@code hurt()} call.
 * <p>
 * Hitbox detection (melee raycast) now runs in
 * {@code MixinEntityNPCInterface.cnpc_multihitbox$detectHitboxHead}
 * at the HEAD of {@code EntityNPCInterface.hurt()}, BEFORE CNPC
 * dispatches its {@code NpcEvent.DamagedEvent} to scripts.
 * <p>
 * The hitbox name is made available to scripts via {@code e.hitboxName}
 * in the {@code damaged(e)} handler — no {@code ImmersiveBossAPI} call needed.
 */
public class HitboxDamageListener {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof EntityNPCInterface)) return;
        if (!(entity instanceof IOBBHolder holder)) return;

        // Clear the last hitbox name after the damage flow completes.
        // Melee detection ran in Mixin's HEAD of hurt(), projectile name
        // was set by ProjectileOBBListener — both consumed by ModifyArg
        // which injected the name into NpcEvent.DamagedEvent.hitboxName.
        holder.cnpc_immersiveboss$setLastHitboxName(null);
    }
}
