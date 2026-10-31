package biggerreactorsrework.calculator;

import biggerreactorsrework.calculator.design.PlantLayout;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Immutable record of one calculation: the request, the conditions it was computed under, the plant block by block
 * and the measured values. Reopening it never recomputes anything. Numeric values live in an ordered map with the
 * keys below so older records stay readable when new values are added.
 */
public record CalculationResult(String id, long createdAt, CalculatorRequest request, Route route, CalculationStatus status,
                                String fingerprint, int engineVersion, String modVersion, @Nullable PlantLayout plant,
                                Map<String, Double> values, Set<String> missingBlocks) {
    public static final int SCHEMA = 1;

    // Measured plant
    public static final String POWER = "power", POWER_MIN = "power_min", POWER_MAX = "power_max", POWER_ALT = "power_alt_order";
    public static final String FUEL = "fuel", EFFICIENCY = "efficiency", STEAM = "steam", HOT_FLOW = "hot_flow", RPM = "rpm";
    public static final String FUEL_TEMPERATURE = "fuel_temperature", REACTOR_LIMIT = "reactor_limit", EXCHANGER_LIMIT = "exchanger_limit";
    public static final String DRIFT = "drift", TICKS = "ticks", WINDOW = "window", PRIMARY_ERROR = "primary_error", WATER_ERROR = "water_error";
    // Settings to reproduce it
    public static final String INSERTION = "insertion", TURBINE_FLOW = "turbine_flow", PRIMARY_CHARGE = "primary_charge", WATER_CHARGE = "water_charge";
    // Stocks after warm-up
    public static final String REACTOR_COLD = "reactor_cold", REACTOR_HOT = "reactor_hot", EXCHANGER_HOT = "exchanger_hot", EXCHANGER_COLD = "exchanger_cold";
    public static final String EXCHANGER_WATER = "exchanger_water", EXCHANGER_STEAM = "exchanger_steam", TURBINE_STEAM = "turbine_steam", TURBINE_WATER = "turbine_water";
    // Capacities and functional counts
    public static final String REACTOR_CAPACITY = "reactor_capacity", EXCHANGER_CAPACITY = "exchanger_capacity", TURBINE_CAPACITY = "turbine_capacity";
    public static final String FUEL_COLUMNS = "fuel_columns", FUEL_RODS = "fuel_rods", MANIFOLDS = "manifolds", MODERATORS = "moderators", CONTACTS = "contacts";
    public static final String CONDENSER = "condenser", EVAPORATOR = "evaporator", EXCHANGER_CONTACTS = "exchanger_contacts";
    public static final String BLADES = "blades", COILS = "coils", SHAFT = "shaft", FLOW_LIMIT = "flow_limit";
    // Search coverage
    public static final String EVALUATED = "evaluated", ELAPSED_MS = "elapsed_ms", SEARCH_LIMITED = "search_limited";

    public double value(String key) {
        return values.getOrDefault(key, 0.0);
    }

    public boolean has(String key) {
        return values.containsKey(key);
    }

    public CompoundTag toTag() {
        var tag = new CompoundTag();
        tag.putInt("schema", SCHEMA);
        tag.putString("id", id);
        tag.putLong("created", createdAt);
        tag.put("request", request.toTag());
        tag.putString("route", route.name());
        tag.putString("status", status.name());
        tag.putString("fingerprint", fingerprint);
        tag.putInt("engine", engineVersion);
        tag.putString("mod", modVersion);
        if (plant != null) tag.put("plant", PlantCodec.encode(plant));
        var numbers = new CompoundTag();
        values.forEach((k, v) -> { if (Double.isFinite(v)) numbers.putDouble(k, v); });
        tag.put("values", numbers);
        return tag;
    }

    /** Null when malformed; a newer schema is reported by {@link #newerSchema(CompoundTag)} instead of being read. */
    @Nullable
    public static CalculationResult fromTag(CompoundTag tag) {
        if (newerSchema(tag)) return null;
        try {
            var request = CalculatorRequest.fromTag(tag.getCompoundOrEmpty("request"));
            if (request == null) return null;
            String id = tag.getStringOr("id", "");
            if (id.isEmpty() || id.length() > 64) return null;
            var missing = new TreeSet<String>();
            PlantLayout plant = null;
            if (tag.contains("plant")) {
                plant = PlantCodec.decode(tag.getCompoundOrEmpty("plant"), missing);
                if (plant == null) return null;
            }
            var numbers = tag.getCompoundOrEmpty("values");
            if (numbers.size() > 256) return null;
            var values = new LinkedHashMap<String, Double>();
            for (String key : numbers.keySet()) {
                double v = numbers.getDoubleOr(key, 0D);
                if (Double.isFinite(v) && key.length() <= 64) values.put(key, v);
            }
            return new CalculationResult(id, tag.getLongOr("created", 0L), request, Route.valueOf(tag.getStringOr("route", "")),
                    CalculationStatus.valueOf(tag.getStringOr("status", "")), tag.getStringOr("fingerprint", ""), tag.getIntOr("engine", 0),
                    tag.getStringOr("mod", ""), plant, Collections.unmodifiableMap(values), Collections.unmodifiableSet(missing));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean newerSchema(CompoundTag tag) {
        return tag.getIntOr("schema", 0) > SCHEMA;
    }
}
