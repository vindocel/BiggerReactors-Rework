package biggerreactorsrework.audio;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MachineAudioResourcesTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/biggerreactorsrework");
    @Test void nativeAttenuationAndClientTrackingAgreeWithoutChangingVolume() throws Exception {
        var sounds = JsonParser.parseString(Files.readString(ASSETS.resolve("sounds.json"))).getAsJsonObject();
        for (var entry : sounds.entrySet()) {
            int kind = entry.getKey().contains("turbine") ? 2 : entry.getKey().contains("boiler") ? 1 : 0;
            var row = entry.getValue().getAsJsonObject().getAsJsonArray("sounds").get(0).getAsJsonObject();
            assertEquals(MachineAudioRange.blocks(kind), row.get("attenuation_distance").getAsInt());
            assertTrue(MachineAudioRange.trackingBlocks(kind) > row.get("attenuation_distance").getAsInt());
            assertEquals(1.0, row.get("volume").getAsDouble());
            assertTrue(Files.exists(ASSETS.resolve("sounds").resolve(row.get("name").getAsString().split(":")[1] + ".ogg")));
        }
        assertEquals(25,MachineAudioRange.blocks(0));
        assertEquals(25,MachineAudioRange.blocks(1));
        assertEquals(25,MachineAudioRange.blocks(2));
    }
    @Test void fullRodVolumesMeetTheNextBlockWithoutMicroGaps() throws Exception {
        for (String material : new String[]{"fuel","waste"}) {
            var root = ASSETS.resolve("models/block/reactor/fuel_rod").resolve(material);
            var full = JsonParser.parseString(Files.readString(root.resolve("level_16.json"))).getAsJsonObject()
                    .getAsJsonArray("elements").get(0).getAsJsonObject();
            assertEquals(16,full.getAsJsonArray("to").get(1).getAsDouble());
            assertEquals(0,full.getAsJsonArray("from").get(1).getAsDouble());
            for (int level = 1; level < 16; level++) {
                var part = JsonParser.parseString(Files.readString(root.resolve("level_"+level+".json"))).getAsJsonObject()
                        .getAsJsonArray("elements").get(0).getAsJsonObject();
                assertTrue(part.getAsJsonArray("to").get(1).getAsDouble() < level);
            }
        }
    }
}
