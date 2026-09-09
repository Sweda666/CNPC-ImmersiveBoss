package sweda.cnpc_immersiveboss.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Hides the real victim while the embedded Gecko player puppet is active. */
public final class ThrowClientEvents {
    private ThrowClientEvents() {
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        StruggleClientState.key(event.getKey(), event.getScanCode(), event.getAction());
    }

    @SubscribeEvent
    public static void onMouse(InputEvent.MouseButton.Post event) {
        StruggleClientState.mouse(event.getButton(), event.getAction());
    }

    @SubscribeEvent
    public static void onGui(RenderGuiEvent.Post event) {
        StruggleClientState.render(event.getGuiGraphics());
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ThrowClientState.clear();
    }

    @SubscribeEvent
    public static void onMovement(MovementInputUpdateEvent event) {
        if (!ThrowClientState.isActiveForTarget(event.getEntity().getId())) return;
        var input = event.getInput();
        input.leftImpulse = 0;
        input.forwardImpulse = 0;
        input.up = input.down = input.left = input.right = false;
        input.jumping = input.shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (ThrowClientState.isActiveForTarget(entity.getId())) {
            event.setCanceled(true);
        }
    }

    /**
     * Some renderer replacements dispatch this player-specific event without
     * going through the generic living-entity path. Keep the source player
     * hidden in those paths as well so only the Gecko victim puppet is visible.
     */
    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (ThrowClientState.isActiveForTarget(player.getId())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null
            && ThrowClientState.isActiveForTarget(minecraft.player.getId())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) ThrowClientState.tick();
    }
}
