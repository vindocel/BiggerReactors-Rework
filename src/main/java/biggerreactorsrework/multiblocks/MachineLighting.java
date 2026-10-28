package biggerreactorsrework.multiblocks;

import biggerreactorsrework.core.multiblock.IAssemblyStateBlock;
import biggerreactorsrework.fluids.Steam;
import biggerreactorsrework.machine.blocks.CyaniteReprocessor;
import biggerreactorsrework.multiblocks.battery.blocks.BatteryTerminal;
import biggerreactorsrework.multiblocks.battery.state.BatteryActivity;
import biggerreactorsrework.multiblocks.heatexchanger.blocks.HeatExchangerCondenserChannelBlock;
import biggerreactorsrework.multiblocks.heatexchanger.blocks.HeatExchangerTerminalBlock;
import biggerreactorsrework.multiblocks.reactor.blocks.ReactorFuelRod;
import biggerreactorsrework.multiblocks.reactor.blocks.ReactorTerminal;
import biggerreactorsrework.multiblocks.reactor.state.ReactorActivity;
import biggerreactorsrework.multiblocks.turbine.blocks.TurbineTerminal;
import biggerreactorsrework.multiblocks.turbine.state.TurbineActivity;
import biggerreactorsrework.registries.FluidTransitionRegistry;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;

/** Added 2026-10-27: modest world light driven by visible machine state, without changing production. */
public final class MachineLighting {
    public static final int TERMINAL = 7;
    public static final int FUEL = 7;
    public static final int HOT_FLUID = 6;
    public static final int PROCESSOR = 6;

    private MachineLighting() { }

    public static int emission(BlockState state) {
        var block = state.getBlock();
        if (block instanceof ReactorFuelRod) return state.getValue(ReactorFuelRod.FUEL_HEIGHT_PROPERTY) > 0 ? FUEL : 0;
        if (block instanceof CyaniteReprocessor) return state.getValue(CyaniteReprocessor.ENABLED) ? PROCESSOR : 0;
        if (block instanceof HeatExchangerCondenserChannelBlock) return state.getValue(BlockStateProperties.LIT) ? HOT_FLUID : 0;
        if (!state.hasProperty(IAssemblyStateBlock.ASSEMBLED) || !state.getValue(IAssemblyStateBlock.ASSEMBLED)) return 0;
        if (block instanceof ReactorTerminal) return state.getValue(ReactorActivity.REACTOR_ACTIVITY_ENUM_PROPERTY) == ReactorActivity.ACTIVE ? TERMINAL : 0;
        if (block instanceof TurbineTerminal) return state.getValue(TurbineActivity.TURBINE_STATE_ENUM_PROPERTY) == TurbineActivity.ACTIVE ? TERMINAL : 0;
        if (block instanceof BatteryTerminal) return state.getValue(BatteryActivity.BATTERY_STATE_ENUM_PROPERTY) == BatteryActivity.ACTIVE ? TERMINAL : 0;
        if (block instanceof HeatExchangerTerminalBlock) return TERMINAL;
        return 0;
    }

    public static boolean hotFluid(Fluid fluid) {
        return fluid != Steam.INSTANCE && (fluid == biggerreactorsrework.fluids.HeatedCoolant.INSTANCE
                || FluidTransitionRegistry.gasTransition(fluid) != null);
    }
}
