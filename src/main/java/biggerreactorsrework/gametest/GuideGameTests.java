package biggerreactorsrework.gametest;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.util.GuideStructures;
import net.minecraft.gametest.framework.GameTestHelper;
import static biggerreactorsrework.core.multiblock.IAssemblyStateBlock.ASSEMBLED;

/** The exact examples rendered in the guide must assemble. */
public final class GuideGameTests {
    private static void build(GameTestHelper h, GuideStructures.Example example) {
        example.parts().forEach(h::setBlock);
        h.succeedWhen(() -> example.parts().forEach((pos,state)-> {
            if(state.hasProperty(ASSEMBLED)) h.assertTrue(h.getBlockState(pos).getValue(ASSEMBLED), example.id()+" unassembled at "+pos);
        }));
    }
    @GameTest(template="empty_7x7x7",timeoutTicks=160)
    public static void minimum(GameTestHelper h) { build(h,GuideStructures.reactor(3,false)); }
    @GameTest(template="empty_7x7x7",timeoutTicks=160)
    public static void moderated(GameTestHelper h) { build(h,GuideStructures.reactor(5,false)); }
    @GameTest(template="empty_7x7x7",timeoutTicks=160)
    public static void active(GameTestHelper h) { build(h,GuideStructures.reactor(5,true)); }
    @GameTest(template="empty_7x7x7",timeoutTicks=160)
    public static void turbine(GameTestHelper h) { build(h,GuideStructures.turbine()); }
    @GameTest(template="empty_7x7x7",timeoutTicks=160)
    public static void exchanger(GameTestHelper h) { build(h,GuideStructures.exchanger()); }
    @GameTest(template="empty_7x7x7",timeoutTicks=160)
    public static void battery(GameTestHelper h) { build(h,GuideStructures.battery()); }
}
