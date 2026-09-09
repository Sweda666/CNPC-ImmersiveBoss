package sweda.cnpc_immersiveboss.mixin;

import noppes.npcs.Resistances;
import noppes.npcs.client.gui.SubGuiNpcResistanceProperties;
import noppes.npcs.shared.client.gui.components.GuiLabel;
import noppes.npcs.shared.client.gui.components.GuiSliderNop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sweda.cnpc_immersiveboss.api.IPushResistance;

@Mixin(value = SubGuiNpcResistanceProperties.class, remap = false)
public abstract class MixinSubGuiNpcResistanceProperties {
    @Unique
    private static final int cnpc_immersiveboss$PUSH_RESISTANCE_SLIDER = 4;

    @Shadow
    private Resistances resistances;

    @Inject(method = {"init()V", "m_7856_()V"}, at = @At("TAIL"),
        remap = false, require = 0)
    private void cnpc_immersiveboss$addPushResistance(CallbackInfo ci) {
        if (!(resistances instanceof IPushResistance pushResistance)) return;

        SubGuiNpcResistanceProperties gui = cnpc_immersiveboss$self();
        if (gui.getSlider(cnpc_immersiveboss$PUSH_RESISTANCE_SLIDER) != null) return;
        float value = pushResistance.cnpc_immersiveboss$getPushResistance();
        gui.addLabel(new GuiLabel(cnpc_immersiveboss$PUSH_RESISTANCE_SLIDER,
            "cnpc_immersiveboss.push_resistance", gui.guiLeft + 4, gui.guiTop + 103));
        gui.addSlider(new GuiSliderNop(gui, cnpc_immersiveboss$PUSH_RESISTANCE_SLIDER,
            gui.guiLeft + 94, gui.guiTop + 98,
            cnpc_immersiveboss$formatPushResistance(value), value));
    }

    @Inject(method = "mouseDragged", at = @At("TAIL"), remap = false, require = 0)
    private void cnpc_immersiveboss$updatePushResistanceLabel(GuiSliderNop slider,
                                                                CallbackInfo ci) {
        if (slider.id == cnpc_immersiveboss$PUSH_RESISTANCE_SLIDER) {
            slider.setString(cnpc_immersiveboss$formatPushResistance(slider.sliderValue));
        }
    }

    @Inject(method = "mouseReleased", at = @At("TAIL"), remap = false, require = 0)
    private void cnpc_immersiveboss$savePushResistance(GuiSliderNop slider,
                                                         CallbackInfo ci) {
        if (slider.id == cnpc_immersiveboss$PUSH_RESISTANCE_SLIDER
            && resistances instanceof IPushResistance pushResistance) {
            pushResistance.cnpc_immersiveboss$setPushResistance(slider.sliderValue);
        }
    }

    @Unique
    private static String cnpc_immersiveboss$formatPushResistance(float value) {
        return Math.round(value * 100.0F) + "%";
    }

    @Unique
    private SubGuiNpcResistanceProperties cnpc_immersiveboss$self() {
        return (SubGuiNpcResistanceProperties) (Object) this;
    }
}
