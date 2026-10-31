package biggerreactorsrework.compat.jade;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.core.energy.IEnergyHandler;
import biggerreactorsrework.machine.blocks.CyaniteReprocessor;
import biggerreactorsrework.machine.tiles.CyaniteReprocessorTile;
import biggerreactorsrework.multiblocks.battery.blocks.BatteryBaseBlock;
import biggerreactorsrework.multiblocks.battery.tiles.BatteryBaseTile;
import biggerreactorsrework.multiblocks.reactor.blocks.ReactorPowerTap;
import biggerreactorsrework.multiblocks.reactor.tiles.ReactorPowerTapTile;
import biggerreactorsrework.multiblocks.turbine.blocks.TurbinePowerTap;
import biggerreactorsrework.multiblocks.turbine.tiles.TurbinePowerTapTile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import biggerreactorsrework.core.transfer.EnergyStore;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.view.HideThingsExtensionProvider;

/**
 * Jade integration: replaces Jade's energy line with the mod's own energy bar on the
 * Cyanite Reprocessor, the reactor and turbine power taps and every block of an assembled battery.
 */
@WailaPlugin
public class BiggerReactorsJadePlugin implements IWailaPlugin {
    static final Identifier UID = Identifier.fromNamespaceAndPath(BiggerReactors.modid, "energy_bar");
    static final String ENERGY = "energy";
    static final String CAPACITY = "capacity";

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(EnergyData.INSTANCE, CyaniteReprocessorTile.class);
        registration.registerBlockDataProvider(EnergyData.INSTANCE, ReactorPowerTapTile.class);
        registration.registerBlockDataProvider(EnergyData.INSTANCE, TurbinePowerTapTile.class);
        registration.registerBlockDataProvider(EnergyData.INSTANCE, BatteryBaseTile.class);
        // The mod's energy bar replaces Jade's own energy line.
        registration.registerEnergyStorage(HideThingsExtensionProvider.instance(), CyaniteReprocessorTile.class);
        registration.registerEnergyStorage(HideThingsExtensionProvider.instance(), ReactorPowerTapTile.class);
        registration.registerEnergyStorage(HideThingsExtensionProvider.instance(), TurbinePowerTapTile.class);
        registration.registerEnergyStorage(HideThingsExtensionProvider.instance(), BatteryBaseTile.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(EnergyBarComponent.INSTANCE, CyaniteReprocessor.class);
        registration.registerBlockComponent(EnergyBarComponent.INSTANCE, ReactorPowerTap.class);
        registration.registerBlockComponent(EnergyBarComponent.INSTANCE, TurbinePowerTap.class);
        registration.registerBlockComponent(EnergyBarComponent.INSTANCE, BatteryBaseBlock.class);
    }

    enum EnergyData implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof CyaniteReprocessorTile tile) {
                EnergyStore storage = tile.energyFor(null);
                data.putLong(ENERGY, storage.getEnergyStored());
                data.putLong(CAPACITY, storage.getMaxEnergyStored());
            } else if (accessor.getBlockEntity() instanceof BatteryBaseTile tile) {
                // Any block of a battery shows the whole battery's charge, but only while it is assembled.
                if (tile.isBatteryAssembled()) {
                    data.putLong(ENERGY, tile.controller().stored());
                    data.putLong(CAPACITY, tile.controller().capacity());
                }
            } else if (accessor.getBlockEntity() instanceof IEnergyHandler handler && handler.maxEnergyStored() > 0) {
                // Power taps show the battery of their reactor or turbine (nothing while it is disassembled).
                data.putLong(ENERGY, handler.energyStored());
                data.putLong(CAPACITY, handler.maxEnergyStored());
            }
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
