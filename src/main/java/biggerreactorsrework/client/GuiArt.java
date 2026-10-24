package biggerreactorsrework.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.material.EmptyFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import biggerreactorsrework.core.client.gui.RenderHelper;

import javax.annotation.Nonnull;
import java.util.function.IntUnaryOperator;

/**
 * Draws the pieces every Bigger Reactors screen is made of (panel, slots, buttons, tabs, selectors, text boxes) with
 * plain fills, pixel for pixel like the old screen sheets. Icons live in their own files, see {@link GuiIcon}.
 */
@OnlyIn(Dist.CLIENT)
public final class GuiArt {

    public static final int BLACK = 0xFF000000;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int PANEL_FILL = 0xFFC6C6C6;
    public static final int PANEL_SHADOW = 0xFF555555;
    public static final int SLOT_FILL = 0xFF8B8B8B;
    public static final int SLOT_SHADOW = 0xFF373737;
    public static final int TAB_FILL = SLOT_FILL;
    public static final int SEPARATOR = PANEL_SHADOW;

    private static final int SELECTOR_EMPTY = 0xFF565656;
    private static final int SELECTOR_EMPTY_SEPARATOR = 0xFF444444;
    private static final int TEXT_BOX_LIGHT = 0xFF636363;
    private static final int TEXT_BOX_SHADOW = 0xFF252525;
    private static final int TEXT_BOX_CORNER = 0xFF474747;

    /**
     * Light, fill and shadow of a beveled surface.
     */
    public record Palette(int light, int fill, int shadow) {
        public static Palette of(int light, int fill, int shadow) {
            return new Palette(0xFF000000 | light, 0xFF000000 | fill, 0xFF000000 | shadow);
        }
    }

    public static final Palette BUTTON = Palette.of(0xA6A6A6, 0x737373, 0x404040);
    // Hover of the standalone push buttons (eject, arrows, apply/revert).
    public static final Palette BUTTON_HOVER = Palette.of(0xC9C3E5, 0xA598B3, 0x947A99);
    // Hover of the common widget buttons (text box enter).
    public static final Palette COMMON_BUTTON_HOVER = Palette.of(0xC9C3E5, 0xA598B3, 0x7B667F);
    public static final Palette BUTTON_DISABLED = Palette.of(0x2A2A2A, 0x1D1D1D, 0x101010);

    /**
     * Receives the rectangles a shape is made of, so the same shape can be drawn whole or clipped.
     */
    @FunctionalInterface
    private interface Painter {
        void fill(int x0, int y0, int x1, int y1, int color);

        default void pixel(int x, int y, int color) {
            this.fill(x, y, x + 1, y + 1, color);
        }
    }

    private GuiArt() {
    }

    private static Painter painter(GuiGraphics graphics) {
        return graphics::fill;
    }

    /**
     * Lets each row through only between {@code low(row)} and {@code high(row)}, both inclusive.
     */
    private static Painter clipped(GuiGraphics graphics, IntUnaryOperator low, IntUnaryOperator high) {
        return (x0, y0, x1, y1, color) -> {
            for (int row = y0; row < y1; row++) {
                int from = Math.max(x0, low.applyAsInt(row));
                // In long, so an open side (Integer.MAX_VALUE) does not wrap around.
                int to = (int) Math.min(x1, high.applyAsInt(row) + 1L);
                if (to > from) {
                    graphics.fill(from, row, to, row + 1, color);
                }
            }
        };
    }

    public static void fill(@Nonnull GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
        graphics.fill(x0, y0, x1, y1, color);
    }

    /**
     * The light grey window every screen sits on: black outline with cut corners, white bevel on the top and left,
     * dark bevel on the bottom and right.
     */
    public static void panel(@Nonnull GuiGraphics graphics, int x, int y, int width, int height) {
        panel(painter(graphics), x, y, width, height, PANEL_FILL);
    }

    private static void panel(Painter p, int x, int y, int w, int h, int fill) {
        // Body.
        p.fill(x + 1, y + 1, x + w - 2, y + h - 2, fill);
        // Highlight: two rows on top, two columns on the left.
        p.fill(x + 1, y + 1, x + w - 2, y + 3, WHITE);
        p.fill(x + 1, y + 1, x + 3, y + h - 2, WHITE);
        p.pixel(x + 3, y + 3, WHITE);
        // Shadow: two rows at the bottom, two columns on the right.
        p.fill(x + 2, y + h - 3, x + w - 1, y + h - 1, PANEL_SHADOW);
        p.fill(x + w - 3, y + 2, x + w - 1, y + h - 1, PANEL_SHADOW);
        p.pixel(x + w - 4, y + h - 4, PANEL_SHADOW);
        // Where highlight and shadow meet.
        p.pixel(x + w - 3, y + 2, fill);
        p.pixel(x + 2, y + h - 3, fill);
        // Outline with cut corners.
        p.fill(x + 2, y, x + w - 3, y + 1, BLACK);
        p.fill(x + 3, y + h - 1, x + w - 2, y + h, BLACK);
        p.fill(x, y + 2, x + 1, y + h - 3, BLACK);
        p.fill(x + w - 1, y + 3, x + w, y + h - 2, BLACK);
        p.pixel(x + 1, y + 1, BLACK);
        p.pixel(x + w - 3, y + 1, BLACK);
        p.pixel(x + w - 2, y + 2, BLACK);
        p.pixel(x + 1, y + h - 3, BLACK);
        p.pixel(x + 2, y + h - 2, BLACK);
        p.pixel(x + w - 2, y + h - 2, BLACK);
        p.pixel(x + w - 1, y + h - 3, BLACK);
    }

    /**
     * A 25x24 tab glued to the side of a panel, like the redstone port's. The tab covers four columns of the panel
     * edge; {@code top} marks the tab that sits on the panel's top corner.
     */
    public static void panelTab(@Nonnull GuiGraphics graphics, int x, int y, boolean right, boolean selected, boolean top) {
        int fill = selected ? PANEL_FILL : TAB_FILL;
        if (!right) {
            int edge = x + 21;
            panel(clipped(graphics, row -> Integer.MIN_VALUE, row -> {
                int r = row - y;
                if (r == 0) {
                    return top ? edge + 1 : edge;
                }
                if (!selected) {
                    return edge - 1;
                }
                return r <= 20 ? edge + 2 : (r == 21 ? edge + 1 : edge);
            }), x, y, 30, 24, fill);
            if (selected && top) {
                graphics.fill(edge + 3, y + 3, edge + 4, y + 4, PANEL_FILL);
            }
            if (!selected) {
                graphics.fill(edge, y + (top ? 2 : 1), edge + 1, y + 24, BLACK);
                if (top) {
                    graphics.fill(edge, y + 1, edge + 1, y + 2, WHITE);
                }
            }
        } else {
            int edge = x + 3;
            panel(clipped(graphics, row -> {
                int r = row - y;
                if (selected) {
                    if (r <= 1) {
                        return top ? edge - 2 : edge;
                    }
                    if (r == 2) {
                        return top ? edge - 2 : edge - 1;
                    }
                    return r <= 22 ? edge - 2 : edge;
                }
                if (r == 0) {
                    return top ? edge - 2 : edge;
                }
                if (top && r == 1) {
                    return edge - 1;
                }
                if (top && r == 2) {
                    return edge;
                }
                return edge + 1;
            }, row -> Integer.MAX_VALUE), x - 5, y, 30, 24, fill);
            if (!selected) {
                graphics.fill(edge, y + (top ? 3 : 1), edge + 1, y + 24, BLACK);
            }
        }
    }

    /**
     * A free-standing 21x24 tab, open on the side that touches the window it belongs to.
     */
    public static void sideTab(@Nonnull GuiGraphics graphics, int x, int y, boolean right, boolean selected) {
        int fill = selected ? PANEL_FILL : TAB_FILL;
        if (right) {
            panel(clipped(graphics, row -> x, row -> Integer.MAX_VALUE), x - 9, y, 30, 24, fill);
        } else {
            panel(clipped(graphics, row -> Integer.MIN_VALUE, row -> x + 20), x, y, 30, 24, fill);
        }
    }

    /**
     * Flat bevel: light top row and left column, shadow bottom row and right column.
     */
    public static void bevel(@Nonnull GuiGraphics graphics, int x, int y, int width, int height, Palette palette) {
        graphics.fill(x, y, x + width, y + height, palette.fill());
        graphics.fill(x, y, x + width - 1, y + 1, palette.light());
        graphics.fill(x, y, x + 1, y + height - 1, palette.light());
        graphics.fill(x + 1, y + height - 1, x + width, y + height, palette.shadow());
        graphics.fill(x + width - 1, y + 1, x + width, y + height, palette.shadow());
    }

    /**
     * Button frame: black border around a bevel.
     */
    public static void button(@Nonnull GuiGraphics graphics, int x, int y, int width, int height, Palette palette) {
        graphics.fill(x, y, x + width, y + height, BLACK);
        bevel(graphics, x + 1, y + 1, width - 2, height - 2, palette);
    }

    /**
     * Button with its glyph. Glyphs sit 4 pixels in, like on the Big buttons.
     */
    public static void button(@Nonnull GuiGraphics graphics, int x, int y, int width, int height, Palette palette, GuiIcon glyph) {
        button(graphics, x, y, width, height, palette);
        glyph.draw(graphics, x + 4, y + 4);
    }

    /**
     * Push button (eject, arrows, apply, revert) in its normal or hovered look.
     */
    public static void pushButton(@Nonnull GuiGraphics graphics, int x, int y, int width, int height, boolean hovered, GuiIcon.Glyph glyph) {
        button(graphics, x, y, width, height, hovered ? BUTTON_HOVER : BUTTON, hovered ? glyph.hover() : glyph.normal());
    }

    /**
     * Sunken slot: shadow on the top and left, white on the bottom and right.
     */
    public static void slot(@Nonnull GuiGraphics graphics, int x, int y, int size) {
        graphics.fill(x, y, x + size, y + size, SLOT_FILL);
        graphics.fill(x, y, x + size - 1, y + 1, SLOT_SHADOW);
        graphics.fill(x, y, x + 1, y + size - 1, SLOT_SHADOW);
        graphics.fill(x + 1, y + size - 1, x + size, y + size, WHITE);
        graphics.fill(x + size - 1, y + 1, x + size, y + size, WHITE);
    }

    /**
     * 1 pixel colored ring.
     */
    public static void ring(@Nonnull GuiGraphics graphics, int x, int y, int size, int color) {
        graphics.fill(x, y, x + size, y + 1, color);
        graphics.fill(x, y + size - 1, x + size, y + size, color);
        graphics.fill(x, y, x + 1, y + size, color);
        graphics.fill(x + size - 1, y, x + size, y + size, color);
    }

    /**
     * Player inventory: three rows of nine slots and the hotbar 4 pixels below them.
     */
    public static void playerInventory(@Nonnull GuiGraphics graphics, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                slot(graphics, x + 18 * column, y + 18 * row, 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            slot(graphics, x + 18 * column, y + 58, 18);
        }
    }

    /**
     * Selector frame with {@code cells} empty 14x12 cells; the selected cell is drawn on top by the caller.
     */
    public static void selector(@Nonnull GuiGraphics graphics, int x, int y, int cells, int selected) {
        graphics.fill(x, y, x + 1 + 15 * cells, y + 14, BLACK);
        for (int i = 0; i < cells; i++) {
            graphics.fill(x + 1 + 15 * i, y + 1, x + 15 + 15 * i, y + 13, SELECTOR_EMPTY);
        }
        for (int i = 1; i < cells; i++) {
            if (i != selected && i - 1 != selected) {
                graphics.fill(x + 15 * i, y + 1, x + 15 * i + 1, y + 13, SELECTOR_EMPTY_SEPARATOR);
            }
        }
    }

    /**
     * Text box frame, drawn as the old one was: left cap, 6 pixel middle pieces up to {@code width}, right cap.
     */
    public static void textBox(@Nonnull GuiGraphics graphics, int x, int y, int width, int charLimit) {
        textBoxPiece(graphics, x, y, 3, true, false);
        for (int i = 0; i <= width; i += 6) {
            textBoxPiece(graphics, x + 3 + i, y, 6, false, false);
        }
        textBoxPiece(graphics, x + 6 + 6 * charLimit, y, 3, false, true);
    }

    private static void textBoxPiece(GuiGraphics graphics, int x, int y, int width, boolean left, boolean right) {
        graphics.fill(x, y, x + width, y + 16, BLACK);
        graphics.fill(x, y, x + width, y + 1, TEXT_BOX_LIGHT);
        graphics.fill(x, y + 15, x + width, y + 16, TEXT_BOX_SHADOW);
        if (left) {
            graphics.fill(x, y, x + 1, y + 16, TEXT_BOX_LIGHT);
            graphics.fill(x, y + 15, x + 1, y + 16, TEXT_BOX_CORNER);
        }
        if (right) {
            graphics.fill(x + width - 1, y, x + width, y + 16, TEXT_BOX_SHADOW);
            graphics.fill(x + width - 1, y, x + width, y + 1, TEXT_BOX_CORNER);
        }
    }

    /**
     * 16x16 symbol filled with a fluid: grey well, the fluid, the panel colored cover around the shape, then the
     * outline 3 pixels in.
     */
    public static void fluidSymbol(@Nonnull GuiGraphics graphics, int x, int y, Fluid fluid, GuiIcon mask, GuiIcon outline, int background, boolean well) {
        if (well) {
            graphics.fill(x, y, x + 16, y + 16, SLOT_FILL);
        }
        if (fluid != null && !(fluid instanceof EmptyFluid)) {
            RenderHelper.drawFluid(graphics, x, y, 0, 16, 16, fluid);
        }
        mask.drawTinted(graphics, x, y, background);
        outline.draw(graphics, x + 3, y);
    }
}
