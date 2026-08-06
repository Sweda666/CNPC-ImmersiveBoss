package sweda.cnpc_immersiveboss.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.event.NpcEvent;
import noppes.npcs.entity.data.DataDisplay;
import sweda.cnpc_immersiveboss.api.IMixinDataDisplay;
import sweda.cnpc_immersiveboss.network.NetworkHandler;
import sweda.cnpc_immersiveboss.network.packet.SyncCustomBossBarPacket;

import java.util.HashMap;
import java.util.Map;

public class NpcUpdateListener {

    private static final int COMBAT_TIMEOUT_TICKS = 100;
    private final Map<Integer, Boolean> lastCombatState = new HashMap<>();

    @SubscribeEvent
    public void onNpcUpdate(NpcEvent.UpdateEvent event) {
        // 服务端判断 NPC 当前是否在战斗中（字段在服务端可用）
        boolean inCombat = isNpcInCombat(event.npc);
        syncBossBar(event.npc, inCombat);
    }

    @SubscribeEvent
    public void onNpcDamaged(NpcEvent.DamagedEvent event) {
        DataDisplay data = (DataDisplay) event.npc.getDisplay();
        if (data.getBossbar() == 4) {
            syncBossBar(event.npc, true);
        }
    }

    @SubscribeEvent
    public void onNpcMeleeAttack(NpcEvent.MeleeAttackEvent event) {
        DataDisplay data = (DataDisplay) event.npc.getDisplay();
        if (data.getBossbar() == 4) {
            syncBossBar(event.npc, true);
        }
    }

    @SubscribeEvent
    public void onNpcTarget(NpcEvent.TargetEvent event) {
        DataDisplay data = (DataDisplay) event.npc.getDisplay();
        if (data.getBossbar() == 4) {
            syncBossBar(event.npc, true);
        }
    }

    /**
     * 服务端检查 NPC 是否处于战斗中
     */
    private static boolean isNpcInCombat(ICustomNpc npc) {
        net.minecraft.world.entity.Entity entity = npc.getMCEntity();
        if (!(entity instanceof LivingEntity)) return false;
        LivingEntity living = (LivingEntity) entity;
        // 检查是否有攻击目标
        if (entity instanceof Mob) {
            Mob mob = (Mob) entity;
            if (mob.getTarget() != null) return true;
        }
        // 检查最近是否受伤或造成伤害
        long gameTime = living.level().getGameTime();
        int lastHurt = living.getLastHurtByMobTimestamp();
        int lastHurtMob = living.getLastHurtMobTimestamp();
        return (lastHurt != 0 && gameTime - lastHurt < COMBAT_TIMEOUT_TICKS)
                || (lastHurtMob != 0 && gameTime - lastHurtMob < COMBAT_TIMEOUT_TICKS);
    }

    private void syncBossBar(ICustomNpc npc, boolean combatActive) {
        DataDisplay data = (DataDisplay) npc.getDisplay();
        int showBossBar = data.getBossbar();

        if (showBossBar < 3) {
            return;
        }

        // 跳过 mode 4 下战斗状态未变化的冗余同步
        int entityId = npc.getMCEntity().getId();
        if (showBossBar == 4) {
            Boolean last = lastCombatState.get(entityId);
            if (last != null && last.equals(combatActive)) {
                return;
            }
        }
        lastCombatState.put(entityId, combatActive);

        String texture = ((IMixinDataDisplay) data).getCustomBossBar();
        int color = ((IMixinDataDisplay) data).getCustomBossColor();
        int xShift = ((IMixinDataDisplay) data).getCustomBossXShift();
        double xScale = ((IMixinDataDisplay) data).getCustomBossXScale();
        double yScale = ((IMixinDataDisplay) data).getCustomBossYScale();
        int bossWidth = ((IMixinDataDisplay) data).getCustomBossWidth();
        int bossHeight = ((IMixinDataDisplay) data).getCustomBossHeight();

        SyncCustomBossBarPacket packet = new SyncCustomBossBarPacket(
                entityId, texture, color, xShift, showBossBar,
                xScale, yScale, bossWidth, bossHeight, combatActive);

        IPlayer[] players = npc.getWorld().getAllPlayers();
        for (IPlayer player : players) {
            ServerPlayer splayer = player.getMCEntity();
            NetworkHandler.sendToPlayer(splayer, packet);
        }
    }
}
