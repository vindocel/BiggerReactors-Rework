package biggerreactorsrework.multiblocks.reactor.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.client.CommonRender;
import biggerreactorsrework.client.GuiArt;
import biggerreactorsrework.client.GuiIcon;
import biggerreactorsrework.multiblocks.reactor.containers.ReactorTerminalContainer;
import biggerreactorsrework.multiblocks.reactor.state.ReactorState;
import biggerreactorsrework.multiblocks.reactor.state.ReactorType;
import biggerreactorsrework.core.client.gui.screens.ModScreen;
import biggerreactorsrework.core.client.gui.RenderHelper;
import biggerreactorsrework.core.client.gui.elements.RenderedElement;
import biggerreactorsrework.core.client.gui.elements.TooltipElement;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public class PassiveReactorTerminalScreen extends ModScreen<ReactorTerminalContainer> {


    private ReactorState reactorState;

    public PassiveReactorTerminalScreen(ReactorTerminalContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory, title, 176, 152);

        // Initialize reactor state.
        reactorState = (ReactorState) this.getMenu().getGuiPacket();
    }

    /**
     * Initialize the screen.
     */
    @Override
    public void init() {
        super.init();

        // Initialize tooltips:
        CommonReactorTerminalScreen.initTooltips(this, reactorState);
        this.initTooltips();

        // Initialize controls:
        CommonReactorTerminalScreen.initControls(this, reactorState);

        // Initialize gauges:
        CommonReactorTerminalScreen.initGauges(this, reactorState);
        this.initGauges();

        // Initialize symbols:
        CommonReactorTerminalScreen.initSymbols(this, reactorState);
    }

    /**
     * Initialize tooltips.
     */
    private void initTooltips() {
        // (Left) RF generation rate tooltip:
        this.addScreenElement(new TooltipElement<>(this, 8, 38, 16, 16, Component.translatable("screen.biggerreactorsrework.reactor_terminal.energy_generation_rate.tooltip")));

        // (Left) RF generation readout tooltip:
        TooltipElement<ReactorTerminalContainer> generationRateReadoutTooltip = new TooltipElement<>(this, 26, 38, 53, 16, Component.empty());
        generationRateReadoutTooltip.onTick = () -> {
            generationRateReadoutTooltip.tooltip = Component.literal(String.format("%.3f FE/t", this.reactorState.reactorOutputRate));
        };
        this.addScreenElement(generationRateReadoutTooltip);

        // (Top) Internal battery tooltip:
        this.addScreenElement(new TooltipElement<>(this, 152, 6, 16, 16, Component.translatable("screen.biggerreactorsrework.reactor_terminal.internal_battery.tooltip")));
    }

    /**
     * Initialize gauges.
     */
    private void initGauges() {
        // (Top) Internal battery:
        RenderedElement<ReactorTerminalContainer> internalBattery = new RenderedElement<>(this, 151, 25, 18, 64, 0, 152, Component.empty());
        internalBattery.onRender = (@Nonnull GuiGraphics graphics, int mX, int mY) -> CommonRender.renderEnergyGauge(graphics,
                internalBattery, reactorState.energyStored, reactorState.energyCapacity);
        this.addScreenElement(internalBattery);
    }

    /**
     * Tick/update this screen.
     */
    @Override
    public void containerTick() {
        // Update reactor state and tick.
        reactorState = (ReactorState) this.getMenu().getGuiPacket();
        super.containerTick();
        // Check if reactor type changed.
        if (reactorState.reactorType != ReactorType.PASSIVE) {
            this.getMinecraft().setScreen(new ActiveReactorTerminalScreen(this.menu, this.inventory, this.title));
        }
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

        // Render the other text:
        CommonReactorTerminalScreen.renderStatusText(graphics, this, reactorState.reactorActivity, reactorState.doAutoEject,
                reactorState.fuelHeatStored, reactorState.fuelUsageRate, reactorState.reactivityRate);

        // Render text for output rate:
        graphics.drawString(this.getFont(), RenderHelper.formatValue(reactorState.reactorOutputRate, "FE/t"), this.getGuiLeft() + 27, this.getGuiTop() + 42, 4210752, false);
    }

    /**
     * Draw the panel and its fixed details.
     */
    @Override
    protected void drawBackground(@Nonnull GuiGraphics graphics, int x, int y) {
        GuiArt.panel(graphics, x, y, 176, 152);
        GuiArt.fill(graphics, x + 6, y + 36, x + 81, y + 37, GuiArt.SEPARATOR);
        GuiArt.fill(graphics, x + 6, y + 55, x + 81, y + 56, GuiArt.SEPARATOR);
        GuiArt.fill(graphics, x + 6, y + 74, x + 81, y + 75, GuiArt.SEPARATOR);
        GuiArt.fill(graphics, x + 6, y + 93, x + 81, y + 94, GuiArt.SEPARATOR);
        GuiIcon.TEMPERATURE.draw(graphics, x + 12, y + 19);
        GuiIcon.ENERGY_GENERATION_RATE.draw(graphics, x + 9, y + 39);
        GuiIcon.FUEL_USAGE_RATE.draw(graphics, x + 8, y + 59);
        GuiIcon.REACTIVITY.draw(graphics, x + 8, y + 76);
        GuiIcon.FUEL_MIX.draw(graphics, x + 86, y + 6);
        GuiIcon.CASE_HEAT.draw(graphics, x + 108, y + 6);
        GuiIcon.FUEL_HEAT.draw(graphics, x + 130, y + 6);
        GuiIcon.BATTERY.draw(graphics, x + 155, y + 6);
    }
}
