package biggerreactorsrework.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import biggerreactorsrework.core.client.gui.elements.InteractiveElement;
import biggerreactorsrework.core.client.gui.screens.ModScreen;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.IntSupplier;

@OnlyIn(Dist.CLIENT)
public class Triselector<T extends AbstractContainerMenu> extends InteractiveElement<T> {
    
    private final IntSupplier renderState;
    
    /**
     * Selector state for the switch.
     */
    private int state;

    /**
     * The color of the left selector.
     */
    private final SelectorColors leftColor;

    /**
     * The color of the center selector.
     */
    private final SelectorColors centerColor;

    /**
     * The color of the right selector.
     */
    private final SelectorColors rightColor;

    /**
     * Default constructor.
     *
     * @param parent       The parent screen of this element.
     * @param x            The x position of this element.
     * @param y            The y position of this element.
     * @param tooltip      The tooltip for this element. If null, a tooltip will not render. If you set a tooltip later, use StringTextComponent.EMPTY.
     * @param initialState The initial switch state to use.
     */
    public Triselector(@Nonnull ModScreen<T> parent, int x, int y, @Nullable Component tooltip, IntSupplier initialState, SelectorColors leftColor, SelectorColors centerColor, SelectorColors rightColor) {
        super(parent, x, y, 46, 14, 0, 0, tooltip);
        this.renderState = initialState;
        this.state = initialState.getAsInt();
        this.leftColor = leftColor;
        this.centerColor = centerColor;
        this.rightColor = rightColor;
    }

    /**
     * Get the current switch state.
     *
     * @return The current switch state.
     */
    public int getState() {
        return this.state;
    }

    /**
     * Render element.
     *
     * @param poseStack The current pose stack.
     * @param mouseX The x position of the mouse.
     * @param mouseY The y position of the mouse.
     */
    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY) {
        // Check conditions.
        if (this.renderEnable) {
            int state = this.renderState.getAsInt();
            int selected = (state == 0 || state == 1) ? state : 2;
            // Draw the selector frame, then the selected cell.
            GuiArt.selector(graphics, this.x, this.y, 3, selected);
            SelectorColors color = !this.actionEnable ? SelectorColors.DISABLED
                    : (selected == 0 ? this.leftColor : (selected == 1 ? this.centerColor : this.rightColor));
            GuiArt.bevel(graphics, this.x + 1 + 15 * selected, this.y + 1, 14, 12, this.isMouseOver(mouseX, mouseY) ? color.hovered : color.idle);
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
        if (this.isMouseOver(mouseX, mouseY)) {
            // Get actual x position.
            int relativeX = this.parent.getGuiLeft() + this.x;
            // Check if the selector is enabled.
            if (this.actionEnable) {
                // Set side depending on position of click.
                if (mouseX > relativeX) {
                    if ((mouseX < relativeX + (int) (this.width / 3))) {
                        // Set to left position (0).
                        this.state = 0;
                    } else if ((mouseX < relativeX + ((int) (this.width / 3) * 2))) {
                        // Set to center position (1).
                        this.state = 1;
                    } else {
                        // Set to right position (2).
                        this.state = 2;
                    }
                    // Play the selection sound.
                    this.playSound(SoundEvents.UI_BUTTON_CLICK);
                    // Trigger user-defined selection logic.
                    if (this.onMouseReleased != null) {
                        this.onMouseReleased.trigger(mouseX, mouseY, button);
                    }
                }
            }
            // The event was consumed.
            return true;
        } else {
            // The event was not consumed.
            return false;
        }
    }
}

