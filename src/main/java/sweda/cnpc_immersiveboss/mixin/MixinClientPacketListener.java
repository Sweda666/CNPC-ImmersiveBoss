package sweda.cnpc_immersiveboss.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sweda.cnpc_immersiveboss.client.DamageParticleData;
import sweda.cnpc_immersiveboss.config.ClientConfig;

/** Moves vanilla server-sent damage-indicator particles to the matching hit OBB. */
@Mixin(ClientPacketListener.class)
public abstract class MixinClientPacketListener {
    @Shadow @Final private RandomSource random;

    @Inject(method = "handleParticleEvent", at = @At("HEAD"), cancellable = true)
    private void cnpc_immersiveboss$damageIndicatorPosition(
            ClientboundLevelParticlesPacket packet, CallbackInfo ci) {
        if (!ClientConfig.DAMAGE_PARTICLES_FOLLOW_HITBOX.get()
            || packet.getParticle().getType() != ParticleTypes.DAMAGE_INDICATOR) return;

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Vec3 center = DamageParticleData.findCenterNear(level, packet.getX(), packet.getY(), packet.getZ());
        if (center == null) return;

        if (packet.getCount() == 0) {
            double vx = packet.getMaxSpeed() * packet.getXDist();
            double vy = packet.getMaxSpeed() * packet.getYDist();
            double vz = packet.getMaxSpeed() * packet.getZDist();
            level.addParticle(packet.getParticle(), packet.isOverrideLimiter(),
                center.x, center.y, center.z, vx, vy, vz);
        } else {
            for (int i = 0; i < packet.getCount(); ++i) {
                double ox = random.nextGaussian() * packet.getXDist();
                double oy = random.nextGaussian() * packet.getYDist();
                double oz = random.nextGaussian() * packet.getZDist();
                double vx = random.nextGaussian() * packet.getMaxSpeed();
                double vy = random.nextGaussian() * packet.getMaxSpeed();
                double vz = random.nextGaussian() * packet.getMaxSpeed();
                level.addParticle(packet.getParticle(), packet.isOverrideLimiter(),
                    center.x + ox, center.y + oy, center.z + oz, vx, vy, vz);
            }
        }
        ci.cancel();
    }
}
