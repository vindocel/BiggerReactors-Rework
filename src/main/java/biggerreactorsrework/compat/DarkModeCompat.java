package biggerreactorsrework.compat;

import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;

/** Optional Dark Mode Everywhere exclusion: our panels and text already manage their own theme. */
public final class DarkModeCompat {
    private DarkModeCompat() {}

    public static void onInterModEnqueue(InterModEnqueueEvent event) {
        if (ModList.get().isLoaded("darkmodeeverywhere")) {
            InterModComms.sendTo("darkmodeeverywhere", "dme-shaderblacklist", () -> "biggerreactorsrework.");
        }
    }
}
