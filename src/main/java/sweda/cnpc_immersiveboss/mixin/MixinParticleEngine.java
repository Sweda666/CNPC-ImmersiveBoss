package sweda.cnpc_immersiveboss.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TrackingEmitter;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import sweda.cnpc_immersiveboss.client.DamageParticleData;
import sweda.cnpc_immersiveboss.client.HitboxTrackingEmitter;
import sweda.cnpc_immersiveboss.config.ClientConfig;

/** Re-centers vanilla critical-hit emitters on the server-confirmed hit OBB. */
@Mixin(net.minecraft.client.particle.ParticleEngine.class)
public abstract class MixinParticleEngine {
    @Redirect(
        method = "createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;)V",
        at = @At(value = "NEW", target = "net/minecraft/client/particle/TrackingEmitter"))
    private TrackingEmitter cnpc_immersiveboss$trackingEmitter(
            ClientLevel level, Entity entity, ParticleOptions options) {
        if (ClientConfig.DAMAGE_PARTICLES_FOLLOW_HITBOX.get()) {
            Vec3 center = DamageParticleData.findCenter(entity);
            if (center != null) {
                return new HitboxTrackingEmitter(level, entity, options, 3, center);
            }
        }
        return new TrackingEmitter(level, entity, options);
    }

    @Redirect(
        method = "createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;I)V",
        at = @At(value = "NEW", target = "net/minecraft/client/particle/TrackingEmitter"))
    private TrackingEmitter cnpc_immersiveboss$trackingEmitterWithLifetime(
            ClientLevel level, Entity entity, ParticleOptions options, int lifetime) {
        if (ClientConfig.DAMAGE_PARTICLES_FOLLOW_HITBOX.get()) {
            Vec3 center = DamageParticleData.findCenter(entity);
            if (center != null) {
                return new HitboxTrackingEmitter(level, entity, options, lifetime, center);
            }
        }
        return new TrackingEmitter(level, entity, options, lifetime);
    }
}
