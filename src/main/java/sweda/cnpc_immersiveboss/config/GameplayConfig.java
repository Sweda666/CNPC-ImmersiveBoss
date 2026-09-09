package sweda.cnpc_immersiveboss.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class GameplayConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue DISABLE_THROW_TARGET_ATTACK;
    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("throw");
        DISABLE_THROW_TARGET_ATTACK = builder.comment(
            "Prevent a player controlled by a throw from attacking until the throw ends.")
            .define("disableTargetAttack", true);
        builder.pop();
        SPEC = builder.build();
    }
    private GameplayConfig() {}
}
