package biggerreactorsrework.multiblocks.reactor.client;

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
import biggerreactorsrework.client.CommonRender;
import biggerreactorsrework.client.GaugeBar;
import biggerreactorsrework.client.GuiArt;
import biggerreactorsrework.client.GuiIcon;
import biggerreactorsrework.multiblocks.reactor.containers.ReactorTerminalContainer;
import biggerreactorsrework.multiblocks.reactor.state.ReactorActivity;
import biggerreactorsrework.multiblocks.reactor.state.ReactorState;
import biggerreactorsrework.multiblocks.reactor.state.ReactorType;
import biggerreactorsrework.core.client.gui.screens.ModScreen;
import biggerreactorsrework.core.client.gui.RenderHelper;
import biggerreactorsrework.core.client.gui.elements.RenderedElement;
import biggerreactorsrework.core.client.gui.elements.TooltipElement;

import javax.annotation.Nonnull;
import java.util.Objects;

@OnlyIn(Dist.CLIENT)
public class ActiveReactorTerminalScreen extends ModScreen<ReactorTerminalContainer> {


    private ReactorState reactorState;

    private final int screenWorkTimeTotal = 400;
    private int screenWorkTime = 0;

    private Fluid coolantFluid = Fluids.EMPTY;
    private Fluid exhaustFluid = Fluids.EMPTY;

    public ActiveReactorTerminalScreen(ReactorTerminalContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory, title, 198, 152);

        // Initialize reactor state.
        reactorState = (ReactorState) this.getMenu().getGuiPacket();
        coolantFluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(reactorState.coolantResourceLocation));
        exhaustFluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(reactorState.exhaustResourceLocation));
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
        this.initSymbols();
    }

    /**
     * Initialize tooltips.
     */
    private void initTooltips() {
        // (Left) Exhaust generation readout tooltip:
        TooltipElement<ReactorTerminalContainer> generationRateReadoutTooltip = new TooltipElement<>(this, 26, 38, 53, 16, Component.empty());
        generationRateReadoutTooltip.onTick = () -> {
            generationRateReadoutTooltip.tooltip = Component.literal(String.format("%.3f mB/t", this.reactorState.reactorOutputRate));
        };
        this.addScreenElement(generationRateReadoutTooltip);
    }

    /**
     * Initialize gauges.
     */
    private void initGauges() {
        // (Top) Coolant intake tank:
        RenderedElement<ReactorTerminalContainer> coolantIntakeTank = new RenderedElement<>(this, 151, 25, 18, 64, 0, 152, Component.empty());
        coolantIntakeTank.onRender = (@Nonnull GuiGraphics graphics, int mX, int mY) -> CommonRender.renderFluidGauge(graphics,
                coolantIntakeTank, reactorState.coolantStored, reactorState.coolantCapacity, coolantFluid);
        this.addScreenElement(coolantIntakeTank);

        // (Top) Hot exhaust tank:
        RenderedElement<ReactorTerminalContainer> hotExhaustTank = new RenderedElement<>(this, 173, 25, 18, 64, 0, 152, Component.empty());
        hotExhaustTank.onRender = (@Nonnull GuiGraphics graphics, int mX, int mY) -> CommonRender.renderFluidGauge(graphics,
                hotExhaustTank, reactorState.exhaustStored, reactorState.exhaustCapacity, exhaustFluid);
        this.addScreenElement(hotExhaustTank);

        // (Bottom) Progress bar:
        RenderedElement<ReactorTerminalContainer> progressBar = new RenderedElement<>(this, 173, 90, 18, 26, 90, 152, null);
        progressBar.onRender = (@Nonnull GuiGraphics graphics, int mX, int mY) -> {
            // Custom rendering. Without coolant, the bar and its tank are drawn empty.
            if (reactorState.coolantStored > 0) {
                ActiveReactorTerminalScreen.renderProgressBar(graphics, progressBar, reactorState.reactorActivity, screenWorkTime++, screenWorkTimeTotal, coolantFluid);
                if (screenWorkTime >= screenWorkTimeTotal) {
                    screenWorkTime = 0;
                }
            } else {
                GaugeBar.drawCoolantFlow(graphics, progressBar.x, progressBar.y, 0, Fluids.EMPTY);
            }
        };
        this.addScreenElement(progressBar);
    }

    /**
     * Initialize symbols.
     */
    private void initSymbols() {
        // (Top) Coolant intake tank symbol:
        RenderedElement<ReactorTerminalContainer> coolantIntakeTankSymbol = new RenderedElement<>(this, 152, 6, 16, 16, 0, 0, Component.translatable("screen.biggerreactorsrework.reactor_terminal.coolant_intake_tank.tooltip"));
        coolantIntakeTankSymbol.onRender = (@Nonnull GuiGraphics graphics, int mX, int mY) -> GuiArt.fluidSymbol(graphics,
                coolantIntakeTankSymbol.x, coolantIntakeTankSymbol.y, coolantFluid, GuiIcon.DROP_INPUT_MASK, GuiIcon.DROP_INPUT, GuiArt.PANEL_FILL, true);
        this.addScreenElement(coolantIntakeTankSymbol);

        // (Top) Hot exhaust tank symbol:
        RenderedElement<ReactorTerminalContainer> hotExhaustTankSymbol = new RenderedElement<>(this, 174, 6, 16, 16, 0, 0, Component.translatable("screen.biggerreactorsrework.reactor_terminal.exhaust_tank.tooltip"));
        hotExhaustTankSymbol.onRender = (@Nonnull GuiGraphics graphics, int mX, int mY) -> GuiArt.fluidSymbol(graphics,
                hotExhaustTankSymbol.x, hotExhaustTankSymbol.y, exhaustFluid, GuiIcon.DROP_OUTPUT_MASK, GuiIcon.DROP_OUTPUT, GuiArt.PANEL_FILL, true);
        this.addScreenElement(hotExhaustTankSymbol);

        // (Left) Exhaust generation rate symbol:
        RenderedElement<ReactorTerminalContainer> exhaustGenerationRateSymbol = new RenderedElement<>(this, 8, 38, 16, 16, 0, 0, Component.translatable("screen.biggerreactorsrework.reactor_terminal.exhaust_generation_rate.tooltip"));
        exhaustGenerationRateSymbol.onRender = (@Nonnull GuiGraphics graphics, int mX, int mY) -> GuiArt.fluidSymbol(graphics,
                exhaustGenerationRateSymbol.x, exhaustGenerationRateSymbol.y, exhaustFluid, GuiIcon.DROP_RATE_MASK, GuiIcon.DROP_RATE, GuiArt.PANEL_FILL, true);
        this.addScreenElement(exhaustGenerationRateSymbol);
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
        if (reactorState.reactorType != ReactorType.ACTIVE) {
            this.getMinecraft().setScreen(new PassiveReactorTerminalScreen(this.menu, this.inventory, this.title));
        }
        // Check if coolant type changed.
        if (!reactorState.coolantResourceLocation.equals(Objects.requireNonNull(BuiltInRegistries.FLUID.getKey(coolantFluid)).toString())) {
            coolantFluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(reactorState.coolantResourceLocation));
        }
        // Check if exhaust type changed.
        if (!reactorState.exhaustResourceLocation.equals(Objects.requireNonNull(BuiltInRegistries.FLUID.getKey(exhaustFluid)).toString())) {
            exhaustFluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(reactorState.exhaustResourceLocation));
        }
    }

    /**
     * Draw the status text for this screen.
     *
//     * @param poseStack    The current pose stack.
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
        graphics.drawString(this.getFont(), RenderHelper.formatValue((reactorState.reactorOutputRate / 1000.0), "B/t"), this.getGuiLeft() + 27, this.getGuiTop() + 42, 4210752, false);
    }

    /**
     * Render the progress bar.
     *
//     * @param poseStack       The current pose stack.
     * @param symbol          The symbol to draw as.
     * @param reactorActivity Current status of the reactor.
     * @param workTime        The time the machine has been working.
     * @param workTimeTotal   The total time needed for completion.
     */
    private static void renderProgressBar(@Nonnull GuiGraphics graphics, @Nonnull RenderedElement<ReactorTerminalContainer> symbol, ReactorActivity reactorActivity, int workTime, int workTimeTotal, Fluid coolant) {
        // Check that the reactor is active. If not, reset work time.
        if (reactorActivity != ReactorActivity.ACTIVE) {
            workTime = 0;
        }
        // Progress rises from the bottom, above the coolant "undertank."
        final double progress = (workTimeTotal > 0) ? (double) workTime / workTimeTotal : 0;
        GaugeBar.drawCoolantFlow(graphics, symbol.x, symbol.y, progress, coolant);
    }

    /**
     * Draw the panel and its fixed details.
     */
    @Override
    protected void drawBackground(@Nonnull GuiGraphics graphics, int x, int y) {
        GuiArt.panel(graphics, x, y, 198, 152);
        GuiArt.fill(graphics, x + 6, y + 36, x + 81, y + 37, GuiArt.SEPARATOR);
        GuiArt.fill(graphics, x + 6, y + 55, x + 81, y + 56, GuiArt.SEPARATOR);
        GuiArt.fill(graphics, x + 6, y + 74, x + 81, y + 75, GuiArt.SEPARATOR);
        GuiArt.fill(graphics, x + 6, y + 93, x + 81, y + 94, GuiArt.SEPARATOR);
        GuiIcon.TEMPERATURE.draw(graphics, x + 12, y + 19);
        GuiIcon.FUEL_USAGE_RATE.draw(graphics, x + 8, y + 59);
        GuiIcon.REACTIVITY.draw(graphics, x + 8, y + 76);
        GuiIcon.FUEL_MIX.draw(graphics, x + 86, y + 6);
        GuiIcon.CASE_HEAT.draw(graphics, x + 108, y + 6);
        GuiIcon.FUEL_HEAT.draw(graphics, x + 130, y + 6);
        GuiArt.fill(graphics, x + 164, y + 89, x + 165, y + 119, GuiArt.SLOT_SHADOW);
        GuiArt.fill(graphics, x + 165, y + 118, x + 166, y + 119, GuiArt.SLOT_SHADOW);
        GuiArt.fill(graphics, x + 165, y + 119, x + 174, y + 120, GuiArt.SLOT_SHADOW);
        GuiArt.fill(graphics, x + 165, y + 89, x + 166, y + 118, GuiArt.WHITE);
        GuiArt.fill(graphics, x + 166, y + 117, x + 167, y + 118, GuiArt.WHITE);
        GuiArt.fill(graphics, x + 166, y + 118, x + 174, y + 119, GuiArt.WHITE);
        GuiArt.fill(graphics, x + 175, y + 117, x + 189, y + 118, GuiArt.SLOT_FILL);
        GuiArt.fill(graphics, x + 174, y + 118, x + 190, y + 120, GuiArt.SLOT_FILL);
        GuiArt.fill(graphics, x + 175, y + 120, x + 189, y + 121, GuiArt.SLOT_FILL);
        GuiArt.fill(graphics, x + 190, y + 118, x + 191, y + 120, GuiArt.WHITE);
    }
}
