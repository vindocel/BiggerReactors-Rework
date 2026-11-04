package biggerreactorsrework.gametest;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.core.serialization.DataCompound;
import biggerreactorsrework.fluids.Steam;
import biggerreactorsrework.multiblocks.turbine.blocks.TurbineFluidPort;
import biggerreactorsrework.multiblocks.turbine.containers.TurbineTerminalContainer;
import biggerreactorsrework.multiblocks.turbine.tiles.TurbineFluidPortTile;
import biggerreactorsrework.util.FluidTransitionTank;
import biggerreactorsrework.util.GuideStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import static biggerreactorsrework.core.multiblock.IAssemblyStateBlock.ASSEMBLED;

// Added 2026-11-02: turbine water outlets share flow equally when balanced.
public final class TurbineBalancedOutputGameTests {
    @GameTest(template="empty_7x7x7",timeoutTicks=160)
    public static void balancedWaterReachesBothDestinations(GameTestHelper h) {
        GuideStructures.turbine().parts().forEach((pos,state)->h.setBlock(pos.offset(0,0,1),state));
        BlockPos north=new BlockPos(2,1,1),south=new BlockPos(2,1,5),terminal=new BlockPos(0,1,3);
        h.setBlock(north.north(),Blocks.CAULDRON);h.setBlock(south.south(),Blocks.CAULDRON);
        h.startSequence().thenWaitUntil(()->h.assertTrue(h.getBlockState(north).getValue(ASSEMBLED),"turbine not assembled"))
            .thenExecute(()->{
                TurbineFluidPortTile a=GameTestSupport.be(h,north),b=GameTestSupport.be(h,south);
                var c=a.controller();
                var player=h.makeMockPlayer(GameType.CREATIVE);
                var absolute=h.absolutePos(terminal);player.setPos(absolute.getX()+.5,absolute.getY()+1,absolute.getZ()+.5);
                player.containerMenu=new TurbineTerminalContainer(23,absolute,player);
                var request=new DataCompound();request.put("menu",23);request.put("position",absolute.asLong());request.put("request","balancedOutputs");request.put("data",true);
                biggerreactorsrework.core.client.gui.GuiSync.handleServerRequest(request,player);
                h.assertTrue(c.balancedOutputs,"terminal request did not reach the turbine");
                request.put("data","invalid");biggerreactorsrework.core.client.gui.GuiSync.handleServerRequest(request,player);
                h.assertTrue(c.balancedOutputs,"malformed command changed the turbine");
                c.read(c.write());
                h.assertTrue(c.balancedOutputs,"balanced mode did not persist");
                a.fill(Steam.INSTANCE,DataComponentPatch.EMPTY,2000,false);
                ((FluidTransitionTank)c.simulation().fluidTank()).absorbRF(1e9);
                h.assertTrue(c.simulation().fluidTank().liquidAmount()==2000,"steam was not condensed");
                a.setDirection(TurbineFluidPort.PortDirection.OUTLET);b.setDirection(TurbineFluidPort.PortDirection.OUTLET);
            })
            .thenWaitUntil(()->{
                h.assertBlockPresent(Blocks.WATER_CAULDRON,north.north());
                h.assertBlockPresent(Blocks.WATER_CAULDRON,south.south());
            })
            .thenExecute(()->h.assertTrue(((TurbineFluidPortTile)GameTestSupport.be(h,north)).controller().simulation().fluidTank().liquidAmount()==0,"water was lost or kept"))
            .thenSucceed();
    }
    private TurbineBalancedOutputGameTests() { }
}
