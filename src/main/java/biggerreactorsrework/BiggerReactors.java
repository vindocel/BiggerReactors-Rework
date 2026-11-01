package biggerreactorsrework;

import biggerreactorsrework.compat.opencomputers.OpenComputersCompat;
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
        biggerreactorsrework.registries.ModParticles.register(modBus);
        biggerreactorsrework.worldgen.UraniumGeneration.register(modBus);
        biggerreactorsrework.advancement.ReworkAdvancements.register(modBus);
        biggerreactorsrework.registries.CoolantComponents.register(modBus);
        version = container.getModInfo().getVersion().toString();
        new Registry(modid, BiggerReactors.class.getPackageName(), modBus, new ReferenceArrayList<>(), new ReferenceArrayList<>());
        biggerreactorsrework.machine.recipe.ReprocessingRecipes.register(modBus);
        modBus.addListener(biggerreactorsrework.registries.MaterialDataMaps::register);
        modBus.addListener(biggerreactorsrework.data.ModDataGeneration::gather);
        CoreEvents.init();
        biggerreactorsrework.registries.CoolantSync.init();
        modBus.addListener(biggerreactorsrework.calculator.service.CalculatorNetwork::register);
        biggerreactorsrework.calculator.service.CalculatorService.init();
        NeoForge.EVENT_BUS.addListener(this::onReloadData);
        // Modified 2026-10-29: optional pickup protection for assembled multiblocks.
        if (ModList.get().isLoaded("carryon")) {
            biggerreactorsrework.compat.carryon.CarryOnCompat.init();
        }
        // Modified 2026-10-31: both optional computer integrations use the same port blocks.
        if (ModList.get().isLoaded("opencomputers")) {
            OpenComputersCompat.init(modBus);
        }
        if (ModList.get().isLoaded("computercraft")) {
            biggerreactorsrework.compat.cctweaked.CCTweakedCompat.init(modBus);
        }
        if (FMLEnvironment.dist.isClient()) {
            BiggerReactorsClient.registerConfigScreen(container);
            BiggerReactorsClient.init(modBus);
        }
    }

    public void onReloadData(final ReloadDataEvent reloadDataEvent) {
        // Calculations read these registries: stop them before the registries are rebuilt.
        biggerreactorsrework.calculator.service.CalculatorService.invalidate();
        ReactorModeratorRegistry.loadRegistry();
        TurbineCoilRegistry.loadRegistry();
        FluidTransitionRegistry.loadRegistry();
    }

    public static String modVersion() {
        return version;
    }
}
