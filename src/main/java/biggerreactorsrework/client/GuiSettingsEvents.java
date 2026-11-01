package biggerreactorsrework.client;

// Modified 2026-11-01: native Minecraft 26.1.2 API.
import biggerreactorsrework.core.client.gui.screens.ModScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Intercepts popup input before subclass controls, vanilla slots and other screen listeners. Modified 2026-10-26:
 * both popups stay open together; the one in front is drawn on top and gets input first, and only the area a popup
 * covers is taken from the screen under it.
 */
public final class GuiSettingsEvents {
    private GuiSettingsEvents() {}

    public static void register() {
        var bus = NeoForge.EVENT_BUS;
        bus.addListener(EventPriority.LOWEST, GuiSettingsEvents::render);
        bus.addListener(EventPriority.HIGHEST, GuiSettingsEvents::press);
        bus.addListener(EventPriority.HIGHEST, GuiSettingsEvents::release);
        bus.addListener(EventPriority.HIGHEST, GuiSettingsEvents::drag);
        bus.addListener(EventPriority.HIGHEST, GuiSettingsEvents::scroll);
        bus.addListener(EventPriority.HIGHEST, GuiSettingsEvents::key);
        bus.addListener(EventPriority.HIGHEST, GuiSettingsEvents::keyReleased);
        bus.addListener(EventPriority.HIGHEST, GuiSettingsEvents::character);
        bus.addListener(EventPriority.HIGHEST, GuiSettingsEvents::tooltip);
    }

    private static boolean anyOpen(ModScreen<?> screen) {
        return screen.statisticsWindow().open() || screen.settingsWindow().open();
    }

    private static void render(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof ModScreen<?> screen) {
            var g = event.getGuiGraphics();
            boolean statisticsFront = screen.statisticsInFront();
            for (int layer = 0; layer < 2; layer++) {
                boolean statistics = (layer == 1) == statisticsFront;
                g.nextStratum();
                if (statistics) screen.statisticsWindow().render(g, event.getMouseX(), event.getMouseY());
                else screen.settingsWindow().render(g, event.getMouseX(), event.getMouseY());
            }
        }
    }

    private static void press(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof ModScreen<?> screen)) return;
        double mx = event.getMouseX(), my = event.getMouseY();
        boolean statisticsFront = screen.statisticsInFront();
        for (int i = 0; i < 2; i++) {
            boolean statistics = (i == 0) == statisticsFront;
            boolean handled = statistics ? screen.statisticsWindow().mousePressed(mx, my, event.getButton())
                    : screen.settingsWindow().mousePressed(mx, my, event.getButton());
            if (handled) {
                screen.bringToFront(statistics);
                event.setCanceled(true);
                return;
            }
        }
    }

    private static void release(ScreenEvent.MouseButtonReleased.Pre event) {
        if (!(event.getScreen() instanceof ModScreen<?> screen)) return;
        boolean statistics = screen.statisticsWindow().release();
        boolean settings = screen.settingsWindow().mouseReleased();
        if (statistics || settings) event.setCanceled(true);
    }

    private static void drag(ScreenEvent.MouseDragged.Pre event) {
        if (!(event.getScreen() instanceof ModScreen<?> screen)) return;
        if (screen.statisticsWindow().drag(event.getMouseX(), event.getMouseY())
                || screen.settingsWindow().mouseDragged(event.getMouseX(), event.getMouseY())) event.setCanceled(true);
    }

    private static void scroll(ScreenEvent.MouseScrolled.Pre event) {
        if (!(event.getScreen() instanceof ModScreen<?> screen)) return;
        double mx = event.getMouseX(), my = event.getMouseY();
        boolean statisticsCovered = screen.statisticsWindow().covers(mx, my);
        boolean settingsCovered = screen.settingsWindow().covers(mx, my);
        if (statisticsCovered && (screen.statisticsInFront() || !settingsCovered)) {
            screen.statisticsWindow().scroll(mx, my, event.getScrollDeltaY());
            event.setCanceled(true);
        } else if (settingsCovered) {
            event.setCanceled(true);
        }
    }

    private static void key(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof ModScreen<?> screen)) return;
        boolean statisticsFront = screen.statisticsInFront();
        for (int i = 0; i < 2; i++) {
            boolean statistics = (i == 0) == statisticsFront;
            boolean handled = statistics ? screen.statisticsWindow().key(event.getKeyEvent().key())
                    : screen.settingsWindow().keyPressed(event.getKeyEvent().key(), event.getKeyEvent().modifiers());
            if (handled) {
                event.setCanceled(true);
                return;
            }
        }
    }

    private static void keyReleased(ScreenEvent.KeyReleased.Pre event) {
        if (event.getScreen() instanceof ModScreen<?> screen && anyOpen(screen)) event.setCanceled(true);
    }

    private static void character(ScreenEvent.CharacterTyped.Pre event) {
        if (event.getScreen() instanceof ModScreen<?> screen && anyOpen(screen)) event.setCanceled(true);
    }

    /** Tooltips of the screen under a popup are hidden only where the popup covers it. */
    private static void tooltip(RenderTooltipEvent.Pre event) {
        if (!(Minecraft.getInstance().screen instanceof ModScreen<?> screen)) return;
        if (screen.statisticsWindow().rendering() || screen.settingsWindow().rendering()) return;
        if (screen.statisticsWindow().covers(event.getX(), event.getY()) || screen.settingsWindow().covers(event.getX(), event.getY())) {
            event.setCanceled(true);
        }
    }
}
