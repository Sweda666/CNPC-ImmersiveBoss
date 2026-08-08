package sweda.cnpc_immersiveboss.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue SHOW_OBB_NAMES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("debug");
        SHOW_OBB_NAMES = builder
            .comment("Show each OBB name at its center while F3+B hitboxes are enabled.")
            .define("showObbNames", true);
        builder.pop();
        SPEC = builder.build();
    }

    private ClientConfig() {}
}
