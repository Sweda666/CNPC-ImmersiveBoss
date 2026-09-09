package sweda.cnpc_immersiveboss.throwing;

import java.util.Locale;

public enum StruggleMode {
    NONE, AD, SPACE, SHIFT;

    public static StruggleMode parse(String value) {
        if (value == null || value.isBlank()) return NONE;
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "none" -> NONE;
            case "ad" -> AD;
            case "space" -> SPACE;
            case "shift" -> SHIFT;
            default -> throw new IllegalArgumentException("Unknown struggle mode: " + value);
        };
    }

    public static StruggleMode parse(int value) {
        return switch (value) {
            case 0 -> NONE;
            case 1 -> AD;
            case 2 -> SPACE;
            case 3 -> SHIFT;
            default -> throw new IllegalArgumentException("Unknown struggle mode id: " + value);
        };
    }
}
