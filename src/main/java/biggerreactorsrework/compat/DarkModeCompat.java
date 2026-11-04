package biggerreactorsrework.compat;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.lang.reflect.Field;

/** Optional Dark Mode Everywhere exclusion: our panels and text already manage their own theme. */
public final class DarkModeCompat {
    private static Field selected;
    private static Object suspended;

    private DarkModeCompat() {}

    public static void onInterModEnqueue(InterModEnqueueEvent event) {
        if (ModList.get().isLoaded("darkmodeeverywhere")) {
            InterModComms.sendTo("darkmodeeverywhere", "dme-shaderblacklist", () -> "biggerreactorsrework.");
        }
    }

    // Added 2026-11-03: its text recoloring ignores the blacklist, so its selected theme is cleared while our screens draw.
    public static void register() {
        if (!ModList.get().isLoaded("darkmodeeverywhere")) return;
        try {
            selected = Class.forName("com.buuz135.darkmodeeverywhere.ClientProxy").getField("SELECTED_SHADER_VALUE");
        } catch (ReflectiveOperationException | RuntimeException unavailable) {
            return;
        }
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, ScreenEvent.Render.Pre.class, event -> {
            if (ours(event.getScreen())) suspend();
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ScreenEvent.Render.Post.class, event -> resume());
    }

    private static boolean ours(Object screen) {
        return screen.getClass().getName().startsWith("biggerreactorsrework.");
    }

    private static void suspend() {
        try {
            suspended = selected.get(null);
            selected.set(null, null);
        } catch (ReflectiveOperationException unavailable) {
            suspended = null;
        }
    }

    private static void resume() {
        if (suspended == null) return;
        try {
            selected.set(null, suspended);
        } catch (ReflectiveOperationException ignored) {
        }
        suspended = null;
    }
}
