package biggerreactorsrework.client;

import biggerreactorsrework.core.client.gui.screens.ModScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/** Shared, draggable appearance popup. Input is intercepted before machine controls and inventory slots. */
public final class GuiSettingsWindow {
    // Modified 2026-10-28: independent machine volume in the existing settings popup.
    private static final int WIDTH = 190;
    private static final int HEIGHT = 106;
    private static final int CLOSE_WIDTH = 10;
    private static final int CLOSE_HEIGHT = 11;
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
    private boolean hasSound() { return screen.hasStatistics(); }
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

    private int closeX() { return x() + WIDTH - 6 - CLOSE_WIDTH; }
    private int closeY() { return y() + 6; }

    /** The tab belongs to the machine background, below item counts and auxiliary windows. */
    public void renderTab(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (!screen.hasAppearanceSettings()) return;
        int tabX = screen.settingsTabX(), tabY = screen.settingsTabY();
        screen.getFrame().ear(g, tabX, tabY, screen.hasStatistics() ? GuiStatisticsWindow.TAB_PITCH + GuiFrame.TAB_SIZE : GuiFrame.TAB_SIZE, true);
        GuiArt.tabButton(g, tabX, tabY, true, onTab(mouseX, mouseY), open, screen.getFrame());
        GuiIcon.SETTINGS.draw(g,
                GuiFrame.tabButtonX(tabX, true) + (GuiFrame.TAB_BUTTON - GuiIcon.SETTINGS.width()) / 2,
                tabY + GuiFrame.TAB_BUTTON_TOP + (GuiFrame.TAB_BUTTON - GuiIcon.SETTINGS.height()) / 2 + 1);
    }

    public void render(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (!screen.hasAppearanceSettings()) return;
        rendering = true;
        g.pose().pushMatrix();
        g.pose().translate(0, 0);
        try {
            if (!open) {
                if (onTab(mouseX, mouseY)) g.setTooltipForNextFrame(screen.getFont(), text("title"), mouseX, mouseY);
                return;
            }
            int x = x(), y = y();
            GuiArt.panel(g, x, y, WIDTH, height(), screen.getFrame());
            GuiArt.scrollingText(g, screen.getFont(), text("title").getString(), x + 8, y + 8, WIDTH - 38, GuiTheme.textColor());
            GuiArt.button(g, closeX(), closeY(), CLOSE_WIDTH, CLOSE_HEIGHT,
                    focus == 3 || inside(mouseX, mouseY, closeX(), closeY(), CLOSE_WIDTH, CLOSE_HEIGHT), GuiIcon.CLOSE, screen.getFrame());
            g.text(screen.getFont(), text("background"), x + 12, y + 28, GuiTheme.textColor(), false);
            boolean dark = GuiThemes.dark(screen.machinePos());
            choice(g, x + 12, y + 42, GuiTheme.LIGHT_PANEL, !dark, "light", 0, mouseX, mouseY);
            choice(g, x + 100, y + 42, GuiTheme.DARK_PANEL, dark, "dark", 1, mouseX, mouseY);
            boolean all = GuiThemes.applyToAll();
            GuiArt.buttonBackground(g, x + 12, y + 78, WIDTH - 24, 18,
                    inside(mouseX, mouseY, x + 12, y + 78, WIDTH - 24, 18), all, false, screen.getFrame());
            if (all) GuiIcon.CONFIRM.draw(g, x + 18, y + 83);
            GuiArt.scrollingText(g, screen.getFont(), text("all").getString(), x + 32, y + 83, WIDTH - 48, 0xFFFFFFFF);
            if (focus == 2) GuiArt.ring(g, x + 12, y + 78, 18, 0xFFACDC42);
            if (inside(mouseX, mouseY, x + 12, y + 78, WIDTH - 24, 18)) {
                g.setTooltipForNextFrame(screen.getFont(), text(all ? "all.on" : "all.off"), mouseX, mouseY);
            }
            if (hasSound()) volume(g, x, y, mouseX, mouseY);
        } finally {
            g.pose().popMatrix();
            rendering = false;
        }
    }

    private void choice(GuiGraphicsExtractor g, int x, int y, int color, boolean selected, String label, int index, int mx, int my) {
        GuiArt.buttonBackground(g, x, y, 78, 22, inside(mx, my, x, y, 78, 22), selected, false, screen.getFrame());
        g.fill(x + 4, y + 4, x + 18, y + 18, color);
        if (selected) GuiIcon.CONFIRM.draw(g, x + 8, y + 7);
        GuiArt.scrollingText(g, screen.getFont(), text(label).getString(), x + 23, y + 7, 50, 0xFFFFFFFF);
        if (focus == index) GuiArt.ring(g, x + 3, y + 3, 16, 0xFFACDC42);
    }

    private void volume(GuiGraphicsExtractor g, int x, int y, int mx, int my) {
        int value = GuiThemes.volume(screen.machinePos());
        Component level = value == 0 ? text("volume.mute") : Component.literal(value + "%");
        g.text(screen.getFont(), Component.translatable("screen.biggerreactorsrework.settings.volume", level), x + 12, y + 105, GuiTheme.textColor(), false);
        for (int i = 0; i < 12; i++) {
            int bx = x + 12 + i * 14;
            boolean selected = i > 0 && i < 11 && value >= i * 10;
            GuiArt.buttonBackground(g, bx, y + 122, 12, 18, inside(mx, my, bx, y + 122, 12, 18), selected, false, screen.getFrame());
            if (i == 0 || i == 11) {
                g.text(screen.getFont(), i == 0 ? "−" : "+", bx + 3, y + 127, 0xFFFFFFFF, false);
                if (focus == (i == 0 ? 4 : 5)) GuiArt.ring(g, bx, y + 122, 12, 18, 0xFFACDC42);
            } else g.fill(bx + 3, y + 126, bx + 9, y + 136, selected ? 0xFF36CFEE : 0xFF44545B);
        }
        if (inside(mx, my, x + 12, y + 103, 166, 37)) {
            String help = inside(mx, my, x + 12, y + 122, 12, 18) ? "volume.decrease"
                    : inside(mx, my, x + 166, y + 122, 12, 18) ? "volume.increase" : "volume.help";
            g.setTooltipForNextFrame(screen.getFont(), text(help), mx, my);
        }
    }

    private void setBarVolume(double mx) {
        int segment = Mth.clamp((int)((mx - x() - 26) / 14) + 1, 1, 10);
        GuiThemes.setVolume(screen.machinePos(), segment * 10);
    }

    public boolean mousePressed(double mx, double my, int button) {
        if (!contains(mx, my)) return false;
        consumeRelease = true;
        if (button != com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) return true;
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
        } else if (inside(mx, my, x() + 12, y() + 78, WIDTH - 24, 18)) {
            activate(2);
        } else if (hasSound() && inside(mx, my, x() + 12, y() + 122, 12, 18)) {
            activate(4);
        } else if (hasSound() && inside(mx, my, x() + 166, y() + 122, 12, 18)) {
            activate(5);
        } else if (hasSound() && inside(mx, my, x() + 26, y() + 122, 138, 18)) {
            volumeDragging = true;
            setBarVolume(mx);
        } else if (inside(mx, my, x(), y(), WIDTH - 25, 22)) {
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
        if (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) { open = false; dragging = false; volumeDragging = false; }
        else if (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_TAB) focus = Math.floorMod(focus + ((modifiers & 1) == 0 ? 1 : -1), choices());
        else if (hasSound() && focus >= 4 && (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_LEFT || keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_RIGHT)) {
            GuiThemes.setVolume(screen.machinePos(), GuiThemes.volume(screen.machinePos()) + (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_LEFT ? -10 : 10));
        }
        else if (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_LEFT || keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_UP) focus = Math.floorMod(focus - 1, choices());
        else if (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_RIGHT || keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_DOWN) focus = (focus + 1) % choices();
        else if (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_RETURN || keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_NUMPADENTER || keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_SPACE) activate(focus);
        return true;
    }

    private static void clickSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}
