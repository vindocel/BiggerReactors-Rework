package biggerreactorsrework.gametest;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.fluids.FrozenObsidian;
import biggerreactorsrework.fluids.HeatedCoolant;
import biggerreactorsrework.fluids.LiquidObsidian;
import biggerreactorsrework.fluids.Steam;
import biggerreactorsrework.multiblocks.MachineLighting;
import biggerreactorsrework.multiblocks.reactor.blocks.ReactorManifold;
import biggerreactorsrework.registries.CoolantComponents;
import biggerreactorsrework.registries.FluidTransitionRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

// Added 2026-11-03: the Frozen Obsidian coolant, its recipe, yield and glow; steam has no bucket.
@GameTestHolder(BiggerReactors.modid) @PrefixGameTestTemplate(false)
public final class FrozenObsidianGameTests {
    @GameTest(template="empty_7x7x7")
    public static void frozenObsidianRecipeYieldAndGlow(GameTestHelper h) {
        h.assertTrue(FrozenObsidian.INSTANCE.getBucket() != Items.AIR, "Frozen Obsidian has no bucket");
        h.assertTrue(Steam.INSTANCE.getBucket() == Items.AIR, "steam still has a bucket");

        var profile = FluidTransitionRegistry.liquidTransition(FrozenObsidian.INSTANCE);
        var sodium = FluidTransitionRegistry.transitions().stream().filter(t -> t.steamPerBatch == 30).findFirst();
        h.assertTrue(profile != null && profile.genericHot() && profile.steamPerBatch == 40, "Frozen Obsidian does not boil 40 per ten hot mB");
        h.assertTrue(sodium.isEmpty() || profile.steamPerBatch > sodium.get().steamPerBatch, "Frozen Obsidian is not above sodium");

        var cold = new FluidStack(FrozenObsidian.INSTANCE, 1);
        var hot = new FluidStack(HeatedCoolant.INSTANCE, 1);
        hot.applyComponents(CoolantComponents.heated(profile));
        h.assertTrue(cold.getFluidType().getLightLevel(cold) == MachineLighting.GLOWING_COOLANT, "cold Frozen Obsidian does not glow");
        h.assertTrue(hot.getFluidType().getLightLevel(hot) == MachineLighting.GLOWING_COOLANT, "heated Frozen Obsidian does not glow");
        var plainHot = new FluidStack(HeatedCoolant.INSTANCE, 1);
        plainHot.applyComponents(CoolantComponents.heated(FluidTransitionRegistry.liquidTransition(LiquidObsidian.INSTANCE)));
        h.assertTrue(plainHot.getFluidType().getLightLevel(plainHot) == 0, "heated Liquid Obsidian started glowing");
        h.assertTrue(MachineLighting.GLOWING_COOLANT > MachineLighting.FUEL, "coolant glow is not above the fuel");
        var lit = ReactorManifold.INSTANCE.defaultBlockState().setValue(BlockStateProperties.LIT, true);
        h.assertTrue(lit.getLightEmission() == 0, "an unassembled manifold glows");

        var recipe = biggerreactorsrework.machine.recipe.ReprocessingRecipes.all(h.getLevel()).stream().map(r -> r.value())
                .filter(r -> r.fluidResult().getFluid() == FrozenObsidian.INSTANCE).findFirst();
        h.assertTrue(recipe.isPresent(), "no Industrial Processor recipe makes Frozen Obsidian");
        var r = recipe.get();
        h.assertTrue(r.ingredient().test(new ItemStack(Items.BLUE_ICE)) && r.count() == 1, "recipe does not take one blue ice");
        h.assertTrue(r.fluid() == LiquidObsidian.INSTANCE && r.totalFluid() == 200, "recipe does not take 200 mB of Liquid Obsidian");
        h.assertTrue(r.fluidResult().getAmount() == 100, "recipe does not make 100 mB");
        h.succeed();
    }

    @GameTest(template="empty_7x7x7", timeoutTicks=100)
    public static void placedFrozenObsidianFreezesEntitiesLikePowderSnow(GameTestHelper h) {
        var pos = new net.minecraft.core.BlockPos(3, 2, 3);
        h.setBlock(pos, FrozenObsidian.INSTANCE.defaultFluidState().createLegacyBlock());
        var pig = h.spawn(net.minecraft.world.entity.EntityType.PIG, pos);
        h.succeedWhen(() -> h.assertTrue(pig.getTicksFrozen() > 20, "entity inside Frozen Obsidian is not freezing"));
    }

    @GameTest(template="empty_7x7x7", timeoutTicks=100)
    public static void placedHeatedCoolantBurnsLikeLava(GameTestHelper h) {
        var pos = new net.minecraft.core.BlockPos(3, 2, 3);
        h.setBlock(pos, HeatedCoolant.INSTANCE.defaultFluidState().createLegacyBlock());
        var pig = h.spawn(net.minecraft.world.entity.EntityType.PIG, pos);
        float health = pig.getHealth();
        h.succeedWhen(() -> h.assertTrue(pig.isOnFire() && pig.getHealth() < health, "entity inside heated coolant is not burning"));
    }

    @GameTest(template="empty_7x7x7")
    public static void heatedCoolantBucketKeepsItsOriginInTanksAndInTheWorld(GameTestHelper h) {
        var profile = FluidTransitionRegistry.liquidTransition(FrozenObsidian.INSTANCE);
        var hot = new FluidStack(HeatedCoolant.INSTANCE, 1000);
        hot.applyComponents(CoolantComponents.heated(profile));
        var bucket = net.neoforged.neoforge.fluids.FluidUtil.getFilledBucket(hot);
        h.assertTrue(bucket.is(HeatedCoolant.INSTANCE.getBucket()), "heated coolant has no bucket");
        var handler = net.neoforged.neoforge.fluids.FluidUtil.getFluidHandler(bucket).orElseThrow();
        h.assertTrue(FluidStack.isSameFluidSameComponents(handler.getFluidInTank(0), hot), "bucket lost the coolant origin");

        var pos = new net.minecraft.core.BlockPos(3, 2, 3);
        var placer = (biggerreactorsrework.fluids.HeatedCoolantBucket.BucketWithOrigin) bucket.getItem();
        h.assertTrue(placer.emptyContents(null, h.getLevel(), h.absolutePos(pos), null, bucket), "bucket did not place heated coolant");
        var state = h.getBlockState(pos);
        h.assertTrue(state.getFluidState().isSourceOfType(HeatedCoolant.INSTANCE), "no heated coolant source was placed");
        var collected = ((net.minecraft.world.level.block.BucketPickup) state.getBlock()).pickupBlock(null, h.getLevel(), h.absolutePos(pos), state);
        h.assertTrue(java.util.Objects.equals(collected.get(CoolantComponents.ORIGIN.get()), bucket.get(CoolantComponents.ORIGIN.get())), "placed block lost the coolant origin");
        h.assertTrue(!placer.emptyContents(null, h.getLevel(), h.absolutePos(pos), null, new ItemStack(HeatedCoolant.INSTANCE.getBucket())), "a bucket without origin was placed");
        h.succeed();
    }

    private FrozenObsidianGameTests() { }
}
