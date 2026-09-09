package sweda.cnpc_immersiveboss.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import sweda.cnpc_immersiveboss.network.NetworkHandler;
import sweda.cnpc_immersiveboss.network.packet.StruggleInputPacket;
import sweda.cnpc_immersiveboss.throwing.StruggleMode;
import sweda.cnpc_immersiveboss.throwing.StruggleProgress;

import java.util.Arrays;

/** Local key transitions and server-confirmed progress for the captured player. */
public final class StruggleClientState {
    private static int sequence;
    private static StruggleMode mode = StruggleMode.NONE;
    private static int difficulty;
    private static int cycles;
    private static int nextAdKey = StruggleProgress.LEFT;
    private static final boolean[] HELD = new boolean[4];

    private StruggleClientState() {}

    public static void start(int newSequence, StruggleMode newMode, int newDifficulty) {
        clear();
        sequence = newSequence;
        mode = newMode;
        difficulty = Math.max(1, newDifficulty);
    }

    public static void stop(int stoppedSequence) {
        if (sequence == stoppedSequence) clear();
    }

    public static void clear() {
        mode = StruggleMode.NONE;
        cycles = 0;
        Arrays.fill(HELD, false);
        nextAdKey = StruggleProgress.LEFT;
    }

    public static void progress(int updatedSequence, int completedCycles) {
        if (sequence == updatedSequence) cycles = Math.max(cycles, Math.min(difficulty, completedCycles));
    }

    private static KeyMapping mapping(int key) {
        var options = Minecraft.getInstance().options;
        return switch (key) {
            case StruggleProgress.LEFT -> options.keyLeft;
            case StruggleProgress.RIGHT -> options.keyRight;
            case StruggleProgress.JUMP -> options.keyJump;
            default -> options.keyShift;
        };
    }

    private static boolean relevant(int key) {
        return switch (mode) {
            case AD -> key == StruggleProgress.LEFT || key == StruggleProgress.RIGHT;
            case SPACE -> key == StruggleProgress.JUMP;
            case SHIFT -> key == StruggleProgress.SNEAK;
            default -> false;
        };
    }

    public static void key(int keyCode, int scanCode, int action) {
        if (action == GLFW.GLFW_REPEAT) return;
        for (int key = 0; key < HELD.length; key++) {
            if (relevant(key) && mapping(key).matches(keyCode, scanCode)) transition(key, action == GLFW.GLFW_PRESS);
        }
    }

    public static void mouse(int button, int action) {
        for (int key = 0; key < HELD.length; key++) {
            if (relevant(key) && mapping(key).matchesMouse(button)) transition(key, action == GLFW.GLFW_PRESS);
        }
    }

    private static void transition(int key, boolean pressed) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) return;
        if (pressed && (minecraft.screen != null || !minecraft.isWindowActive() || minecraft.isPaused())) return;
        if (HELD[key] == pressed) return;
        HELD[key] = pressed;
        if (pressed && mode == StruggleMode.AD) nextAdKey = key == StruggleProgress.LEFT
            ? StruggleProgress.RIGHT : StruggleProgress.LEFT;
        NetworkHandler.INSTANCE.sendToServer(new StruggleInputPacket(sequence, key, pressed));
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || !minecraft.isWindowActive()) {
            for (int key = 0; key < HELD.length; key++) {
                if (HELD[key]) transition(key, false);
            }
        }
    }

    public static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (mode == StruggleMode.NONE || minecraft.player == null || minecraft.options.hideGui
            || minecraft.screen != null) return;
        int nextKey = mode == StruggleMode.AD ? nextAdKey
            : mode == StruggleMode.SPACE ? StruggleProgress.JUMP : StruggleProgress.SNEAK;
        String labelText = (difficulty <= 0 ? 0 : (cycles * 100L / difficulty)) + "%";
        Component label = Component.literal(labelText);
        int center = graphics.guiWidth() / 2;
        int y = graphics.guiHeight() - 76;
        int iconWidth = nextKey == StruggleProgress.JUMP ? 34 : nextKey == StruggleProgress.SNEAK ? 38 : 22;
        int groupWidth = iconWidth + 8 + minecraft.font.width(label);
        int left = center - groupWidth / 2;
        drawKeyIcon(graphics, minecraft, nextKey, left + iconWidth / 2, y - 20, HELD[nextKey]);
        graphics.drawString(minecraft.font, label, left + iconWidth + 8, y - 16, 0xF2F2F2, true);
        int width = Math.min(160, graphics.guiWidth() - 24);
        graphics.fill(center - width / 2, y, center + width / 2, y + 6, 0xAA303030);
        int filled = (int) (width * (double) cycles / difficulty);
        graphics.fill(center - width / 2, y, center - width / 2 + filled, y + 6, 0xFFE8E8E8);
    }

    private static void drawKeyIcon(GuiGraphics graphics, Minecraft minecraft, int key,
                                    int x, int y, boolean down) {
        int bg = down ? 0xFFF8F8F8 : 0xFFD0D0D0;
        int fg = down ? 0xFF555555 : 0xFF303030;
        int w = key == StruggleProgress.JUMP ? 34 : key == StruggleProgress.SNEAK ? 38 : 22;
        // Slight perspective: a dark lower edge and offset top highlight give the key a raised profile.
        graphics.fill(x - w / 2, y + 2, x + w / 2 + 2, y + 18, 0xFF777777);
        graphics.fill(x - w / 2, y, x + w / 2, y + 15, bg);
        graphics.fill(x - w / 2 + 2, y + 1, x + w / 2 - 2, y + 3, 0xFFFFFFFF);
        String symbol = key == StruggleProgress.LEFT ? "A" : key == StruggleProgress.RIGHT ? "D"
            : key == StruggleProgress.JUMP ? "____" : "SHIFT";
        graphics.drawCenteredString(minecraft.font, Component.literal(symbol), x, y + 4, fg);
    }
}
