package sweda.cnpc_immersiveboss.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import noppes.npcs.client.gui.mainmenu.GuiNpcDisplay;
import noppes.npcs.entity.data.DataDisplay;
import sweda.cnpc_immersiveboss.api.IMixinDataDisplay;
import sweda.cnpc_immersiveboss.api.IMixinGuiNpcDisplay;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SubGuiCustomBossBar extends Screen {

    private final GuiNpcDisplay parent;
    private final DataDisplay display;
    private EditBox textureField;
    private EditBox colorField;
    private EditBox xShiftField;
    private EditBox xScaleField;
    private EditBox yScaleField;
    private EditBox widthField;
    private EditBox heightField;

    public SubGuiCustomBossBar(GuiNpcDisplay parent) {
        super(Component.translatable("cnpc_immersiveboss.gui.title"));
        this.parent = parent;
        this.display = ((IMixinGuiNpcDisplay) parent).getDisplay();
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int leftX = centerX - 150;
        int rightX = centerX + 100;

        // ========== 左侧组件 ==========
        // 纹理路径输入框 (y=50)
        this.textureField = new EditBox(this.font, leftX, 50, 145, 20, Component.translatable("cnpc_immersiveboss.gui.texture_path"));
        this.textureField.setMaxLength(256);
        if (this.display instanceof IMixinDataDisplay mixinDisplay) {
            this.textureField.setValue(mixinDisplay.getCustomBossBar());
        }
        this.addRenderableWidget(this.textureField);

        // 纹理选择按钮
        this.addRenderableWidget(Button.builder(Component.translatable("cnpc_immersiveboss.gui.select"), button -> {
            openTextureSelector();
        }).bounds(leftX + 150, 50, 35, 20).build());

        // 颜色输入框 (y=95)
        this.colorField = new EditBox(this.font, leftX, 95, 200, 20, Component.translatable("cnpc_immersiveboss.gui.color"));
        this.colorField.setMaxLength(8);
        if (this.display instanceof IMixinDataDisplay mixinDisplay) {
            int color = mixinDisplay.getCustomBossColor();
            if (color != -1) {
                this.colorField.setValue(String.format("%06X", color & 0xFFFFFF));
            }
        }
        this.addRenderableWidget(this.colorField);

        // X偏移输入框 (y=140)
        this.xShiftField = new EditBox(this.font, leftX, 140, 200, 20, Component.translatable("cnpc_immersiveboss.gui.x_shift"));
        this.xShiftField.setMaxLength(6);
        if (this.display instanceof IMixinDataDisplay mixinDisplay) {
            this.xShiftField.setValue(String.valueOf(mixinDisplay.getCustomBossXShift()));
        }
        this.addRenderableWidget(this.xShiftField);

        // ========== 右侧组件 ==========
        // X缩放输入框 (y=50)
        this.xScaleField = new EditBox(this.font, rightX, 50, 45, 20, Component.translatable("cnpc_immersiveboss.gui.x_scale_label"));
        this.xScaleField.setMaxLength(6);
        if (this.display instanceof IMixinDataDisplay mixinDisplay) {
            this.xScaleField.setValue(String.valueOf(mixinDisplay.getCustomBossXScale()));
        }
        this.addRenderableWidget(this.xScaleField);

        // Y缩放输入框 (y=50)
        this.yScaleField = new EditBox(this.font, rightX + 50, 50, 45, 20, Component.translatable("cnpc_immersiveboss.gui.y_scale_label"));
        this.yScaleField.setMaxLength(6);
        if (this.display instanceof IMixinDataDisplay mixinDisplay) {
            this.yScaleField.setValue(String.valueOf(mixinDisplay.getCustomBossYScale()));
        }
        this.addRenderableWidget(this.yScaleField);

        // 宽度输入框 (y=95)
        this.widthField = new EditBox(this.font, rightX, 95, 45, 20, Component.translatable("cnpc_immersiveboss.gui.width"));
        this.widthField.setMaxLength(4);
        if (this.display instanceof IMixinDataDisplay mixinDisplay) {
            this.widthField.setValue(String.valueOf(mixinDisplay.getCustomBossWidth()));
        }
        this.addRenderableWidget(this.widthField);

        // 高度输入框 (y=95)
        this.heightField = new EditBox(this.font, rightX + 50, 95, 45, 20, Component.translatable("cnpc_immersiveboss.gui.height"));
        this.heightField.setMaxLength(4);
        if (this.display instanceof IMixinDataDisplay mixinDisplay) {
            this.heightField.setValue(String.valueOf(mixinDisplay.getCustomBossHeight()));
        }
        this.addRenderableWidget(this.heightField);

        // 保存按钮 (y=140)
        this.addRenderableWidget(Button.builder(Component.translatable("cnpc_immersiveboss.gui.save"), button -> {
            saveSettings();
            this.onClose();
        }).bounds(rightX, 140, 45, 20).build());

        // 取消按钮 (y=140)
        this.addRenderableWidget(Button.builder(Component.translatable("cnpc_immersiveboss.gui.cancel"), button -> {
            this.onClose();
        }).bounds(rightX + 50, 140, 45, 20).build());
    }

    private void openTextureSelector() {
        List<String> textures = getAvailableTextures();
        SubGuiTextureSelector selector = new SubGuiTextureSelector(
                this,
                Component.translatable("cnpc_immersiveboss.gui.title").getString(),
                textures,
                selected -> {
                    // 直接写入 DataDisplay，init() 重新执行时会自动读取
                    if (this.display instanceof IMixinDataDisplay mixinDisplay) {
                        mixinDisplay.setCustomBossBar(selected);
                    }
                }
        );
        if (this.minecraft != null) {
            this.minecraft.setScreen(selector);
        }
    }

    private List<String> getAvailableTextures() {
        List<String> textures = new ArrayList<>();
        try {
            ResourceManager rm = Minecraft.getInstance().getResourceManager();
            Map<ResourceLocation, Resource> resources = rm.listResources(
                    "textures",
                    rl -> rl.getPath().endsWith(".png")
            );
            for (ResourceLocation rl : resources.keySet()) {
                textures.add(rl.getNamespace() + ":" + rl.getPath());
            }
        } catch (Exception e) {
            // Fallback: return empty list
        }
        return textures;
    }

    private void saveSettings() {
        if (this.display instanceof IMixinDataDisplay mixinDisplay) {
            // 保存纹理路径
            String texturePath = this.textureField.getValue();
            mixinDisplay.setCustomBossBar(texturePath.isEmpty() ? "" : texturePath);

            // 保存颜色
            String colorText = this.colorField.getValue();
            if (!colorText.isEmpty()) {
                try {
                    int color = (int) Long.parseLong(colorText, 16);
                    mixinDisplay.setCustomBossColor(color);
                } catch (NumberFormatException e) {
                    mixinDisplay.setCustomBossColor(-1);
                }
            } else {
                mixinDisplay.setCustomBossColor(-1);
            }

            // 保存X偏移
            String xShiftText = this.xShiftField.getValue();
            if (!xShiftText.isEmpty()) {
                try {
                    int xShift = Integer.parseInt(xShiftText);
                    mixinDisplay.setCustomBossXShift(xShift);
                } catch (NumberFormatException e) {
                    mixinDisplay.setCustomBossXShift(0);
                }
            } else {
                mixinDisplay.setCustomBossXShift(0);
            }

            // 保存X缩放
            String xScaleText = this.xScaleField.getValue();
            if (!xScaleText.isEmpty()) {
                try {
                    double xScale = Double.parseDouble(xScaleText);
                    mixinDisplay.setCustomBossXScale(xScale > 0 ? xScale : 0.5);
                } catch (NumberFormatException e) {
                    mixinDisplay.setCustomBossXScale(0.5);
                }
            } else {
                mixinDisplay.setCustomBossXScale(0.5);
            }

            // 保存Y缩放
            String yScaleText = this.yScaleField.getValue();
            if (!yScaleText.isEmpty()) {
                try {
                    double yScale = Double.parseDouble(yScaleText);
                    mixinDisplay.setCustomBossYScale(yScale > 0 ? yScale : 0.5);
                } catch (NumberFormatException e) {
                    mixinDisplay.setCustomBossYScale(0.5);
                }
            } else {
                mixinDisplay.setCustomBossYScale(0.5);
            }

            // 保存宽度
            String widthText = this.widthField.getValue();
            if (!widthText.isEmpty()) {
                try {
                    int width = Integer.parseInt(widthText);
                    mixinDisplay.setCustomBossWidth(width > 0 ? width : 516);
                } catch (NumberFormatException e) {
                    mixinDisplay.setCustomBossWidth(516);
                }
            } else {
                mixinDisplay.setCustomBossWidth(516);
            }

            // 保存高度
            String heightText = this.heightField.getValue();
            if (!heightText.isEmpty()) {
                try {
                    int height = Integer.parseInt(heightText);
                    mixinDisplay.setCustomBossHeight(height > 0 ? height : 95);
                } catch (NumberFormatException e) {
                    mixinDisplay.setCustomBossHeight(95);
                }
            } else {
                mixinDisplay.setCustomBossHeight(95);
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int leftX = centerX - 150;
        int rightX = centerX + 100;

        // 渲染标题
        guiGraphics.drawCenteredString(this.font, Component.translatable("cnpc_immersiveboss.gui.title").getString(), centerX, 15, 0xFFFFFF);

        // 渲染左侧标签
        guiGraphics.drawString(this.font, Component.translatable("cnpc_immersiveboss.gui.texture_path").getString() + ":", leftX, 37, 0xFFFFFF);
        guiGraphics.drawString(this.font, Component.translatable("cnpc_immersiveboss.gui.color").getString() + ":", leftX, 82, 0xFFFFFF);
        guiGraphics.drawString(this.font, Component.translatable("cnpc_immersiveboss.gui.x_shift").getString() + ":", leftX, 127, 0xFFFFFF);

        // 渲染右侧标签
        guiGraphics.drawString(this.font, Component.translatable("cnpc_immersiveboss.gui.scale").getString() + ":", rightX, 37, 0xFFFFFF);
        guiGraphics.drawString(this.font, Component.translatable("cnpc_immersiveboss.gui.size").getString() + ":", rightX, 82, 0xFFFFFF);

        // 渲染帮助文本
        guiGraphics.drawString(this.font, Component.translatable("cnpc_immersiveboss.gui.help_texture").getString(), leftX, 190, 0xAAAAAA);
        guiGraphics.drawString(this.font, Component.translatable("cnpc_immersiveboss.gui.help_ranges").getString(), leftX, 205, 0xAAAAAA);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
