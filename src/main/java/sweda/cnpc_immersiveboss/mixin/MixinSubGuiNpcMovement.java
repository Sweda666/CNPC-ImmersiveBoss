package sweda.cnpc_immersiveboss.mixin;

import noppes.npcs.client.gui.SubGuiNpcMovement;
import noppes.npcs.entity.data.DataAI;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiLabel;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sweda.cnpc_immersiveboss.api.IMixinDataAI;
import sweda.cnpc_immersiveboss.entity.NpcTurnSpeedManager;

@Mixin(value = SubGuiNpcMovement.class, remap = false)
public abstract class MixinSubGuiNpcMovement {
    @Unique
    private static final int cnpc_immersiveboss$BASE_WIDTH = 256;
    @Unique
    private static final int cnpc_immersiveboss$RIGHT_COLUMN_WIDTH = 360;
    @Unique
    private static final int cnpc_immersiveboss$CONTROL_WIDTH = 60;
    @Unique
    private static final int cnpc_immersiveboss$ROTATION_ENABLED_BUTTON = 1000;
    @Unique
    private static final int cnpc_immersiveboss$ROTATION_SPEED_FIELD = 1001;
    @Unique
    private static final int cnpc_immersiveboss$MINIMUM_SPEED_FIELD = 1002;
    @Unique
    private static final int cnpc_immersiveboss$ROTATION_ENABLED_LABEL = 1001;
    @Unique
    private static final int cnpc_immersiveboss$ROTATION_SPEED_LABEL = 1002;
    @Unique
    private static final int cnpc_immersiveboss$MINIMUM_SPEED_LABEL = 1003;

    @Shadow
    private DataAI ai;

    @Unique
    private boolean cnpc_immersiveboss$useRightColumn;

    @Inject(method = "init", at = @At("HEAD"), remap = false, require = 0)
    private void cnpc_immersiveboss$resizeMovementSettings(CallbackInfo ci) {
        SubGuiNpcMovement gui = cnpc_immersiveboss$self();
        // Wandering and path movement fill the original column down to the Done button.
        cnpc_immersiveboss$useRightColumn = ai != null && ai.getMovingType() != 0;
        gui.imageWidth = cnpc_immersiveboss$useRightColumn
            ? cnpc_immersiveboss$RIGHT_COLUMN_WIDTH
            : cnpc_immersiveboss$BASE_WIDTH;
    }

    @Inject(method = "init", at = @At("TAIL"), remap = false, require = 0)
    private void cnpc_immersiveboss$addRotationSettings(CallbackInfo ci) {
        SubGuiNpcMovement gui = cnpc_immersiveboss$self();
        GuiTextFieldNop movementSpeed = gui.getTextField(14);
        if (movementSpeed == null || !(ai instanceof IMixinDataAI settings)) return;

        int enabledLabelX;
        int speedLabelX;
        int minimumLabelX;
        int enabledControlX;
        int speedControlX;
        int minimumControlX;
        int enabledLabelY;
        int speedLabelY;
        int minimumLabelY;
        int enabledControlY;
        int speedControlY;
        int minimumControlY;

        if (cnpc_immersiveboss$useRightColumn) {
            int columnX = gui.guiLeft + 260;
            int controlX = gui.guiLeft + 278;
            enabledLabelX = speedLabelX = minimumLabelX = columnX;
            enabledControlX = speedControlX = minimumControlX = controlX;
            enabledLabelY = gui.guiTop + 8;
            enabledControlY = gui.guiTop + 18;
            speedLabelY = gui.guiTop + 47;
            speedControlY = gui.guiTop + 57;
            minimumLabelY = gui.guiTop + 86;
            minimumControlY = gui.guiTop + 96;
        } else {
            int labelY = movementSpeed.getY() + 21;
            int controlY = labelY + 10;
            enabledLabelX = gui.guiLeft + 4;
            speedLabelX = gui.guiLeft + 82;
            minimumLabelX = gui.guiLeft + 166;
            enabledControlX = gui.guiLeft + 10;
            speedControlX = gui.guiLeft + 91;
            minimumControlX = gui.guiLeft + 179;
            enabledLabelY = speedLabelY = minimumLabelY = labelY;
            enabledControlY = speedControlY = minimumControlY = controlY;
        }

        gui.addLabel(new GuiLabel(cnpc_immersiveboss$ROTATION_ENABLED_LABEL,
            "cnpc_immersiveboss.rotation.enabled", enabledLabelX, enabledLabelY));
        gui.addButton(new GuiButtonNop(gui,
            cnpc_immersiveboss$ROTATION_ENABLED_BUTTON,
            enabledControlX, enabledControlY, cnpc_immersiveboss$CONTROL_WIDTH, 18,
            new String[]{"gui.no", "gui.yes"},
            settings.cnpc_immersiveboss$isRotationLimitEnabled() ? 1 : 0));

        gui.addLabel(new GuiLabel(cnpc_immersiveboss$ROTATION_SPEED_LABEL,
            "cnpc_immersiveboss.rotation.speed", speedLabelX, speedLabelY));
        GuiTextFieldNop rotationSpeed = new GuiTextFieldNop(
            cnpc_immersiveboss$ROTATION_SPEED_FIELD, gui,
            speedControlX, speedControlY, cnpc_immersiveboss$CONTROL_WIDTH, 18,
            Float.toString(settings.cnpc_immersiveboss$getRotationSpeed()));
        rotationSpeed.setFloatsOnly();
        rotationSpeed.setMinMaxDefault(0.0F, NpcTurnSpeedManager.MAX_TURN_SPEED,
            NpcTurnSpeedManager.DEFAULT_TURN_SPEED);
        gui.addTextField(rotationSpeed);

        gui.addLabel(new GuiLabel(cnpc_immersiveboss$MINIMUM_SPEED_LABEL,
            "cnpc_immersiveboss.rotation.minimum_navigation_speed",
            minimumLabelX, minimumLabelY));
        GuiTextFieldNop minimumSpeed = new GuiTextFieldNop(
            cnpc_immersiveboss$MINIMUM_SPEED_FIELD, gui,
            minimumControlX, minimumControlY, cnpc_immersiveboss$CONTROL_WIDTH, 18,
            Float.toString(settings.cnpc_immersiveboss$getMinimumNavigationSpeedScale()));
        minimumSpeed.setFloatsOnly();
        minimumSpeed.setMinMaxDefault(0.0F, 1.0F,
            NpcTurnSpeedManager.DEFAULT_MINIMUM_NAVIGATION_SPEED_SCALE);
        gui.addTextField(minimumSpeed);

        GuiButtonNop done = gui.getButton(66);
        if (done != null) {
            done.setX(gui.guiLeft + (gui.imageWidth - done.getWidth()) / 2);
        }

        cnpc_immersiveboss$updateFieldState(
            settings.cnpc_immersiveboss$isRotationLimitEnabled());
    }

    @Inject(method = "buttonEvent", at = @At("TAIL"), remap = false, require = 0)
    private void cnpc_immersiveboss$handleRotationButton(GuiButtonNop button, CallbackInfo ci) {
        if (button.id != cnpc_immersiveboss$ROTATION_ENABLED_BUTTON
            || !(ai instanceof IMixinDataAI settings)) {
            return;
        }

        boolean enabled = button.getValue() == 1;
        settings.cnpc_immersiveboss$setRotationLimitEnabled(enabled);
        cnpc_immersiveboss$updateFieldState(enabled);
    }

    @Inject(method = "buttonEvent", at = @At("HEAD"), remap = false, require = 0)
    private void cnpc_immersiveboss$flushRotationFieldsOnClose(GuiButtonNop button,
                                                               CallbackInfo ci) {
        if (button.id == 66) cnpc_immersiveboss$flushRotationFields();
    }

    @Inject(method = "unFocused", at = @At("TAIL"), remap = false, require = 0)
    private void cnpc_immersiveboss$handleRotationTextField(GuiTextFieldNop field,
                                                            CallbackInfo ci) {
        if (!(ai instanceof IMixinDataAI settings)) return;
        if (field.id == cnpc_immersiveboss$ROTATION_SPEED_FIELD && field.isFloat()) {
            settings.cnpc_immersiveboss$setRotationSpeed(field.getFloat());
        } else if (field.id == cnpc_immersiveboss$MINIMUM_SPEED_FIELD && field.isFloat()) {
            settings.cnpc_immersiveboss$setMinimumNavigationSpeedScale(field.getFloat());
        }
    }

    @Unique
    private void cnpc_immersiveboss$flushRotationFields() {
        if (!(ai instanceof IMixinDataAI settings)) return;
        SubGuiNpcMovement gui = cnpc_immersiveboss$self();
        GuiTextFieldNop rotationSpeed = gui.getTextField(
            cnpc_immersiveboss$ROTATION_SPEED_FIELD);
        GuiTextFieldNop minimumSpeed = gui.getTextField(
            cnpc_immersiveboss$MINIMUM_SPEED_FIELD);
        if (rotationSpeed != null && rotationSpeed.isFloat()) {
            settings.cnpc_immersiveboss$setRotationSpeed(rotationSpeed.getFloat());
        }
        if (minimumSpeed != null && minimumSpeed.isFloat()) {
            settings.cnpc_immersiveboss$setMinimumNavigationSpeedScale(
                minimumSpeed.getFloat());
        }
    }

    @Unique
    private void cnpc_immersiveboss$updateFieldState(boolean enabled) {
        SubGuiNpcMovement gui = cnpc_immersiveboss$self();
        cnpc_immersiveboss$setFieldEnabled(gui.getTextField(
            cnpc_immersiveboss$ROTATION_SPEED_FIELD), enabled);
        cnpc_immersiveboss$setFieldEnabled(gui.getTextField(
            cnpc_immersiveboss$MINIMUM_SPEED_FIELD), enabled);
    }

    @Unique
    private static void cnpc_immersiveboss$setFieldEnabled(GuiTextFieldNop field,
                                                            boolean enabled) {
        if (field == null) return;
        field.enabled = enabled;
        field.active = enabled;
    }

    @Unique
    private SubGuiNpcMovement cnpc_immersiveboss$self() {
        return (SubGuiNpcMovement) (Object) this;
    }
}
