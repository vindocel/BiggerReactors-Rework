package biggerreactorsrework.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import biggerreactorsrework.core.client.gui.screens.ModScreen;
import biggerreactorsrework.core.client.gui.elements.InteractiveElement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public class CommonButton<T extends AbstractContainerMenu> extends InteractiveElement<T> {

    /**
     * Glyph drawn in the normal and hovered looks.
     */
    private final GuiIcon.Glyph glyph;

    /**
     * Glyph drawn while the button is disabled.
     */
    private final GuiIcon disabledGlyph;

    /**
     * Default constructor.
     *
     * @param parent        The parent screen of this element.
     * @param x             The x position of this element.
     * @param y             The y position of this element.
     * @param width         The width of this element.
     * @param height        The height of this element.
     * @param glyph         The glyph to draw on the button.
     * @param disabledGlyph The glyph to draw while the button is disabled.
     * @param tooltip       The tooltip for this element. If null, a tooltip will not render. If you set a tooltip later, use StringTextComponent.EMPTY.
     */
    public CommonButton(@Nonnull ModScreen<T> parent, int x, int y, int width, int height, GuiIcon.Glyph glyph, GuiIcon disabledGlyph, @Nullable Component tooltip) {
        super(parent, x, y, width, height, 0, 0, tooltip);
        this.glyph = glyph;
        this.disabledGlyph = disabledGlyph;
    }

    /**
     * Render element.
     *
     * @param graphics The current pose stack.
     * @param mouseX The x position of the mouse.
     * @param mouseY The y position of the mouse.
     */
    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY) {
        // Check conditions.
        if (this.renderEnable) {
            if (!this.actionEnable) {
                // Draw disabled button.
                GuiArt.button(graphics, this.x, this.y, this.width, this.height, GuiArt.BUTTON_DISABLED, this.disabledGlyph);
            } else if (this.isMouseOver(mouseX, mouseY)) {
                // Draw active/hovered button.
                GuiArt.button(graphics, this.x, this.y, this.width, this.height, GuiArt.COMMON_BUTTON_HOVER, this.glyph.hover());
            } else {
                // Draw inactive/non-hovered button.
                GuiArt.button(graphics, this.x, this.y, this.width, this.height, GuiArt.BUTTON, this.glyph.normal());
            }
            // Trigger user-defined render logic.
            if (this.onRender != null) {
                this.onRender.trigger(graphics, mouseX, mouseY);
            }
        }
    }

    /**
     * Triggered when the mouse is released.
     *
     * @param mouseX The x position of the mouse.
     * @param mouseY The y position of the mouse.
     * @param button The button clicked.
     * @return Whether the event was consumed.
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // Check conditions.
        if (this.actionEnable && this.isMouseOver(mouseX, mouseY)) {
            // Play the selection sound.
            this.playSound(SoundEvents.UI_BUTTON_CLICK);
            // Trigger user-defined selection logic.
            if (this.onMouseReleased != null) {
                this.onMouseReleased.trigger(mouseX, mouseY, button);
            }
            // The event was consumed.
            return true;
        } else {
            // The event was not consumed.
            return false;
        }
    }
}
