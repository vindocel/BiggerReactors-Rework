package biggerreactorsrework.client;

/** CPU equivalent of the dark GUI shader, for controls drawn with plain fills. */
final class GuiThemePalette {
    static final float DARKNESS = 0.32F;

    private GuiThemePalette() {}

    static int chrome(int argb, boolean dark) {
        if (!dark) return argb;
        int r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
        float chroma = (Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b))) / 255F;
        float t = Math.max(0, Math.min(1, (chroma - 0.12F) / (0.35F - 0.12F)));
        float factor = DARKNESS + (1 - DARKNESS) * t * t * (3 - 2 * t);
        return argb & 0xFF000000 | Math.round(r * factor) << 16 | Math.round(g * factor) << 8 | Math.round(b * factor);
    }
}
