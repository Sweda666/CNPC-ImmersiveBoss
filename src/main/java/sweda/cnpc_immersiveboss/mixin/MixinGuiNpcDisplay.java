package sweda.cnpc_immersiveboss.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import noppes.npcs.client.gui.mainmenu.GuiNpcDisplay;
import noppes.npcs.entity.data.DataDisplay;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sweda.cnpc_immersiveboss.api.IMixinGuiNpcDisplay;
import sweda.cnpc_immersiveboss.client.gui.SubGuiCustomBossBar;

@Mixin(GuiNpcDisplay.class)
public class MixinGuiNpcDisplay implements IMixinGuiNpcDisplay {

    @Shadow
    private DataDisplay display;

    @Override
    public DataDisplay getDisplay() {
        return this.display;
    }

    // 1. 保留你原来的注入点，用于添加新按钮
    // 注意：原代码中 y = guiTop + 4 + 23 * 8 是计算出来的，为了精确对齐，建议也用变量计算
    @Inject(method = "init", at = @At("TAIL"), remap = false)
    private void onGuiNpcDisplayInt(CallbackInfo ci) {
        GuiNpcDisplay gui = (GuiNpcDisplay) (Object) this;
        // 重新计算 y 坐标：起始 guiTop+4，然后 +23 像素 8 行
        int y = gui.guiTop + 4 + (23 * 8);
        gui.addButton(new GuiButtonNop(gui, 20, gui.guiLeft + 365, y, 40, 20, Component.translatable("cnpc_immersiveboss.gui.edit").getString()));
    }

    // 2. 新增 Redirect，用于修改 ID=10 的 Bossbar 按钮
    @Redirect(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnoppes/npcs/client/gui/mainmenu/GuiNpcDisplay;addButton(Lnoppes/npcs/shared/client/gui/components/GuiButtonNop;)V"
            )
    )
    private void modifyBossbarOptions(GuiNpcDisplay gui, GuiButtonNop button) {
        // 检查是否是 ID 为 10 的 Bossbar 按钮
        if (button.id == 10) {
            // 保留原版的 x, y, width, height 等位置信息
            int x = button.getX();
            int y = button.getY();
            int width = button.getWidth();
            int height = button.getHeight();

            // 原生选项使用 CustomNPCs 的翻译键，自定义选项使用本 mod 的本地化
            String[] newValues = new String[]{
                    "display.hide",
                    "display.show",
                    "display.showAttacking",
                    Component.translatable("cnpc_immersiveboss.bossbar.mode_customize").getString(),
                    Component.translatable("cnpc_immersiveboss.bossbar.mode_customize_attacking").getString()
            };

            // 获取当前选中的值，防止越界（如果旧存档数据是 3 或 4，这里可能会出错，简单处理为取模或限制范围）
            // 假设你的新逻辑能处理 0-4 的值
            int currentValue = this.display.getBossbar();

            // 创建新按钮
            GuiButtonNop newButton = new GuiButtonNop(
                    gui,
                    10, // ID 必须保持为 10
                    x,
                    y,
                    width,
                    height,
                    newValues,
                    currentValue
            );

            // 添加新按钮（代替原版按钮）
            gui.addButton(newButton);
        } else {
            // 如果不是目标按钮，执行原版的 addButton 逻辑
            ((GuiNpcDisplay) (Object) this).addButton(button);
        }
    }

    // 3. 新增按钮事件处理
    @Inject(method = "buttonEvent", at = @At("HEAD"), cancellable = true, remap = false)
    private void onButtonEvent(GuiButtonNop button, CallbackInfo ci) {
        if (button.id == 20) {
            // 打开自定义 BossBar 设置 GUI
            Minecraft.getInstance().setScreen(new SubGuiCustomBossBar((GuiNpcDisplay) (Object) this));
            ci.cancel();
        }
    }
}