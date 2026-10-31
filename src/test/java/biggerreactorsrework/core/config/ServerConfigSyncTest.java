package biggerreactorsrework.core.config;

// Modified 2026-10-30: real server configuration nodes must round-trip and restore local preferences.

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ServerConfigSyncTest {
    public static class Values {
        @ConfigValue(range = "[1,)") public int capacity = 20;
        @ConfigValue public boolean enabled = true;
    }

    private ConfigRegistration registration(Values values) {
        return new ConfigRegistration(values, "example", "rules", "folder", "", ConfigFormat.TOML,
                ConfigType.SERVER, ConfigType.SERVER, false, List.of(), List.of());
    }

    @Test void authoritativeValuesApplyWithoutWritingLocalFilesAndRestoreOnDisconnect() {
        var local = new Values();
        var remote = new Values(); remote.capacity = 500; remote.enabled = false;
        var client = registration(local);
        client.loadRemoteConfig(registration(remote).rootConfigSpecNode.generateSyncElement(), false);
        assertEquals(500, local.capacity);
        assertFalse(local.enabled);
        client.unloadRemoteConfig();
        assertEquals(20, local.capacity);
        assertTrue(local.enabled);
    }
}
