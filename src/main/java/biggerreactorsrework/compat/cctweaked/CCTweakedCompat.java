package biggerreactorsrework.compat.cctweaked;

import biggerreactorsrework.core.registry.RegisterTile;
import biggerreactorsrework.multiblocks.reactor.tiles.ReactorComputerPortTile;
import biggerreactorsrework.multiblocks.turbine.tiles.TurbineComputerPortTile;
import biggerreactorsrework.multiblocks.heatexchanger.tiles.HeatExchangerComputerPortTile;
import dan200.computercraft.api.peripheral.*;
import net.minecraft.world.level.block.entity.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Added 2026-10-31: register the existing three computer ports with the native CC:Tweaked capability. */
public final class CCTweakedCompat {
    public static void init(IEventBus bus) { bus.addListener(CCTweakedCompat::register); }
    private static <T extends BlockEntity> BlockEntityType<T> type(BlockEntityType.BlockEntitySupplier<T> supplier) {
        return ((RegisterTile.Producer<T>)supplier).TYPE;
    }
    private static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(PeripheralCapability.get(),type(ReactorComputerPortTile.SUPPLIER),(tile,side)->new ReactorPeripheral(tile));
        event.registerBlockEntity(PeripheralCapability.get(),type(TurbineComputerPortTile.SUPPLIER),(tile,side)->new TurbinePeripheral(tile));
        event.registerBlockEntity(PeripheralCapability.get(),type(HeatExchangerComputerPortTile.SUPPLIER),(tile,side)->new HeatExchangerPeripheral(tile));
    }
    private CCTweakedCompat() { }
}
