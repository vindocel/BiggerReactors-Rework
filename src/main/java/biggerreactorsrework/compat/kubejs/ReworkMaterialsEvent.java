package biggerreactorsrework.compat.kubejs;

import biggerreactorsrework.registries.MaterialDataMaps;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Added 2026-11-01: validated native data-map overlays, regenerated with server scripts on every reload. */
public final class ReworkMaterialsEvent implements KubeEvent {
    private final Map<String, JsonObject> maps = new LinkedHashMap<>();
    private static String key(String value) {
        boolean tag = value.startsWith("#"); var id = ResourceLocation.tryParse(tag ? value.substring(1) : value);
        if (id == null) throw new IllegalArgumentException("Invalid item/block/fluid key: " + value);
        return (tag ? "#" : "") + id;
    }
    private void put(String map, String target, JsonObject data, Codec<?> codec) {
        codec.parse(JsonOps.INSTANCE, data).getOrThrow();
        var values = maps.computeIfAbsent(map, ignored -> new JsonObject());
        if (values.size() >= 4096) throw new IllegalArgumentException("Too many material overrides");
        values.add(key(target), data);
    }
    public void moderator(String block, double absorption, double efficiency, double moderation, double conductivity) {
        moderator("block/reactor_moderator", block, absorption, efficiency, moderation, conductivity);
    }
    private void moderator(String map, String target, double absorption, double efficiency, double moderation, double conductivity) {
        var data = new JsonObject(); data.addProperty("absorption", absorption); data.addProperty("efficiency", efficiency);
        data.addProperty("moderation", moderation); data.addProperty("conductivity", conductivity);
        put(map, target, data, MaterialDataMaps.Moderator.CODEC);
    }
    public void coil(String block, double efficiency, double extractionRate, double bonus) {
        var data = new JsonObject(); data.addProperty("efficiency", efficiency); data.addProperty("extractionRate", extractionRate); data.addProperty("bonus", bonus);
        put("block/turbine_coil", block, data, MaterialDataMaps.Coil.CODEC);
    }
    public void fuel(String item, double amount) {
        if (!Double.isFinite(amount) || amount != Math.rint(amount)) throw new IllegalArgumentException("Fuel yield must be an integer");
        var data = new JsonObject(); data.addProperty("amount", (long) amount);
        put("item/reactor_fuel", item, data, MaterialDataMaps.ReactorFuel.CODEC);
    }
    public void transition(String liquid, String gas, double latentHeat, double boilingPoint, double liquidConductivity,
                           double gasConductivity, double turbineMultiplier, double recovery) {
        var data = new JsonObject(); data.addProperty("gas", key(gas)); data.addProperty("latentHeat", latentHeat);
        data.addProperty("boilingPoint", boilingPoint); data.addProperty("liquidThermalConductivity", liquidConductivity);
        data.addProperty("gasThermalConductivity", gasConductivity); data.addProperty("turbineMultiplier", turbineMultiplier); data.addProperty("recovery", recovery);
        put("fluid/fluid_transition", liquid, data, MaterialDataMaps.Transition.CODEC);
    }
    Map<String, JsonObject> maps() {
        var result = new LinkedHashMap<String, JsonObject>();
        maps.forEach((map, values) -> { var file = new JsonObject(); file.add("values", values.deepCopy()); result.put(map, file); });
        return result;
    }
}
