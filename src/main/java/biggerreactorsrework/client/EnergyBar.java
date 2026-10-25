package biggerreactorsrework.client;

import biggerreactorsrework.BiggerReactors;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Vertical energy bar shared by every screen: a framed bar with an animated blue fill ({@code #36CFEE})
 * that grows from the bottom.
 * <p>
 * The source art is 14 px wide (3 px borders around an 8 px fill). Wider bars keep the borders at their pixel size
 * and stretch only the middle and the fill, so the bar can fill the 18 px gauge slots of the screens.
 * The frame is {@value #FRAME_HEIGHT} px tall at most; shorter bars keep the frame's top border and its
 * bottom-aligned part so the marks and the fill stay aligned.
 */
public final class EnergyBar {
    private EnergyBar() {
    }

    static final int FRAME_HEIGHT = 72;
    private static final int TEXTURE_WIDTH = 14;
    private static final int BORDER = 3;
    private static final int FILL_WIDTH = 8;
    private static final int FILL_HEIGHT = 66;
    private static final int FRAMES = 42;
    private static final long FRAME_MS = 100;

    private static final ResourceLocation EMPTY = ResourceLocation.fromNamespaceAndPath(BiggerReactors.modid, "textures/gui/energy/energy_empty.png");
    private static final ResourceLocation SHIMMER = ResourceLocation.fromNamespaceAndPath(BiggerReactors.modid, "textures/gui/energy/energy_shimmer_sheet.png");

    /**
     * Fraction of the bar to fill, safe for any stored/capacity pair (negative, zero or larger than an int).
     */
    public static double fraction(long stored, long capacity) {
        if (capacity <= 0 || stored <= 0) {
            return 0;
        }
        if (stored >= capacity) {
            return 1;
        }
        return (double) stored / (double) capacity;
    }

    /**
     * Draws a frame row band: left border, stretched middle, right border, all from texture rows {@code v..v+h}.
     */
    private static void frameBand(GuiGraphics g, int x, int y, int width, int v, int h) {
        final int middle = width - 2 * BORDER;
        GuiTheme.blit(g, EMPTY, x, y, BORDER, h, 0, v, BORDER, h, TEXTURE_WIDTH, FRAME_HEIGHT);
        GuiTheme.blit(g, EMPTY, x + BORDER, y, middle, h, BORDER, v, FILL_WIDTH, h, TEXTURE_WIDTH, FRAME_HEIGHT);
        GuiTheme.blit(g, EMPTY, x + width - BORDER, y, BORDER, h, TEXTURE_WIDTH - BORDER, v, BORDER, h, TEXTURE_WIDTH, FRAME_HEIGHT);
    }

    public static void draw(GuiGraphics g, int x, int y, int width, int height, double fraction) {
        final int w = Math.max(TEXTURE_WIDTH, width);
        final int h = Mth.clamp(height, 2 * BORDER, FRAME_HEIGHT);
        frameBand(g, x, y, w, 0, BORDER);
        frameBand(g, x, y + BORDER, w, FRAME_HEIGHT - (h - BORDER), h - BORDER);
        final int area = h - 2 * BORDER;
        final double safeFraction = Double.isNaN(fraction) ? 0 : Mth.clamp(fraction, 0, 1);
        final int filled = (int) Math.round(area * safeFraction);
        if (filled > 0) {
            final int frame = (int) ((Util.getMillis() / FRAME_MS) % FRAMES);
            g.blit(SHIMMER, x + BORDER, y + BORDER + area - filled, w - 2 * BORDER, filled,
                    0, frame * FILL_HEIGHT + FILL_HEIGHT - filled, FILL_WIDTH, filled, FILL_WIDTH, FILL_HEIGHT * FRAMES);
        }
    }
}
