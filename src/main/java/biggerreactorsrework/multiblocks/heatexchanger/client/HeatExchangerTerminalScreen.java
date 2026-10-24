package biggerreactorsrework.multiblocks.heatexchanger.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.Config;
import biggerreactorsrework.client.CommonRender;
import biggerreactorsrework.client.GaugeBar;
import biggerreactorsrework.client.GuiArt;
import biggerreactorsrework.client.GuiIcon;
import biggerreactorsrework.multiblocks.heatexchanger.containers.HeatExchangerTerminalContainer;
import biggerreactorsrework.multiblocks.heatexchanger.state.HeatExchangerState;
import biggerreactorsrework.core.client.gui.screens.ModScreen;
import biggerreactorsrework.core.client.gui.RenderHelper;
import biggerreactorsrework.core.client.gui.elements.RenderedElement;
import biggerreactorsrework.core.client.gui.elements.TooltipElement;

import javax.annotation.Nonnull;
import java.util.Objects;

@OnlyIn(Dist.CLIENT)
public class HeatExchangerTerminalScreen extends ModScreen<HeatExchangerTerminalContainer> {


    private HeatExchangerState heatExchangerState;

    private Fluid condenserIntakeFluid = Fluids.EMPTY;
    private Fluid condenserExhaustFluid = Fluids.EMPTY;
    private Fluid evaporatorIntakeFluid = Fluids.EMPTY;
    private Fluid evaporatorExhaustFluid = Fluids.EMPTY;

    public HeatExchangerTerminalScreen(HeatExchangerTerminalContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory, Component.translatable("screen.biggerreactorsrework.heat_exchanger_terminal"), 144, 144);

        // Initialize heat exchanger state.
        this.heatExchangerState = (HeatExchangerState) this.getMenu().getGuiPacket();
        this.condenserIntakeFluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(this.heatExchangerState.condenserIntakeFluid));
        this.condenserExhaustFluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(this.heatExchangerState.condenserExhaustFluid));
        this.evaporatorIntakeFluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(this.heatExchangerState.evaporatorIntakeFluid));
        this.evaporatorExhaustFluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(this.heatExchangerState.evaporatorExhaustFluid));
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
        this.initTooltips();

        // Initialize gauges:
        this.initGauges();
    }

    /**
     * Initialize tooltips.
     **/
    private void initTooltips() {
        // (Bottom) Condenser temperature tooltip:
        this.addScreenElement(new TooltipElement<>(this, 9, 103, 16, 16, Component.translatable("screen.biggerreactorsrework.heat_exchanger_terminal.temperature.condenser.tooltip")));

        // (Bottom) Evaporator temperature tooltip:
        this.addScreenElement(new TooltipElement<>(this, 9, 122, 16, 16, Component.translatable("screen.biggerreactorsrework.heat_exchanger_terminal.temperature.evaporator.tooltip")));

        // (Bottom) Condenser flow rate tooltip:
        this.addScreenElement(new TooltipElement<>(this, 75, 103, 16, 16, Component.translatable("screen.biggerreactorsrework.heat_exchanger_terminal.flow_rate.condenser.tooltip")));

        // (Bottom) Evaporator flow rate tooltip:
        this.addScreenElement(new TooltipElement<>(this, 75, 122, 16, 16, Component.translatable("screen.biggerreactorsrework.heat_exchanger_terminal.flow_rate.evaporator.tooltip")));

        // (Bottom) Condenser intake gauge tooltip:
        this.addScreenElement(new TooltipElement<>(this, 9, 17, 16, 16, Component.translatable("screen.biggerreactorsrework.heat_exchanger_terminal.intake_gauge.condenser.tooltip")));

        // (Top) Evaporator intake gauge tooltip:
        this.addScreenElement(new TooltipElement<>(this, 31, 17, 16, 16, Component.translatable("screen.biggerreactorsrework.heat_exchanger_terminal.intake_gauge.evaporator.tooltip")));

        // (Top) Condenser temperature gauge tooltip:
        this.addScreenElement(new TooltipElement<>(this, 53, 17, 16, 16, Component.translatable("screen.biggerreactorsrework.heat_exchanger_terminal.temperature.condenser.tooltip")));

        // (Top) Evaporator temperature gauge tooltip:
        this.addScreenElement(new TooltipElement<>(this, 75, 17, 16, 16, Component.translatable("screen.biggerreactorsrework.heat_exchanger_terminal.temperature.evaporator.tooltip")));

        // (Top) Condenser exhaust gauge tooltip:
        this.addScreenElement(new TooltipElement<>(this, 97, 17, 16, 16, Component.translatable("screen.biggerreactorsrework.heat_exchanger_terminal.exhaust_gauge.condenser.tooltip")));

        // (Top) Evaporator exhaust gauge tooltip:
        this.addScreenElement(new TooltipElement<>(this, 119, 17, 16, 16, Component.translatable("screen.biggerreactorsrework.heat_exchanger_terminal.exhaust_gauge.evaporator.tooltip")));
    }

    /**
     * Initialize gauges.
     */
    private void initGauges() {
        // (Top) Condenser intake tank:
        RenderedElement<HeatExchangerTerminalContainer> condenserIntakeTank = new RenderedElement<>(this, 8, 36, 18, 64, 0, 144, Component.empty());
        condenserIntakeTank.onRender = (@Nonnull GuiGraphics graphics, int mX, int mY) -> CommonRender.renderFluidGauge(graphics,
                condenserIntakeTank, heatExchangerState.condenserIntakeFluidAmount, heatExchangerState.condenserTankSize, this.condenserIntakeFluid);
        this.addScreenElement(condenserIntakeTank);

        // (Top) Evaporator intake tank:
        RenderedElement<HeatExchangerTerminalContainer> evaporatorIntakeTank = new RenderedElement<>(this, 30, 36, 18, 64, 0, 144, Component.empty());
        evaporatorIntakeTank.onRender = (@Nonnull GuiGraphics graphics, int mX, int mY) -> CommonRender.renderFluidGauge(graphics,
                evaporatorIntakeTank, heatExchangerState.evaporatorIntakeFluidAmount, heatExchangerState.evaporatorTankSize, this.evaporatorIntakeFluid);
        this.addScreenElement(evaporatorIntakeTank);

        // (Top) Condenser heat gauge:
        RenderedElement<HeatExchangerTerminalContainer> condenserHeatGauge = new RenderedElement<>(this, 52, 36, 18, 64, 0, 144, Component.empty());
        condenserHeatGauge.onRender = (@Nonnull GuiGraphics graphics, int mX, int mY) -> HeatExchangerTerminalScreen.renderHeatGauge(graphics, condenserHeatGauge, heatExchangerState.condenserChannelTemperature, Config.CONFIG.HeatExchanger.gui.HeatDisplayMax);
        this.addScreenElement(condenserHeatGauge);

        // (Top) Evaporator heat gauge:
        RenderedElement<HeatExchangerTerminalContainer> evaporatorHeatGauge = new RenderedElement<>(this, 74, 36, 18, 64, 0, 144, Component.empty());
        evaporatorHeatGauge.onRender = (@Nonnull GuiGraphics graphics, int mX, int mY) -> HeatExchangerTerminalScreen.renderHeatGauge(graphics, evaporatorHeatGauge, heatExchangerState.evaporatorChannelTemperature, Config.CONFIG.HeatExchanger.gui.HeatDisplayMax);
        this.addScreenElement(evaporatorHeatGauge);

        // (Top) Condenser exhaust tank:
        RenderedElement<HeatExchangerTerminalContainer> condenserExhaustTank = new RenderedElement<>(this, 96, 36, 18, 64, 0, 144, Component.empty());
        condenserExhaustTank.onRender = (@Nonnull GuiGraphics graphics, int mX, int mY) -> CommonRender.renderFluidGauge(graphics,
                condenserExhaustTank, heatExchangerState.condenserExhaustFluidAmount, heatExchangerState.condenserTankSize, this.condenserExhaustFluid);
        this.addScreenElement(condenserExhaustTank);

        // (Top) Evaporator exhaust tank:
        RenderedElement<HeatExchangerTerminalContainer> evaporatorExhaustTank = new RenderedElement<>(this, 118, 36, 18, 64, 0, 144, Component.empty());
        evaporatorExhaustTank.onRender = (@Nonnull GuiGraphics graphics, int mX, int mY) -> CommonRender.renderFluidGauge(graphics,
                evaporatorExhaustTank, heatExchangerState.evaporatorExhaustFluidAmount, heatExchangerState.evaporatorTankSize, this.evaporatorExhaustFluid);
        this.addScreenElement(evaporatorExhaustTank);
    }

    /**
     * Tick/update this screen.
     */
    @Override
    public void containerTick() {
        // Check if condenser intake fluid changed.
        if (!heatExchangerState.condenserIntakeFluid.equals(Objects.requireNonNull(BuiltInRegistries.FLUID.getKey(condenserIntakeFluid)).toString())) {
            condenserIntakeFluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(heatExchangerState.condenserIntakeFluid));
        }

        // Check if evaporator intake fluid changed.
        if (!heatExchangerState.evaporatorIntakeFluid.equals(Objects.requireNonNull(BuiltInRegistries.FLUID.getKey(evaporatorIntakeFluid)).toString())) {
            evaporatorIntakeFluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(heatExchangerState.evaporatorIntakeFluid));
        }

        // Check if condenser exhaust fluid changed.
        if (!heatExchangerState.condenserExhaustFluid.equals(Objects.requireNonNull(BuiltInRegistries.FLUID.getKey(condenserExhaustFluid)).toString())) {
            condenserExhaustFluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(heatExchangerState.condenserExhaustFluid));
        }

        // Check if evaporator exhaust fluid changed.
        if (!heatExchangerState.evaporatorExhaustFluid.equals(Objects.requireNonNull(BuiltInRegistries.FLUID.getKey(evaporatorExhaustFluid)).toString())) {
            evaporatorExhaustFluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(heatExchangerState.evaporatorExhaustFluid));
        }
    }

    /**
     * Render a heat exchanger heat gauge.
     *
     * @param graphics    The current pose stack.
     * @param symbol       The symbol to draw as.
     * @param heatStored   The heat value to draw.
     * @param heatCapacity The max heat capacity this gauge can display.
     */
    public static void renderHeatGauge(@Nonnull GuiGraphics graphics, @Nonnull RenderedElement<HeatExchangerTerminalContainer> symbol, double heatStored, double heatCapacity) {
        GaugeBar.drawHeat(graphics, symbol.x, symbol.y, heatStored, heatCapacity);
        // Update tooltip.
        symbol.tooltip = Component.literal(String.format("%.1f/%.1f K", heatStored, heatCapacity));
    }

    /**
     * Draw the status text for this screen.
     *
     * @param graphics    The current pose stack.
     * @param mouseX       The x position of the mouse.
     * @param mouseY       The y position of the mouse.
     * @param partialTicks Partial ticks.
     */
    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);

        // Render text for condenser channel temperature:
        graphics.drawString(this.getFont(), String.format("%.0f K", this.heatExchangerState.condenserChannelTemperature), this.getGuiLeft() + 27, this.getGuiTop() + 107, 4210752, false);

        // Render text for evaporator channel temperature:
        graphics.drawString(this.getFont(), String.format("%.0f K", this.heatExchangerState.evaporatorChannelTemperature), this.getGuiLeft() + 27, this.getGuiTop() + 127, 4210752, false);

        // Render text for condenser channel flow rate:
        graphics.drawString(this.getFont(), RenderHelper.formatValue((this.heatExchangerState.condenserChannelFlowRate / 1000.0), 1, "B/t", true), this.getGuiLeft() + 93, this.getGuiTop() + 107, 4210752, false);

        // Render text for evaporator channel flow rate:
        graphics.drawString(this.getFont(), RenderHelper.formatValue((this.heatExchangerState.evaporatorChannelFlowRate / 1000.0), 1, "B/t", true), this.getGuiLeft() + 93, this.getGuiTop() + 127, 4210752, false);
    }

    /**
     * Draw the panel and its fixed details.
     */
    @Override
    protected void drawBackground(@Nonnull GuiGraphics graphics, int x, int y) {
        GuiArt.panel(graphics, x, y, 144, 144);
        GuiArt.fill(graphics, x + 9, y + 120, x + 71, y + 121, GuiArt.SEPARATOR);
        GuiArt.fill(graphics, x + 73, y + 120, x + 136, y + 121, GuiArt.SEPARATOR);
        GuiIcon.CONDENSER_INTAKE.draw(graphics, x + 9, y + 17);
        GuiIcon.EVAPORATOR_INTAKE.draw(graphics, x + 31, y + 17);
        GuiIcon.CONDENSER_TEMPERATURE.draw(graphics, x + 53, y + 17);
        GuiIcon.EVAPORATOR_TEMPERATURE.draw(graphics, x + 75, y + 17);
        GuiIcon.CONDENSER_EXHAUST.draw(graphics, x + 97, y + 17);
        GuiIcon.EVAPORATOR_EXHAUST.draw(graphics, x + 119, y + 17);
        GuiIcon.CONDENSER_TEMPERATURE.draw(graphics, x + 9, y + 103);
        GuiIcon.EVAPORATOR_TEMPERATURE.draw(graphics, x + 9, y + 122);
        GuiIcon.CONDENSER_FLOW_RATE.draw(graphics, x + 75, y + 103);
        GuiIcon.EVAPORATOR_FLOW_RATE.draw(graphics, x + 75, y + 122);
    }
}
