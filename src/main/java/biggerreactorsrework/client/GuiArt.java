package biggerreactorsrework.client;

import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.EmptyFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import biggerreactorsrework.core.client.gui.RenderHelper;

import org.joml.Vector3f;

import javax.annotation.Nonnull;

/**
 * Draws the pieces every Bigger Reactors screen is made of (panel, slots, buttons, tabs, selectors, text boxes) with
 * plain fills; the metal frame around the panels comes from {@link GuiFrame}. Icons live in their own files, see {@link GuiIcon}.
 */
@OnlyIn(Dist.CLIENT)
public final class GuiArt {

    public static final int BLACK = 0xFF000000;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int PANEL_FILL = 0xFFC6C6C6;
    public static final int PANEL_SHADOW = 0xFF555555;
    public static final int SLOT_FILL = 0xFF8B8B8B;
    public static final int SLOT_SHADOW = 0xFF373737;
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

    private GuiArt() {
    }

    public static void fill(@Nonnull GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
        graphics.fill(x0, y0, x1, y1, GuiTheme.chromeColor(color));
    }

    /**
     * The themed center of a screen or window, set into the unchanged metal frame of its machine.
     */
    public static void panel(@Nonnull GuiGraphics graphics, int x, int y, int width, int height, @Nonnull GuiFrame frame) {
        frame.panel(graphics, x, y, width, height, GuiTheme.panelColor());
    }

    /**
     * Button of a side tab whose ear starts at {@code earX} and whose top is {@code y}: switched on while the tab is
     * selected. The ears themselves are drawn by {@link GuiFrame#ear}, one per block of stacked tabs.
     */
    public static void tabButton(@Nonnull GuiGraphics graphics, int earX, int y, boolean leftSide, boolean hovered, boolean selected, @Nonnull GuiFrame frame) {
        buttonBackground(graphics, GuiFrame.tabButtonX(earX, leftSide), y + GuiFrame.TAB_BUTTON_TOP, GuiFrame.TAB_BUTTON,
                GuiFrame.TAB_BUTTON, hovered, selected, false, frame);
    }

    /**
     * Icon of {@code width} x {@code height} centered on the button of a tab (see {@link #tabButton}), drawn at
     * {@link GuiFrame#TAB_ICON_SCALE}; {@code draw} paints it with its top-left corner at (0, 0).
     */
    public static void tabIcon(@Nonnull GuiGraphics graphics, int earX, int y, boolean leftSide, int width, int height, Runnable draw) {
        float scale = GuiFrame.TAB_ICON_SCALE;
        graphics.pose().pushPose();
        graphics.pose().translate(GuiFrame.tabButtonX(earX, leftSide) + (GuiFrame.TAB_BUTTON - width * scale) / 2,
                y + GuiFrame.TAB_BUTTON_TOP + (GuiFrame.TAB_BUTTON - height * scale) / 2, 0);
        graphics.pose().scale(scale, scale, 1);
        draw.run();
        graphics.pose().popPose();
    }

    /**
     * Every button of the screens: dark outline (cyan when hovered), light border (grey when disabled), slate fill
     * (teal when the button is switched on) and its glyph centered. The dark parts take the tone of the screen's frame.
     */
    public static void button(@Nonnull GuiGraphics graphics, int x, int y, int width, int height, boolean hovered, boolean active, boolean disabled, GuiIcon glyph, @Nonnull GuiFrame frame) {
        buttonBackground(graphics, x, y, width, height, hovered, active, disabled, frame);
        glyph.draw(graphics, x + (width - glyph.width()) / 2, y + (height - glyph.height()) / 2);
    }

    public static void button(@Nonnull GuiGraphics graphics, int x, int y, int width, int height, boolean hovered, GuiIcon glyph, @Nonnull GuiFrame frame) {
        button(graphics, x, y, width, height, hovered, false, false, glyph, frame);
    }

    /**
     * Button without a glyph.
     */
    public static void buttonBackground(@Nonnull GuiGraphics graphics, int x, int y, int width, int height, boolean hovered, boolean active, boolean disabled, @Nonnull GuiFrame frame) {
        buttonBorder(graphics, x, y, width, height, hovered, disabled, frame);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, buttonFill(active, disabled, frame));
    }

    /**
     * Only the outline and the light border of a button, for symbols that cover the inside of the button.
     */
    public static void buttonBorder(@Nonnull GuiGraphics graphics, int x, int y, int width, int height, boolean hovered, boolean disabled, @Nonnull GuiFrame frame) {
        int outline = hovered && !disabled ? TOGGLE_OUTLINE_HOVER : frame.tint(BUTTON_OUTLINE);
        fill(graphics, x, y, x + width, y + 1, outline);
        fill(graphics, x, y + height - 1, x + width, y + height, outline);
        fill(graphics, x, y + 1, x + 1, y + height - 1, outline);
        fill(graphics, x + width - 1, y + 1, x + width, y + height - 1, outline);
        int border = disabled ? frame.tint(TOGGLE_BORDER_DISABLED) : TOGGLE_BORDER;
        fill(graphics, x + 1, y + 1, x + width - 1, y + 2, border);
        fill(graphics, x + 1, y + height - 2, x + width - 1, y + height - 1, border);
        fill(graphics, x + 1, y + 2, x + 2, y + height - 2, border);
        fill(graphics, x + width - 2, y + 2, x + width - 1, y + height - 2, border);
    }

    /**
     * Inside color of a button.
     */
    public static int buttonFill(boolean active, boolean disabled, @Nonnull GuiFrame frame) {
        return GuiTheme.chromeColor(disabled ? frame.tint(BUTTON_FILL_DISABLED) : (active ? BUTTON_FILL_ACTIVE : frame.tint(BUTTON_FILL)));
    }

    /**
     * Sunken slot: shadow on the top and left, white on the bottom and right.
     */
    public static void slot(@Nonnull GuiGraphics graphics, int x, int y, int size) {
        fill(graphics, x, y, x + size, y + size, SLOT_FILL);
        fill(graphics, x, y, x + size - 1, y + 1, SLOT_SHADOW);
        fill(graphics, x, y, x + 1, y + size - 1, SLOT_SHADOW);
        fill(graphics, x + 1, y + size - 1, x + size, y + size, WHITE);
        fill(graphics, x + size - 1, y + 1, x + size, y + size, WHITE);
    }

    /**
     * 1 pixel colored ring.
     */
    public static void ring(@Nonnull GuiGraphics graphics, int x, int y, int size, int color) {
        fill(graphics, x, y, x + size, y + 1, color);
        fill(graphics, x, y + size - 1, x + size, y + size, color);
        fill(graphics, x, y, x + 1, y + size, color);
        fill(graphics, x + size - 1, y, x + size, y + size, color);
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
     * Toggles: dark rounded frame (the outline turns cyan when hovered, the light border greys out when disabled); the
     * dark parts take the tone of the screen's frame.
     */
    private static void toggleFrame(GuiGraphics graphics, int x, int y, int width, boolean hovered, boolean disabled, GuiFrame frame) {
        fill(graphics, x, y, x + width, y + TOGGLE_HEIGHT, hovered ? TOGGLE_OUTLINE_HOVER : frame.tint(TOGGLE_OUTLINE));
        fill(graphics, x + 1, y + 1, x + width - 1, y + TOGGLE_HEIGHT - 1, disabled ? frame.tint(TOGGLE_BORDER_DISABLED) : TOGGLE_BORDER);
        fill(graphics, x + 2, y + 2, x + width - 2, y + TOGGLE_HEIGHT - 2, frame.tint(TOGGLE_INSET));
        fill(graphics, x + 3, y + 3, x + width - 3, y + TOGGLE_HEIGHT - 3, frame.tint(TOGGLE_TRACK));
    }

    /**
     * On/off switch, 31x14: the knob sits on the left when off; when on it sits on the right, after a block in the
     * switch's color.
     */
    public static void toggleSwitch(@Nonnull GuiGraphics graphics, int x, int y, boolean on, boolean hovered, boolean disabled, SelectorColors color, @Nonnull GuiFrame frame) {
        toggleFrame(graphics, x, y, SWITCH_WIDTH, hovered, disabled, frame);
        if (on) {
            // Status colors are semantic indicators, not neutral chrome; retain their exact original RGB.
            graphics.fill(x + 12, y + 3, x + 20, y + 4, disabled ? GuiTheme.chromeColor(SWITCH_LIT_TOP_DISABLED) : color.switchTop);
            graphics.fill(x + 12, y + 4, x + 20, y + 10, disabled ? GuiTheme.chromeColor(SWITCH_LIT_FILL_DISABLED) : color.switchFill);
            graphics.fill(x + 12, y + 10, x + 20, y + 11, color.switchBottom);
        }
        knob(graphics, x + (on ? 20 : 3), y + 3, disabled);
    }

    private static void knob(GuiGraphics graphics, int x, int y, boolean disabled) {
        fill(graphics, x, y, x + 8, y + 8, disabled ? KNOB_FILL_DISABLED : KNOB_FILL);
        fill(graphics, x + 1, y, x + 7, y + 1, disabled ? KNOB_TOP_DISABLED : KNOB_TOP);
        fill(graphics, x + 1, y + 1, x + 2, y + 6, KNOB_SHINE);
        fill(graphics, x + 3, y + 2, x + 6, y + 3, KNOB_SHINE);
        fill(graphics, x, y, x + 1, y + 8, KNOB_EDGE);
        fill(graphics, x, y + 7, x + 8, y + 8, KNOB_EDGE);
    }

    /**
     * Selector with {@code colors.length} cells of 12x8 split by the track; only the {@code selected} cell is lit.
     */
    public static void toggleCells(@Nonnull GuiGraphics graphics, int x, int y, int selected, boolean hovered, boolean disabled, @Nonnull GuiFrame frame, SelectorColors... colors) {
        toggleFrame(graphics, x, y, cellsWidth(colors.length), hovered, disabled, frame);
        for (int i = 0; i < colors.length; i++) {
            int cx = x + 3 + 14 * i;
            boolean lit = i == selected;
            fill(graphics, cx, y + 3, cx + 12, y + 11, frame.tint(CELL_FRAME));
            int top = !lit ? frame.tint(CELL_TOP) : (disabled ? CELL_LIT_TOP_DISABLED : colors[i].cellTop);
            int left = !lit ? frame.tint(CELL_LEFT) : (disabled ? CELL_LIT_LEFT_DISABLED : colors[i].cellLeft);
            int fill = !lit ? frame.tint(CELL_FILL) : (disabled ? CELL_LIT_FILL_DISABLED : colors[i].cellFill);
            graphics.fill(cx + 1, y + 4, cx + 11, y + 10, lit && !disabled ? fill : GuiTheme.chromeColor(fill));
            graphics.fill(cx + 1, y + 4, cx + 11, y + 5, lit && !disabled ? top : GuiTheme.chromeColor(top));
            graphics.fill(cx + 1, y + 5, cx + 2, y + 9, lit && !disabled ? left : GuiTheme.chromeColor(left));
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
        fill(graphics, x, y, x + width, y + 16, BLACK);
        fill(graphics, x, y, x + width, y + 1, TEXT_BOX_LIGHT);
        fill(graphics, x, y + 15, x + width, y + 16, TEXT_BOX_SHADOW);
        if (left) {
            fill(graphics, x, y, x + 1, y + 16, TEXT_BOX_LIGHT);
            fill(graphics, x, y + 15, x + 1, y + 16, TEXT_BOX_CORNER);
        }
        if (right) {
            fill(graphics, x + width - 1, y, x + width, y + 16, TEXT_BOX_SHADOW);
            fill(graphics, x + width - 1, y, x + width, y + 1, TEXT_BOX_CORNER);
        }
    }

    /**
     * 16x16 symbol filled with a fluid: grey well, the fluid, the panel colored cover around the shape, then the
     * outline 3 pixels in.
     */
    public static void fluidSymbol(@Nonnull GuiGraphics graphics, int x, int y, Fluid fluid, GuiIcon mask, GuiIcon outline, int background, boolean well) {
        if (well) {
            fill(graphics, x, y, x + 16, y + 16, SLOT_FILL);
        }
        if (fluid != null && !(fluid instanceof EmptyFluid)) {
            RenderHelper.drawFluid(graphics, x, y, 0, 16, 16, fluid);
        }
        mask.drawTinted(graphics, x, y, background);
        outline.draw(graphics, x + 3, y);
    }

    /**
     * Text at (x, y) kept inside {@code width}: drawn as is when it fits; otherwise it slides from one side to the other
     * (and back) inside that width, so all of it can be read without shrinking or cutting it.
     */
    public static void scrollingText(@Nonnull GuiGraphics graphics, @Nonnull Font font, String text, int x, int y, int width, int color) {
        scrollingText(graphics, font, net.minecraft.network.chat.Component.literal(text), x, y, width, color);
    }

    public static void scrollingText(@Nonnull GuiGraphics graphics, @Nonnull Font font, net.minecraft.network.chat.Component text, int x, int y, int width, int color) {
        int textWidth = font.width(text);
        if (textWidth <= width) {
            graphics.drawString(font, text, x, y, color, false);
            return;
        }
        int overflow = textWidth - width;
        double seconds = Util.getMillis() / 1000.0;
        double period = Math.max(overflow * 0.5, 3.0);
        double phase = Math.sin(Math.PI / 2 * Math.cos(2 * Math.PI * seconds / period)) / 2 + 0.5;
        int offset = (int) Math.round(Mth.lerp(phase, 0, overflow));
        // The scissor works in screen coordinates, while the text may be drawn relative to the screen's corner.
        Vector3f origin = graphics.pose().last().pose().getTranslation(new Vector3f());
        int left = Math.round(origin.x) + x;
        int top = Math.round(origin.y) + y;
        graphics.enableScissor(left, top - 1, left + width, top + font.lineHeight + 1);
        graphics.drawString(font, text, x - offset, y, color, false);
        graphics.disableScissor();
    }
}
