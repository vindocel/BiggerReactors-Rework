package biggerreactorsrework.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Added 2026-10-30: crisp, dedicated redstone-control symbols; other machine icons stay unchanged. */
public final class RedstoneIcon {
    private enum Symbol { DISABLED, ABOVE, BELOW, CONFIRM, REVERT, ENTER }
    public static final RedstoneIcon DISABLED = new RedstoneIcon(Symbol.DISABLED);
    public static final RedstoneIcon ABOVE = new RedstoneIcon(Symbol.ABOVE);
    public static final RedstoneIcon BELOW = new RedstoneIcon(Symbol.BELOW);
    public static final RedstoneIcon CONFIRM = new RedstoneIcon(Symbol.CONFIRM);
    public static final RedstoneIcon REVERT = new RedstoneIcon(Symbol.REVERT);
    public static final RedstoneIcon ENTER = new RedstoneIcon(Symbol.ENTER);
    private static final int SIZE = 12;
    private static final int ACTION_SIZE = 9;
    private final Symbol symbol;
    private final GuiIcon image;
    private RedstoneIcon(Symbol symbol) { this.symbol = symbol; this.image = null; }
    private RedstoneIcon(GuiIcon image) { this.image = image; this.symbol = null; }
    public static RedstoneIcon image(GuiIcon image) { return new RedstoneIcon(image); }

    /** Only illustrated status icons retain the existing tab scale. Symbols use whole GUI pixels. */
    public void tab(GuiGraphicsExtractor graphics, int earX, int y, boolean left) {
        if (image != null) {
            GuiArt.tabIcon(graphics, earX, y, left, image.width(), image.height(), () -> image.draw(graphics, 0, 0));
        } else {
            draw(graphics, GuiFrame.tabButtonX(earX, left) + (GuiFrame.TAB_BUTTON - SIZE) / 2,
                    y + GuiFrame.TAB_BUTTON_TOP + (GuiFrame.TAB_BUTTON - SIZE) / 2);
        }
    }
    public void button(GuiGraphicsExtractor graphics, int x, int y, int width, int height, boolean hovered, boolean disabled, GuiFrame frame) {
        GuiArt.buttonBackground(graphics, x, y, width, height, hovered, false, disabled, frame);
        if (image != null) image.draw(graphics, x + (width - image.width()) / 2, y + (height - image.height()) / 2);
        else {
            boolean compact = symbol == Symbol.CONFIRM || symbol == Symbol.REVERT || symbol == Symbol.ENTER;
            int size = compact ? ACTION_SIZE : SIZE;
            paint(graphics, x + (width - size) / 2, y + (height - size) / 2,
                    disabled ? 0xFF92948E : 0xFFF4F4EC, compact);
        }
    }
    public void draw(GuiGraphicsExtractor graphics, int x, int y) {
        if (image != null) image.draw(graphics, x, y);
        else paint(graphics, x, y, 0xFFF4F4EC);
    }
    private boolean pixel(int x, int y) {
        if (x < 1 || x > 10 || y < 1 || y > 10) return false;
        double radius = (x - 5.5) * (x - 5.5) + (y - 5.5) * (y - 5.5);
        return switch (symbol) {
            case DISABLED -> radius <= 25 && (radius >= 12 || Math.abs(x + y - 11) <= 1);
            case ABOVE -> arrow(x, y);
            case BELOW -> arrow(x, 11 - y);
            case CONFIRM -> x <= 4 && Math.abs(y - x - 3) <= 1 || x >= 4 && Math.abs(y + x - 11) <= 1;
            case ENTER -> x >= 8 && y <= 7 || y >= 6 && y <= 8 && x >= 3 || x <= 4 && Math.abs(y - 7) <= x - 1;
            case REVERT -> x <= 4 && Math.abs(y - 4) <= x - 1
                    || y >= 3 && y <= 4 && x >= 4 && x <= 8
                    || x >= 8 && y >= 4 && y <= 8
                    || y >= 8 && y <= 9 && x >= 4 && x <= 8;
        };
    }
    private static boolean arrow(int x, int y) {
        return y <= 5 && Math.abs(x - 5.5) <= y - .5 || y >= 5 && x >= 4 && x <= 7;
    }
    /** Modified 2026-10-30: action buttons need smaller, lighter glyphs than the side tabs. */
    private boolean actionPixel(int x, int y) {
        if (x < 0 || x > 7 || y < 0 || y > 7) return false;
        return switch (symbol) {
            case CONFIRM -> x <= 3 && y - x >= 3 && y - x <= 4 || x >= 3 && y + x >= 9 && y + x <= 10;
            case ENTER -> x >= 6 && y >= 1 && y <= 5 || y >= 4 && y <= 5 && x >= 1
                    || x <= 2 && Math.abs(y - 4) <= x;
            case REVERT -> x <= 2 && Math.abs(y - 2) <= x || x >= 2 && x <= 5 && y >= 1 && y <= 2
                    || x >= 5 && x <= 6 && y >= 2 && y <= 5 || x >= 2 && x <= 5 && y >= 5 && y <= 6;
            default -> false;
        };
    }
    private void paint(GuiGraphicsExtractor graphics, int x, int y, int color) {
        paint(graphics, x, y, color, false);
    }
    private void paint(GuiGraphicsExtractor graphics, int x, int y, int color, boolean compact) {
        // Shadow and foreground are drawn separately, so neighbouring pixels never erase one another.
        int first = compact ? 0 : 1, last = compact ? 7 : 10;
        for (int pass = 0; pass < 2; pass++) for (int row = first; row <= last; row++) {
            int start = -1;
            for (int column = first; column <= last + 1; column++) {
                boolean on = compact ? actionPixel(column, row) : pixel(column, row);
                if (on && start < 0) start = column;
                if (!on && start >= 0) {
                    int offset = pass == 0 ? 1 : 0;
                    graphics.fill(x + start + offset, y + row + offset, x + column + offset, y + row + offset + 1,
                            pass == 0 ? 0xFF30332D : color);
                    start = -1;
                }
            }
        }
    }
}
