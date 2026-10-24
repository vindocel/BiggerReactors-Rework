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

    public static final int SWITCH_WIDTH = 31;
    public static final int TOGGLE_HEIGHT = 14;
    private static final int TOGGLE_OUTLINE = 0xFF303B42;
    private static final int TOGGLE_OUTLINE_HOVER = 0xFF4AABB9;
    private static final int TOGGLE_BORDER = 0xFFE8EDEB;
    private static final int TOGGLE_BORDER_DISABLED = 0xFF879399;
    private static final int TOGGLE_INSET = 0xFF182229;
    private static final int TOGGLE_TRACK = 0xFF29363D;
    private static final int KNOB_EDGE = 0xFF3E4A50;
    private static final int KNOB_SHINE = 0xFFD3DDE0;
    private static final int KNOB_TOP = 0xFFF2F5F4;
    private static final int KNOB_FILL = 0xFFB2BFC5;
    private static final int KNOB_TOP_DISABLED = 0xFFA3ADB1;
    private static final int KNOB_FILL_DISABLED = 0xFF829097;
    private static final int SWITCH_LIT_TOP_DISABLED = 0xFF69777B;
    private static final int SWITCH_LIT_FILL_DISABLED = 0xFF506066;
    private static final int BUTTON_OUTLINE = 0xFF1A242B;
    private static final int BUTTON_FILL = 0xFF69767E;
    private static final int BUTTON_FILL_ACTIVE = 0xFF26858F;
    private static final int BUTTON_FILL_DISABLED = 0xFF506066;
    private static final int CELL_FRAME = 0xFF111B22;
    private static final int CELL_TOP = 0xFF465259;
    private static final int CELL_LEFT = 0xFF404C54;
    private static final int CELL_FILL = 0xFF36434B;
    private static final int CELL_LIT_TOP_DISABLED = 0xFF889298;
    private static final int CELL_LIT_LEFT_DISABLED = 0xFF7D888F;
    private static final int CELL_LIT_FILL_DISABLED = 0xFF627078;
    private static final int TEXT_BOX_LIGHT = 0xFF636363;
    private static final int TEXT_BOX_SHADOW = 0xFF252525;
    private static final int TEXT_BOX_CORNER = 0xFF474747;

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
     * Every button of the screens: dark outline (cyan when hovered), light border (grey when disabled), slate fill
     * (teal when the button is switched on) and its glyph centered.
     */
    public static void button(@Nonnull GuiGraphics graphics, int x, int y, int width, int height, boolean hovered, boolean active, boolean disabled, GuiIcon glyph) {
        graphics.fill(x, y, x + width, y + height, hovered && !disabled ? TOGGLE_OUTLINE_HOVER : BUTTON_OUTLINE);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, disabled ? TOGGLE_BORDER_DISABLED : TOGGLE_BORDER);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, disabled ? BUTTON_FILL_DISABLED : (active ? BUTTON_FILL_ACTIVE : BUTTON_FILL));
        glyph.draw(graphics, x + (width - glyph.width()) / 2, y + (height - glyph.height()) / 2);
    }

    public static void button(@Nonnull GuiGraphics graphics, int x, int y, int width, int height, boolean hovered, GuiIcon glyph) {
        button(graphics, x, y, width, height, hovered, false, false, glyph);
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
     * Toggles: dark rounded frame (the outline turns cyan when hovered, the light border greys out when disabled).
     */
    private static void toggleFrame(GuiGraphics graphics, int x, int y, int width, boolean hovered, boolean disabled) {
        graphics.fill(x, y, x + width, y + TOGGLE_HEIGHT, hovered ? TOGGLE_OUTLINE_HOVER : TOGGLE_OUTLINE);
        graphics.fill(x + 1, y + 1, x + width - 1, y + TOGGLE_HEIGHT - 1, disabled ? TOGGLE_BORDER_DISABLED : TOGGLE_BORDER);
        graphics.fill(x + 2, y + 2, x + width - 2, y + TOGGLE_HEIGHT - 2, TOGGLE_INSET);
        graphics.fill(x + 3, y + 3, x + width - 3, y + TOGGLE_HEIGHT - 3, TOGGLE_TRACK);
    }

    /**
     * On/off switch, 31x14: the knob sits on the left when off; when on it sits on the right, after a block in the
     * switch's color.
     */
    public static void toggleSwitch(@Nonnull GuiGraphics graphics, int x, int y, boolean on, boolean hovered, boolean disabled, SelectorColors color) {
        toggleFrame(graphics, x, y, SWITCH_WIDTH, hovered, disabled);
        if (on) {
            graphics.fill(x + 12, y + 3, x + 20, y + 4, disabled ? SWITCH_LIT_TOP_DISABLED : color.switchTop);
            graphics.fill(x + 12, y + 4, x + 20, y + 10, disabled ? SWITCH_LIT_FILL_DISABLED : color.switchFill);
            graphics.fill(x + 12, y + 10, x + 20, y + 11, color.switchBottom);
        }
        knob(graphics, x + (on ? 20 : 3), y + 3, disabled);
    }

    private static void knob(GuiGraphics graphics, int x, int y, boolean disabled) {
        graphics.fill(x, y, x + 8, y + 8, disabled ? KNOB_FILL_DISABLED : KNOB_FILL);
        graphics.fill(x + 1, y, x + 7, y + 1, disabled ? KNOB_TOP_DISABLED : KNOB_TOP);
        graphics.fill(x + 1, y + 1, x + 2, y + 6, KNOB_SHINE);
        graphics.fill(x + 3, y + 2, x + 6, y + 3, KNOB_SHINE);
        graphics.fill(x, y, x + 1, y + 8, KNOB_EDGE);
        graphics.fill(x, y + 7, x + 8, y + 8, KNOB_EDGE);
    }

    /**
     * Selector with {@code colors.length} cells of 12x8 split by the track; only the {@code selected} cell is lit.
     */
    public static void toggleCells(@Nonnull GuiGraphics graphics, int x, int y, int selected, boolean hovered, boolean disabled, SelectorColors... colors) {
        toggleFrame(graphics, x, y, cellsWidth(colors.length), hovered, disabled);
        for (int i = 0; i < colors.length; i++) {
            int cx = x + 3 + 14 * i;
            boolean lit = i == selected;
            graphics.fill(cx, y + 3, cx + 12, y + 11, CELL_FRAME);
            int top = !lit ? CELL_TOP : (disabled ? CELL_LIT_TOP_DISABLED : colors[i].cellTop);
            int left = !lit ? CELL_LEFT : (disabled ? CELL_LIT_LEFT_DISABLED : colors[i].cellLeft);
            int fill = !lit ? CELL_FILL : (disabled ? CELL_LIT_FILL_DISABLED : colors[i].cellFill);
            graphics.fill(cx + 1, y + 4, cx + 11, y + 10, fill);
            graphics.fill(cx + 1, y + 4, cx + 11, y + 5, top);
            graphics.fill(cx + 1, y + 5, cx + 2, y + 9, left);
        }
    }

    public static int cellsWidth(int cells) {
        return 6 + 14 * cells - 2;
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
