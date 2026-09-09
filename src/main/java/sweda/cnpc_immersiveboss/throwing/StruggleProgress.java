package sweda.cnpc_immersiveboss.throwing;

/** Counts fresh key presses; AD requires two opposite presses per cycle. */
public final class StruggleProgress {
    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    public static final int JUMP = 2;
    public static final int SNEAK = 3;
    public static final int DEFAULT_DIFFICULTY = 5;

    private final StruggleMode mode;
    private final int difficulty;
    private final boolean[] held = new boolean[4];
    private int lastDirection = -1;
    private int alternatingPresses;
    private int cycles;
    private long inputTick = Long.MIN_VALUE;
    private int pressesThisTick;

    public StruggleProgress(StruggleMode mode, int difficulty) {
        if (difficulty <= 0) throw new IllegalArgumentException("Struggle difficulty must be positive");
        this.mode = mode;
        this.difficulty = difficulty;
    }

    public boolean input(int key, boolean pressed, long tick) {
        if (key < LEFT || key > SNEAK || mode == StruggleMode.NONE || complete()) return false;
        if (!pressed) {
            held[key] = false;
            return false;
        }
        if (held[key]) return false;
        held[key] = true;
        if (inputTick != tick) {
            inputTick = tick;
            pressesThisTick = 0;
        }
        // Bound packet bursts while allowing rapid taps arriving in the same tick.
        if (++pressesThisTick > 4) return false;
        if (mode == StruggleMode.AD && (key == LEFT || key == RIGHT)) {
            if (key == lastDirection) return false;
            lastDirection = key;
            if (++alternatingPresses % 2 != 0) return false;
        } else if (!((mode == StruggleMode.SPACE && key == JUMP)
            || (mode == StruggleMode.SHIFT && key == SNEAK))) {
            return false;
        }
        cycles++;
        return true;
    }

    public int cycles() { return cycles; }
    public int difficulty() { return difficulty; }
    public StruggleMode mode() { return mode; }
    public boolean complete() { return cycles >= difficulty; }
}
