package biggerreactorsrework.gametest;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.multiblocks.reactor.blocks.ReactorCasing;
import biggerreactorsrework.multiblocks.reactor.blocks.ReactorControlRod;
import biggerreactorsrework.multiblocks.reactor.blocks.ReactorFuelRod;
import biggerreactorsrework.multiblocks.reactor.blocks.ReactorTerminal;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

import static biggerreactorsrework.core.multiblock.IAssemblyStateBlock.ASSEMBLED;

/**
 * Formation and disassembly of the multiblocks, driven by the internal multiblock framework.
 */
public final class MultiblockGameTests {
    private MultiblockGameTests() {
    }

    private static final BlockPos MIN = new BlockPos(2, 1, 2);
    private static final BlockPos MAX = new BlockPos(4, 3, 4);
    private static final BlockPos TERMINAL = new BlockPos(2, 2, 3);
    private static final BlockPos FUEL_ROD = new BlockPos(3, 2, 3);
    private static final BlockPos CONTROL_ROD = new BlockPos(3, 3, 3);

    /**
     * Smallest valid reactor: 3x3x3 casing shell, one fuel rod under one control rod, and a terminal.
     */
    private static void buildMinimalReactor(GameTestHelper helper) {
        for (int x = MIN.getX(); x <= MAX.getX(); x++) {
            for (int y = MIN.getY(); y <= MAX.getY(); y++) {
                for (int z = MIN.getZ(); z <= MAX.getZ(); z++) {
                    boolean exterior = x == MIN.getX() || x == MAX.getX() || y == MIN.getY() || y == MAX.getY() || z == MIN.getZ() || z == MAX.getZ();
                    if (exterior) {
                        helper.setBlock(x, y, z, ReactorCasing.INSTANCE);
                    }
                }
            }
        }
        helper.setBlock(FUEL_ROD, ReactorFuelRod.INSTANCE);
        helper.setBlock(CONTROL_ROD, ReactorControlRod.INSTANCE);
        helper.setBlock(TERMINAL, ReactorTerminal.INSTANCE);
    }

    private static boolean assembled(GameTestHelper helper, BlockPos pos) {
        final var state = helper.getBlockState(pos);
        return state.hasProperty(ASSEMBLED) && state.getValue(ASSEMBLED);
    }

    @GameTest(template = "empty_7x7x7", timeoutTicks = 100)
    public static void minimalReactorAssembles(GameTestHelper helper) {
        buildMinimalReactor(helper);
        helper.succeedWhen(() -> {
            helper.assertTrue(assembled(helper, TERMINAL), "terminal not assembled");
            helper.assertTrue(assembled(helper, MIN), "corner casing not assembled");
        });
    }

    @GameTest(template = "empty_7x7x7", timeoutTicks = 160)
    public static void reactorDisassemblesWhenCasingBroken(GameTestHelper helper) {
        buildMinimalReactor(helper);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(assembled(helper, TERMINAL), "terminal not assembled"))
                .thenExecute(() -> helper.setBlock(MAX, Blocks.AIR))
                .thenWaitUntil(() -> helper.assertFalse(assembled(helper, TERMINAL), "terminal still assembled"))
                .thenSucceed();
    }
}
