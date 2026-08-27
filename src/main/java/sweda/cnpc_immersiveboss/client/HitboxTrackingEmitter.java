package sweda.cnpc_immersiveboss.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TrackingEmitter;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Vanilla tracking emitter whose initial particle cloud is centered on a hit OBB. */
public final class HitboxTrackingEmitter extends TrackingEmitter {
    private final ParticleOptions particleType;
    private final Vec3 center;
    private final int hitboxLifetime;
    private int hitboxAge;

    public HitboxTrackingEmitter(ClientLevel level, Entity target, ParticleOptions particleType,
                                 int lifetime, Vec3 center) {
        // TrackingEmitter calls tick() from its constructor. The override is a no-op
        // until this subclass has initialized its center and lifetime.
        super(level, target, particleType, lifetime);
        this.particleType = particleType;
        this.center = center;
        this.hitboxLifetime = lifetime;
        this.hitboxAge = 0;
        tick();
    }

    @Override
    public void tick() {
        if (center == null || particleType == null) return;

        for (int i = 0; i < 16; ++i) {
            double dx = random.nextFloat() * 2.0F - 1.0F;
            double dy = random.nextFloat() * 2.0F - 1.0F;
            double dz = random.nextFloat() * 2.0F - 1.0F;
            if (dx * dx + dy * dy + dz * dz <= 1.0D) {
                level.addParticle(particleType, false,
                    center.x + dx / 4.0D,
                    center.y + dy / 4.0D,
                    center.z + dz / 4.0D,
                    dx, dy + 0.2D, dz);
            }
        }

        if (++hitboxAge >= hitboxLifetime) remove();
    }
}
