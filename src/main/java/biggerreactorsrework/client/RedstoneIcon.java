//? if =26.1.2 || =26.3 {
package biggerreactorsrework.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Modified 2026-11-18: redstone-control symbols are image files in textures/gui/icons, drawn at whole GUI pixels. */
public final class RedstoneIcon {
//?} else if =1.21.1 {
/*package biggerreactorsrework.client;

import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/^* Modified 2026-11-18: redstone-control symbols are image files in textures/gui/icons, drawn at whole GUI pixels. ^/
@OnlyIn(Dist.CLIENT)
public final class RedstoneIcon {
*/
//?}
    public static final RedstoneIcon DISABLED = symbol("redstone_disabled", 12);
    public static final RedstoneIcon ABOVE = symbol("redstone_above", 12);
    public static final RedstoneIcon BELOW = symbol("redstone_below", 12);
    public static final RedstoneIcon CONFIRM = symbol("redstone_confirm", 8);
    public static final RedstoneIcon REVERT = symbol("redstone_revert", 8);
    public static final RedstoneIcon ENTER = symbol("redstone_enter", 8);
    private static final int DISABLED_TINT = 0x92948E;
    private final GuiIcon image;
    private final boolean symbol;
    private RedstoneIcon(GuiIcon image, boolean symbol) { this.image = image; this.symbol = symbol; }
    private static RedstoneIcon symbol(String name, int size) { return new RedstoneIcon(GuiIcon.named(name, size, size), true); }
//? if =26.1.2 || =26.3 {
    public static RedstoneIcon image(GuiIcon image) { return new RedstoneIcon(image, false); }

    /** Only illustrated status icons retain the existing tab scale. Symbols use whole GUI pixels. */
    public void tab(GuiGraphicsExtractor graphics, int earX, int y, boolean left) {
//?} else if =1.21.1 {
/*    public static RedstoneIcon image(GuiIcon image) { return new RedstoneIcon(image, false); }

    /^* Only illustrated status icons retain the existing tab scale. Symbols use whole GUI pixels. ^/
    public void tab(GuiGraphics graphics, int earX, int y, boolean left) {
*/
//?}
        if (!symbol) {
            GuiArt.tabIcon(graphics, earX, y, left, image.width(), image.height(), () -> image.draw(graphics, 0, 0));
        } else {
            image.draw(graphics, GuiFrame.tabButtonX(earX, left) + (GuiFrame.TAB_BUTTON - image.width()) / 2,
                    y + GuiFrame.TAB_BUTTON_TOP + (GuiFrame.TAB_BUTTON - image.height()) / 2);
        }
//? if =26.1.2 || =26.3 {
    }
    public void button(GuiGraphicsExtractor graphics, int x, int y, int width, int height, boolean hovered, boolean disabled, GuiFrame frame) {
        GuiArt.buttonBackground(graphics, x, y, width, height, hovered, false, disabled, frame);
//?} else if =1.21.1 {
/*    }
    public void button(GuiGraphics graphics, int x, int y, int width, int height, boolean hovered, boolean disabled, GuiFrame frame) {
        GuiArt.buttonBackground(graphics, x, y, width, height, hovered, false, disabled, frame);
*/
//?}
        int ix = x + (width - image.width()) / 2, iy = y + (height - image.height()) / 2;
        if (symbol && disabled) image.drawTinted(graphics, ix, iy, DISABLED_TINT);
        else image.draw(graphics, ix, iy);
//? if =26.1.2 || =26.3 {
    }
    public void draw(GuiGraphicsExtractor graphics, int x, int y) {
//?} else if =1.21.1 {
/*    }
    public void draw(GuiGraphics graphics, int x, int y) {
*/
//?}
        image.draw(graphics, x, y);
    }
}
