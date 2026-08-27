package sweda.cnpc_immersiveboss.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue SHOW_OBB_NAMES;
    public static final ForgeConfigSpec.BooleanValue DAMAGE_PARTICLES_FOLLOW_HITBOX;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("debug");
        SHOW_OBB_NAMES = builder
            .comment("Show each OBB name at its center while F3+B hitboxes are enabled.")
            .define("showObbNames", true);
        builder.pop();

        builder.push("effects");
        DAMAGE_PARTICLES_FOLLOW_HITBOX = builder
            .comment("Place damage, critical-hit, and damage-indicator particles at the hit OBB when available.")
            .define("damageParticlesFollowHitbox", true);
        builder.pop();
        SPEC = builder.build();
    }

    private ClientConfig() {}
}
