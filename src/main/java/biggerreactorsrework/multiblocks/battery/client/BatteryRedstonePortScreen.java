package biggerreactorsrework.multiblocks.battery.client;

import biggerreactorsrework.client.*;
import biggerreactorsrework.core.client.gui.screens.ModScreen;
import biggerreactorsrework.multiblocks.battery.containers.BatteryRedstonePortContainer;
import biggerreactorsrework.multiblocks.battery.tiles.BatteryRedstonePortTile;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

/** Added 2026-10-30: native battery frame, mode tabs and a contextual percentage field. */
public final class BatteryRedstonePortScreen extends ModScreen<BatteryRedstonePortContainer> {
    private static final int PANEL_X = 28, WIDTH = 220, HEIGHT = 210;
    private static final RedstoneIcon[] ICONS = {RedstoneIcon.DISABLED, RedstoneIcon.ABOVE, RedstoneIcon.BELOW,
            RedstoneIcon.image(GuiIcon.BATTERY), RedstoneIcon.image(GuiIcon.BATTERY)};
    private final ArrayList<Button> tabs = new ArrayList<>();
    private int mode;
    private boolean draftLoaded;
    private EditBox threshold;
    private Button apply, enter, revert;

    public BatteryRedstonePortScreen(BatteryRedstonePortContainer menu, Inventory inventory, Component title) {
        super(menu, inventory, title, PANEL_X + WIDTH, HEIGHT);
        inventoryLabelY = 1000;
    }
    private Component text(String key, Object... args) { return Component.translatable("screen.biggerreactorsrework.battery_redstone." + key, args); }
    private boolean percentage() { return mode == 1 || mode == 2; }
    private static double parse(String value) {
        try { return Double.parseDouble(value.replace(',', '.')); }
        catch (NumberFormatException e) { return Double.NaN; }
    }
    private double selectedThreshold() { return percentage() ? parse(threshold.getValue()) : menu.tile.threshold(); }
    private boolean valid() { return BatteryRedstonePortTile.valid(mode, selectedThreshold()); }

    private Button iconButton(int x, int y, RedstoneIcon icon, Component label, Button.OnPress action) {
        var button = addRenderableWidget(new Button(leftPos + x, topPos + y, 15, 15, label, action, supplier -> supplier.get()) {
            @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
                icon.button(g, getX(), getY(), getWidth(), getHeight(), isHoveredOrFocused(), !active, GuiFrame.BATTERY);
            }
        });
        button.setTooltip(Tooltip.create(label));
        return button;
    }
    @Override protected void init() {
        String draft = threshold == null ? Double.toString(menu.tile.threshold()) : threshold.getValue();
        super.init();
        tabs.clear();
        for (int i = 0; i < ICONS.length; i++) {
            final int index = i, y = 4 + i * 25;
            var button = addRenderableWidget(new Button(leftPos + GuiFrame.tabButtonX(0, true), topPos + y + GuiFrame.TAB_BUTTON_TOP,
                    GuiFrame.TAB_BUTTON, GuiFrame.TAB_BUTTON, text("mode." + i), b -> { mode = index; refresh(); }, supplier -> supplier.get()) {
                @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
                    GuiArt.tabButton(g, leftPos, topPos + y, true, isHoveredOrFocused(), mode == index, GuiFrame.BATTERY);
                    ICONS[index].tab(g, leftPos, topPos + y, true);
                }
            });
            button.setTooltip(Tooltip.create(text("mode." + i)));
            tabs.add(button);
        }
        threshold = addRenderableWidget(new EditBox(font, leftPos + PANEL_X + 10, topPos + 119, 170, 16, text("threshold")));
        threshold.setMaxLength(16);
        threshold.setValue(draft);
        threshold.setResponder(value -> refresh());
        enter = iconButton(PANEL_X + WIDTH - 26, 119, RedstoneIcon.ENTER, text("apply"), b -> apply());
        revert = iconButton(PANEL_X + WIDTH - 44, 188, RedstoneIcon.REVERT, text("revert"), b -> load());
        apply = iconButton(PANEL_X + WIDTH - 26, 188, RedstoneIcon.CONFIRM, text("apply"), b -> apply());
        if (!draftLoaded && menu.tile.settingsReady) load();
        refresh();
    }
    private void load() {
        mode = menu.tile.mode().ordinal();
        threshold.setValue(Double.toString(menu.tile.threshold()));
        draftLoaded = true;
        refresh();
    }
    @Override public void containerTick() {
        super.containerTick();
        if (!draftLoaded && menu.tile.settingsReady) load();
    }
    private void refresh() {
        if (threshold == null || apply == null) return;
        threshold.visible = enter.visible = percentage();
        threshold.setEditable(draftLoaded);
        tabs.forEach(tab -> tab.active = draftLoaded);
        revert.active = draftLoaded;
        apply.active = enter.active = draftLoaded && valid();
        threshold.setTextColor(valid() ? 0xFFE0E0E0 : 0xFFFF7777);
    }
    private void apply() {
        if (draftLoaded && valid()) menu.runRequest("configure", Map.of("mode", mode, "threshold", selectedThreshold()));
    }
    @Override protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        GuiArt.panel(g, leftPos + PANEL_X, topPos, WIDTH, HEIGHT, GuiFrame.BATTERY);
        GuiFrame.BATTERY.ear(g, leftPos, topPos + 4, 127, true);
    }
    @Override public void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        int x = PANEL_X + 10;
        GuiArt.scrollingText(g, font, title.getString(), x, 8, WIDTH - 20, GuiTheme.textColor());
        GuiArt.scrollingText(g, font, text("mode." + mode).getString(), x, 30, WIDTH - 20, GuiTheme.textColor());
        int y = 50;
        for (var line : font.split(text("help." + mode), WIDTH - 20)) {
            g.drawString(font, line, x, y, GuiTheme.textColor(), false); y += font.lineHeight;
        }
        if (percentage()) g.drawString(font, text("threshold"), x, 107, GuiTheme.textColor(), false);
        if (draftLoaded && !valid()) g.drawString(font, text("invalid"), x, 140, 0xFF9F2828, false);
        GuiArt.scrollingText(g, font, text("charge", String.format(Locale.ROOT, "%.2f", menu.tile.charge)).getString(), x, 157, WIDTH - 20, GuiTheme.textColor());
        GuiArt.scrollingText(g, font, text(!draftLoaded ? "loading" : !menu.tile.assembled ? "unassembled" : menu.tile.output ? "on" : "off").getString(),
                x, 170, WIDTH - 20, GuiTheme.textColor());
    }
    @Override protected GuiFrame frame() { return GuiFrame.BATTERY; }
}
