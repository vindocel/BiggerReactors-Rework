package biggerreactorsrework.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Added 2026-10-31: validate asset selectors against every actual registered port state. */
public final class PortModelGameTests {
    private static JsonObject asset(String path) throws Exception {
        try (var stream = PortModelGameTests.class.getResourceAsStream("/assets/biggerreactorsrework/" + path)) {
            if (stream == null) throw new AssertionError("Missing asset: " + path);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
    private static <T extends Comparable<T>> String value(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }
    private static boolean matches(String selector, BlockState state) {
        for (String part : selector.split(",")) {
            String[] pair = part.split("=");
            if (pair.length != 2) return false;
            var property = state.getBlock().getStateDefinition().getProperty(pair[0]);
            if (property == null || !value(state, property).equals(pair[1])) return false;
        }
        return true;
    }
    @GameTest(template="empty_7x7x7")
    public static void everyPortStateSelectsExactlyOneCompatibleModel(GameTestHelper h) throws Exception {
        for (String name : new String[]{"reactor_access_port","reactor_coolant_port","reactor_power_tap","reactor_redstone_port",
                "reactor_computer_port","turbine_fluid_port","turbine_power_tap","turbine_redstone_port","turbine_computer_port",
                "heat_exchanger_fluid_port","heat_exchanger_computer_port","battery_energy_port","battery_redstone_port"}) {
            var block = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("biggerreactorsrework", name));
            var variants = asset("blockstates/" + name + ".json").getAsJsonObject("variants");
            for (var state : block.getStateDefinition().getPossibleStates()) {
                var selected = variants.entrySet().stream().filter(v -> matches(v.getKey(), state)).toList();
                h.assertTrue(selected.size() == 1, name + " has ambiguous/missing model for " + state);
                String model = selected.getFirst().getValue().getAsJsonObject().get("model").getAsString();
                var modelData = asset("models/" + model.split(":")[1] + ".json");
                boolean assembled = state.getValue(biggerreactorsrework.core.multiblock.IAssemblyStateBlock.ASSEMBLED);
                h.assertTrue(!assembled || modelData.get("parent").getAsString().equals("biggerreactorsrework:block/assembled_port"),
                        "assembled port missing its face template");
                var textures = modelData.getAsJsonObject("textures");
                h.assertTrue(textures.has(assembled ? "front" : "all"), "missing functional face texture");
                for (var texture : textures.entrySet()) {
                    String id = texture.getValue().getAsString();
                    if (id.startsWith("#")) continue;
                    try (var image = PortModelGameTests.class.getResourceAsStream("/assets/biggerreactorsrework/textures/" + id.split(":")[1] + ".png")) {
                        h.assertTrue(image != null, "missing port texture: " + id);
                    }
                }
            }
        }
        var faces = asset("models/block/assembled_port.json").getAsJsonObject("textures");
        h.assertTrue(faces.get("north").getAsString().equals("#front") && faces.get("south").getAsString().equals("#front"), "front/back connection missing");
        for (String side : new String[]{"east","west","up","down"}) h.assertTrue(faces.get(side).getAsString().equals("#side"), "side template missing");
        h.succeed();
    }
}
