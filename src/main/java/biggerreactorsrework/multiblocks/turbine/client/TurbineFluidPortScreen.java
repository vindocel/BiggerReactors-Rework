package biggerreactorsrework.multiblocks.turbine.client;

import biggerreactorsrework.client.GuiTheme;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.client.Biselector;
import biggerreactorsrework.client.SelectorColors;
import biggerreactorsrework.multiblocks.turbine.containers.TurbineFluidPortContainer;
import biggerreactorsrework.multiblocks.turbine.state.TurbineFluidPortState;
import biggerreactorsrework.core.client.gui.screens.ModScreen;
import biggerreactorsrework.client.GuiFrame;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public class TurbineFluidPortScreen extends ModScreen<TurbineFluidPortContainer> {


    private TurbineFluidPortState turbineFluidPortState;

    public TurbineFluidPortScreen(TurbineFluidPortContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory, title, 142, 40);

        // Initialize access port state.
        turbineFluidPortState = (TurbineFluidPortState) this.getMenu().getGuiPacket();
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
        Biselector<TurbineFluidPortContainer> directionToggle = new Biselector<>(this, 8, 18, Component.translatable("screen.biggerreactorsrework.turbine_fluid_port.direction_toggle.tooltip"),
                () -> turbineFluidPortState.direction ? 0 : 1, SelectorColors.BLUE);
        directionToggle.onMouseReleased = (mX, mY, btn) -> {
            // Click logic.
            this.getMenu().executeRequest("setDirection", directionToggle.getState() == 0 ? 1 : 0);
            return true;
        };
        this.addScreenElement(directionToggle);
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
        super.render(graphics, mouseX, mouseY, partialTicks);

        // Render text for input/output direction:
        if (turbineFluidPortState.direction) {
            // Text for an inlet:
            graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactorsrework.turbine_fluid_port.direction_toggle.input").getString(), this.getGuiLeft() + 42, this.getGuiTop() + 22, GuiTheme.textColor(), false);

        } else {
            // Text for an outlet:
            graphics.drawString(this.getFont(), Component.translatable("screen.biggerreactorsrework.turbine_fluid_port.direction_toggle.output").getString(), this.getGuiLeft() + 42, this.getGuiTop() + 22, GuiTheme.textColor(), false);
        }
    }

    @Override
    protected GuiFrame frame() {
        return GuiFrame.TURBINE;
    }
}
