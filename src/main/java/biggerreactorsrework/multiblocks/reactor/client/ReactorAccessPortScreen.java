package biggerreactorsrework.multiblocks.reactor.client;

import biggerreactorsrework.client.GuiTheme;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.client.Biselector;
import biggerreactorsrework.client.SelectorColors;
import biggerreactorsrework.client.GuiArt;
import biggerreactorsrework.client.GuiFrame;
import biggerreactorsrework.client.GuiIcon;
import biggerreactorsrework.multiblocks.reactor.containers.ReactorAccessPortContainer;
import biggerreactorsrework.multiblocks.reactor.state.ReactorAccessPortState;
import biggerreactorsrework.core.client.gui.screens.ModScreen;
import biggerreactorsrework.core.client.gui.elements.InteractiveElement;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public class ReactorAccessPortScreen extends ModScreen<ReactorAccessPortContainer> {


    private ReactorAccessPortState reactorAccessPortState;

    public ReactorAccessPortScreen(ReactorAccessPortContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory, title, 142, 72);

        // Initialize access port state.
        reactorAccessPortState = (ReactorAccessPortState) this.getMenu().getGuiPacket();
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
    private Biselector<ReactorAccessPortContainer> recoveryToggle;

    // Modified 2026-11-04: outlets always send cyanite; inlets can recover stored fuel.
    public void initControls() {
        // (Left) Direction toggle:
        Biselector<ReactorAccessPortContainer> directionToggle = new Biselector<>(this, 8, 18, Component.translatable("screen.biggerreactorsrework.reactor_access_port.direction_toggle.tooltip"),
                () -> reactorAccessPortState.direction ? 0 : 1, SelectorColors.GREEN, SelectorColors.BLUE);
        directionToggle.onMouseReleased = (mX, mY, btn) -> {
            // Click logic.
            this.getMenu().executeRequest("setDirection", directionToggle.getState() == 0 ? 1 : 0);
            return true;
        };
        this.addScreenElement(directionToggle);

        recoveryToggle = new Biselector<>(this, 8, 34,
                Component.translatable("screen.biggerreactorsrework.reactor_access_port.recover_fuel.tooltip"),
                () -> reactorAccessPortState.recoverFuel ? 1 : 0, SelectorColors.GREEN);
        recoveryToggle.onMouseReleased = (mouseX, mouseY, button) -> {
            if (!reactorAccessPortState.direction) return false;
            getMenu().executeRequest("recoverFuel", recoveryToggle.getState() == 0);
            return true;
        };
        recoveryToggle.disable();
        addScreenElement(recoveryToggle);
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
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (reactorAccessPortState.direction) recoveryToggle.enable();
        else recoveryToggle.disable();
        super.render(graphics, mouseX, mouseY, partialTicks);

        // Render text for input/output direction:
        if (reactorAccessPortState.direction) {
            // Text for an inlet:
            graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactorsrework.reactor_access_port.direction_toggle.input").getString(), this.getGuiLeft() + 42, this.getGuiTop() + 22, GuiTheme.textColor(), false);

        } else {
            // Text for an outlet:
            graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactorsrework.reactor_access_port.direction_toggle.output").getString(), this.getGuiLeft() + 42, this.getGuiTop() + 22, GuiTheme.textColor(), false);
        }

        if (reactorAccessPortState.direction) {
            GuiArt.scrollingText(graphics, getFont(),
                    Component.translatable("screen.biggerreactorsrework.reactor_access_port.recover_fuel"),
                    getGuiLeft() + 42, getGuiTop() + 38, imageWidth - 50, GuiTheme.textColor());
        }
    }

    @Override
    protected GuiFrame frame() {
        return GuiFrame.REACTOR;
    }
}