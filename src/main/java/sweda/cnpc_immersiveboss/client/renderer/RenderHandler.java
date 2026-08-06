package sweda.cnpc_immersiveboss.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import sweda.cnpc_immersiveboss.client.bossbar.ClientBossBarData;
import sweda.cnpc_immersiveboss.client.bossbar.CustomBossBar;

import java.util.Map;

public class RenderHandler {

    private static final double MAX_RENDER_DISTANCE = 128.0;
    private static final int BAR_GAP = 6;    // 血条之间的间距
    private static final int TOP_MARGIN = 10; // 第一条血条距屏幕顶部的距离
    private static final int MAX_BARS = 3;    // 最多同时渲染 3 条自定义血条

    @SubscribeEvent
    public void onRenderGuiOverlayEvent(RenderGuiOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        GuiGraphics guiGraphics = event.getGuiGraphics();
        Map<Integer, ClientBossBarData.BossBarEntry> bossBars = ClientBossBarData.getAllBossBars();

        int nextY = TOP_MARGIN;
        int barCount = 0;

        for (ClientBossBarData.BossBarEntry entry : bossBars.values()) {
            if (barCount >= MAX_BARS) break;

            // 获取实体
            Entity entity = mc.level.getEntity(entry.entityId);
            if (!(entity instanceof LivingEntity livingEntity)) continue;

            // Mode 3: 超过 128 格不显示
            if (entry.bossBarMode == 3 && mc.player.distanceTo(entity) > MAX_RENDER_DISTANCE) continue;
            // Mode 4: only show in combat (checked via server-synced combat timer)
            if (entry.bossBarMode == 4 && !ClientBossBarData.isInCombat(entry.entityId)) continue;

            // Use pre-parsed ResourceLocation from BossBarEntry
            ResourceLocation resourceLocation = entry.resourceLocation;
            if (resourceLocation == null) continue;

            // 获取血量
            float health = livingEntity.getHealth();
            float maxHealth = livingEntity.getMaxHealth();

            // 创建并渲染 BossBar
            CustomBossBar bossBar = new CustomBossBar(
                    resourceLocation,
                    entry.color,
                    (int) health,
                    (int) maxHealth,
                    entry.xShift,
                    entry.xScale,
                    entry.yScale,
                    entry.width,
                    entry.height
            );
            int usedHeight = bossBar.render(guiGraphics, nextY);
            nextY += usedHeight + BAR_GAP;
            barCount++;
        }
    }
}
