package biggerreactorsrework.client;

import biggerreactorsrework.BiggerReactors;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModList;


public final class ScreenPaletteGuard {
    private record PaletteOverride(Field field, Object suspendedValue) {
    }

    private static List<PaletteOverride> overrides;

    private ScreenPaletteGuard() {
    }

    private static List<PaletteOverride> overrides() {
        if (overrides == null) {
            List<PaletteOverride> found = new ArrayList<>();
            add(found, "darkmode", "my.celium.org.darkmode.DarkGate", "active", false);
            add(found, "darkmodeeverywhere", "com.buuz135.darkmodeeverywhere.ClientProxy", "SELECTED_SHADER_VALUE", null);
            overrides = List.copyOf(found);
        }
        return overrides;
    }

    private static void add(List<PaletteOverride> found, String mod, String type, String name, Object suspendedValue) {
        if (!ModList.get().isLoaded(mod)) {
            return;
        }
        try {
            Field field = Class.forName(type).getDeclaredField(name);
            field.setAccessible(true);
            found.add(new PaletteOverride(field, suspendedValue));
        } catch (ReflectiveOperationException | RuntimeException unavailable) {
            BiggerReactors.LOGGER.warn("Screen palette protection is unavailable for {}", mod, unavailable);
        }
    }

    public static void draw(Screen screen, Runnable drawing) {
        if (!screen.getClass().getName().startsWith("biggerreactorsrework.")) {
            drawing.run();
            return;
        }
        List<PaletteOverride> bindings = overrides();
        Object[] previous = new Object[bindings.size()];
        int applied = 0;
        try {
            for (int i = 0; i < bindings.size(); i++) {
                PaletteOverride binding = bindings.get(i);
                try {
                    previous[i] = binding.field().get(null);
                    binding.field().set(null, binding.suspendedValue());
                    applied++;
                } catch (IllegalAccessException unavailable) {
                    throw new IllegalStateException("Cannot suspend the external screen palette", unavailable);
                }
            }
            drawing.run();
        } finally {
            IllegalStateException failure = null;
            for (int i = applied - 1; i >= 0; i--) {
                try {
                    bindings.get(i).field().set(null, previous[i]);
                } catch (IllegalAccessException unavailable) {
                    if (failure == null) failure = new IllegalStateException("Cannot restore the external screen palette");
                    failure.addSuppressed(unavailable);
                }
            }
            if (failure != null) throw failure;
        }
    }
}
