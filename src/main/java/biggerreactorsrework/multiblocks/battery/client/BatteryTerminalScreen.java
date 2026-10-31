package biggerreactorsrework.multiblocks.battery.client;

// Modified 2026-10-27: top plus charges items; bottom minus drains items into the battery.

import biggerreactorsrework.client.CommonRender;
import biggerreactorsrework.client.GuiArt;
import biggerreactorsrework.client.GuiFrame;
import biggerreactorsrework.client.GuiIcon;
import biggerreactorsrework.client.GuiTheme;
import biggerreactorsrework.core.client.gui.RenderHelper;
import biggerreactorsrework.core.client.gui.elements.RenderedElement;
import biggerreactorsrework.core.client.gui.elements.TooltipElement;
import biggerreactorsrework.core.client.gui.screens.ModScreen;
import biggerreactorsrework.multiblocks.battery.containers.BatteryTerminalContainer;
import biggerreactorsrework.multiblocks.battery.state.BatteryState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

import static biggerreactorsrework.multiblocks.battery.containers.BatteryTerminalContainer.*;

@OnlyIn(Dist.CLIENT)
public class BatteryTerminalScreen extends ModScreen<BatteryTerminalContainer> {

    private static final int HEIGHT = INVENTORY_Y + 58 + 18 + 6;
    private static final int READOUT_X = 50;
    private static final int READOUT_WIDTH = DISCHARGE_X - 7 - READOUT_X;
    private static final int SEPARATOR_Y = 88;
    private static final int SLOT_MARK = 0xFF737373;
    private static final int ARMOR_EAR_Y = ARMOR_Y - 5;
    private static final int ARMOR_EAR_HEIGHT = OFFHAND_Y + 18 + 3 - ARMOR_EAR_Y;

    private final BatteryState batteryState;

    public BatteryTerminalScreen(BatteryTerminalContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory, Component.translatable("screen.biggerreactorsrework.battery_terminal"), WIDTH, HEIGHT);
        this.batteryState = (BatteryState) this.getMenu().getGuiPacket();
    }

    @Override
    public void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.getTitle())) / 2;

        this.addScreenElement(new TooltipElement<>(this, 30, 20, 16, 16, Component.translatable("screen.biggerreactorsrework.battery_terminal.energy_stored.tooltip")));
        this.addScreenElement(new TooltipElement<>(this, 30, 42, 16, 16, Component.translatable("screen.biggerreactorsrework.battery_terminal.input_rate.tooltip")));
        this.addScreenElement(new TooltipElement<>(this, 30, 64, 16, 16, Component.translatable("screen.biggerreactorsrework.battery_terminal.output_rate.tooltip")));

        RenderedElement<BatteryTerminalContainer> energyGauge = new RenderedElement<>(this, 8, 20, 18, 64, 0, 0, Component.empty());
        energyGauge.onRender = (@Nonnull GuiGraphics graphics, int mX, int mY) -> CommonRender.renderEnergyGauge(graphics,
                energyGauge, batteryState.energyStored, batteryState.energyCapacity);
        this.addScreenElement(energyGauge);
    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        int x = this.getGuiLeft() + READOUT_X;
        int y = this.getGuiTop();
        int color = GuiTheme.textColor();

        GuiArt.scrollingText(graphics, this.getFont(), RenderHelper.formatValue(batteryState.energyStored, "FE") + " / "
                + RenderHelper.formatValue(batteryState.energyCapacity, "FE"), x, y + 24, READOUT_WIDTH, color);
        GuiArt.scrollingText(graphics, this.getFont(), Component.translatable("screen.biggerreactorsrework.battery_terminal.input_rate",
                RenderHelper.formatValue(batteryState.lastInput, "FE/t")).getString(), x, y + 46, READOUT_WIDTH, color);
        GuiArt.scrollingText(graphics, this.getFont(), Component.translatable("screen.biggerreactorsrework.battery_terminal.output_rate",
                RenderHelper.formatValue(batteryState.lastOutput, "FE/t")).getString(), x, y + 68, READOUT_WIDTH, color);
        GuiArt.scrollingText(graphics, this.getFont(), Component.translatable("screen.biggerreactorsrework.battery_terminal.summary",
                batteryState.cellCount, RenderHelper.formatValue(batteryState.transferLimit, "FE/t")).getString(), this.getGuiLeft() + 8, y + SEPARATOR_Y + 4, WIDTH - 16, color);

        if (this.hoveredSlot != null && this.hoveredSlot.index < 2 && !this.hoveredSlot.hasItem() && this.menu.getCarried().isEmpty()) {
            String key = this.hoveredSlot.index == 0 ? "discharge_slot" : "charge_slot";
            List<Component> lines = Arrays.stream(Component.translatable("screen.biggerreactorsrework.battery_terminal." + key + ".tooltip")
                    .getString().split("\n")).map(line -> (Component) Component.literal(line)).toList();
            graphics.renderComponentTooltip(this.getFont(), lines, mouseX, mouseY);
        }
    }

    @Override
    protected void drawBackground(@Nonnull GuiGraphics graphics, int x, int y) {
        GuiArt.panel(graphics, x, y, WIDTH, HEIGHT, this.frame());
        GuiArt.fill(graphics, x + 8, y + SEPARATOR_Y, x + WIDTH - 8, y + SEPARATOR_Y + 1, GuiArt.SEPARATOR);
        GuiIcon.BATTERY.draw(graphics, x + 33, y + 20);
        GuiIcon.ENERGY_INPUT.draw(graphics, x + 32, y + 43);
        GuiIcon.ENERGY_OUTPUT.draw(graphics, x + 32, y + 65);

        drawBatterySlot(graphics, x + DISCHARGE_X - 1, y + DISCHARGE_Y - 1, false);
        drawBatterySlot(graphics, x + CHARGE_X - 1, y + CHARGE_Y - 1, true);
        GuiArt.playerInventory(graphics, x + INVENTORY_X - 1, y + INVENTORY_Y - 1);

        this.frame().ear(graphics, x + WIDTH, y + ARMOR_EAR_Y, ARMOR_EAR_HEIGHT, false);
        for (int i = 0; i < 4; i++) {
            GuiArt.slot(graphics, x + ARMOR_X - 1, y + ARMOR_Y - 1 + i * 18, 18);
        }
        GuiArt.slot(graphics, x + ARMOR_X - 1, y + OFFHAND_Y - 1, 18);
    }

    private static void drawBatterySlot(GuiGraphics graphics, int slotX, int slotY, boolean plus) {
        GuiArt.slot(graphics, slotX, slotY, 18);
        GuiArt.fill(graphics, slotX + 5, slotY + 8, slotX + 13, slotY + 10, SLOT_MARK);
        if (plus) {
            GuiArt.fill(graphics, slotX + 8, slotY + 5, slotX + 10, slotY + 13, SLOT_MARK);
        }
    }

    @Override
    public List<net.minecraft.client.renderer.Rect2i> extraAreas() {
        var areas = super.extraAreas();
        areas.add(new net.minecraft.client.renderer.Rect2i(this.getGuiLeft() + WIDTH, this.getGuiTop() + ARMOR_EAR_Y, GuiFrame.TAB_SIZE, ARMOR_EAR_HEIGHT));
        return areas;
    }

    private boolean onArmorTab(double mouseX, double mouseY) {
        int earX = this.getGuiLeft() + WIDTH;
        int earY = this.getGuiTop() + ARMOR_EAR_Y;
        return mouseX >= earX && mouseX < earX + GuiFrame.TAB_SIZE && mouseY >= earY && mouseY < earY + ARMOR_EAR_HEIGHT;
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int button) {
        return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, button) && !this.onArmorTab(mouseX, mouseY);
    }

    @Override
    protected GuiFrame frame() {
        return GuiFrame.BATTERY;
    }

    @Override
    public boolean hasAppearanceSettings() {
        // Modified 2026-10-29: do not save a clicked-block theme before the shared identity arrives.
        return batteryState != null && batteryState.appearanceReady;
    }
}
