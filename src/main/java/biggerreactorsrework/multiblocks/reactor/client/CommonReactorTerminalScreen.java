package biggerreactorsrework.multiblocks.reactor.client;

import biggerreactorsrework.client.GuiTheme;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import biggerreactorsrework.Config;
import biggerreactorsrework.client.Biselector;
import biggerreactorsrework.client.GaugeBar;
import biggerreactorsrework.client.SelectorColors;
import biggerreactorsrework.client.GuiArt;
import biggerreactorsrework.client.GuiFrame;
import biggerreactorsrework.client.GuiIcon;
import biggerreactorsrework.multiblocks.reactor.containers.ReactorTerminalContainer;
import biggerreactorsrework.multiblocks.reactor.state.ReactorActivity;
import biggerreactorsrework.multiblocks.reactor.state.ReactorState;
import biggerreactorsrework.multiblocks.reactor.state.ReactorType;
import biggerreactorsrework.core.client.gui.screens.ModScreen;
import biggerreactorsrework.core.client.gui.RenderHelper;
import biggerreactorsrework.core.client.gui.elements.InteractiveElement;
import biggerreactorsrework.core.client.gui.elements.RenderedElement;
import biggerreactorsrework.core.client.gui.elements.TooltipElement;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public class CommonReactorTerminalScreen extends ModScreen<ReactorTerminalContainer> {

    // Modified 2026-10-25: server-authoritative burn limits and current bottleneck.
    private static Component burnTooltip(ModScreen<ReactorTerminalContainer> screen) {
        var tooltip=Component.empty();
        for(var row:biggerreactorsrework.core.client.gui.GuiSync.statistics(screen.getMenu())) {
            String key=String.valueOf(row.get("key"));
            if(!java.util.Set.of("burn_potential","manual_limit","burn_current","status").contains(key))continue;
            if(!tooltip.getString().isEmpty())tooltip.append("\n");
            tooltip.append(Component.translatable("screen.biggerreactorsrework.statistics."+key)).append(": ")
                .append(biggerreactorsrework.client.GuiStatisticsWindow.value(String.valueOf(row.get("value"))));
        }
        return tooltip.getString().isEmpty()?Component.translatable("screen.biggerreactorsrework.statistics.status.calculating"):tooltip;
    }

    /**
     * Width of the readouts in the left column, up to the end of its separators.
     */
    public static final int READOUT_WIDTH = 78;
    /**
     * The title ends before the icons over the gauges.
     */
    public static final int TITLE_WIDTH = 98;

    // This state is used once, and as such can be final. Most other states should NOT be final.
    private final ReactorState initialState;

    public CommonReactorTerminalScreen(ReactorTerminalContainer container, Inventory playerInventory, Component title) {
        // We override whatever Minecraft wants to set the title to. It wants "Reactor Terminal," but that's too long.
        super(container, playerInventory, Component.translatable("screen.biggerreactorsrework.reactor_terminal"), RenderHelper.getBlankTextureResource(), 200, 152);

        // Initialize reactor terminal state.
        initialState = (ReactorState) this.getMenu().getGuiPacket();
    }

    /**
     * Initialize whichever subscreen we need.
     */
    @Override
    public void init() {
        if (initialState.reactorType == ReactorType.ACTIVE) {
            // Initialize an actively-cooled reactor screen.
            this.getMinecraft().setScreen(new ActiveReactorTerminalScreen(this.menu, this.inventory, this.title));
        } else {
            // Initialize a passively-cooled reactor screen.
            this.getMinecraft().setScreen(new PassiveReactorTerminalScreen(this.menu, this.inventory, this.title));
        }
    }

    /**
     * Initialize common/shared tooltips.
     *
     * @param screen The screen to initialize.
     */
    public static void initTooltips(@Nonnull ModScreen<ReactorTerminalContainer> screen, ReactorState reactorState) {
        // (Left) Temperature tooltip:
        screen.addScreenElement(new TooltipElement<>(screen, 8, 19, 16, 16, Component.translatable("screen.biggerreactorsrework.reactor_terminal.temperature.tooltip")));

        // (Left) Temperature readout tooltip:
        TooltipElement<ReactorTerminalContainer> temperatureReadoutTooltip = new TooltipElement<>(screen, 26, 19, 77, 16, Component.empty());
        temperatureReadoutTooltip.onTick = () -> {
            temperatureReadoutTooltip.tooltip = Component.literal(String.format("%.3f K", reactorState.fuelHeatStored));
        };
        screen.addScreenElement(temperatureReadoutTooltip);

        // (Left) Fuel consumption rate tooltip:
        var fuelIconTooltip=new TooltipElement<>(screen,8,57,16,16,Component.empty());
        fuelIconTooltip.onTick=()->fuelIconTooltip.tooltip=burnTooltip(screen);
        screen.addScreenElement(fuelIconTooltip);

        // (Left) Fuel consumption readout tooltip:
        TooltipElement<ReactorTerminalContainer> fuelConsumptionReadoutTooltip = new TooltipElement<>(screen, 26, 57, 77, 16, Component.empty());
        fuelConsumptionReadoutTooltip.onTick = () -> {
            fuelConsumptionReadoutTooltip.tooltip = burnTooltip(screen);
        };
        screen.addScreenElement(fuelConsumptionReadoutTooltip);

        // (Left) Reactivity rate tooltip:
        screen.addScreenElement(new TooltipElement<>(screen, 8, 76, 16, 16, Component.translatable("screen.biggerreactorsrework.reactor_terminal.reactivity_rate.tooltip")));

        // (Left) Reactivity rate readout tooltip:
        TooltipElement<ReactorTerminalContainer> reactivityRateTooltip = new TooltipElement<>(screen, 26, 76, 77, 16, Component.empty());
        reactivityRateTooltip.onTick = () -> {
            reactivityRateTooltip.tooltip = Component.literal(String.format("%.1f%%", (reactorState.reactivityRate * 100.0)));
        };
        screen.addScreenElement(reactivityRateTooltip);

        // (Top) Fuel mix gauge tooltip:
        screen.addScreenElement(new TooltipElement<>(screen, 110, 6, 16, 16, Component.translatable("screen.biggerreactorsrework.reactor_terminal.fuel_mix.tooltip")));

        // (Top) Case heat gauge tooltip:
        screen.addScreenElement(new TooltipElement<>(screen, 132, 6, 16, 16, Component.translatable("screen.biggerreactorsrework.reactor_terminal.case_heat.tooltip")));

        // (Top) Fuel heat gauge tooltip:
        screen.addScreenElement(new TooltipElement<>(screen, 154, 6, 16, 16, Component.translatable("screen.biggerreactorsrework.reactor_terminal.fuel_heat.tooltip")));
    }

    /**
     * Initialize common/shared controls.
     *
     * @param screen The screen to initialize.
     */
    public static void initControls(@Nonnull ModScreen<ReactorTerminalContainer> screen, ReactorState reactorState) {
        // (Left) Activity toggle:
        Biselector<ReactorTerminalContainer> activityToggle = new Biselector<>(screen, 8, 98, Component.translatable("screen.biggerreactorsrework.reactor_terminal.activity_toggle.tooltip"),
                () -> reactorState.reactorActivity.toInt(), SelectorColors.GREEN);
        activityToggle.onMouseReleased = (mX, mY, btn) -> {
            // Click logic.
            screen.getMenu().executeRequest("setActive", activityToggle.getState() == 0 ? 1 : 0);
            return true;
        };
        screen.addScreenElement(activityToggle);

        // (Left) Auto-eject toggle:
        Biselector<ReactorTerminalContainer> autoEjectToggle = new Biselector<>(screen, 8, 114, Component.translatable("screen.biggerreactorsrework.reactor_terminal.auto_eject_toggle.tooltip"),
                () -> reactorState.doAutoEject ? 1 : 0, SelectorColors.CYAN);
        autoEjectToggle.onMouseReleased = (mX, mY, btn) -> {
            // Click logic.
            screen.getMenu().executeRequest("setAutoEject", autoEjectToggle.getState() == 0 ? 1 : 0);
            return true;
        };
        screen.addScreenElement(autoEjectToggle);

        // (Left) Manual eject button:
        InteractiveElement<ReactorTerminalContainer> manualEjectButton = new InteractiveElement<>(screen, 8, 130, 15, 15, 0, 0, Component.translatable("screen.biggerreactorsrework.reactor_terminal.manual_eject.tooltip"));
        manualEjectButton.onMouseReleased = (mX, mY, btn) -> {
            // Click logic. Extra check necessary since this is an "in-class" button.
            if (manualEjectButton.isMouseOver(mX, mY)) {
                // Mouse is hovering, do the thing.
                //screen.getContainer().executeRequest("ejectWaste", true);
                Minecraft.getInstance().player.sendSystemMessage(Component.literal("No effect. This button will be removed in the future."));
                Minecraft.getInstance().player.sendSystemMessage(Component.literal("Use the access ports to eject waste!"));
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
            GuiArt.button(mS, manualEjectButton.x, manualEjectButton.y, manualEjectButton.width, manualEjectButton.height, manualEjectButton.isMouseOver(mX, mY), GuiIcon.EJECT, screen.getFrame());
        });
        // TODO: Remove with reactor manual eject.
        // This element will be removed soon, in favor of having the eject button be in the access ports.
        //screen.addElement(manualEjectButton);
    }

    /**
     * Initialize common/shared gauges.
     *
     * @param screen The screen to initialize.
     */
    public static void initGauges(@Nonnull ModScreen<ReactorTerminalContainer> screen, ReactorState reactorState) {
        // (Top) Fuel mix gauge:
        RenderedElement<ReactorTerminalContainer> fuelMixGauge = new RenderedElement<>(screen, 109, 25, 18, 64, 0, 152, Component.empty());
        fuelMixGauge.onRender = (@Nonnull GuiGraphics graphics, int mX, int mY) -> CommonReactorTerminalScreen.renderFuelMixGauge(graphics, fuelMixGauge, reactorState.wasteStored, reactorState.fuelStored, reactorState.fuelCapacity);
        screen.addScreenElement(fuelMixGauge);

        // (Top) Case heat gauge:
        RenderedElement<ReactorTerminalContainer> caseHeatGauge = new RenderedElement<>(screen, 131, 25, 18, 64, 0, 152, Component.empty());
        caseHeatGauge.onRender = (@Nonnull GuiGraphics graphics, int mX, int mY) -> CommonReactorTerminalScreen.renderHeatGauge(graphics, caseHeatGauge, reactorState.caseHeatStored, Config.CONFIG.Reactor.GUI.HeatDisplayMax);
        screen.addScreenElement(caseHeatGauge);

        // (Top) Fuel heat gauge:
        RenderedElement<ReactorTerminalContainer> fuelHeatGauge = new RenderedElement<>(screen, 153, 25, 18, 64, 0, 152, Component.empty());
        fuelHeatGauge.onRender = (@Nonnull GuiGraphics graphics, int mX, int mY) -> CommonReactorTerminalScreen.renderHeatGauge(graphics, fuelHeatGauge, reactorState.fuelHeatStored, Config.CONFIG.Reactor.GUI.HeatDisplayMax);
        screen.addScreenElement(fuelHeatGauge);
    }

    /**
     * Initialize common/shared symbols.
     *
     * @param screen The screen to initialize.
     */
    public static void initSymbols(@Nonnull ModScreen<ReactorTerminalContainer> screen, ReactorState reactorState) {
        // None (yet).
    }

    /**
     * Render a reactor heat gauge.
     *
     * @param poseStack    The current pose stack.
     * @param symbol       The symbol to draw as.
     * @param heatStored   The heat value to draw.
     * @param heatCapacity The max heat capacity this gauge can display.
     */
    public static void renderHeatGauge(@Nonnull GuiGraphics graphics, @Nonnull RenderedElement<ReactorTerminalContainer> symbol, double heatStored, double heatCapacity) {
        GaugeBar.drawHeat(graphics, symbol.x, symbol.y, heatStored, heatCapacity);
        // Update tooltip.
        symbol.tooltip = Component.literal(String.format("%.1f/%.1f K", heatStored, heatCapacity));
    }

    /**
     * Render a reactor fuel mix gauge.
     *
     * @param poseStack    The current pose stack.
     * @param symbol       The symbol to draw as.
     * @param wasteStored  The waste value to draw.
     * @param fuelStored   The fuel value to draw.
     * @param fuelCapacity The max fuel capacity this gauge can display.
     */
    public static void renderFuelMixGauge(@Nonnull GuiGraphics graphics, @Nonnull RenderedElement<ReactorTerminalContainer> symbol, double wasteStored, double fuelStored, double fuelCapacity) {
        GaugeBar.drawFuel(graphics, symbol.x, symbol.y, wasteStored, fuelStored, fuelCapacity);
        // Update tooltip.
        symbol.tooltip = Component.literal(String.format(
                Component.translatable("screen.biggerreactorsrework.reactor_terminal.fuel_mix_gauge.tooltip").getString(),
                RenderHelper.formatValue((fuelStored + wasteStored) / 1000.0, "B", true),
                RenderHelper.formatValue(fuelCapacity / 1000.0, "B", true),
                RenderHelper.formatValue(fuelStored / 1000.0, "B", true),
                RenderHelper.formatValue(wasteStored / 1000.0, "B", true)));
    }

    /**
     * A readout of the left column, next to its icon; long values slide inside the column.
     */
    public static void renderReadout(@Nonnull GuiGraphics graphics, @Nonnull ModScreen<ReactorTerminalContainer> screen, String text, int y) {
        GuiArt.scrollingText(graphics, screen.getFont(), text, screen.getGuiLeft() + 27, screen.getGuiTop() + y, READOUT_WIDTH, GuiTheme.textColor());
    }

    /**
     * Render status text.
     *
     * @param poseStack       The current pose stack.
     * @param reactorActivity The reactor status to draw.
     * @param doAutoEject     The auto-eject status to draw.
     * @param heatStored      The heat value to draw.
     * @param fuelUsageRate   The fuel usage rate to draw.
     * @param reactivityRate  The reactivity rate to draw.
     * @param statusRight     Where the status lines next to the toggles must end, relative to the screen.
     * @implNote Output rate is not rendered by this function, since it changes depending on reactor type. Do that yourself.
     */
    public static void renderStatusText(@Nonnull GuiGraphics graphics, @Nonnull ModScreen<ReactorTerminalContainer> screen, ReactorActivity reactorActivity, boolean doAutoEject, double heatStored, double fuelUsageRate, double reactivityRate, int statusRight) {
        // Render text for reactor temperature (no fancy suffix for Celsius):
        renderReadout(graphics, screen, String.format("%.0f K", heatStored), 23);

        // Render text for fuel consumption rate:
        renderReadout(graphics, screen, RenderHelper.formatValue(fuelUsageRate, 3, "mB/t", true), 61);

        // Render text for reactivity rate (no fancy suffix for percentages):
        renderReadout(graphics, screen, String.format("%.1f%%", (reactivityRate * 100.0)), 80);

        // Render text for online/offline status:
        if (reactorActivity == ReactorActivity.ACTIVE) {
            // Text for an online reactor:
            GuiArt.scrollingText(graphics, screen.getFont(), Component.translatable("screen.biggerreactorsrework.reactor_terminal.activity_toggle.online").getString(), screen.getGuiLeft() + 42, screen.getGuiTop() + 102, statusRight - 42, GuiTheme.textColor());

        } else {
            // Text for an offline reactor:
            GuiArt.scrollingText(graphics, screen.getFont(), Component.translatable("screen.biggerreactorsrework.reactor_terminal.activity_toggle.offline").getString(), screen.getGuiLeft() + 42, screen.getGuiTop() + 102, statusRight - 42, GuiTheme.textColor());
        }

        // Render text for auto-eject status:
        if (doAutoEject) {
            // Text for enabled auto-ejection:
            GuiArt.scrollingText(graphics, screen.getFont(), Component.translatable("screen.biggerreactorsrework.reactor_terminal.auto_eject_toggle.enabled").getString(), screen.getGuiLeft() + 42, screen.getGuiTop() + 118, statusRight - 42, GuiTheme.textColor());
        } else {
            // Text for disabled auto-ejection:
            GuiArt.scrollingText(graphics, screen.getFont(), Component.translatable("screen.biggerreactorsrework.reactor_terminal.auto_eject_toggle.disabled").getString(), screen.getGuiLeft() + 42, screen.getGuiTop() + 118, statusRight - 42, GuiTheme.textColor());
        }

        // Render text for manual eject button:
        // TODO: Remove with reactor manual eject.
        //screen.getFont().drawString(mStack, Component.translatable("screen.biggerreactorsrework.reactor_terminal.manual_eject").getString(), screen.getGuiLeft() + 26, screen.getGuiTop() + 134, GuiTheme.textColor());
    }

    @Override
    protected GuiFrame frame() {
        return GuiFrame.REACTOR;
    }
}
