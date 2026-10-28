package biggerreactorsrework;

import biggerreactorsrework.compat.computercraft.ComputerCraftCompat;
import biggerreactorsrework.core.CoreEvents;
import net.neoforged.fml.ModList;
import biggerreactorsrework.core.event.ReloadDataEvent;
import biggerreactorsrework.core.registry.Registry;
import biggerreactorsrework.registries.FluidTransitionRegistry;
import biggerreactorsrework.registries.ReactorModeratorRegistry;
import biggerreactorsrework.registries.TurbineCoilRegistry;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(BiggerReactors.modid)
public class BiggerReactors {

    public static final String modid = "biggerreactorsrework";

    public static final Logger LOGGER = LogManager.getLogger();
    public static final boolean LOG_DEBUG = LOGGER.isDebugEnabled();

    private static String version;

    public BiggerReactors(IEventBus modBus, ModContainer container) {
        // Modified 2026-10-28: common events, with playback isolated to the client.
        biggerreactorsrework.registries.MachineSounds.register(modBus);
        biggerreactorsrework.registries.CoolantComponents.register(modBus);
        version = container.getModInfo().getVersion().toString();
        new Registry(modid, BiggerReactors.class.getPackageName(), modBus, new ReferenceArrayList<>(), new ReferenceArrayList<>());
        biggerreactorsrework.machine.recipe.ReprocessingRecipes.register(modBus);
        modBus.addListener(biggerreactorsrework.registries.MaterialDataMaps::register);
        modBus.addListener(biggerreactorsrework.data.ModDataGeneration::gather);
        CoreEvents.init();
        biggerreactorsrework.registries.CoolantSync.init();
        NeoForge.EVENT_BUS.addListener(this::onReloadData);
        if (ModList.get().isLoaded("computercraft")) {
            ComputerCraftCompat.init(modBus);
        }
        if (FMLEnvironment.dist.isClient()) {
            BiggerReactorsClient.init(modBus);
        }
    }

    public void onReloadData(final ReloadDataEvent reloadDataEvent) {
        ReactorModeratorRegistry.loadRegistry();
        TurbineCoilRegistry.loadRegistry();
        FluidTransitionRegistry.loadRegistry();
    }

    public static String modVersion() {
        return version;
    }
}
