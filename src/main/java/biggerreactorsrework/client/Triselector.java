package biggerreactorsrework.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import biggerreactorsrework.core.client.gui.elements.InteractiveElement;
import biggerreactorsrework.core.client.gui.screens.ModScreen;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.IntSupplier;

public class Triselector<T extends AbstractContainerMenu> extends InteractiveElement<T> {
    
    private final IntSupplier renderState;
    
    /**
     * Selector state for the switch.
     */
    private int state;

    /**
     * The color of each cell, left to right.
     */
    private final SelectorColors[] colors;

    /**
     * The state each cell stands for, left to right.
     */
    private final int[] cellStates;

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
        this(parent, x, y, tooltip, initialState, new int[]{0, 1, 2}, leftColor, centerColor, rightColor);
    }

    /**
     * Selector whose cells, left to right, stand for {@code cellStates}.
     */
    public Triselector(@Nonnull ModScreen<T> parent, int x, int y, @Nullable Component tooltip, IntSupplier initialState, int[] cellStates, SelectorColors leftColor, SelectorColors centerColor, SelectorColors rightColor) {
        super(parent, x, y, GuiArt.cellsWidth(3), GuiArt.TOGGLE_HEIGHT, 0, 0, tooltip);
        this.renderState = initialState;
        this.state = initialState.getAsInt();
        this.cellStates = cellStates;
        this.colors = new SelectorColors[]{leftColor, centerColor, rightColor};
    }

    /**
     * The cell that stands for {@code state}; states no cell stands for show on the right cell.
     */
    private int cellOf(int state) {
        for (int i = 0; i < this.cellStates.length; i++) {
            if (this.cellStates[i] == state) {
                return i;
            }
        }
        return this.cellStates.length - 1;
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
    public void render(@Nonnull GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Check conditions.
        if (this.renderEnable) {
            GuiArt.toggleCells(graphics, this.x, this.y, this.cellOf(this.renderState.getAsInt()), this.actionEnable && this.isMouseOver(mouseX, mouseY), !this.actionEnable, this.parent.getFrame(), this.colors);
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
            int relativeX = this.parent.getLeftPos() + this.x;
            // Check if the selector is enabled.
            if (this.actionEnable) {
                // Set side depending on position of click.
                if (mouseX > relativeX) {
                    if ((mouseX < relativeX + (int) (this.width / 3))) {
                        // Left cell.
                        this.state = this.cellStates[0];
                    } else if ((mouseX < relativeX + ((int) (this.width / 3) * 2))) {
                        // Center cell.
                        this.state = this.cellStates[1];
                    } else {
                        // Right cell.
                        this.state = this.cellStates[2];
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

