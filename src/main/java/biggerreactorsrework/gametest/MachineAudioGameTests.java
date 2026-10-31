package biggerreactorsrework.gametest;

// Added 2026-10-28: snapshots must survive chunk synchronization without GUI access.
import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.audio.MachineAudioState;
import biggerreactorsrework.core.modular.tile.ModularTile;
import biggerreactorsrework.multiblocks.reactor.blocks.ReactorTerminal;
import biggerreactorsrework.multiblocks.reactor.tiles.ReactorTerminalTile;
import biggerreactorsrework.multiblocks.reactor.state.ReactorActivity;
import biggerreactorsrework.multiblocks.heatexchanger.tiles.HeatExchangerTerminalTile;
import biggerreactorsrework.util.GuideStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import static biggerreactorsrework.core.multiblock.IAssemblyStateBlock.ASSEMBLED;

public final class MachineAudioGameTests {
    @GameTest(template="empty_7x7x7",timeoutTicks=200)
    public static void reactorTerminalsShareAudioAndDisassemblyStopsIt(GameTestHelper h) {
        GuideStructures.reactor(3,false).parts().forEach(h::setBlock);
        var a=new BlockPos(0,1,1);var b=new BlockPos(1,1,2);
        h.setBlock(b,ReactorTerminal.INSTANCE);
        h.startSequence().thenWaitUntil(()->h.assertTrue(h.getBlockState(a).getValue(ASSEMBLED),"not assembled"))
            .thenExecute(()->{
                var tile=(ReactorTerminalTile)GameTestSupport.be(h,a);
                tile.controller().refuel(10000,false);
                tile.controller().setActive(ReactorActivity.ACTIVE);
            })
            .thenWaitUntil(()->h.assertTrue(((ModularTile)GameTestSupport.be(h,a)).machineAudio.running(),"running reactor has no audio snapshot"))
            .thenExecute(()->{
                var first=(ModularTile)GameTestSupport.be(h,a);var second=(ModularTile)GameTestSupport.be(h,b);
                h.assertTrue(first.machineAudio.equals(second.machineAudio),"terminals disagree on machine identity");
                var full=first.getUpdateTag(h.getLevel().registryAccess());
                var incremental=first.getUpdatePacket().getTag();
                h.assertTrue(MachineAudioState.load(full.getCompoundOrEmpty("machine_audio")).equals(first.machineAudio),"chunk snapshot missing");
                h.assertTrue(MachineAudioState.load(incremental.getCompoundOrEmpty("machine_audio")).equals(first.machineAudio),"update snapshot missing");
                h.setBlock(new BlockPos(2,2,2),Blocks.AIR);
            })
            .thenWaitUntil(()->h.assertFalse(((ModularTile)GameTestSupport.be(h,a)).machineAudio.running(),"disassembled machine retained running sound"))
            .thenSucceed();
    }
    @GameTest(template="empty_7x7x7",timeoutTicks=120)
    public static void emptyBoilerAndMalformedSnapshots(GameTestHelper h) {
        GuideStructures.exchanger().parts().forEach(h::setBlock);var p=new BlockPos(1,1,0);
        h.startSequence().thenWaitUntil(()->h.assertTrue(h.getBlockState(p).getValue(ASSEMBLED),"not assembled"))
            .thenIdle(12).thenExecute(()->{
                var tile=(HeatExchangerTerminalTile)GameTestSupport.be(h,p);
                h.assertTrue(tile.machineAudio.kind()==1 && !tile.machineAudio.running(),"idle boiler should be silent");
                var saved=tile.saveWithFullMetadata(h.getLevel().registryAccess());
                h.assertFalse(saved.contains("machine_audio"),"transient sound state persisted");
                var tag=tile.machineAudio.save();tag.putDouble("rpm",Double.NaN);
                h.assertTrue(MachineAudioState.load(tag).equals(MachineAudioState.EMPTY),"nonfinite speed accepted");
                var reloaded=new HeatExchangerTerminalTile(tile.getType(),tile.getBlockPos(),tile.getBlockState());
                GameTestSupport.update(reloaded,tile.getUpdateTag(h.getLevel().registryAccess()),h);
                h.assertTrue(reloaded.machineAudio.equals(tile.machineAudio),"initial chunk sync lost identity");
            }).thenSucceed();
    }
}
