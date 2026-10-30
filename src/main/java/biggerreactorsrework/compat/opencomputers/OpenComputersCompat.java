package biggerreactorsrework.compat.opencomputers;

// Added 2026-10-30: optional native OpenComputers cable components.
import biggerreactorsrework.compat.ComputerPortState;
import biggerreactorsrework.core.registry.RegisterTile;
import biggerreactorsrework.multiblocks.reactor.tiles.ReactorComputerPortTile;
import biggerreactorsrework.multiblocks.turbine.tiles.TurbineComputerPortTile;
import biggerreactorsrework.multiblocks.heatexchanger.tiles.HeatExchangerComputerPortTile;
import li.cil.oc.api.network.Environment;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class OpenComputersCompat {
    // The API classifier excludes the Scala implementation's capability accessor.
    // NeoForge interns capabilities by ID and type: this is the native OC capability.
    public static final BlockCapability<Environment, Direction> ENVIRONMENT = BlockCapability.createSided(
            ResourceLocation.fromNamespaceAndPath("opencomputers", "environment"), Environment.class);

    public static void init(IEventBus bus) {
        ComputerPortState.factory = tile -> {
            if (tile instanceof ReactorComputerPortTile reactor) return new ReactorComponent(reactor);
            if (tile instanceof TurbineComputerPortTile turbine) return new TurbineComponent(turbine);
            if (tile instanceof HeatExchangerComputerPortTile exchanger) return new HeatExchangerComponent(exchanger);
            throw new IllegalArgumentException("Unsupported computer port");
        };
        bus.addListener(OpenComputersCompat::register);
    }
    private static <T extends BlockEntity> BlockEntityType<T> type(BlockEntityType.BlockEntitySupplier<T> supplier) {
        return ((RegisterTile.Producer<T>) supplier).TYPE;
    }
    private static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(ENVIRONMENT, type(ReactorComputerPortTile.SUPPLIER),
                (tile, side) -> (Environment) tile.computerPortState().component());
        event.registerBlockEntity(ENVIRONMENT, type(TurbineComputerPortTile.SUPPLIER),
                (tile, side) -> (Environment) tile.computerPortState().component());
        event.registerBlockEntity(ENVIRONMENT, type(HeatExchangerComputerPortTile.SUPPLIER),
                (tile, side) -> (Environment) tile.computerPortState().component());
    }
    private OpenComputersCompat() {}
}
