package biggerreactorsrework.gametest;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.multiblocks.reactor.ReactorMultiblockController;
import biggerreactorsrework.multiblocks.reactor.blocks.*;
import biggerreactorsrework.multiblocks.reactor.tiles.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

/** Added 2026-11-15: fuel rod rendering must tolerate a fuel tank holding more than its capacity. */
@GameTestHolder(BiggerReactors.modid)
@PrefixGameTestTemplate(false)
public final class FuelRenderOverfillGameTests {
    private static final BlockPos TERMINAL=new BlockPos(1,4,2),BROKEN=new BlockPos(3,4,2);
    private static ReactorTerminalTile terminal(GameTestHelper h){return (ReactorTerminalTile)h.getBlockEntity(TERMINAL);}
    private static boolean ready(GameTestHelper h){var t=terminal(h);return t.nullableController()!=null && t.controller().simulation()!=null;}
    private static void build(GameTestHelper h) {
        for(int x=1;x<=3;x++)for(int y=1;y<=8;y++)for(int z=1;z<=3;z++)
            if(x!=2 || z!=2 || y==1)h.setBlock(x,y,z,ReactorCasing.INSTANCE);
        for(int y=2;y<=7;y++)h.setBlock(2,y,2,ReactorFuelRod.INSTANCE);
        h.setBlock(2,8,2,ReactorControlRod.INSTANCE);
        h.setBlock(TERMINAL,ReactorTerminal.INSTANCE);
    }
    private static void setTank(ReactorMultiblockController c,long fuel,long waste) {
        var data=c.simulation().save();var tank=data.getCompound("fuelTank");
        tank.put("fuel",fuel);tank.put("waste",waste);data.put("fuelTank",tank);c.simulation().load(data);
    }
    private static void clearRenderState(ReactorMultiblockController c) {
        setTank(c,0,0);
        try {
            for(String name:new String[]{"currentFuelRenderLevel","currentWasteRenderLevel"}) {
                var field=ReactorMultiblockController.class.getDeclaredField(name);field.setAccessible(true);field.setLong(c,0);
            }
        } catch(ReflectiveOperationException error){throw new AssertionError(error);}
    }

    @GameTest(template="empty_17x17x17",timeoutTicks=200)
    public static void overfilledTankKeepsFuelRenderingInsideReactor(GameTestHelper h) {
        build(h);
        long[] stored=new long[1];
        h.startSequence().thenWaitUntil(()->h.assertTrue(ready(h),"reactor not ready"))
            .thenExecute(()->{
                var c=terminal(h).controller();long capacity=c.simulation().fuelTank().capacity();
                setTank(c,capacity*16/10,capacity*3/10);
                stored[0]=c.simulation().fuelTank().totalStored();
                h.setBlock(BROKEN,Blocks.AIR);
            }).thenExecuteAfter(5,()->h.setBlock(BROKEN,ReactorCasing.INSTANCE))
            .thenWaitUntil(()->h.assertTrue(ready(h),"reactor did not reassemble"))
            .thenExecute(()->{
                var c=terminal(h).controller();
                h.assertTrue(c.simulation().fuelTank().totalStored()==stored[0],"reassembly did not preserve the overfilled stock");
                c.extractWaste(c.simulation().fuelTank().capacity()/20,false);
                try {c.tick();}
                catch(IndexOutOfBoundsException error) {
                    clearRenderState(c);
                    h.fail("fuel rendering crashed with an overfilled tank: "+error.getMessage());
                }
            }).thenSucceed();
    }
}
