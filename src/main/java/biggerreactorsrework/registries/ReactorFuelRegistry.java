package biggerreactorsrework.registries;

import biggerreactorsrework.Config;
import biggerreactorsrework.blocks.materials.MaterialBlock;
import biggerreactorsrework.items.ingots.BlutoniumIngot;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Added 2026-11-01: registered items/tags supply configurable yields of the existing generic reactor fuel. */
public final class ReactorFuelRegistry {
    private ReactorFuelRegistry() {}
    public record Entry(Identifier id, boolean tag, long amount) {}
    public static List<Entry> parse(String text) {
        if (text.length() > 32768) throw new IllegalArgumentException("Fuel entries exceed 32768 characters");
        var result = new ArrayList<Entry>();
        var seen = new HashSet<String>();
        for (String token : text.split(";")) {
            if (token.isBlank()) continue;
            var pair = token.trim().split("=", -1);
            if (pair.length != 2) throw new IllegalArgumentException("Expected item-or-tag=amount: " + token);
            String key = pair[0].trim(); boolean tag = key.startsWith("#");
            var id = Identifier.tryParse(tag ? key.substring(1) : key);
            long amount;
            try { amount = Long.parseLong(pair[1].trim()); }
            catch (NumberFormatException error) { throw new IllegalArgumentException("Fuel amount must be an integer: " + token); }
            if (id == null || amount < 0 || amount > Long.MAX_VALUE / Integer.MAX_VALUE || !seen.add((tag ? "#" : "") + id))
                throw new IllegalArgumentException("Invalid or duplicate fuel entry: " + token);
            result.add(new Entry(id, tag, amount));
            if (result.size() > 1024) throw new IllegalArgumentException("Too many fuel entries");
        }
        return List.copyOf(result);
    }
    private static String cachedText;
    private static List<Entry> cachedEntries = List.of();
    private static synchronized List<Entry> entries() {
        String text = Config.CONFIG.Reactor.CustomFuelEntries;
        if (!Objects.equals(text, cachedText)) {
            try { cachedEntries = parse(text); }
            catch (IllegalArgumentException invalid) {
                biggerreactorsrework.BiggerReactors.LOGGER.error("Ignoring invalid Reactor.CustomFuelEntries: {}", invalid.getMessage());
                cachedEntries = List.of();
            }
            cachedText = text;
        }
        return cachedEntries;
    }
    public static long amount(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        var item = stack.getItem().builtInRegistryHolder();
        var overrides = entries();
        for (var entry : overrides) if (!entry.tag && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(entry.id)) return entry.amount;
        for (var entry : overrides) if (entry.tag && item.is(TagKey.create(BuiltInRegistries.ITEM.key(), entry.id))) return entry.amount;
        var data = item.getData(MaterialDataMaps.REACTOR_FUEL);
        if (data != null) return data.amount();
        long unit = Config.CONFIG.Reactor.FuelMBPerIngot;
        if (item.is(TagKey.create(BuiltInRegistries.ITEM.key(), Identifier.parse("c:storage_blocks/uranium"))) || stack.is(MaterialBlock.BLUTONIUM.asItem()))
            return unit <= Long.MAX_VALUE / 9 ? unit * 9 : 0;
        if (item.is(TagKey.create(BuiltInRegistries.ITEM.key(), Identifier.parse("c:ingots/uranium"))) || stack.is(BlutoniumIngot.INSTANCE)) return unit;
        return 0;
    }
}
