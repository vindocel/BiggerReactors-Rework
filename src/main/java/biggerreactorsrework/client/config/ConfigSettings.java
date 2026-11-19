package biggerreactorsrework.client.config;

import biggerreactorsrework.core.config.ConfigType;
import biggerreactorsrework.core.config.PackConfiguration;
import biggerreactorsrework.core.config.spec.SpecBoolNode;
import biggerreactorsrework.core.config.spec.SpecNumberNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Bridges the editor to the existing option catalog without changing configuration defaults. */
final class ConfigSettings {
    record Entry(PackConfiguration.Option option) {
        Object getDefault() { return value(option, option.node().defaultValueAsString()); }
    }

    private ConfigSettings() {}

    static Map<String, Entry> clientOptions() { return options(true); }
    static Map<String, Entry> serverOptions() { return options(false); }

    private static Map<String, Entry> options(boolean client) {
        var result = new LinkedHashMap<String, Entry>();
        for (var option : PackConfiguration.options()) {
            if ((option.registration().configType == ConfigType.CLIENT) == client) {
                result.put(option.key(), new Entry(option));
            }
        }
        return result;
    }

    static Map<String, Object> appearanceDefaults() {
        return Map.of("appearance.dark", false, "appearance.shared", true);
    }

    static Map<String, Object> fileValues(boolean client) {
        var result = new LinkedHashMap<String, Object>();
        options(client).forEach((key, entry) -> result.put(key, value(entry.option, entry.option.current())));
        return result;
    }

    static Map<String, Object> effectiveServerValues() { return fileValues(false); }

    static boolean isLoaded(boolean client) {
        return options(client).values().stream().allMatch(entry -> entry.option.registration().isLocalLoaded());
    }

    static Object checkedValue(String key, Object raw, boolean client) {
        if (appearanceDefaults().containsKey(key)) {
            if (!(raw instanceof Boolean)) throw new IllegalArgumentException("Expected a boolean");
            return raw;
        }
        var entry = options(client).get(key);
        if (entry == null) throw new IllegalArgumentException("Unknown option");
        String text = raw instanceof List<?> entries
                ? String.join(";", entries.stream().map(Object::toString).toList()) : raw.toString();
        return value(entry.option, entry.option.checked(text));
    }

    private static Object value(PackConfiguration.Option option, String text) {
        if (option.node() instanceof SpecBoolNode) return Boolean.parseBoolean(text);
        if (option.key().equals("Reactor.CustomFuelEntries")) {
            return Arrays.stream(text.split(";")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        }
        return text;
    }

    static Component description(String key) {
        String translation = "config.biggerreactorsrework.description." + key;
        return net.minecraft.locale.Language.getInstance().has(translation) ? Component.translatable(translation)
                : Component.translatable("config.biggerreactorsrework.description", name(key));
    }

    static Component name(String key) {
        return Component.translatableWithFallback("config.biggerreactorsrework.option." + key,
                key.replaceAll("([a-z])([A-Z])", "$1 $2").replace('.', ' '));
    }

    static Component range(String key) {
        var option = PackConfiguration.options().stream().filter(o -> o.key().equals(key)).findFirst().orElseThrow();
        if (option.node() instanceof SpecNumberNode number) {
            return Component.translatable("config.biggerreactorsrework.range",
                    (number.lowerInclusive ? "[" : "(") + number.lowerBoundAsString() + ", "
                    + number.upperBoundAsString() + (number.upperInclusive ? "]" : ")"));
        }
        if (option.node() instanceof biggerreactorsrework.core.config.spec.SpecEnumNode values) {
            return Component.translatable("config.biggerreactorsrework.range", String.join(", ", values.allowedValuesAsStrings()));
        }
        return Component.empty();
    }

    static void saveValues(boolean client, Map<String, Object> values) {
        var checked = new LinkedHashMap<String, Object>();
        values.forEach((key, raw) -> {
            Object value = checkedValue(key, raw, client);
            checked.put(key, value instanceof List<?> list
                    ? String.join(";", list.stream().map(Object::toString).toList()) : value);
        });
        try {
            PackConfiguration.saveEditorValues(checked, client);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
//? if =26.1.2 || =1.21.1 {
    }

    static void screen(Minecraft minecraft, Screen screen) { minecraft.setScreen(screen); }
}
//?} else if =26.3 {
/*    }

    static void screen(Minecraft minecraft, Screen screen) { minecraft.gui.setScreen(screen); }
}
*/
//?}
