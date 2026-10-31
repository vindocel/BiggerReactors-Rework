package biggerreactorsrework.gametest;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.multiblocks.reactor.blocks.*;
import biggerreactorsrework.multiblocks.reactor.tiles.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.*;

/** Modified 2026-10-27: coolant identity and volumes must survive casing repair within the same session. */
public final class ReactorCoolantReassemblyGameTests {
    @GameTest(template="empty_7x7x7",timeoutTicks=160)
    public static void cancelledCoolantInsertionDoesNotBlockObsidian(GameTestHelper h) {
        build(h);
        h.startSequence().thenWaitUntil(() -> h.assertTrue(terminal(h).nullableController()!=null &&
                terminal(h).controller().simulation()!=null, "reactor not ready"))
            .thenExecute(() -> {
                var portPos = h.absolutePos(new BlockPos(1,3,2));
                var handler = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK,
                        portPos, net.minecraft.core.Direction.WEST);
                h.assertTrue(handler != null, "coolant port capability missing");
                var water = net.neoforged.neoforge.transfer.fluid.FluidResource.of(Fluids.WATER);
                var obsidian = net.neoforged.neoforge.transfer.fluid.FluidResource.of(biggerreactorsrework.fluids.LiquidObsidian.INSTANCE);
                var tank = terminal(h).controller().coolantTank();
                try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                    h.assertTrue(handler.insert(water,1000,tx)==1000,"water preview rejected");
                }
                h.assertTrue(tank.liquidAmount()==0 && tank.activeTransition()==null,
                        "cancelled transfer left unidentified coolant in the reactor");
                try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                    h.assertTrue(handler.insert(obsidian,1000,tx)==1000,"obsidian rejected after cancelled water transfer");
                }
                h.assertTrue(tank.liquidAmount()==0 && tank.activeTransition()==null,"obsidian preview changed tank");
                try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                    h.assertTrue(handler.insert(obsidian,1000,tx)==1000,"obsidian insertion failed");
                    tx.commit();
                }
                h.assertTrue(tank.liquidAmount()==1000 && tank.liquidType()==biggerreactorsrework.fluids.LiquidObsidian.INSTANCE,
                        "committed obsidian did not reach the reactor");
                var internal = (biggerreactorsrework.multiblocks.reactor.simulation.base.CoolantTank)
                        terminal(h).controller().simulation().coolantTank();
                internal.absorbRF(-1000.5);
                var before = internal.save().toROBNMap();
                var hot = net.neoforged.neoforge.transfer.fluid.FluidResource.of(tank.getFluidInTank(1));
                h.assertTrue(!hot.isEmpty(), "reactor did not heat the obsidian");
                ((ReactorCoolantPortTile) h.getLevel().getBlockEntity(portPos)).setDirection(
                        ReactorAccessPort.PortDirection.OUTLET);
                try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                    h.assertTrue(handler.extract(0,hot,1000,tx)==0,"hot coolant exposed in the cold input chamber");
                    h.assertTrue(handler.extract(1,hot,1000,tx)>0,"pipe could not extract heated coolant from its reported chamber");
                }
                h.assertTrue(internal.save().toROBNMap().equals(before),
                        "cancelled coolant extraction changed volume, thermal residue or backpressure");
                // Pipez 26.3 uses this utility, which extracts by the chamber index reported by the source.
                var exchangerPos = new BlockPos(6,3,3);
                h.setBlock(exchangerPos, biggerreactorsrework.multiblocks.heatexchanger.blocks.HeatExchangerFluidPortBlock.INSTANCE);
                var exchanger = (biggerreactorsrework.multiblocks.heatexchanger.tiles.HeatExchangerFluidPortTile)
                        h.getLevel().getBlockEntity(h.absolutePos(exchangerPos));
                var primary = new biggerreactorsrework.util.FluidTransitionTank(true);
                primary.perSideCapacity = 10000;
                exchanger.setHETank(primary);
                exchanger.setInlet(true);
                var destination = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK,
                        h.absolutePos(exchangerPos), net.minecraft.core.Direction.WEST);
                h.assertTrue(destination != null, "exchanger port capability missing");
                int available = handler.getAmountAsInt(1);
                try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                    h.assertTrue(net.neoforged.neoforge.transfer.ResourceHandlerUtil.move(handler,destination,r -> true,1000,tx)==available,
                            "Pipez transfer routine could not move heated obsidian");
                }
                h.assertTrue(tank.vaporAmount()==available && primary.fluidAmountInTank(0)==0,
                        "cancelled pipe transfer changed either machine");
                int moved = net.neoforged.neoforge.transfer.ResourceHandlerUtil.move(handler,destination,r -> true,1000,null);
                h.assertTrue(moved==available && tank.vaporAmount()==0 && primary.fluidAmountInTank(0)==available,
                        "pipe transfer did not conserve heated coolant");
                h.assertTrue(hot.matches(primary.getFluidInTank(0)) &&
                        primary.activeTransition()==biggerreactorsrework.registries.FluidTransitionRegistry.liquidTransition(
                                biggerreactorsrework.fluids.LiquidObsidian.INSTANCE),
                        "pipe transfer lost the obsidian coolant components");
            }).thenSucceed();
    }

    private static final BlockPos TERMINAL=new BlockPos(1,3,3),BROKEN=new BlockPos(1,5,1);
    private static ReactorTerminalTile terminal(GameTestHelper h){return (ReactorTerminalTile)GameTestSupport.be(h,TERMINAL);}
    private static void build(GameTestHelper h) {
        for(int x=1;x<=5;x++)for(int y=1;y<=5;y++)for(int z=1;z<=5;z++)
            if(x==1 || x==5 || y==1 || y==5 || z==1 || z==5)h.setBlock(x,y,z,ReactorCasing.INSTANCE);
        for(int y=2;y<=4;y++){
            h.setBlock(3,y,3,ReactorFuelRod.INSTANCE);
            h.setBlock(2,y,3,ReactorManifold.INSTANCE);h.setBlock(4,y,3,ReactorManifold.INSTANCE);
        }
        h.setBlock(3,5,3,ReactorControlRod.INSTANCE);h.setBlock(TERMINAL,ReactorTerminal.INSTANCE);
        h.setBlock(1,3,2,ReactorCoolantPort.INSTANCE);h.setBlock(5,3,2,ReactorCoolantPort.INSTANCE);
    }
    @GameTest(template="empty_7x7x7",timeoutTicks=160)
    public static void coolantIdentitySurvivesRepair(GameTestHelper h) {
        build(h);
        h.startSequence().thenWaitUntil(()->h.assertTrue(terminal(h).nullableController()!=null && terminal(h).controller().simulation()!=null,"reactor not ready"))
            .thenExecute(()->{
                var c=terminal(h).controller();
                h.assertTrue(c.coolantTank().fill(Fluids.WATER,DataComponentPatch.EMPTY,5000,false)==5000,"cold fill failed");
                c.simulation().coolantTank().insertVapor(1000);
                h.setBlock(BROKEN,Blocks.AIR);
            }).thenExecuteAfter(5,()->h.setBlock(BROKEN,ReactorCasing.INSTANCE))
            .thenWaitUntil(()->{
                var c=terminal(h).controller();h.assertTrue(c.simulation()!=null,"reactor did not reassemble");
                var tank=c.coolantTank();
                h.assertTrue(tank.liquidAmount()==5000 && tank.vaporAmount()==1000,"coolant volumes changed during repair");
                h.assertTrue(tank.liquidType()==Fluids.WATER,"stored coolant lost its identity and became empty/air");
                h.assertTrue(tank.fill(Fluids.WATER,DataComponentPatch.EMPTY,1,true)==1,"matching coolant rejected after repair");
            }).thenSucceed();
    }

    @GameTest(template="empty_7x7x7")
    public static void unidentifiedStockRequiresExplicitDumpBeforeRefill(GameTestHelper h) {
        var d=OperationGameTests.description(3);for(int y=0;y<3;y++)d.setManifold(0,y,1,true);
        var sim=new biggerreactorsrework.multiblocks.reactor.simulation.cpu.TimeSlicedReactorSimulation(d,
                new biggerreactorsrework.multiblocks.reactor.simulation.SimulationConfiguration(293.15,false));
        sim.coolantTank().insertLiquid(5000);
        var tank=new biggerreactorsrework.multiblocks.reactor.util.ReactorTransitionTank(sim.coolantTank());
        h.assertTrue(tank.liquidAmount()==5000 && tank.liquidType()==Fluids.EMPTY,"orphan setup failed");
        h.assertTrue(tank.fill(Fluids.WATER,DataComponentPatch.EMPTY,1000,false)==0,"unidentified stock was silently retyped");
        tank.dumpLiquid();tank.dumpVapor();
        h.assertTrue(tank.fill(Fluids.WATER,DataComponentPatch.EMPTY,1000,false)==1000,"explicit clearing did not restore input");
        h.assertTrue(tank.liquidType()==Fluids.WATER && tank.liquidAmount()==1000,"refill has incorrect identity or amount");h.succeed();
    }
    @GameTest(template="empty_7x7x7",timeoutTicks=160)
    public static void mountedReactorWithLegacyLavaStopsUntilBothTanksAreCleared(GameTestHelper h) {
        build(h);
        h.startSequence().thenWaitUntil(()->h.assertTrue(terminal(h).nullableController()!=null && terminal(h).controller().simulation()!=null,"reactor not ready"))
          .thenExecute(()->{
            var c=terminal(h).controller();var tank=c.coolantTank();
            c.simulation().fuelTank().insertFuel(5000,false);
            tank.fill(biggerreactorsrework.fluids.LiquidObsidian.INSTANCE,DataComponentPatch.EMPTY,5000,false);
            c.simulation().coolantTank().insertVapor(1000);
            var old=tank.serializeNBT();old.putString("outFluid","minecraft:lava");tank.deserializeNBT(old);
            c.setActive(biggerreactorsrework.multiblocks.reactor.state.ReactorActivity.ACTIVE);
            for(int i=0;i<40;i++)c.tick();
            h.assertTrue(c.simulation().fuelTank().fuel()==5000,"invalid mounted reactor burned fuel");
            h.assertTrue(tank.hasUnresolvedProfile() && tank.activeTransition()==null,"invalid reactor reinterpreted lava");
            h.assertTrue(tank.liquidAmount()==5000 && tank.vaporAmount()==1000,"invalid reactor changed its volumes");
            tank.dumpLiquid();
            h.assertTrue(tank.fill(Fluids.WATER,DataComponentPatch.EMPTY,100,false)==0,"one-sided cleanup unlocked reactor");
            tank.dumpVapor();
            h.assertTrue(tank.fill(Fluids.WATER,DataComponentPatch.EMPTY,100,false)==100,"reactor remained locked after both tanks cleared");
          }).thenSucceed();
    }

}
