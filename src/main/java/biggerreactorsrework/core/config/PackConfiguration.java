package biggerreactorsrework.core.config;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.core.config.spec.*;
import biggerreactorsrework.core.parsers.Element;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Added 2026-11-01: one validated option catalog for local editing and optional server-script overrides. */
public final class PackConfiguration {
    private PackConfiguration() {}
    public record Option(String key, ConfigRegistration registration, SpecValueNode node) {
        public String current() { return node.currentValueAsString(); }
        public String checked(Object raw) {
            var type = node.generateSyncElement().type;
            String value;
            if (type == Element.Type.Number) {
                if (!(raw instanceof Number || raw instanceof String)) throw new IllegalArgumentException("Expected a number: " + key);
                try {
                    var number = new BigDecimal(raw.toString());
                    value = node instanceof SpecIntegralNode ? Long.toString(number.longValueExact()) : number.toString();
                    Class<?> fieldType = node.field.getType();
                    if (node instanceof SpecIntegralNode && (fieldType == int.class && (number.compareTo(BigDecimal.valueOf(Integer.MIN_VALUE)) < 0 || number.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0)
                            || fieldType == short.class && (number.compareTo(BigDecimal.valueOf(Short.MIN_VALUE)) < 0 || number.compareTo(BigDecimal.valueOf(Short.MAX_VALUE)) > 0)
                            || fieldType == byte.class && (number.compareTo(BigDecimal.valueOf(Byte.MIN_VALUE)) < 0 || number.compareTo(BigDecimal.valueOf(Byte.MAX_VALUE)) > 0)))
                        throw new ArithmeticException("Integer overflow");
                    if (fieldType == float.class && !Float.isFinite(number.floatValue())) throw new ArithmeticException("Float overflow");
                } catch (ArithmeticException | NumberFormatException error) { throw new IllegalArgumentException("Invalid number: " + key); }
            } else if (type == Element.Type.Boolean) {
                if (!(raw instanceof Boolean || raw instanceof String)) throw new IllegalArgumentException("Expected a boolean: " + key);
                value = raw.toString();
            } else {
                if (!(raw instanceof String)) throw new IllegalArgumentException("Expected text: " + key);
                value = (String) raw;
            }
            if (value.length() > 32768 || !node.isValueValid(value)) throw new IllegalArgumentException("Invalid value for " + key + ": " + value);
            if (key.equals("Reactor.CustomFuelEntries")) biggerreactorsrework.registries.ReactorFuelRegistry.parse(value);
            return value;
        }
    }
    public static List<Option> options() {
        var result = new ArrayList<Option>();
        for (var registration : ConfigManager.getAllConfigsForMod(BiggerReactors.modid)) collect(registration, registration.rootConfigSpecNode, "", result);
        result.sort(Comparator.comparing((Option o) -> o.registration.configType.name()).thenComparing(Option::key));
        return List.copyOf(result);
    }
    private static void collect(ConfigRegistration registration, SpecObjectNode object, String prefix, List<Option> out) {
        for (var node : object.subNodeList) {
            if (node.hidden) continue;
            if (node instanceof SpecObjectNode child) collect(registration, child, prefix + node.name + ".", out);
            else if (node instanceof SpecValueNode value) out.add(new Option(prefix + node.name, registration, value));
        }
    }
    public static Map<String, Option> serverOptions() {
        var result = new TreeMap<String, Option>();
        for (var option : options()) if (option.registration.configType != ConfigType.CLIENT) result.put(option.key, option);
        return Collections.unmodifiableMap(result);
    }
    public static boolean editable(Option option) {
        return option.registration.isLocalLoaded() && (option.registration.configType == ConfigType.CLIENT || !ConfigManager.hasActiveWorld());
    }
    public static void save(Option option, String raw) throws IOException {
        if (!editable(option)) throw new IllegalStateException("Server rules can only be edited locally before opening a world");
        String value = option.checked(raw);
        var registration = option.registration;
        var before = registration.rootConfigSpecNode.generateSyncElement();
        Path destination = registration.writeFile.toPath().toAbsolutePath();
        Path temporary = null;
        try {
            option.node.writeFromString(value);
            Files.createDirectories(destination.getParent());
            temporary = Files.createTempFile(destination.getParent(), destination.getFileName().toString(), ".tmp");
            Files.writeString(temporary, registration.fileFormat.parse(registration.rootConfigSpecNode.generateCurrentElement(true)), StandardCharsets.UTF_8);
            try { Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException | RuntimeException error) {
            registration.rootConfigSpecNode.writeElement(before);
            throw error;
        } finally { if (temporary != null) Files.deleteIfExists(temporary); }
        ConfigManager.rulesChanged();
    }

    private static final Map<ConfigRegistration, Element> originals = new LinkedHashMap<>();
    private static Map<String, String> overrides = Map.of();
    public static Map<String, String> overrides() { return overrides; }
    public static String fileValue(String key) {
        var option = serverOptions().get(key);
        if (option == null) throw new IllegalArgumentException("Unknown server configuration key: " + key);
        var tree = originals.get(option.registration);
        if (tree == null) return option.current();
        for (var name : key.split("\\.")) {
            Element found = null;
            if (tree.subArray != null) for (var child : tree.subArray) if (name.equals(child.name)) { found = child; break; }
            if (found == null) throw new IllegalStateException("Missing base option " + key);
            tree = found;
        }
        return String.valueOf(tree.value);
    }
    public static void setOverrides(Map<String, ?> requested) {
        var catalog = serverOptions();
        var checked = new TreeMap<String, String>();
        if (requested.size() > catalog.size()) throw new IllegalArgumentException("Too many configuration overrides");
        requested.forEach((key, raw) -> {
            var option = catalog.get(key);
            if (option == null) throw new IllegalArgumentException("Unknown server configuration key: " + key);
            checked.put(key, option.checked(raw));
        });
        // Most simulation dimensions/capacities are captured at assembly. Never mutate live stocks on /reload.
        if (ConfigManager.hasActiveWorld()) {
            if (!checked.equals(overrides)) throw new IllegalStateException("Configuration script changes require reopening the world or restarting the server");
            return;
        }
        biggerreactorsrework.calculator.service.CalculatorService.invalidate();
        restore();
        for (var entry : checked.entrySet()) {
            var option = catalog.get(entry.getKey());
            originals.computeIfAbsent(option.registration, r -> r.rootConfigSpecNode.generateSyncElement());
            option.node.writeFromString(entry.getValue());
        }
        overrides = Map.copyOf(checked);
        ConfigManager.rulesChanged();
    }
    private static void restore() { originals.forEach((registration, tree) -> registration.rootConfigSpecNode.writeElement(tree)); originals.clear(); }
    public static void clearOverrides() { restore(); overrides = Map.of(); }
}
