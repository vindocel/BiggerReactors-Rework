package biggerreactorsrework.compat.kubejs;

import biggerreactorsrework.core.config.PackConfiguration;
import biggerreactorsrework.registries.ReactorFuelRegistry;
import dev.latvian.mods.kubejs.event.KubeEvent;
import java.util.*;

/** Added 2026-11-01: validated, memory-only server overrides; simulation changes require a world restart. */
public final class ReworkConfigEvent implements KubeEvent {
    private final Map<String, Object> values = new LinkedHashMap<>();
    private final Map<String, Long> fuels = new LinkedHashMap<>();
    public List<String> getKeys() { return List.copyOf(PackConfiguration.serverOptions().keySet()); }
    public void set(String key, Object value) {
        var option = PackConfiguration.serverOptions().get(key);
        if (option == null) throw new IllegalArgumentException("Unknown server configuration key: " + key);
        values.put(key, option.checked(value));
    }
    /** Supply an item or #item-tag and its yield in mB of existing generic reactor fuel; zero disables it. */
    public void fuel(String itemOrTag, double amount) {
        if (!Double.isFinite(amount) || amount != Math.rint(amount) || amount < 0 || amount > Long.MAX_VALUE / Integer.MAX_VALUE)
            throw new IllegalArgumentException("Fuel yield must be a whole nonnegative number within the safe range");
        var entry = ReactorFuelRegistry.parse(itemOrTag + "=" + (long) amount).getFirst();
        fuels.put((entry.tag() ? "#" : "") + entry.id(), (long) amount);
    }
    Map<String, Object> values() {
        if (!fuels.isEmpty()) {
            var merged = new LinkedHashMap<String, Long>();
            String base = String.valueOf(values.getOrDefault("Reactor.CustomFuelEntries", PackConfiguration.fileValue("Reactor.CustomFuelEntries")));
            for (var entry : ReactorFuelRegistry.parse(base)) merged.put((entry.tag() ? "#" : "") + entry.id(), entry.amount());
            merged.putAll(fuels);
            var text = new StringJoiner(";"); merged.forEach((key, amount) -> text.add(key + "=" + amount));
            set("Reactor.CustomFuelEntries", text.toString());
        }
        return Map.copyOf(values);
    }
}
