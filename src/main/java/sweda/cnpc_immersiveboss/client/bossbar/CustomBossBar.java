package sweda.cnpc_immersiveboss.client.bossbar;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class CustomBossBar {
    private final ResourceLocation customBossBar;
    private final float colorR, colorG, colorB;
    private final int value;
    private final int max;
    private final int xShift;
    private final double xScale;
    private final double yScale;
    private final int width;
    private final int height;

    public CustomBossBar(ResourceLocation customBossBar, int color, int value, int max, int xShift, double xScale, double yScale, int width, int height) {
        this.customBossBar = customBossBar;
        this.colorR = ((color >> 16) & 0xFF) / 255.0F;
        this.colorG = ((color >> 8) & 0xFF) / 255.0F;
        this.colorB = (color & 0xFF) / 255.0F;
        this.xShift = xShift;
        this.value = value;
        this.max = max;
        this.xScale = xScale;
        this.yScale = yScale;
        this.width = width;
        this.height = height;
    }

    public int render(GuiGraphics guiGraphics, int y) {
        if (customBossBar == null) return 0;
        if (value <= 0) return 0;
        int textureWidth = width;
        int textureHeight = height;

        if (textureWidth == 0 || textureHeight == 0) return 0;

        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();

        RenderSystem.setShaderTexture(0, customBossBar);

        int displayWidth = (int) (textureWidth * xScale);
        int displayHeight = (int) (textureHeight * yScale);
        int barHeight = displayHeight / 2;           // 上半=边框, 下半=填充
        float fillVOffset = displayHeight * 0.5f;    // 精确中心，避免整数截断导致的 0.5px 偏差
        int x = screenWidth / 2 - displayWidth / 2;

        float healthPercent = Math.min(1.0F, (float) value / max);
        int progress = (int) (((displayWidth - xShift * xScale * 2) * healthPercent) + (xShift * xScale));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(colorR, colorG, colorB, 1.0F);
        guiGraphics.blit(customBossBar, x, y, 0f, fillVOffset, progress, barHeight, displayWidth, displayHeight);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        guiGraphics.blit(customBossBar, x, y, 0f, 0f, displayWidth, barHeight, displayWidth, displayHeight);

        return barHeight;
    }
}