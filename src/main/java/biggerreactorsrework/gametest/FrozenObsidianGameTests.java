package biggerreactorsrework.gametest;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.fluids.FrozenObsidian;
import biggerreactorsrework.fluids.HeatedCoolant;
import biggerreactorsrework.fluids.HeatedCoolantBucket;
import biggerreactorsrework.fluids.LiquidObsidian;
import biggerreactorsrework.fluids.Steam;
import biggerreactorsrework.multiblocks.MachineLighting;
import biggerreactorsrework.multiblocks.reactor.blocks.ReactorManifold;
import biggerreactorsrework.registries.CoolantComponents;
import biggerreactorsrework.registries.FluidTransitionRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

// Added 2026-11-03: the Frozen Obsidian coolant, its recipe, yield and glow; steam has no bucket.
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
    public static void placedCoolantsMatchWaterTravelAndCurrent(GameTestHelper h) {
        var water = measureTravel(h, (FlowingFluid) Fluids.WATER);
        h.assertTrue(water.x > 0 && Math.abs(water.y) > 0.0001, "water baseline did not push or allow travel");
        for (var coolant : new FlowingFluid[]{LiquidObsidian.INSTANCE, FrozenObsidian.INSTANCE, HeatedCoolant.INSTANCE}) {
            var movement = measureTravel(h, coolant);
            h.assertTrue(Math.abs(movement.x - water.x) < 0.000001 && Math.abs(movement.y - water.y) < 0.000001,
                    "coolant current or travel differs from water: " + coolant + " " + movement + ", water " + water);
        }
        h.succeed();
    }

    private static Vec3 measureTravel(GameTestHelper h, FlowingFluid fluid) {
        var pos = new BlockPos(3, 2, 3);
        h.setBlock(pos.below(), Blocks.STONE);
        h.setBlock(pos.west(), fluid.defaultFluidState().createLegacyBlock());
        h.setBlock(pos, fluid.getFlowing(7, false).createLegacyBlock());
        var pig = h.spawn(EntityTypes.PIG, pos);
        pig.setYRot(0);
        pig.baseTick();
        h.assertTrue(pig.isInWater(), "entity did not enter water movement in " + fluid);
        double current = pig.getDeltaMovement().x;
        double startX = pig.getX();
        pig.travel(new Vec3(-1, 0, 0));
        double travel = pig.getX() - startX;
        pig.discard();
        return new Vec3(current, travel, 0);
    }

    @GameTest(template="empty_7x7x7", timeoutTicks=100)
    public static void placedFrozenObsidianFreezesEntitiesLikePowderSnow(GameTestHelper h) {
        var pos = new net.minecraft.core.BlockPos(3, 2, 3);
        h.setBlock(pos, FrozenObsidian.INSTANCE.defaultFluidState().createLegacyBlock());
        var pig = h.spawn(EntityTypes.PIG, pos);
        h.succeedWhen(() -> h.assertTrue(pig.getTicksFrozen() > 20, "entity inside Frozen Obsidian is not freezing"));
    }

    @GameTest(template="empty_7x7x7", timeoutTicks=100)
    public static void placedHeatedCoolantBurnsLikeLava(GameTestHelper h) {
        var pos = new net.minecraft.core.BlockPos(3, 2, 3);
        h.setBlock(pos, HeatedCoolant.INSTANCE.defaultFluidState().createLegacyBlock());
        var pig = h.spawn(EntityTypes.PIG, pos);
        float health = pig.getHealth();
        h.succeedWhen(() -> h.assertTrue(pig.isOnFire() && pig.getHealth() < health, "entity inside heated coolant is not burning"));
    }

    @GameTest(template="empty_7x7x7")
    public static void heatedCoolantFlowKeepsOrigin(GameTestHelper h) {
        var profile = FluidTransitionRegistry.liquidTransition(FrozenObsidian.INSTANCE);
        var hot = new FluidStack(HeatedCoolant.INSTANCE, 1000);
        hot.applyComponents(CoolantComponents.heated(profile));
        var bucket = HeatedCoolantBucket.filled(hot);
        var placer = (HeatedCoolantBucket.BucketWithOrigin) bucket.getItem();
        var source = h.absolutePos(new BlockPos(3, 4, 3));
        var flow = source.below();
        h.assertTrue(placer.emptyContents(null, h.getLevel(), source, null, bucket), "heated coolant was not placed");
        h.startSequence().thenWaitUntil(() -> {
            h.assertTrue(HeatedCoolant.INSTANCE.isSame(h.getLevel().getFluidState(flow).getType()), "heated coolant has not flowed");
        }).thenExecute(() -> {
            var chunk = h.getLevel().getChunkAt(flow);
            h.assertTrue(HeatedCoolantBucket.origin(bucket).equals(biggerreactorsrework.fluids.PlacedCoolantOrigins.get(chunk, flow)),
                    "flow lost its source's coolant origin");
            h.getLevel().setBlock(flow, Blocks.STONE.defaultBlockState(), 3);
            h.assertTrue(biggerreactorsrework.fluids.PlacedCoolantOrigins.get(chunk, flow) == null,
                    "replaced fluid left a stale origin");
        }).thenSucceed();
    }

    @GameTest(template="empty_7x7x7")
    public static void heatedCoolantBucketKeepsItsOriginInTanksAndInTheWorld(GameTestHelper h) {
        var profile = FluidTransitionRegistry.liquidTransition(FrozenObsidian.INSTANCE);
        var hot = new FluidStack(HeatedCoolant.INSTANCE, 1000);
        hot.applyComponents(CoolantComponents.heated(profile));
        var bucket = hot.getFluidType().getBucket(hot);
        h.assertTrue(bucket.is(HeatedCoolant.INSTANCE.getBucket()), "heated coolant has no bucket");
        var handler = net.neoforged.neoforge.transfer.access.ItemAccess.forStack(bucket).getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.ITEM);
        h.assertTrue(FluidStack.isSameFluidSameComponents(net.neoforged.neoforge.transfer.fluid.FluidUtil.getStack(handler, 0), hot), "bucket lost the coolant origin");

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
