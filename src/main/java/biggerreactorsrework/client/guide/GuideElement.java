package biggerreactorsrework.client.guide;

import net.minecraft.client.gui.GuiGraphics;

/**
 * One block of a guide chapter, laid out top to bottom in the scrolling page. Coordinates are absolute; the
 * mouse position is outside the page ({@code -1}) whenever the pointer is not over its visible area.
 */
interface GuideElement {
    /** Height for the given page width. */
    int height(GuideScreen screen, int width);

    void render(GuideScreen screen, GuiGraphics g, int x, int y, int width, int mouseX, int mouseY);

    /** Before any page of the frame is drawn (x, y: its top-left corner), for hover state shared between pages. */
    default void prepare(GuideScreen screen, int x, int y, int width, int mouseX, int mouseY) {
    }

    /** A click on the element (x, y: its top-left corner); true when handled. */
    default boolean click(GuideScreen screen, double mouseX, double mouseY, int x, int y, int width, int button) {
        return false;
    }

    /** A drag that started on this element; true when handled. */
    default boolean drag(GuideScreen screen, double dragX, double dragY) {
        return false;
    }

    /** The button of a drag that started on this element was released. */
    default void release() {
    }

    /** Mouse wheel over the element; true when it scrolled something instead of turning the page. */
    default boolean scroll(GuideScreen screen, double mouseX, double mouseY, int x, int y, int width, int height, double amount) {
        return false;
    }

    /** Keys reach a focused element before the book's shortcuts; true when consumed. */
    default boolean keyPressed(GuideScreen screen, int keyCode, int scanCode, int modifiers) {
        return false;
    }

    default boolean charTyped(GuideScreen screen, char codePoint, int modifiers) {
        return false;
    }

    /** Another click happened: drop keyboard focus. */
    default void unfocus() {
    }
}
