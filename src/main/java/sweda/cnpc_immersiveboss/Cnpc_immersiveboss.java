package sweda.cnpc_immersiveboss;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import org.slf4j.Logger;
import sweda.cnpc_immersiveboss.client.DebugOBBRenderer;
import sweda.cnpc_immersiveboss.client.renderer.RenderHandler;
import sweda.cnpc_immersiveboss.config.ClientConfig;
import sweda.cnpc_immersiveboss.event.EntityCollisionListener;
import sweda.cnpc_immersiveboss.event.HitboxDamageListener;
import sweda.cnpc_immersiveboss.event.NpcUpdateListener;
import sweda.cnpc_immersiveboss.event.ProjectileOBBListener;
import sweda.cnpc_immersiveboss.network.NetworkHandler;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Cnpc_immersiveboss.MODID)
public class Cnpc_immersiveboss {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "cnpc_immersiveboss";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PROTOCOL_VERSION = "1";

    public Cnpc_immersiveboss() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);

        // Register the Deferred Register to the mod event bus so tabs get registered
        // No need to register blocks or items anymore

        // Register ourselves for game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register the client setup method for modloading
        modEventBus.addListener(this::clientSetup);

        // Register common setup
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onLoadComplete);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Register network packets
        NetworkHandler.INSTANCE = NetworkRegistry.newSimpleChannel(
                ResourceLocation.fromNamespaceAndPath(Cnpc_immersiveboss.MODID, "main"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals
        );
        LOGGER.info("Registering network packets...");
        NetworkHandler.register();

        // Register OBB-precise projectile raycasting
        MinecraftForge.EVENT_BUS.register(ProjectileOBBListener.class);
        // Register tick-level entity-vs-entity OBB collision
        MinecraftForge.EVENT_BUS.register(EntityCollisionListener.class);
        // Register hitbox damage tracking — fires HitboxDamagedEvent on LivingHurtEvent
        MinecraftForge.EVENT_BUS.register(HitboxDamageListener.class);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        // Register the custom boss bar renderer
        LOGGER.info("Registering custom boss bar renderer...");
        MinecraftForge.EVENT_BUS.register(new RenderHandler());

        // Register OBB debug wireframe renderer (shown with F3+B)
        MinecraftForge.EVENT_BUS.register(DebugOBBRenderer.class);
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Additional client setup if needed in the future
            LOGGER.info("HELLO FROM CLIENT SETUP");
        }
    }

    public void onLoadComplete(net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent event) {
        try {
            var api = noppes.npcs.api.NpcAPI.Instance();
            if (api != null) {
                api.events().register(new NpcUpdateListener());
            } else {
                System.err.println("=== [失败] NpcAPI 实例为空 ===");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

