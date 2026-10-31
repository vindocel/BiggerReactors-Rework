package biggerreactorsrework.multiblocks.reactor.client;

import biggerreactorsrework.client.GuiTheme;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.client.Biselector;
import biggerreactorsrework.client.SelectorColors;
import biggerreactorsrework.client.GuiArt;
import biggerreactorsrework.client.GuiFrame;
import biggerreactorsrework.client.GuiIcon;
import biggerreactorsrework.multiblocks.reactor.containers.ReactorCoolantPortContainer;
import biggerreactorsrework.multiblocks.reactor.state.ReactorCoolantPortState;
import biggerreactorsrework.core.client.gui.elements.InteractiveElement;
import biggerreactorsrework.core.client.gui.screens.ModScreen;

import javax.annotation.Nonnull;

public class ReactorCoolantPortScreen extends ModScreen<ReactorCoolantPortContainer> {


    private ReactorCoolantPortState reactorCoolantPortState;

    public ReactorCoolantPortScreen(ReactorCoolantPortContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory, title, 142, 56);

        // Initialize access port state.
        reactorCoolantPortState = (ReactorCoolantPortState) this.getMenu().getGuiPacket();
    }

    /**
     * Initialize the screen.
     */
    @Override
    public void init() {
        super.init();

        // Set title to be drawn in the center.
        this.titleLabelX = (this.imageWidth - this.font.width(this.getTitle())) / 2;

        // Initialize tooltips:

        // Initialize controls:
        this.initControls();

        // Initialize gauges:

        // Initialize symbols:
    }

    /**
     * Initialize controls.
     */
    public void initControls() {
        // (Left) Direction toggle:
        Biselector<ReactorCoolantPortContainer> directionToggle = new Biselector<>(this, 8, 18, Component.translatable("screen.biggerreactorsrework.reactor_coolant_port.direction_toggle.tooltip"),
                () -> reactorCoolantPortState.direction ? 0 : 1, SelectorColors.RED);
        directionToggle.onMouseReleased = (mX, mY, btn) -> {
            // Click logic.
            this.getMenu().executeRequest("setDirection", directionToggle.getState() == 0 ? 1 : 0);
            return true;
        };
        this.addScreenElement(directionToggle);

        // (Left) Manual dump button:
        InteractiveElement<ReactorCoolantPortContainer> manualEjectButton = new InteractiveElement<>(this, 8, 34, 15, 15, 0, 0, Component.translatable("screen.biggerreactorsrework.reactor_coolant_port.manual_dump.tooltip"));
        manualEjectButton.onMouseReleased = (mX, mY, btn) -> {
            // Click logic. Extra check necessary since this is an "in-class" button.
            if (manualEjectButton.isMouseOver(mX, mY)) {
                // Mouse is hovering, do the thing.
                this.getMenu().executeRequest("dumpTanks", true);
                // Play the selection sound.
                manualEjectButton.playSound(SoundEvents.UI_BUTTON_CLICK);
                return true;
            } else {
                // It ain't hovered, don't do the thing.
                return false;
            }
        };
        manualEjectButton.onRender = ((mS, mX, mY) -> {
            // Custom rendering.
            GuiArt.button(mS, manualEjectButton.x, manualEjectButton.y, manualEjectButton.width, manualEjectButton.height, manualEjectButton.isMouseOver(mX, mY), GuiIcon.EJECT, this.frame());
        });
        this.addScreenElement(manualEjectButton);
    }

    /**
     * Draw the status text for this screen.
     *
     * @param poseStack    The current pose stack.
     * @param mouseX       The x position of the mouse.
     * @param mouseY       The y position of the mouse.
     * @param partialTicks Partial ticks.
     */
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);

        // Render text for input/output direction:
        if (reactorCoolantPortState.direction) {
            // Text for an inlet:
            graphics.text(this.getFont(), Component.translatable("screen.biggerreactorsrework.reactor_coolant_port.direction_toggle.input").getString(), this.getLeftPos() + 42, this.getTopPos() + 22, GuiTheme.textColor(), false);

        } else {
            // Text for an outlet:
            graphics.text(this.getFont(), Component.translatable("screen.biggerreactorsrework.reactor_coolant_port.direction_toggle.output").getString(), this.getLeftPos() + 42, this.getTopPos() + 22, GuiTheme.textColor(), false);
        }

        // Render text for manual tank eject:
        graphics.text(this.getFont(), Component.translatable("screen.biggerreactorsrework.reactor_coolant_port.manual_dump").getString(), this.getLeftPos() + 26, this.getTopPos() + 38, GuiTheme.textColor(), false);
    }

    @Override
    protected GuiFrame frame() {
        return GuiFrame.REACTOR;
    }
}
