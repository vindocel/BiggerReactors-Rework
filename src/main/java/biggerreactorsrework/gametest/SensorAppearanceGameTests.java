package biggerreactorsrework.gametest;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.core.client.gui.GuiSync;
import biggerreactorsrework.core.multiblock.IAssemblyStateBlock;
import biggerreactorsrework.multiblocks.reactor.blocks.*;
import biggerreactorsrework.multiblocks.reactor.containers.*;
import biggerreactorsrework.multiblocks.reactor.tiles.*;
import biggerreactorsrework.multiblocks.turbine.blocks.*;
import biggerreactorsrework.multiblocks.turbine.containers.*;
import biggerreactorsrework.multiblocks.turbine.tiles.*;
import biggerreactorsrework.util.GuideStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;

import java.util.List;

/** Added 2026-10-30: terminal and sensor snapshots share appearance, without moving menu/network origins. */
@GameTestHolder(BiggerReactors.modid)
@PrefixGameTestTemplate(false)
public final class SensorAppearanceGameTests {
    private static final BlockPos FIRST = new BlockPos(0,1,2), SECOND = new BlockPos(4,2,2);
    private static final BlockPos SENSOR = new BlockPos(0,2,1), OTHER_SENSOR = new BlockPos(0,2,3);

    private static void check(GameTestHelper h, BlockPos expected, List<BlockPos> positions) {
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
        for (var relative : positions) {
            var tile = h.getBlockEntity(relative);
            var pos = tile.getBlockPos();
            GuiSync.IGUIPacketProvider menu;
            if (tile instanceof ReactorTerminalTile) menu = new ReactorTerminalContainer(42, pos, player);
            else if (tile instanceof ReactorRedstonePortTile) menu = new ReactorRedstonePortContainer(42, pos, player);
            else if (tile instanceof TurbineTerminalTile) menu = new TurbineTerminalContainer(42, pos, player);
            else menu = new TurbineRedstonePortContainer(42, pos, player);
            var data = menu.getGuiPacket().write();
            h.assertTrue(data.get("appearancePos") instanceof Long value && value == h.absolutePos(expected).asLong(), "separate appearance identity at " + relative);
            h.assertTrue(menu.machinePos().equals(pos), "appearance sharing moved the request/menu origin");
            var received = tile.getType().create(pos, tile.getBlockState());
            if (received instanceof ReactorTerminalTile terminal) terminal.getState().read(data);
            else if (received instanceof ReactorRedstonePortTile sensor) sensor.getCurrentChanges().read(data);
            else if (received instanceof TurbineTerminalTile terminal) terminal.turbineState.read(data);
            else ((TurbineRedstonePortTile) received).read(data);
            var appearance = received instanceof ReactorBaseTile reactor ? reactor.appearancePos : ((TurbineBaseTile) received).appearancePos;
            h.assertTrue(appearance.equals(h.absolutePos(expected)), "client lost the common appearance identity");
        }
    }

    private static void verify(GameTestHelper h, boolean reactor) {
        (reactor ? GuideStructures.reactor(5, false) : GuideStructures.turbine()).parts().forEach(h::setBlock);
        h.setBlock(SECOND, reactor ? ReactorTerminal.INSTANCE : TurbineTerminal.INSTANCE);
        for (var pos : List.of(SENSOR, OTHER_SENSOR)) h.setBlock(pos, reactor ? ReactorRedstonePort.INSTANCE : TurbineRedstonePort.INSTANCE);
        h.startSequence().thenWaitUntil(() -> h.assertTrue(h.getBlockState(SENSOR).getValue(IAssemblyStateBlock.ASSEMBLED), "machine not assembled"))
                .thenExecute(() -> {
                    check(h, FIRST, List.of(FIRST, SECOND, SENSOR, OTHER_SENSOR));
                    h.setBlock(FIRST, reactor ? ReactorCasing.INSTANCE : TurbineCasing.INSTANCE);
                }).thenIdle(5).thenWaitUntil(() -> h.assertTrue(h.getBlockState(SENSOR).getValue(IAssemblyStateBlock.ASSEMBLED), "machine not reassembled"))
                .thenExecute(() -> check(h, SECOND, List.of(SECOND, SENSOR, OTHER_SENSOR))).thenSucceed();
    }
    @GameTest(template = "empty_7x7x7", timeoutTicks = 240)
    public static void reactorSensorsFollowTheReactorAppearance(GameTestHelper h) { verify(h, true); }
    @GameTest(template = "empty_7x7x7", timeoutTicks = 240)
    public static void turbineSensorsFollowTheTurbineAppearance(GameTestHelper h) { verify(h, false); }
}
