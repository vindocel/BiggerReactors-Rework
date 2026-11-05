package biggerreactorsrework.gametest;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.core.multiblock.validated.IValidatedMultiblock;
import biggerreactorsrework.multiblocks.reactor.blocks.*;
import biggerreactorsrework.multiblocks.reactor.tiles.ReactorTerminalTile;
import biggerreactorsrework.multiblocks.turbine.blocks.*;
import biggerreactorsrework.multiblocks.turbine.tiles.TurbineTerminalTile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class MixedMaterialsGameTests {
    private static Block ludicrite() {
        return BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(BiggerReactors.modid, "ludicrite_block"));
    }

    @GameTest(template = "empty_7x7x7", timeoutTicks = 120)
    public static void reactorAssemblesWithMixedModerators(GameTestHelper h) {
        for (int x = 1; x <= 5; x++) for (int y = 1; y <= 5; y++) for (int z = 1; z <= 5; z++) {
            boolean wall = x == 1 || x == 5 || y == 1 || y == 5 || z == 1 || z == 5;
            if (wall) h.setBlock(x, y, z, ReactorCasing.INSTANCE);
            else if (x == 3 && z == 3) h.setBlock(x, y, z, ReactorFuelRod.INSTANCE);
            else h.setBlock(x, y, z, (x + y + z) % 2 == 0 ? Blocks.DIAMOND_BLOCK : ludicrite());
        }
        h.setBlock(3, 5, 3, ReactorControlRod.INSTANCE);
        h.setBlock(1, 3, 3, ReactorTerminal.INSTANCE);
        h.succeedWhen(() -> {
            ReactorTerminalTile terminal = GameTestSupport.be(h, new BlockPos(1, 3, 3));
            h.assertTrue(terminal.nullableController() != null
                    && terminal.controller().assemblyState() == IValidatedMultiblock.AssemblyState.ASSEMBLED,
                    "reactor with mixed moderators did not assemble: " + (terminal.nullableController() == null ? "no controller" : terminal.controller().getDebugInfo()));
        });
    }

    @GameTest(template = "empty_7x7x7")
    public static void diamondIsATurbineCoil(GameTestHelper h) {
        var diamond = biggerreactorsrework.registries.TurbineCoilRegistry.getCoilData(Blocks.DIAMOND_BLOCK);
        h.assertTrue(diamond != null && diamond.fePerMB() == 7.25 && diamond.steamPerTick() == 1250, "diamond coil missing or changed");
        h.succeed();
    }

    @GameTest(template = "empty_7x7x7", timeoutTicks = 120)
    public static void turbineAssemblesWithMixedCoils(GameTestHelper h) {
        for (int x = 1; x <= 5; x++) for (int y = 1; y <= 5; y++) for (int z = 1; z <= 5; z++)
            if (x == 1 || x == 5 || y == 1 || y == 5 || z == 1 || z == 5) h.setBlock(x, y, z, TurbineCasing.INSTANCE);
        h.setBlock(1, 3, 3, TurbineTerminal.INSTANCE);
        h.setBlock(3, 1, 3, TurbineRotorBearing.INSTANCE);
        h.setBlock(3, 5, 3, TurbineRotorBearing.INSTANCE);
        for (int y = 2; y <= 4; y++) h.setBlock(3, y, 3, TurbineRotorShaft.INSTANCE);
        for (int[] o : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) h.setBlock(3 + o[0], 2, 3 + o[1], TurbineRotorBlade.INSTANCE);
        int i = 0;
        for (int x = 2; x <= 4; x++) for (int z = 2; z <= 4; z++) {
            if (x == 3 && z == 3) continue;
            h.setBlock(x, 4, z, switch (i++ % 4) { case 0 -> Blocks.IRON_BLOCK; case 1 -> Blocks.GOLD_BLOCK; case 2 -> Blocks.DIAMOND_BLOCK; default -> ludicrite(); });
        }
        h.succeedWhen(() -> {
            TurbineTerminalTile terminal = GameTestSupport.be(h, new BlockPos(1, 3, 3));
            h.assertTrue(terminal.nullableController() != null
                    && terminal.controller().assemblyState() == IValidatedMultiblock.AssemblyState.ASSEMBLED,
                    "turbine with mixed coils did not assemble: " + (terminal.nullableController() == null ? "no controller" : terminal.controller().getDebugInfo()));
        });
    }

    private MixedMaterialsGameTests() { }
}
