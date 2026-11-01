package biggerreactorsrework.compat.kubejs;

import biggerreactorsrework.core.config.PackConfiguration;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.ScriptManager;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.server.ServerScriptManager;
import dev.latvian.mods.kubejs.script.data.GeneratedDataStage;
import net.minecraft.resources.Identifier;

/** Added 2026-11-01: KubeJS alone discovers this optional plugin; the base mod never loads its API classes. */
public final class ReworkKubeJSPlugin implements KubeJSPlugin {
    private static final EventGroup GROUP = EventGroup.of("BiggerReactorsEvents");
    private static final EventHandler CONFIG = GROUP.server("config", () -> ReworkConfigEvent.class);
    private static final EventHandler MATERIALS = GROUP.server("materials", () -> ReworkMaterialsEvent.class);
    @Override public void registerEvents(EventGroupRegistry registry) { registry.register(GROUP); }
    @Override public void afterScriptsLoaded(ScriptManager manager) {
        if (manager.scriptType != ScriptType.SERVER || !(manager instanceof ServerScriptManager server)) return;
        var config = new ReworkConfigEvent(); CONFIG.post(ScriptType.SERVER, config);
        PackConfiguration.setOverrides(config.values());
        var materials = new ReworkMaterialsEvent(); MATERIALS.post(ScriptType.SERVER, materials);
        materials.maps().forEach((map, json) -> server.virtualPacks.get(GeneratedDataStage.LAST)
                .json(Identifier.fromNamespaceAndPath("biggerreactorsrework", "data_maps/" + map), json));
    }
}
