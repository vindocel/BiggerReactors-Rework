package biggerreactorsrework.gametest;

import biggerreactorsrework.fluids.LiquidObsidian;
import biggerreactorsrework.multiblocks.heatexchanger.blocks.HeatExchangerFluidPortBlock;
import biggerreactorsrework.multiblocks.heatexchanger.tiles.HeatExchangerFluidPortTile;
import biggerreactorsrework.multiblocks.reactor.blocks.*;
import biggerreactorsrework.multiblocks.reactor.simulation.base.CoolantTank;
import biggerreactorsrework.multiblocks.reactor.tiles.*;
import biggerreactorsrework.registries.FluidTransitionRegistry;
import biggerreactorsrework.util.FluidTransitionTank;
import biggerreactorsrework.util.GuideStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import static biggerreactorsrework.core.multiblock.IAssemblyStateBlock.ASSEMBLED;

/** Added 2026-11-01: heated coolant through the native transfer API and, when installed, a real Pipez network. */
public final class CoolantTransferGameTests {
    private static final BlockPos TERMINAL = new BlockPos(1,3,3), OUTLET = new BlockPos(5,3,2);

    private static ReactorTerminalTile terminal(GameTestHelper h) { return (ReactorTerminalTile) GameTestSupport.be(h, TERMINAL); }
    private static void build(GameTestHelper h) {
        for(int x=1;x<=5;x++)for(int y=1;y<=5;y++)for(int z=1;z<=5;z++)
            if(x==1 || x==5 || y==1 || y==5 || z==1 || z==5)h.setBlock(x,y,z,ReactorCasing.INSTANCE);
        for(int y=2;y<=4;y++){
            h.setBlock(3,y,3,ReactorFuelRod.INSTANCE);
            h.setBlock(2,y,3,ReactorManifold.INSTANCE);h.setBlock(4,y,3,ReactorManifold.INSTANCE);
        }
        h.setBlock(3,5,3,ReactorControlRod.INSTANCE);h.setBlock(TERMINAL,ReactorTerminal.INSTANCE);
        h.setBlock(1,3,2,ReactorCoolantPort.INSTANCE);h.setBlock(OUTLET,ReactorCoolantPort.INSTANCE);
    }
    private static ResourceHandler<FluidResource> handler(GameTestHelper h, BlockPos pos, Direction side) {
        var handler = h.getLevel().getCapability(Capabilities.Fluid.BLOCK, h.absolutePos(pos), side);
        h.assertTrue(handler != null, "fluid capability missing at " + pos);
        return handler;
    }
    private static FluidTransitionTank exchanger(GameTestHelper h, BlockPos pos, int capacity) {
        h.setBlock(pos, HeatExchangerFluidPortBlock.INSTANCE);
        var port = (HeatExchangerFluidPortTile) GameTestSupport.be(h, pos);
        var primary = new FluidTransitionTank(true);
        primary.perSideCapacity = capacity;
        port.setHETank(primary);
        port.setInlet(true);
        return primary;
    }
    /** Fills with obsidian, converts it to its heated product and switches the east port to outlet. */
    private static FluidStack heat(GameTestHelper h) {
        var inlet = handler(h, new BlockPos(1,3,2), Direction.WEST);
        try (var tx = Transaction.openRoot()) {
            h.assertTrue(inlet.insert(FluidResource.of(LiquidObsidian.INSTANCE),1000,tx)==1000, "obsidian insertion failed");
            tx.commit();
        }
        var internal = (CoolantTank) terminal(h).controller().simulation().coolantTank();
        internal.absorbRF(-1000.5);
        ((ReactorCoolantPortTile) GameTestSupport.be(h, OUTLET)).setDirection(ReactorAccessPort.PortDirection.OUTLET);
        var hot = terminal(h).controller().coolantTank().getFluidInTank(1).copy();
        h.assertTrue(!hot.isEmpty(), "reactor did not heat the obsidian");
        return hot;
    }

    @GameTest(template="empty_7x7x7",timeoutTicks=160)
    public static void partialAcceptanceConservesHeatedCoolant(GameTestHelper h) {
        build(h);
        h.startSequence().thenWaitUntil(()->h.assertTrue(terminal(h).nullableController()!=null && terminal(h).controller().simulation()!=null,"reactor not ready"))
            .thenExecute(()->{
                heat(h);
                var tank = terminal(h).controller().coolantTank();
                var source = handler(h, OUTLET, Direction.EAST);
                var primary = exchanger(h, new BlockPos(6,3,2), 10);
                var destination = handler(h, new BlockPos(6,3,2), Direction.WEST);
                long total = tank.vaporAmount();
                int moved = ResourceHandlerUtil.move(source,destination,r->true,1000,null);
                h.assertTrue(moved==10 && tank.vaporAmount()==total-10 && primary.fluidAmountInTank(0)==10,"partial acceptance lost or created coolant");
                h.assertTrue(ResourceHandlerUtil.move(source,destination,r->true,1000,null)==0,"full destination accepted more coolant");
                h.assertTrue(tank.vaporAmount()==total-10,"full destination consumed coolant");
            }).thenSucceed();
    }

    @GameTest(template="empty_64x48x40",timeoutTicks=600)
    public static void pipezNearestDistributionMovesHeatedObsidian(GameTestHelper h) { pipeNetwork(h,"NEAREST"); }
    @GameTest(template="empty_64x48x40",timeoutTicks=600)
    public static void pipezRoundRobinDistributionMovesHeatedObsidian(GameTestHelper h) { pipeNetwork(h,"ROUND_ROBIN"); }

    private static HeatExchangerFluidPortTile placeExchanger(GameTestHelper h, BlockPos origin) {
        GuideStructures.exchanger().parts().forEach((pos,block)->h.setBlock(origin.offset(pos),block));
        return (HeatExchangerFluidPortTile) GameTestSupport.be(h, origin.offset(0,1,1));
    }

    /** A real reactor outlet feeding two assembled exchangers through one real pipe network. */
    private static void pipeNetwork(GameTestHelper h, String distribution) {
        if(!ModList.get().isLoaded("pipez")){h.succeed();return;}
        build(h);
        var pipePos = new BlockPos(6,3,2);
        var near = placeExchanger(h, new BlockPos(7,2,1));
        var far = placeExchanger(h, new BlockPos(7,6,1));
        var state = new Object() { long total; int rounds; net.minecraft.world.level.block.entity.BlockEntity pipe; FluidStack hot; };
        h.startSequence().thenWaitUntil(()->{
                h.assertTrue(terminal(h).nullableController()!=null && terminal(h).controller().simulation()!=null,"reactor not ready");
                h.assertTrue(near.getBlockState().getValue(ASSEMBLED) && far.getBlockState().getValue(ASSEMBLED),"exchangers not assembled");
            }).thenExecute(()->{
                for(var port:new HeatExchangerFluidPortTile[]{near,far})if(!port.isInlet())port.controller().setInletPort(port,true);
                state.hot = heat(h);
                state.total = terminal(h).controller().coolantTank().vaporAmount();
                for(int y=3;y<=7;y++)h.setBlock(6,y,2,BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("pipez","fluid_pipe")));
                try { state.pipe = Pipez.configure(h.getLevel(), h.absolutePos(pipePos), distribution); }
                catch (ReflectiveOperationException e) { throw new AssertionError(e); }
            }).thenWaitUntil(()->{
                try { Pipez.tick(state.pipe); }
                catch (ReflectiveOperationException e) { throw new AssertionError(e); }
                var tank = terminal(h).controller().coolantTank();
                // A single small batch fits one connection's share: keep producing so distribution can alternate.
                if(state.rounds<4 && tank.vaporAmount()==0) {
                    state.rounds++;
                    ((CoolantTank) terminal(h).controller().simulation().coolantTank()).absorbRF(-1000.5);
                    state.total += tank.vaporAmount();
                }
                h.assertTrue(state.rounds==4 && tank.vaporAmount()==0,"heated coolant still in the reactor: "+tank.vaporAmount()+", round "+state.rounds);
            }).thenExecute(()->{
                long received = 0;
                for(var port:new HeatExchangerFluidPortTile[]{near,far}) {
                    var condenser = port.controller().condenserTank;
                    long amount = condenser.fluidAmountInTank(0)+condenser.fluidAmountInTank(1);
                    received += amount;
                    if("ROUND_ROBIN".equals(distribution))h.assertTrue(amount>0,"round robin skipped an exchanger");
                    if(condenser.fluidAmountInTank(0)>0)h.assertTrue(FluidStack.isSameFluidSameComponents(condenser.getFluidInTank(0),state.hot),"pipe stripped the coolant components");
                    if(amount>0)h.assertTrue(condenser.activeTransition()==FluidTransitionRegistry.liquidTransition(LiquidObsidian.INSTANCE),"exchanger resolved the wrong profile");
                }
                h.assertTrue(received==state.total,"pipe network lost or created coolant: "+received+" of "+state.total);
            }).thenSucceed();
    }

    /** Separate class: the base test environment has no Pipez classes. */
    private static final class Pipez {
        static net.minecraft.world.level.block.entity.BlockEntity configure(net.minecraft.world.level.Level level, BlockPos pos, String distribution) throws ReflectiveOperationException {
            var type = Class.forName("de.maxhenkel.pipez.blocks.tileentity.types.FluidPipeType").getField("INSTANCE").get(null);
            var pipeType = Class.forName("de.maxhenkel.pipez.blocks.tileentity.types.PipeType");
            var modes = Class.forName("de.maxhenkel.pipez.blocks.tileentity.UpgradeTileEntity$Distribution");
            var block = level.getBlockState(pos).getBlock();
            // The block creates its tile only once a side has data, as a player's wrench does.
            block.getClass().getMethod("setExtracting",net.minecraft.world.level.Level.class,BlockPos.class,Direction.class,boolean.class).invoke(block,level,pos,Direction.WEST,true);
            var pipe = level.getBlockEntity(pos);
            var upgrade = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("pipez","ultimate_upgrade")));
            pipe.getClass().getMethod("setUpgradeItem",Direction.class,ItemStack.class).invoke(pipe,Direction.WEST,upgrade);
            pipe.getClass().getMethod("setDistribution",Direction.class,pipeType,modes)
                    .invoke(pipe,Direction.WEST,type,modes.getMethod("valueOf",String.class).invoke(null,distribution));
            Class.forName("de.maxhenkel.pipez.blocks.tileentity.PipeTileEntity").getMethod("markPipesDirty",net.minecraft.world.level.Level.class,BlockPos.class)
                    .invoke(null,level,pos);
            return pipe;
        }
        static void tick(net.minecraft.world.level.block.entity.BlockEntity pipe) throws ReflectiveOperationException {
            pipe.getClass().getMethod("tick").invoke(pipe);
        }
    }
}
