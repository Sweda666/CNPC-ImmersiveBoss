package sweda.cnpc_immersiveboss.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import sweda.cnpc_immersiveboss.config.GameplayConfig;
import sweda.cnpc_immersiveboss.throwing.ThrowManager;

public final class ThrowAttackListener {
    private ThrowAttackListener() {}

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (GameplayConfig.DISABLE_THROW_TARGET_ATTACK.get()
            && event.getEntity() instanceof ServerPlayer player
            && ThrowManager.isActive(player)) {
            event.setCanceled(true);
        }
    }
}
