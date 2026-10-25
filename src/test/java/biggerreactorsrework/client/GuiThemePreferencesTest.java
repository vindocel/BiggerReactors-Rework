package biggerreactorsrework.client;

import java.io.StringReader;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GuiThemePreferencesTest {
    @Test void lightIsTheDefaultForAllMachines() {
        var themes = new GuiThemePreferences();
        assertTrue(themes.applyToAll());
        assertFalse(themes.dark("world|dimension|1,2,3"));
    }

    @Test void localChoicesAreIsolatedAcrossMachinesWorldsAndDimensions() {
        var themes = new GuiThemePreferences();
        String machine = "world1|overworld|1,2,3";
        themes.setApplyToAll(machine, false);
        themes.choose(machine, true);
        assertTrue(themes.dark(machine));
        assertFalse(themes.dark("world1|overworld|4,2,3"));
        assertFalse(themes.dark("world1|nether|1,2,3"));
        assertFalse(themes.dark("world2|overworld|1,2,3"));
    }

    @Test void applyingToAllUsesCurrentMachineChoiceImmediately() {
        var themes = new GuiThemePreferences();
        themes.setApplyToAll("reactor", false);
        themes.choose("reactor", true);
        themes.setApplyToAll("reactor", true);
        assertTrue(themes.dark("turbine"));
        assertTrue(themes.dark("new_machine"));
        themes.choose("turbine", false);
        assertFalse(themes.dark("reactor"));
    }

    @Test void disablingAllKeepsCurrentLookAndTheSharedFallback() {
        var themes = new GuiThemePreferences();
        themes.choose("reactor", true);
        themes.setApplyToAll("reactor", false);
        assertTrue(themes.dark("reactor"));
        assertTrue(themes.dark("new_machine"));
        themes.choose("reactor", false);
        assertFalse(themes.dark("reactor"));
        assertTrue(themes.dark("new_machine"));
    }

    @Test void independentAndGlobalChoicesSurviveRestart() {
        var themes = new GuiThemePreferences();
        themes.choose("reactor", true);
        themes.setApplyToAll("reactor", false);
        themes.choose("turbine", false);
        var writer = new StringWriter();
        themes.write(writer);
        var loaded = GuiThemePreferences.read(new StringReader(writer.toString()));
        assertFalse(loaded.applyToAll());
        assertTrue(loaded.dark("reactor"));
        assertFalse(loaded.dark("turbine"));
        assertTrue(loaded.dark("new_machine"));
        loaded.setApplyToAll("turbine", true);
        writer = new StringWriter();
        loaded.write(writer);
        loaded = GuiThemePreferences.read(new StringReader(writer.toString()));
        assertTrue(loaded.applyToAll());
        assertFalse(loaded.dark("reactor"));
    }

    @Test void emptyPreferencesUseDefaultsAndUnknownThemeIsRejected() {
        assertFalse(GuiThemePreferences.read(new StringReader("{}")).dark("reactor"));
        assertThrows(IllegalArgumentException.class, () -> GuiThemePreferences.read(
                new StringReader("{\"machines\":{\"reactor\":\"invalid\"}}")));
    }
}
