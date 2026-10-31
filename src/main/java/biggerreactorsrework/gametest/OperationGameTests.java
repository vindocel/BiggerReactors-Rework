package biggerreactorsrework.gametest;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.multiblocks.reactor.blocks.ReactorRedstonePort;
import biggerreactorsrework.multiblocks.reactor.tiles.ReactorRedstonePortTile;
import biggerreactorsrework.multiblocks.reactor.simulation.SimulationConfiguration;
import biggerreactorsrework.multiblocks.reactor.simulation.SimulationDescription;
import biggerreactorsrework.multiblocks.reactor.simulation.cpu.FullPassReactorSimulation;
import biggerreactorsrework.registries.ReactorModeratorRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

/** Operational regressions. */
public final class OperationGameTests {
    public static SimulationDescription description(int height) {
        var description = new SimulationDescription();
        description.setSize(3, height, 3);
        description.setControlRod(1, 1, true);
        description.setDefaultIModeratorProperties(new ReactorModeratorRegistry.ModeratorProperties(0.1, 0.25, 1.1, 0.05));
        return description;
    }

    @GameTest(template = "empty_7x7x7")
    public static void asyncStopsBurningAfterPendingResult(GameTestHelper helper) {
        var simulation = new FullPassReactorSimulation.MultiThreaded(description(4), new SimulationConfiguration(293.15, true), true);
        simulation.fuelTank().insertFuel(10000, false);
        for (int i = 0; i < 8; i++) simulation.tick(true);
        simulation.tick(false);
        long fuel = simulation.fuelTank().fuel();
        for (int i = 0; i < 100; i++) simulation.tick(false);
        helper.assertTrue(fuel == simulation.fuelTank().fuel(), "inactive asynchronous reactor keeps burning fuel");
        helper.assertTrue(simulation.fuelTank().burnedLastTick() == 0, "inactive reactor reports fresh consumption");
        helper.succeed();
    }

    @GameTest(template = "empty_7x7x7")
    public static void redstoneRejectsMalformedRequests(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, ReactorRedstonePort.INSTANCE);
        var tile = (ReactorRedstonePortTile) GameTestSupport.be(helper,pos);
        tile.runRequest("setSelectedTab", "invalid");
        tile.runRequest("setSelectedTab", -1);
        tile.runRequest("setTriggerPS", null);
        tile.runRequest("setTextBufferA", "NaN");
        tile.runRequest("applyChanges", null);
        helper.assertTrue(tile.getState().textBufferA.isEmpty(), "malformed edit was committed");
        tile.runRequest("setTextBufferA", "12");
        tile.runRequest("setTextBufferB", "not a number");
        tile.runRequest("applyChanges", null);
        helper.assertTrue(tile.getState().textBufferA.isEmpty(), "edit was partially committed");
        tile.runRequest("revertChanges", null);
        helper.assertTrue(tile.getCurrentChanges().textBufferA.isEmpty(), "revert failed");
        helper.succeed();
    }
}
