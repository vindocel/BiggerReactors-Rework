package biggerreactorsrework.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import javax.annotation.Nonnull;

/**
 * Metal frame drawn in code around the screens and their windows: a 4 px band outside the panel (dark outline with
 * chamfered corners, light bevel on the top and left, shade on the bottom and right, a rivet in each corner) and a soft
 * shadow under it. Side tabs are ears of the same metal growing out of the band. Each machine has its own metal, taken
 * from the tones of its casing texture, so the screen matches the block it belongs to.
 */
public enum GuiFrame {
    /**
     * Dark grey of the reactor casing.
     */
    REACTOR(0xFF0D0D0D, 0xFF8A8A8A, 0xFF5E5E5E, 0xFF2A2A2A, 0xFF444444, 0xFF222222, 0xFF585858,
            0xFF979797, 0xFF6D6D6D, 0xFF1A1A1A),
    /**
     * Light blue-grey of the turbine casing.
     */
    TURBINE(0xFF1D2529, 0xFFE2E6E8, 0xFFC3C9CC, 0xFF5F6F77, 0xFF9BA5AA, 0xFF4A575E, 0xFF7D8A90,
            0xFFF2F4F5, 0xFF7D8A90, 0xFF3C474D),
    /**
     * Reddish brown of the heat exchanger casing.
     */
    HEAT_EXCHANGER(0xFF170F0B, 0xFFA57B64, 0xFF7A5240, 0xFF3A271E, 0xFF5E4032, 0xFF2E1F17, 0xFF684637,
            0xFFB0A39C, 0xFF6D6D6D, 0xFF241812),
    /**
     * Warm off-white of the battery casing.
     */
    BATTERY(0xFF25241F, 0xFFF0EEE6, 0xFFE1DED2, 0xFF7F7D74, 0xFFC9C6BA, 0xFF6E6C64, 0xFF9B998F,
            0xFFFAF9F4, 0xFF9B998F, 0xFF55534C),
    /**
     * Medium grey of the cyanite reprocessor.
     */
    CYANITE_REPROCESSOR(0xFF111111, 0xFFA6A6A6, 0xFF7C7C7C, 0xFF393939, 0xFF5A5A5A, 0xFF2D2D2D, 0xFF6D6D6D,
            0xFFB4B4B4, 0xFF7C7C7C, 0xFF262626);

    /**
     * Width of the band outside the panel.
     */
    public static final int BAND = 4;
    /**
     * Width of a side tab, band included, and the button it holds.
     */
    public static final int TAB_SIZE = 28;
    public static final int TAB_BUTTON = 20;
    /**
     * Button of a left tab inside its ear (a right tab mirrors it).
     */
    public static final int TAB_BUTTON_LEFT = 5;
    public static final int TAB_BUTTON_TOP = 4;
    /**
     * Icons on tab buttons are drawn smaller than their real size (a 16 px icon takes 12 px), leaving room around them.
     */
    public static final float TAB_ICON_SCALE = 0.75F;

    private static final int SHADOW = 0x66000000;

    private final int outline;
    private final int lightTop;
    private final int lightSide;
    private final int shade;
    private final int body;
    /**
     * Line between the band and the panel: shade on the top and left, light on the bottom and right.
     */
    private final int inner;
    private final int innerLight;
    private final int rivetLight;
    private final int rivetMid;
    private final int rivetDark;
    /**
     * Hue and saturation of the metal, given to the controls of the screen so they match the frame.
     */
    private final float hue;
    private final float saturation;

    GuiFrame(int outline, int lightTop, int lightSide, int shade, int body, int inner, int innerLight,
             int rivetLight, int rivetMid, int rivetDark) {
        this.outline = outline;
        this.lightTop = lightTop;
        this.lightSide = lightSide;
        this.shade = shade;
        this.body = body;
        this.inner = inner;
        this.innerLight = innerLight;
        this.rivetLight = rivetLight;
        this.rivetMid = rivetMid;
        this.rivetDark = rivetDark;
        float[] hsl = hsl(body);
        this.hue = hsl[0];
        // A softer tone than the metal itself, so the controls lean towards the frame without matching it.
        this.saturation = hsl[1] * 0.6F;
    }

    /**
     * {@code color} with the hue and saturation of this metal and its own lightness: the dark parts of the buttons and
     * selectors take the tone of the frame around them.
     */
    public int tint(int color) {
        float lightness = hsl(color)[2];
        float chroma = (1 - Math.abs(2 * lightness - 1)) * this.saturation;
        float h = this.hue * 6;
        float second = chroma * (1 - Math.abs(h % 2 - 1));
        float r;
        float g;
        float b;
        if (h < 1) {
            r = chroma; g = second; b = 0;
        } else if (h < 2) {
            r = second; g = chroma; b = 0;
        } else if (h < 3) {
            r = 0; g = chroma; b = second;
        } else if (h < 4) {
            r = 0; g = second; b = chroma;
        } else if (h < 5) {
            r = second; g = 0; b = chroma;
        } else {
            r = chroma; g = 0; b = second;
        }
        float m = lightness - chroma / 2;
        return (color & 0xFF000000) | channel(r + m) << 16 | channel(g + m) << 8 | channel(b + m);
    }

    private static int channel(float value) {
        return Math.round(Math.max(0, Math.min(1, value)) * 255);
    }

    /**
     * Hue (0 to 1), saturation and lightness of an ARGB color.
     */
    private static float[] hsl(int color) {
        float r = (color >> 16 & 0xFF) / 255F;
        float g = (color >> 8 & 0xFF) / 255F;
        float b = (color & 0xFF) / 255F;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float lightness = (max + min) / 2;
        float delta = max - min;
        if (delta == 0) {
            return new float[]{0, 0, lightness};
        }
        float saturation = delta / (1 - Math.abs(2 * lightness - 1));
        float hue;
        if (max == r) {
            hue = ((g - b) / delta) % 6;
        } else if (max == g) {
            hue = (b - r) / delta + 2;
        } else {
            hue = (r - g) / delta + 4;
        }
        hue /= 6;
        if (hue < 0) {
            hue += 1;
        }
        return new float[]{hue, saturation, lightness};
    }

    /**
     * The panel at (x, y, width, height), filled with {@code fill}, with the band around it.
     */
    public void panel(@Nonnull GuiGraphicsExtractor graphics, int x, int y, int width, int height, int fill) {
        int x0 = x - BAND;
        int y0 = y - BAND;
        int x1 = x + width + BAND;
        int y1 = y + height + BAND;
        // Shadow under the bottom edge only, tapered like the chamfered corners so it never shows at the sides.
        graphics.fill(x0 + 2, y1, x1 - 2, y1 + 1, SHADOW);
        graphics.fill(x0 + 3, y1 + 1, x1 - 3, y1 + 2, SHADOW);
        graphics.fill(x0 + 2, y0, x1 - 2, y0 + 1, this.outline);
        graphics.fill(x0 + 2, y1 - 1, x1 - 2, y1, this.outline);
        graphics.fill(x0, y0 + 2, x0 + 1, y1 - 2, this.outline);
        graphics.fill(x1 - 1, y0 + 2, x1, y1 - 2, this.outline);
        pixel(graphics, x0 + 1, y0 + 1, this.outline);
        pixel(graphics, x1 - 2, y0 + 1, this.outline);
        pixel(graphics, x0 + 1, y1 - 2, this.outline);
        pixel(graphics, x1 - 2, y1 - 2, this.outline);
        graphics.fill(x0 + 2, y0 + 1, x1 - 2, y0 + 2, this.lightTop);
        graphics.fill(x0 + 1, y0 + 2, x0 + 2, y1 - 2, this.lightSide);
        graphics.fill(x0 + 2, y1 - 2, x1 - 2, y1 - 1, this.shade);
        graphics.fill(x1 - 2, y0 + 2, x1 - 1, y1 - 2, this.shade);
        graphics.fill(x0 + 2, y0 + 2, x1 - 2, y1 - 2, this.body);
        this.rivet(graphics, x0 + 2, y0 + 2);
        this.rivet(graphics, x1 - 4, y0 + 2);
        this.rivet(graphics, x0 + 2, y1 - 4);
        this.rivet(graphics, x1 - 4, y1 - 4);
        // Flat panel set into the band by one line.
        graphics.fill(x, y, x + width, y + height, fill);
        graphics.fill(x, y, x + width, y + 1, this.inner);
        graphics.fill(x, y, x + 1, y + height, this.inner);
        graphics.fill(x, y + height - 1, x + width, y + height, this.innerLight);
        graphics.fill(x + width - 1, y + 1, x + width, y + height, this.innerLight);
    }

    /**
     * Tab ear {@link #TAB_SIZE} wide and {@code height} tall at (x, y): on the left side it ends on the panel's left
     * edge, on the right side it starts on the panel's right edge; the side next to the panel opens into the band. Tabs
     * stacked next to each other share one tall ear. Drawn after the panel, so the ear covers the band's outline and the
     * two read as one piece.
     */
    public void ear(@Nonnull GuiGraphicsExtractor graphics, int x, int y, int height, boolean leftSide) {
        int last = height - 1;
        // Columns counted from the outer side; the band itself takes the last BAND columns.
        int open = TAB_SIZE - BAND;
        this.column(graphics, x, leftSide, 0, y + 2, y + last - 1, this.outline);
        this.row(graphics, x, leftSide, 2, open, y, this.outline);
        this.row(graphics, x, leftSide, 2, open, y + last, this.outline);
        this.column(graphics, x, leftSide, 1, y + 1, y + 2, this.outline);
        this.column(graphics, x, leftSide, 1, y + last - 1, y + last, this.outline);
        this.column(graphics, x, leftSide, 1, y + 2, y + last - 1, leftSide ? this.lightSide : this.shade);
        this.row(graphics, x, leftSide, 2, open + 1, y + 1, this.lightTop);
        this.row(graphics, x, leftSide, 2, open + 1, y + last - 1, this.shade);
        for (int r = y + 2; r < y + last - 1; r++) {
            this.row(graphics, x, leftSide, 2, open + 1, r, this.body);
        }
    }

    /**
     * Left edge of the button of a tab whose ear starts at {@code earX}.
     */
    public static int tabButtonX(int earX, boolean leftSide) {
        return earX + (leftSide ? TAB_BUTTON_LEFT : TAB_SIZE - TAB_BUTTON_LEFT - TAB_BUTTON);
    }

    private void row(GuiGraphicsExtractor graphics, int x, boolean leftSide, int from, int to, int y, int color) {
        int a = leftSide ? x + from : x + TAB_SIZE - 1 - to;
        int b = leftSide ? x + to : x + TAB_SIZE - 1 - from;
        graphics.fill(a, y, b + 1, y + 1, color);
    }

    private void column(GuiGraphicsExtractor graphics, int x, boolean leftSide, int column, int fromY, int toY, int color) {
        int c = leftSide ? x + column : x + TAB_SIZE - 1 - column;
        graphics.fill(c, fromY, c + 1, toY, color);
    }

    private void rivet(GuiGraphicsExtractor graphics, int x, int y) {
        pixel(graphics, x, y, this.rivetLight);
        pixel(graphics, x + 1, y, this.rivetMid);
        pixel(graphics, x, y + 1, this.rivetMid);
        pixel(graphics, x + 1, y + 1, this.rivetDark);
    }

    private static void pixel(GuiGraphicsExtractor graphics, int x, int y, int color) {
        graphics.fill(x, y, x + 1, y + 1, color);
    }
}
