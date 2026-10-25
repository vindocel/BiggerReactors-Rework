package biggerreactorsrework.client;

import biggerreactorsrework.core.client.gui.screens.ModScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Intercepts popup input before subclass controls, vanilla slots and other screen listeners. */
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

    private static void render(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof ModScreen<?> screen) {
            screen.settingsWindow().render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY());
        }
    }

    private static void press(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getScreen() instanceof ModScreen<?> screen && screen.settingsWindow().mousePressed(event.getMouseX(), event.getMouseY(), event.getButton())) event.setCanceled(true);
    }

    private static void release(ScreenEvent.MouseButtonReleased.Pre event) {
        if (event.getScreen() instanceof ModScreen<?> screen && screen.settingsWindow().mouseReleased()) event.setCanceled(true);
    }

    private static void drag(ScreenEvent.MouseDragged.Pre event) {
        if (event.getScreen() instanceof ModScreen<?> screen && screen.settingsWindow().mouseDragged(event.getMouseX(), event.getMouseY())) event.setCanceled(true);
    }

    private static void scroll(ScreenEvent.MouseScrolled.Pre event) {
        if (event.getScreen() instanceof ModScreen<?> screen && screen.settingsWindow().open()) event.setCanceled(true);
    }

    private static void key(ScreenEvent.KeyPressed.Pre event) {
        if (event.getScreen() instanceof ModScreen<?> screen && screen.settingsWindow().keyPressed(event.getKeyCode(), event.getModifiers())) event.setCanceled(true);
    }

    private static void keyReleased(ScreenEvent.KeyReleased.Pre event) {
        if (event.getScreen() instanceof ModScreen<?> screen && screen.settingsWindow().open()) event.setCanceled(true);
    }

    private static void character(ScreenEvent.CharacterTyped.Pre event) {
        if (event.getScreen() instanceof ModScreen<?> screen && screen.settingsWindow().open()) event.setCanceled(true);
    }

    private static void tooltip(RenderTooltipEvent.Pre event) {
        if (Minecraft.getInstance().screen instanceof ModScreen<?> screen && screen.settingsWindow().open() && !screen.settingsWindow().rendering()) event.setCanceled(true);
    }
}
