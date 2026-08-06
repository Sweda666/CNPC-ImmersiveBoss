package sweda.cnpc_immersiveboss.mixin;

import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataDisplay;
import noppes.npcs.util.ValueUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sweda.cnpc_immersiveboss.api.IMixinDataDisplay;

@Mixin(DataDisplay.class)
public class MixinDataDisplay implements IMixinDataDisplay {
    @Shadow
    EntityNPCInterface npc;
    @Shadow
    private byte showBossBar;
    // 1. 只保留这两个字段
    @Unique
    private String customBossBar = "";
    @Unique
    private int customBossColor = -1;
    @Unique
    private int customBossXShift = 0;
    @Unique
    private double customBossXScale = 0.5;
    @Unique
    private double customBossYScale = 0.5;
    @Unique
    private int customBossWidth = 516;
    @Unique
    private int customBossHeight = 95;

    // --- 字段 Getter 和 Setter ---
    public String getCustomBossBar() {
        return this.customBossBar;
    }

    public void setCustomBossBar(String url) {
        this.customBossBar = url;
    }

    public int getCustomBossColor() {
        return this.customBossColor;
    }

    public void setCustomBossColor(int color) {
        this.customBossColor = color;
    }

    public int getCustomBossXShift() {
        return this.customBossXShift;
    }

    public void setCustomBossXShift(int xShift) {
        this.customBossXShift = xShift;
    }

    public double getCustomBossXScale() {
        return this.customBossXScale;
    }

    public double getCustomBossYScale() {
        return this.customBossYScale;
    }

    public void setCustomBossXScale(double xScale) {
        this.customBossXScale = xScale;
    }

    public void setCustomBossYScale(double yScale) {
        this.customBossYScale = yScale;
    }

    public int getCustomBossWidth() {
        return this.customBossWidth;
    }

    public void setCustomBossWidth(int width) {
        this.customBossWidth = width;
    }

    public int getCustomBossHeight() {
        return this.customBossHeight;
    }

    public void setCustomBossHeight(int height) {
        this.customBossHeight = height;
    }

    /**
     * Overwrites the original setBossbar method to expand boss bar options from 0-2 to 0-4.
     * Custom modes 3 and 4 disable vanilla bossInfo rendering so RenderHandler takes over.
     * @param type the boss bar mode (0=hide, 1=show, 2=show when attacking, 3=custom always, 4=custom in combat)
     */
    @Overwrite(remap = false)
    public void setBossbar(int type) {
        // 原版逻辑是 CorrectInt(type, 0, 2)，这里改为 3
        byte newValue = (byte) ValueUtil.CorrectInt(type, 0, 4);

        if (newValue != this.showBossBar) {
            this.showBossBar = newValue;
            // 注意：原版逻辑中，只有 showBossBar == 1 时才显示 BossBar
            // 当值为 3 时，这里设为 false 是正确的，因为我们打算用自定义渲染覆盖它
            // 如果你的自定义渲染逻辑依赖 npc.bossInfo.setVisible(true)，请在这里手动设置
            this.npc.bossInfo.setVisible(newValue == 1);
            this.npc.updateClient = true;
        }
    }

    // --- NBT 读取修复：readToNBT 完成后，若原始 NBT 的 Bossbar 值为 3 或 4 ---
    //    则直接写入 showBossBar 字段，绕过原版 CorrectInt 的 0-2 限制
    @Inject(method = "readToNBT", at = @At("RETURN"), remap = false)
    private void fixBossbarRangeAfterRead(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("Bossbar")) {
            int rawValue = tag.getInt("Bossbar");
            if (rawValue >= 3 && rawValue <= 4) {
                this.showBossBar = (byte) rawValue;
            }
        }
    }

    // --- NBT 保存：追加自定义字段 ---
    @Inject(method = "save", at = @At("RETURN"), remap = false)
    private void onSave(CompoundTag nbttagcompound, CallbackInfoReturnable<CompoundTag> cir) {
        nbttagcompound.putString("CustomBossBar", this.customBossBar);
        nbttagcompound.putInt("CustomBossColor", this.customBossColor);
        nbttagcompound.putInt("CustomBossXShift", this.customBossXShift);
        // 保存缩放比例数组
        nbttagcompound.putDouble("CustomBossXScale", this.customBossXScale);
        nbttagcompound.putDouble("CustomBossYScale", this.customBossYScale);

        nbttagcompound.putInt("CustomBossWidth", this.customBossWidth);
        nbttagcompound.putInt("CustomBossHeight", this.customBossHeight);
    }

    // --- NBT 读取：恢复自定义字段 ---
    @Inject(method = "readToNBT", at = @At("RETURN"), remap = false)
    private void onRead(CompoundTag nbttagcompound, CallbackInfo ci) {
        this.customBossBar = nbttagcompound.getString("CustomBossBar");
        this.customBossColor = nbttagcompound.getInt("CustomBossColor");
        this.customBossXShift = nbttagcompound.getInt("CustomBossXShift");
        // 读取缩放比例数组
        double xScale = nbttagcompound.contains("CustomBossXScale") ? nbttagcompound.getDouble("CustomBossXScale") : 0.5;
        double yScale = nbttagcompound.contains("CustomBossYScale") ? nbttagcompound.getDouble("CustomBossYScale") : 0.5;
        this.customBossXScale = xScale;
        this.customBossYScale = yScale;

        this.customBossWidth = nbttagcompound.contains("CustomBossWidth") ? nbttagcompound.getInt("CustomBossWidth") : 516;
        this.customBossHeight = nbttagcompound.contains("CustomBossHeight") ? nbttagcompound.getInt("CustomBossHeight") : 95;
        // 注意：showBossBar 已经由上面的 fixBossbarRangeAfterRead 注入正确恢复了
    }
}