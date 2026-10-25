package biggerreactorsrework.multiblocks.turbine.client;

import biggerreactorsrework.client.GuiArt;
import biggerreactorsrework.client.GuiFrame;
import biggerreactorsrework.client.GuiIcon;
import biggerreactorsrework.client.SelectorColors;
import biggerreactorsrework.multiblocks.turbine.containers.TurbineRedstonePortContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import biggerreactorsrework.core.client.gui.screens.ModScreen;
import biggerreactorsrework.client.GuiTheme;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.Map;

/** Reactor-style redstone tabs and contextual controls. */
public final class TurbineRedstonePortScreen extends ModScreen<TurbineRedstonePortContainer> {
    private static final int PANEL_X = 28;
    private static final int PANEL_WIDTH = 206;
    private static final int RIGHT_EAR = PANEL_X + PANEL_WIDTH;
    // Same top inset as the face configuration window.
    private static final int TAB_Y = 4;
    private static final GuiIcon[] ICONS = {GuiIcon.CLOSE, GuiIcon.REACTOR_ACTIVITY, GuiIcon.ROTOR_EFFICIENCY,
            GuiIcon.FLOW_RATE, GuiIcon.TACHOMETER, GuiIcon.BATTERY, GuiIcon.DROP_INPUT, GuiIcon.WATER_DROP};
    private int mode, behavior;
    private boolean draftLoaded;
    private EditBox low, high;
    private Button selector, apply, enterLow, enterHigh;

    public TurbineRedstonePortScreen(TurbineRedstonePortContainer menu, Inventory inventory, Component title) {
        super(menu, inventory, title, PANEL_WIDTH + 2 * PANEL_X, 214);
        imageWidth = PANEL_WIDTH + 2 * PANEL_X;
        imageHeight = 214;
        inventoryLabelY = 1000;
    }

    private Component text(String key) {
        return Component.translatable("screen.biggerreactorsrework.turbine_redstone." + key);
    }

    private boolean sensor() { return mode >= 4; }
    private boolean usesLow() { return mode == 3 || sensor(); }
    private boolean usesHigh() { return mode == 3 && behavior == 0 || sensor() && behavior == 4; }

    private Component help() {
        return text("help." + mode + (mode > 0 && mode < 4 ? "." + behavior : ""));
    }

    @FunctionalInterface
    private interface Painter {
        void draw(GuiGraphics graphics, Button button);
    }

    private Button button(int x, int y, int width, int height, Component label, Button.OnPress action, Painter painter) {
        return addRenderableWidget(new Button(leftPos + x, topPos + y, width, height, label, action, supplier -> supplier.get()) {
            @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
                painter.draw(g, this);
            }
        });
    }

    private Button iconButton(int x, int y, GuiIcon icon, Component label, Button.OnPress action) {
        var button = button(x, y, 15, 15, label, action, (g, b) ->
                GuiArt.button(g, b.getX(), b.getY(), b.getWidth(), b.getHeight(), b.isHoveredOrFocused(),
                        false, !b.active, icon, GuiFrame.TURBINE));
        button.setTooltip(Tooltip.create(label));
        return button;
    }

    @Override protected void init() {
        String lowDraft = low == null ? Double.toString(menu.tile.low) : low.getValue();
        String highDraft = high == null ? Double.toString(menu.tile.high) : high.getValue();
        super.init();
        if (!draftLoaded) {
            mode = menu.tile.mode;
            behavior = menu.tile.behavior;
            draftLoaded = true;
        }
        for (int i = 0; i < ICONS.length; i++) {
            final int tab = i;
            boolean left = i < 4;
            int ear = left ? 0 : RIGHT_EAR;
            int y = TAB_Y + (i % 4) * 25;
            var tabButton = button(GuiFrame.tabButtonX(ear, left), y + GuiFrame.TAB_BUTTON_TOP,
                    GuiFrame.TAB_BUTTON, GuiFrame.TAB_BUTTON, text("mode." + i), b -> {
                        mode = tab;
                        behavior = sensor() ? 2 : 0;
                        refresh();
                    }, (g, b) -> {
                        int tabY = topPos + y;
                        GuiArt.tabButton(g, leftPos + ear, tabY, left, b.isHoveredOrFocused(), mode == tab, GuiFrame.TURBINE);
                        GuiArt.tabIcon(g, leftPos + ear, tabY, left, ICONS[tab].width(), ICONS[tab].height(),
                                () -> ICONS[tab].draw(g, 0, 0));
                    });
            tabButton.setTooltip(Tooltip.create(Component.empty().append(text(left ? "input" : "output"))
                    .append(": ").append(text("mode." + i))));
        }
        selector = button(PANEL_X + 10, 80, GuiArt.cellsWidth(3), GuiArt.TOGGLE_HEIGHT,
                text("behavior." + behavior), b -> {
                    behavior = sensor() ? 2 + (behavior - 1) % 3 : (behavior + 1) % 2;
                    refresh();
                }, (g, b) -> {
                    if (sensor()) {
                        GuiArt.toggleCells(g, b.getX(), b.getY(), behavior - 2, b.isHoveredOrFocused(), false,
                                GuiFrame.TURBINE, SelectorColors.RED, SelectorColors.CYAN, SelectorColors.YELLOW);
                    } else {
                        GuiArt.toggleSwitch(g, b.getX(), b.getY(), behavior == 1, b.isHoveredOrFocused(), false,
                                SelectorColors.RED, GuiFrame.TURBINE);
                    }
                });
        low = addRenderableWidget(new EditBox(font, leftPos + PANEL_X + 10, topPos + 119, 160, 16, text("threshold")));
        high = addRenderableWidget(new EditBox(font, leftPos + PANEL_X + 10, topPos + 154, 160, 16, text("maximum")));
        low.setMaxLength(32);
        high.setMaxLength(32);
        low.setValue(lowDraft);
        high.setValue(highDraft);
        enterLow = iconButton(PANEL_X + 178, 119, GuiIcon.ARROW_ENTER, text("apply"), b -> apply());
        enterHigh = iconButton(PANEL_X + 178, 154, GuiIcon.ARROW_ENTER, text("apply"), b -> apply());
        iconButton(PANEL_X + PANEL_WIDTH - 44, 194, GuiIcon.REVERT, text("revert"), b -> {
            mode = menu.tile.mode;
            behavior = menu.tile.behavior;
            low.setValue(Double.toString(menu.tile.low));
            high.setValue(Double.toString(menu.tile.high));
            refresh();
        });
        apply = iconButton(PANEL_X + PANEL_WIDTH - 26, 194, GuiIcon.CONFIRM, text("apply"), b -> apply());
        low.setResponder(value -> refresh());
        high.setResponder(value -> refresh());
        refresh();
    }

    private void refresh() {
        if (low == null || high == null || apply == null) return;
        selector.visible = mode != 0;
        selector.setWidth(sensor() ? GuiArt.cellsWidth(3) : GuiArt.SWITCH_WIDTH);
        selector.setMessage(text("behavior." + behavior));
        selector.setTooltip(Tooltip.create(text("behavior.help." + behavior)));
        low.visible = enterLow.visible = usesLow();
        high.visible = enterHigh.visible = usesHigh();
        low.setMessage(text(mode == 3 ? (behavior == 0 ? "while_on" : "set_flow") : "threshold"));
        high.setMessage(text(mode == 3 ? "while_off" : "maximum"));
        low.setTooltip(Tooltip.create(low.getMessage().copy().append(" (" + unit() + ")")));
        high.setTooltip(Tooltip.create(high.getMessage().copy().append(" (" + unit() + ")")));
        boolean valid = valid();
        apply.active = enterLow.active = enterHigh.active = valid;
        low.setTextColor(validNumber(low.getValue()) ? 0xFFE0E0E0 : 0xFFFF7777);
        high.setTextColor(validNumber(high.getValue()) ? 0xFFE0E0E0 : 0xFFFF7777);
    }

    private static boolean validNumber(String value) {
        try {
            double number = Double.parseDouble(value);
            return Double.isFinite(number) && number >= 0;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private boolean valid() {
        if (usesLow() && !validNumber(low.getValue()) || usesHigh() && !validNumber(high.getValue())) return false;
        double l = usesLow() ? Double.parseDouble(low.getValue()) : 0;
        double h = usesHigh() ? Double.parseDouble(high.getValue()) : 0;
        return !(mode == 3 && (l > 100 || h > 100)) && !(sensor() && behavior == 4 && h < l);
    }

    private String unit() {
        return mode == 4 ? "RPM" : mode >= 6 ? "mB" : "%";
    }

    private void apply() {
        if (!valid()) return;
        menu.runRequest("configure", Map.of("mode", mode, "behavior", behavior,
                "low", usesLow() ? Double.parseDouble(low.getValue()) : 0.0,
                "high", usesHigh() ? Double.parseDouble(high.getValue()) : 0.0));
    }

    @Override protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        GuiArt.panel(g, leftPos + PANEL_X, topPos, PANEL_WIDTH, imageHeight, GuiFrame.TURBINE);
        GuiFrame.TURBINE.ear(g, leftPos, topPos + TAB_Y, 102, true);
        GuiFrame.TURBINE.ear(g, leftPos + RIGHT_EAR, topPos + TAB_Y, 102, false);
    }

    @Override public void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        int x = PANEL_X + 10;
        GuiArt.scrollingText(g, font, title.getString(), x, 8, PANEL_WIDTH - 20, GuiTheme.textColor());
        ICONS[mode].draw(g, x, 28);
        GuiArt.scrollingText(g, font, text("mode." + mode).getString(), x + 21, 32, PANEL_WIDTH - 42, GuiTheme.textColor());
        int lineY = 49;
        for (var line : font.split(help(), PANEL_WIDTH - 20)) {
            g.drawString(font, line, x, lineY, GuiTheme.textColor(), false);
            lineY += font.lineHeight;
        }
        if (mode != 0) GuiArt.scrollingText(g, font, text("behavior." + behavior).getString(),
                x + GuiArt.cellsWidth(3) + 5, 83, PANEL_WIDTH - 20 - GuiArt.cellsWidth(3) - 5, GuiTheme.textColor());
        if (usesLow()) {
            GuiArt.scrollingText(g, font, low.getMessage().getString() + " (" + unit() + ")", x, 107, PANEL_WIDTH - 20, GuiTheme.textColor());
        }
        if (usesHigh()) {
            GuiArt.scrollingText(g, font, high.getMessage().getString() + " (" + unit() + ")", x, 142, PANEL_WIDTH - 20, GuiTheme.textColor());
        }
        if (!valid()) g.drawString(font, text("invalid"), x, 177, 0xFF9F2828, false);
    }
    @Override protected GuiFrame frame() { return GuiFrame.TURBINE; }
}
