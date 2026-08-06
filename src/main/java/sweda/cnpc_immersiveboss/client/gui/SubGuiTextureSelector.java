package sweda.cnpc_immersiveboss.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.components.GuiLabel;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;

import java.util.List;
import java.util.function.Consumer;

public class SubGuiTextureSelector extends GuiNPCInterface implements ICustomScrollListener {

    public GuiCustomScrollNop scroll;
    public Consumer<String> action;
    public Screen parentScreen;
    public String title;
    public List<String> options;

    public SubGuiTextureSelector(Screen parentScreen, String title, List<String> options, Consumer<String> action) {
        drawDefaultBackground = false;
        this.parentScreen = parentScreen;
        this.action = action;
        this.title = title;
        this.options = options;
    }

    @Override
    public void init() {
        super.init();
        addLabel(new GuiLabel(0, title, width / 2 - (this.font.width(title) / 2), 8, 0xffffff));
        options.sort(String.CASE_INSENSITIVE_ORDER);

        int scrollWidth = width - 40;
        int scrollHeight = height - 70;
        scroll = new GuiCustomScrollNop(this, 0);
        scroll.setSize(scrollWidth, scrollHeight);
        scroll.guiLeft = width / 2 - scrollWidth / 2;
        scroll.guiTop = 25;
        scroll.setList(options);
        scroll.listener = this;
        addScroll(scroll);

        this.addButton(new GuiButtonNop(this, 2, width / 2 - 100, height - 32, 98, 20, "gui.back"));
    }

    @Override
    public void render(GuiGraphics matrixStack, int mouseX, int mouseY, float partialTicks) {
        scroll.guiLeft = width / 2 - scroll.getWidth() / 2;
        scroll.guiTop = 25;
        scroll.render(matrixStack, mouseX, mouseY, partialTicks);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public void scrollClicked(double mouseX, double mouseY, int button, GuiCustomScrollNop scroll) {
        // single click handled by GuiCustomScrollNop internally
    }

    @Override
    public void scrollDoubleClicked(String selected, GuiCustomScrollNop scroll) {
        if (selected != null && !selected.isEmpty()) {
            action.accept(selected);
        }
        if (this.minecraft != null) {
            this.minecraft.setScreen(parentScreen);
        }
    }

    @Override
    public void buttonEvent(GuiButtonNop guibutton) {
        if (guibutton.id == 2) {
            if (this.minecraft != null) {
                this.minecraft.setScreen(parentScreen);
            }
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parentScreen);
        }
    }
}
