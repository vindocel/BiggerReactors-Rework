package biggerreactorsrework.compat.computercraft;

import biggerreactorsrework.core.registry.RegisterTile;
import biggerreactorsrework.multiblocks.heatexchanger.deps.HeatExchangerPeripheral;
import biggerreactorsrework.multiblocks.heatexchanger.tiles.HeatExchangerComputerPortTile;
import biggerreactorsrework.multiblocks.reactor.deps.ReactorPeripheral;
import biggerreactorsrework.multiblocks.reactor.tiles.ReactorComputerPortTile;
import biggerreactorsrework.multiblocks.turbine.deps.TurbinePeripheral;
import biggerreactorsrework.multiblocks.turbine.tiles.TurbineComputerPortTile;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Optional CC:Tweaked integration: exposes the computer ports as peripherals.
 * Only loaded when CC:Tweaked is present ({@code computercraft} mod id); nothing else in the mod references it.
 */
public final class ComputerCraftCompat {
    private ComputerCraftCompat() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(ComputerCraftCompat::registerCapabilities);
    }

    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity> BlockEntityType<T> typeOf(BlockEntityType.BlockEntitySupplier<T> supplier) {
        final var type = ((RegisterTile.Producer<T>) supplier).TYPE;
        if (type == null) {
            throw new IllegalStateException("block entity type not registered yet");
        }
        return type;
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(PeripheralCapability.get(), typeOf(ReactorComputerPortTile.SUPPLIER), (tile, side) -> {
            if (tile.peripheral == null) {
                final var peripheral = ReactorPeripheral.create(tile::nullableController);
                tile.peripheral = peripheral;
                tile.onAssemblyHook = peripheral::rebuildControlRodList;
            }
            return (ReactorPeripheral) tile.peripheral;
        });
        event.registerBlockEntity(PeripheralCapability.get(), typeOf(TurbineComputerPortTile.SUPPLIER), (tile, side) -> {
            if (tile.peripheral == null) {
                tile.peripheral = TurbinePeripheral.create(tile::nullableController);
            }
            return (TurbinePeripheral) tile.peripheral;
        });
        event.registerBlockEntity(PeripheralCapability.get(), typeOf(HeatExchangerComputerPortTile.SUPPLIER), (tile, side) -> {
            if (tile.peripheral == null) {
                tile.peripheral = HeatExchangerPeripheral.create(tile::nullableController);
            }
            return (HeatExchangerPeripheral) tile.peripheral;
        });
    }
}
