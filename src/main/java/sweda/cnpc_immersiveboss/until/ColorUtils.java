package sweda.cnpc_immersiveboss.until;

public class ColorUtils {

    public static float[] intToRgbFloat(int color) {
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        return new float[]{r, g, b};
    }

}