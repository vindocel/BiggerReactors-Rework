package biggerreactorsrework.gametest;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.util.GuideStructures;
import biggerreactorsrework.multiblocks.reactor.tiles.ReactorBaseTile;
import biggerreactorsrework.multiblocks.reactor.ReactorMultiblockController;
import biggerreactorsrework.multiblocks.reactor.state.ReactorActivity;
import biggerreactorsrework.multiblocks.turbine.blocks.TurbineRedstonePort;
import biggerreactorsrework.multiblocks.turbine.tiles.TurbineRedstonePortTile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import java.util.Map;
import static biggerreactorsrework.core.multiblock.IAssemblyStateBlock.ASSEMBLED;

@GameTestHolder(BiggerReactors.modid)
@PrefixGameTestTemplate(false)
public final class LifecycleGameTests {
    @GameTest(template="empty_7x7x7",timeoutTicks=200)
    public static void unloadKeepsIntentAndBreakCancelsIt(GameTestHelper h){
        GuideStructures.reactor(3,false).parts().forEach(h::setBlock);
        BlockPos terminal=new BlockPos(0,1,1),corner=BlockPos.ZERO;
        var original=new ReactorMultiblockController[1];
        var part=new ReactorBaseTile[1];
        h.startSequence()
            .thenWaitUntil(()->h.assertTrue(h.getBlockState(terminal).getValue(ASSEMBLED),"reactor not assembled"))
            .thenExecute(()->{
                part[0]=(ReactorBaseTile)h.getBlockEntity(corner);original[0]=part[0].controller();
                original[0].setActive(ReactorActivity.ACTIVE);part[0].multiblockModule().stopTicking();
            })
            .thenIdle(4)
            .thenExecute(()->{
                h.assertTrue(original[0].isActive(),"unload lost active intent");
                var restored=new ReactorMultiblockController(h.getLevel());restored.read(original[0].write());
                h.assertTrue(restored.isActive(),"saved intent did not restore");
                part[0].multiblockModule().startTicking();
            })
            .thenWaitUntil(()->h.assertTrue(h.getBlockState(terminal).getValue(ASSEMBLED),"reactor did not reassemble"))
            .thenExecute(()->h.setBlock(corner,Blocks.AIR))
            .thenIdle(4)
            .thenExecute(()->h.assertFalse(((ReactorBaseTile)h.getBlockEntity(terminal)).controller().isActive(),"manual break kept active intent"))
            .thenSucceed();
    }
    @GameTest(template="empty_7x7x7",timeoutTicks=200)
    public static void turbineRedstoneCommandsAndSensors(GameTestHelper h){
        GuideStructures.turbine().parts().forEach(h::setBlock);
        BlockPos port=new BlockPos(4,2,2),signal=port.east();h.setBlock(port,TurbineRedstonePort.INSTANCE);
        var tile=(TurbineRedstonePortTile)h.getBlockEntity(port);
        h.startSequence()
            .thenWaitUntil(()->h.assertTrue(h.getBlockState(port).getValue(ASSEMBLED),"port not assembled"))
            .thenExecute(()->{
                h.assertTrue(tile.configure(Map.of("mode",1,"behavior",0,"low",0d,"high",0d)),"valid level config rejected");h.setBlock(signal,Blocks.REDSTONE_BLOCK);
            })
            .thenIdle(3)
            .thenExecute(()->{h.assertTrue(tile.controller().simulation().active(),"level did not activate turbine");tile.controller().setActive(false);})
            .thenIdle(3)
            .thenExecute(()->{h.assertFalse(tile.controller().simulation().active(),"unchanged level overrode newer command");h.setBlock(signal,Blocks.AIR);})
            .thenIdle(2)
            .thenExecute(()->h.setBlock(signal,Blocks.REDSTONE_BLOCK))
            .thenIdle(2)
            .thenExecute(()->{
                h.assertTrue(tile.controller().simulation().active(),"changed level not reapplied");
                h.assertFalse(tile.configure(Map.of("mode",4,"behavior",4,"low",10d,"high",1d)),"inverted interval accepted");
                h.assertTrue(tile.configure(Map.of("mode",4,"behavior",2,"low",0d,"high",0d)),"sensor config rejected");
            })
            .thenIdle(2)
            .thenExecute(()->{
                h.assertTrue(tile.emitting(Direction.WEST),"inclusive zero RPM threshold not emitted outward");
                h.setBlock(BlockPos.ZERO,Blocks.AIR);
            })
            .thenIdle(3)
            .thenExecute(()->h.assertFalse(tile.emitting(Direction.WEST),"sensor stayed powered after disassembly"))
            .thenSucceed();
    }
}
