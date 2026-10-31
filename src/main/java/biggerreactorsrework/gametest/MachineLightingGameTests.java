package biggerreactorsrework.gametest;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.core.util.Util;
import biggerreactorsrework.util.GuideStructures;
import biggerreactorsrework.fluids.*;
import biggerreactorsrework.items.ingots.CyaniteIngot;
import biggerreactorsrework.machine.blocks.CyaniteReprocessor;
import biggerreactorsrework.machine.tiles.CyaniteReprocessorTile;
import biggerreactorsrework.multiblocks.battery.blocks.BatteryTerminal;
import biggerreactorsrework.multiblocks.battery.state.BatteryActivity;
import biggerreactorsrework.multiblocks.heatexchanger.blocks.HeatExchangerTerminalBlock;
import biggerreactorsrework.multiblocks.heatexchanger.tiles.HeatExchangerBaseTile;
import biggerreactorsrework.multiblocks.reactor.blocks.*;
import biggerreactorsrework.multiblocks.reactor.state.ReactorActivity;
import biggerreactorsrework.multiblocks.turbine.blocks.TurbineTerminal;
import biggerreactorsrework.multiblocks.turbine.state.TurbineActivity;
import biggerreactorsrework.registries.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import biggerreactorsrework.core.transfer.FluidTanks.FluidAction;
import net.neoforged.neoforge.gametest.*;
import static biggerreactorsrework.core.multiblock.IAssemblyStateBlock.ASSEMBLED;

/** Added 2026-10-27: state emission, real processing/coolant transitions and batched relighting. */
public final class MachineLightingGameTests {
    @GameTest(template="empty_7x7x7")
    public static void terminalsAndFuelUseVisibleOperatingStates(GameTestHelper h) {
        var reactor=ReactorTerminal.INSTANCE.defaultBlockState().setValue(ReactorActivity.REACTOR_ACTIVITY_ENUM_PROPERTY,ReactorActivity.ACTIVE);
        var turbine=TurbineTerminal.INSTANCE.defaultBlockState().setValue(TurbineActivity.TURBINE_STATE_ENUM_PROPERTY,TurbineActivity.ACTIVE);
        var battery=BatteryTerminal.INSTANCE.defaultBlockState().setValue(BatteryActivity.BATTERY_STATE_ENUM_PROPERTY,BatteryActivity.ACTIVE);
        for(var state:java.util.List.of(reactor,turbine,battery,HeatExchangerTerminalBlock.INSTANCE.defaultBlockState())) {
            h.assertTrue(state.getLightEmission()==0,"unassembled terminal emits light");
            h.assertTrue(state.setValue(ASSEMBLED,true).getLightEmission()==7,"operating terminal does not emit level 7");
        }
        h.assertTrue(ReactorTerminal.INSTANCE.defaultBlockState().setValue(ASSEMBLED,true).getLightEmission()==0,"inactive reactor emits light");
        h.assertTrue(TurbineTerminal.INSTANCE.defaultBlockState().setValue(ASSEMBLED,true).getLightEmission()==0,"inactive turbine emits light");
        h.assertTrue(BatteryTerminal.INSTANCE.defaultBlockState().setValue(ASSEMBLED,true).getLightEmission()==0,"idle battery emits light");
        var rod=ReactorFuelRod.INSTANCE.defaultBlockState();
        h.assertTrue(rod.getLightEmission()==0 && rod.setValue(ReactorFuelRod.WASTE_HEIGHT_PROPERTY,16).getLightEmission()==0,"empty/waste-only rod glows");
        h.assertTrue(rod.setValue(ReactorFuelRod.FUEL_HEIGHT_PROPERTY,1).getLightEmission()==7,"visible fuel does not emit light");
        h.succeed();
    }

    @GameTest(template="empty_7x7x7")
    public static void industrialLightFollowsProcessingAndPauses(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);h.setBlock(pos,CyaniteReprocessor.INSTANCE);
        var tile=(CyaniteReprocessorTile)GameTestSupport.be(h,pos);tile.tick();
        tile.energyFor(null).receiveEnergy(500,false);
        tile.fluidFor(null).fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE);
        tile.itemHandlerFor(null).insertItem(0,new ItemStack(CyaniteIngot.INSTANCE,2),false);tile.tick();
        h.assertTrue(h.getBlockState(pos).getLightEmission()==6,"processing face does not emit light");
        tile.getItemHandler().setStackInSlot(0,ItemStack.EMPTY);tile.tick();
        h.assertTrue(h.getBlockState(pos).getLightEmission()==0,"paused processor retained light");h.succeed();
    }

    @GameTest(template="empty_7x7x7",timeoutTicks=150)
    public static void hotChannelLightTracksConversionDrainingAndDisassembly(GameTestHelper h) {
        GuideStructures.exchanger().parts().forEach(h::setBlock);var terminal=new BlockPos(1,1,0);
        h.startSequence().thenWaitUntil(()->h.assertTrue(h.getBlockState(terminal).getValue(ASSEMBLED),"exchanger not assembled"))
            .thenExecute(()->{
                var c=((HeatExchangerBaseTile)GameTestSupport.be(h,terminal)).controller();
                var profile=FluidTransitionRegistry.liquidTransition(LiquidObsidian.INSTANCE);
                h.assertTrue(c.condenserTank.fill(HeatedCoolant.INSTANCE,CoolantComponents.heated(profile),1000,false)==1000,"hot coolant rejected");
                c.onReassembled();
                for(var part:c.condenserChannels)h.assertTrue(part.getBlockState().getLightEmission()==6,"hot path is dark");
                c.condenserTank.absorbRF(profile.latentHeat*500);c.onReassembled();
                for(var part:c.condenserChannels) {
                    boolean hot=part.coolantPathIndex<c.condenserChannels.size()/2.0;
                    h.assertTrue(part.getBlockState().getLightEmission()==(hot?6:0),"light does not follow the hot half of the path");
                }
                c.condenserTank.dumpTank(0);c.condenserTank.dumpTank(1);c.onReassembled();
                for(var part:c.condenserChannels)h.assertTrue(part.getBlockState().getLightEmission()==0,"empty channel retains light");
                c.condenserTank.fill(Steam.INSTANCE,DataComponentPatch.EMPTY,1000,false);c.onReassembled();
                for(var part:c.condenserChannels)h.assertTrue(part.getBlockState().getLightEmission()==0,"steam incorrectly emits light");
                c.condenserTank.dumpTank(0);c.condenserTank.dumpTank(1);
                c.condenserTank.fill(HeatedCoolant.INSTANCE,CoolantComponents.heated(profile),1000,false);c.onReassembled();c.onDisassembled();
                for(var part:c.condenserChannels)h.assertTrue(!part.getBlockState().getValue(BlockStateProperties.LIT),"disassembly retained channel light");
            }).thenSucceed();
    }

    @GameTest(template="empty_7x7x7",timeoutTicks=100)
    public static void batchedStateUpdatesRefreshWorldLight(GameTestHelper h) {
        var local=new BlockPos(3,3,3);var pos=h.absolutePos(local);
        var off=Blocks.LIGHT.defaultBlockState().setValue(BlockStateProperties.LEVEL,0);
        var on=off.setValue(BlockStateProperties.LEVEL,7);h.setBlock(local,off);
        // Isolate from the luminous result markers of neighboring GameTests.
        for(var side:net.minecraft.core.Direction.values())h.setBlock(local.relative(side),Blocks.BEDROCK);
        h.startSequence().thenIdle(5).thenExecute(()->Util.setBlockStates(java.util.Map.of(pos,on),h.getLevel()))
            .thenWaitUntil(()->h.assertTrue(h.getLevel().getBrightness(net.minecraft.world.level.LightLayer.BLOCK,pos)==7,"map batch did not relight"))
            .thenExecute(()->{var states=new Long2ObjectLinkedOpenHashMap<BlockState>();states.put(pos.asLong(),off);Util.setBlockStates(states,h.getLevel());})
            .thenWaitUntil(()->h.assertTrue(h.getLevel().getBrightness(net.minecraft.world.level.LightLayer.BLOCK,pos)==0,"long batch left stale light: "+h.getLevel().getBrightness(net.minecraft.world.level.LightLayer.BLOCK,pos)))
            .thenSucceed();
    }
}
