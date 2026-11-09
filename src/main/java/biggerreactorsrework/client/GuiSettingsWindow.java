package biggerreactorsrework.client;

import biggerreactorsrework.core.client.gui.screens.ModScreen;
import biggerreactorsrework.multiblocks.heatexchanger.containers.HeatExchangerTerminalContainer;
import biggerreactorsrework.multiblocks.reactor.containers.ReactorTerminalContainer;
import biggerreactorsrework.multiblocks.turbine.containers.TurbineTerminalContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/** Shared, draggable appearance popup. Input is intercepted before machine controls and inventory slots. */
public final class GuiSettingsWindow {
    // Modified 2026-10-28: independent machine volume in the existing settings popup.
    private static final int WIDTH = 190;
    private static final int HEIGHT = 106;
    private static final int CLOSE_WIDTH = 10;
    private static final int CLOSE_HEIGHT = 11;
    private static final int APPLY_Y = 78;
    private static final int VOLUME_Y = 122;
    private static final int VOLUME_BUTTON = 12;
    private static final int VOLUME_GAP = 4;
    private static final int VOLUME_STEPS = 10;
    private static final int SEGMENT_SIZE = 8;
    private static final int SEGMENT_PITCH = SEGMENT_SIZE + 1;
    private static final int BAR_WIDTH = VOLUME_STEPS * SEGMENT_PITCH - 1;
    private final ModScreen<?> screen;
    private boolean open;
    private boolean dragging;
    private boolean volumeDragging;
    private boolean consumeRelease;
    private boolean rendering;
    private int focus;
    private int offsetX, offsetY;
    private double dragX, dragY;

    public GuiSettingsWindow(ModScreen<?> screen) { this.screen = screen; }
    private boolean hasSound() {
        var menu = screen.getMenu();
        return menu instanceof ReactorTerminalContainer
                || menu instanceof TurbineTerminalContainer
                || menu instanceof HeatExchangerTerminalContainer;
    }
    private int height() { return hasSound() ? 152 : HEIGHT; }
    private int choices() { return hasSound() ? 6 : 4; }

    public boolean open() { return screen.hasAppearanceSettings() && open; }
    public boolean rendering() { return rendering; }

    private int x() { return Mth.clamp((screen.width - WIDTH) / 2 + offsetX, 4, Math.max(4, screen.width - WIDTH - 4)); }
    private int y() { return Mth.clamp((screen.height - height()) / 2 + offsetY, 4, Math.max(4, screen.height - height() - 6)); }

    /** The area the open window covers, kept free of other overlays. */
    public net.minecraft.client.renderer.Rect2i area() { return new net.minecraft.client.renderer.Rect2i(x() - 4, y() - 4, WIDTH + 8, height() + 10); }

    private static boolean inside(double mx, double my, int x, int y, int width, int height) {
        return mx >= x && mx < x + width && my >= y && my < y + height;
    }

    private boolean onTab(double mx, double my) {
        return screen.hasAppearanceSettings() && !screen.isSettingsTabCovered(mx, my)
                && inside(mx, my, screen.settingsTabX(), screen.settingsTabY(), GuiFrame.TAB_SIZE,
                screen.hasStatistics() ? GuiStatisticsWindow.TAB_PITCH + GuiFrame.TAB_BUTTON_TOP - 1 : GuiFrame.TAB_SIZE);
    }

    public boolean contains(double mx, double my) {
        return onTab(mx, my) || covers(mx, my);
    }

    /** Whether the open window is under the point. */
    public boolean covers(double mx, double my) {
        return open() && inside(mx, my, x() - 4, y() - 4, WIDTH + 8, height() + 10);
    }

    private static Component text(String key) {
        return Component.translatable("screen.biggerreactorsrework.settings." + key);
    }

    private int closeX() { return x() + 6; }
    private int closeY() { return y() + 6; }
    private int applyX() { return x() + WIDTH - 6 - GuiArt.SWITCH_WIDTH; }
    private int barX() { return x() + (WIDTH - BAR_WIDTH) / 2; }
    private int minusX() { return barX() - VOLUME_GAP - VOLUME_BUTTON; }
    private int plusX() { return barX() + BAR_WIDTH + VOLUME_GAP; }

    /** The tab belongs to the machine background, below item counts and auxiliary windows. */
    public void renderTab(GuiGraphics g, int mouseX, int mouseY) {
        if (!screen.hasAppearanceSettings()) return;
        int tabX = screen.settingsTabX(), tabY = screen.settingsTabY();
        screen.getFrame().ear(g, tabX, tabY, screen.hasStatistics() ? GuiStatisticsWindow.TAB_PITCH + GuiFrame.TAB_SIZE : GuiFrame.TAB_SIZE, true);
        GuiArt.tabButton(g, tabX, tabY, true, onTab(mouseX, mouseY), open, screen.getFrame());
        GuiIcon.SETTINGS.draw(g,
                GuiFrame.tabButtonX(tabX, true) + (GuiFrame.TAB_BUTTON - GuiIcon.SETTINGS.width()) / 2,
                tabY + GuiFrame.TAB_BUTTON_TOP + (GuiFrame.TAB_BUTTON - GuiIcon.SETTINGS.height()) / 2 + 1);
    }

    public void render(GuiGraphics g, int mouseX, int mouseY) {
        if (!screen.hasAppearanceSettings()) return;
        rendering = true;
        g.pose().pushPose();
        g.flush();
        g.pose().translate(0, 0, 800);
        try {
            if (!open) {
                if (onTab(mouseX, mouseY)) g.renderTooltip(screen.getFont(), text("title"), mouseX, mouseY);
                return;
            }
            int x = x(), y = y();
            GuiArt.panel(g, x, y, WIDTH, height(), screen.getFrame());
            GuiArt.scrollingText(g, screen.getFont(), text("title").getString(), x + 22, y + 8, WIDTH - 30, GuiTheme.textColor());
            GuiArt.button(g, closeX(), closeY(), CLOSE_WIDTH, CLOSE_HEIGHT,
                    focus == 3 || inside(mouseX, mouseY, closeX(), closeY(), CLOSE_WIDTH, CLOSE_HEIGHT), GuiIcon.CLOSE, screen.getFrame());
            g.drawString(screen.getFont(), text("background"), x + 12, y + 28, GuiTheme.textColor(), false);
            boolean dark = GuiThemes.dark(screen.machinePos());
            choice(g, x + 12, y + 42, GuiTheme.LIGHT_PANEL, !dark, "light", 0, mouseX, mouseY);
            choice(g, x + 100, y + 42, GuiTheme.DARK_PANEL, dark, "dark", 1, mouseX, mouseY);
            boolean all = GuiThemes.applyToAll();
            int applyX = applyX(), applyY = y + APPLY_Y;
            boolean hovered = inside(mouseX, mouseY, applyX, applyY, GuiArt.SWITCH_WIDTH, GuiArt.TOGGLE_HEIGHT);
            GuiArt.scrollingText(g, screen.getFont(), text("all"), x + 6, applyY + 3,
                    applyX - x - 8, GuiTheme.textColor());
            GuiArt.toggleSwitch(g, applyX, applyY, all, hovered || focus == 2, false, SelectorColors.CYAN, screen.getFrame());
            if (hovered) {
                g.renderTooltip(screen.getFont(), text(all ? "all.on" : "all.off"), mouseX, mouseY);
            }
            if (hasSound()) volume(g, x, y, mouseX, mouseY);
        } finally {
            g.flush();
            g.pose().popPose();
            rendering = false;
        }
    }

    private void choice(GuiGraphics g, int x, int y, int color, boolean selected, String label, int index, int mx, int my) {
        GuiArt.buttonBackground(g, x, y, 78, 22, inside(mx, my, x, y, 78, 22), selected, false, screen.getFrame());
        g.fill(x + 4, y + 4, x + 18, y + 18, color);
        if (selected) GuiIcon.CONFIRM.draw(g, x + 8, y + 7);
        GuiArt.scrollingText(g, screen.getFont(), text(label).getString(), x + 23, y + 7, 50, 0xFFFFFFFF);
        if (focus == index) GuiArt.ring(g, x + 3, y + 3, 16, 0xFFACDC42);
    }

    private void volume(GuiGraphics g, int x, int y, int mx, int my) {
        int value = GuiThemes.volume(screen.machinePos());
        Component level = value == 0 ? text("volume.mute") : Component.literal(value + "%");
        g.drawString(screen.getFont(), Component.translatable("screen.biggerreactorsrework.settings.volume", level), x + 12, y + 105, GuiTheme.textColor(), false);
        int rowY = y + VOLUME_Y;
        volumeButton(g, minusX(), rowY, mx, my, "−", 4);
        volumeButton(g, plusX(), rowY, mx, my, "+", 5);
        int segmentY = rowY + (VOLUME_BUTTON - SEGMENT_SIZE) / 2;
        for (int i = 0; i < VOLUME_STEPS; i++) {
            int sx = barX() + i * SEGMENT_PITCH;
            g.fill(sx, segmentY, sx + SEGMENT_SIZE, segmentY + SEGMENT_SIZE,
                    value >= (i + 1) * 10 ? 0xFF36CFEE : 0xFF44545B);
        }
        if (inside(mx, my, x + 12, y + 103, WIDTH - 24, VOLUME_Y + VOLUME_BUTTON - 103)) {
            String help = inside(mx, my, minusX(), rowY, VOLUME_BUTTON, VOLUME_BUTTON) ? "volume.decrease"
                    : inside(mx, my, plusX(), rowY, VOLUME_BUTTON, VOLUME_BUTTON) ? "volume.increase" : "volume.help";
            g.renderTooltip(screen.getFont(), text(help), mx, my);
        }
    }

    private void volumeButton(GuiGraphics g, int x, int y, int mx, int my, String label, int index) {
        GuiArt.buttonBackground(g, x, y, VOLUME_BUTTON, VOLUME_BUTTON,
                inside(mx, my, x, y, VOLUME_BUTTON, VOLUME_BUTTON), false, false, screen.getFrame());
        g.drawString(screen.getFont(), label, x + (VOLUME_BUTTON - screen.getFont().width(label)) / 2, y + 2, 0xFFFFFFFF, false);
        if (focus == index) GuiArt.ring(g, x, y, VOLUME_BUTTON, 0xFFACDC42);
    }

    private void setBarVolume(double mx) {
        int segment = Mth.clamp((int)((mx - barX()) / SEGMENT_PITCH) + 1, 1, VOLUME_STEPS);
        GuiThemes.setVolume(screen.machinePos(), segment * 10);
    }

    public boolean mousePressed(double mx, double my, int button) {
        if (!contains(mx, my)) return false;
        consumeRelease = true;
        if (button != 0) return true;
        if (onTab(mx, my)) {
            open = !open;
            dragging = false;
            volumeDragging = false;
            if (open) focus = GuiThemes.dark(screen.machinePos()) ? 1 : 0;
            screen.setFocused(null);
            clickSound();
        } else if (inside(mx, my, closeX(), closeY(), CLOSE_WIDTH, CLOSE_HEIGHT)) {
            activate(3);
        } else if (inside(mx, my, x() + 12, y() + 42, 78, 22)) {
            activate(0);
        } else if (inside(mx, my, x() + 100, y() + 42, 78, 22)) {
            activate(1);
        } else if (inside(mx, my, applyX(), y() + APPLY_Y, GuiArt.SWITCH_WIDTH, GuiArt.TOGGLE_HEIGHT)) {
            activate(2);
        } else if (hasSound() && inside(mx, my, minusX(), y() + VOLUME_Y, VOLUME_BUTTON, VOLUME_BUTTON)) {
            activate(4);
        } else if (hasSound() && inside(mx, my, plusX(), y() + VOLUME_Y, VOLUME_BUTTON, VOLUME_BUTTON)) {
            activate(5);
        } else if (hasSound() && inside(mx, my, barX(), y() + VOLUME_Y, BAR_WIDTH, VOLUME_BUTTON)) {
            volumeDragging = true;
            setBarVolume(mx);
        } else if (inside(mx, my, x(), y(), WIDTH, 22)) {
            dragging = true;
            dragX = mx - x();
            dragY = my - y();
        }
        return true;
    }

    private void activate(int index) {
        focus = index;
        switch (index) {
            case 0 -> GuiThemes.choose(screen.machinePos(), false);
            case 1 -> GuiThemes.choose(screen.machinePos(), true);
            case 2 -> GuiThemes.setApplyToAll(screen.machinePos(), !GuiThemes.applyToAll());
            case 3 -> { open = false; dragging = false; volumeDragging = false; }
            case 4 -> GuiThemes.setVolume(screen.machinePos(), GuiThemes.volume(screen.machinePos()) - 10);
            case 5 -> GuiThemes.setVolume(screen.machinePos(), GuiThemes.volume(screen.machinePos()) + 10);
            default -> throw new IllegalArgumentException("Unknown settings choice");
        }
        clickSound();
    }

    public boolean mouseReleased() {
        dragging = false;
        volumeDragging = false;
        boolean consumed = consumeRelease;
        consumeRelease = false;
        return consumed;
    }

    public boolean mouseDragged(double mx, double my) {
        if (open && volumeDragging) { setBarVolume(mx); return true; }
        if (!open || !dragging) return false;
        if (dragging) {
            offsetX = (int) Math.round(mx - dragX) - (screen.width - WIDTH) / 2;
            offsetY = (int) Math.round(my - dragY) - (screen.height - height()) / 2;
            offsetX = x() - (screen.width - WIDTH) / 2;
            offsetY = y() - (screen.height - height()) / 2;
        }
        return true;
    }

    public boolean keyPressed(int keyCode, int modifiers) {
        if (!open) return false;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { open = false; dragging = false; volumeDragging = false; }
        else if (keyCode == GLFW.GLFW_KEY_TAB) focus = Math.floorMod(focus + ((modifiers & GLFW.GLFW_MOD_SHIFT) == 0 ? 1 : -1), choices());
        else if (hasSound() && focus >= 4 && (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT)) {
            GuiThemes.setVolume(screen.machinePos(), GuiThemes.volume(screen.machinePos()) + (keyCode == GLFW.GLFW_KEY_LEFT ? -10 : 10));
        }
        else if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_UP) focus = Math.floorMod(focus - 1, choices());
        else if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_DOWN) focus = (focus + 1) % choices();
        else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) activate(focus);
        return true;
    }

    private static void clickSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}
