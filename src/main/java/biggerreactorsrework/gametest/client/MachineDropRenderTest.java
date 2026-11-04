package biggerreactorsrework.gametest.client;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.client.FluidSprites;
import biggerreactorsrework.client.HeatedFluidSprites;
import biggerreactorsrework.core.multiblock.IAssemblyStateBlock;
import biggerreactorsrework.fluids.FrozenObsidian;
import biggerreactorsrework.fluids.HeatedCoolant;
import biggerreactorsrework.fluids.HeatedCoolantBucket;
import biggerreactorsrework.fluids.LiquidObsidian;
import biggerreactorsrework.registries.CoolantComponents;
import biggerreactorsrework.registries.FluidTransitionRegistry;
import biggerreactorsrework.util.GuideStructures;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Added 2026-11-03: opt-in real client regression for rendering machine drops with shaders. */
@EventBusSubscriber(modid = BiggerReactors.modid, value = Dist.CLIENT)
public final class MachineDropRenderTest {
    private static List<GuideStructures.Example> machines;
    private static boolean started;
    private static int caseIndex;
    private static int tick;
    private static int visibleTicks;
    private static BlockPos target;
    private static Block expectedDrop;

    private MachineDropRenderTest() { }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) throws ReflectiveOperationException {
        if (!Boolean.getBoolean("rework.testMachineDrops")) return;
        var client = Minecraft.getInstance();
        if (!started && client.gui.screen() instanceof TitleScreen) {
            started = true;
            require(SharedConstants.IS_RUNNING_IN_IDE && !Boolean.getBoolean("neoforge.disableGlValidation"),
                    "Development graphics validation must be enabled");
            requireShader();
            machines = List.of(GuideStructures.exchanger(), GuideStructures.reactor(5, true),
                    GuideStructures.turbine(), GuideStructures.battery());
            var name = "MachineDropRenderTest" + System.currentTimeMillis();
            var settings = new LevelSettings(name, GameType.CREATIVE,
                    new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT);
            client.createWorldOpenFlows().createFreshLevel(name, settings, new WorldOptions(0, false, false),
                    registries -> registries.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)
                            .value().createWorldDimensions(), client.gui.screen());
            return;
        }
        if (!started || client.level == null || client.player == null || client.getSingleplayerServer() == null) return;
        if (caseIndex == machines.size() * 2) {
            verifyHeatedCoolant(client);
            return;
        }

        var example = machines.get(caseIndex / 2);
        tick++;
        if (tick == 10) place(client, example);
        if (tick >= 60 && tick < 80) client.level.destroyBlockProgress(1000, target, (tick - 60) / 2);
        if (tick == 80) {
            requireShader();
            client.level.destroyBlockProgress(1000, target, -1);
            onServer(client, () -> {
                var level = client.getSingleplayerServer().overworld();
                var state = level.getBlockState(target);
                require(state.hasProperty(IAssemblyStateBlock.ASSEMBLED) && state.getValue(IAssemblyStateBlock.ASSEMBLED),
                        "Machine did not assemble: " + example.id());
                require(level.destroyBlock(target, true), "Failed to break " + expectedDrop);
            });
        }
        if (tick > 80 && client.level.getEntitiesOfClass(ItemEntity.class, new AABB(target).inflate(3),
                item -> item.getItem().is(expectedDrop.asItem())).size() > 0) visibleTicks++;
        if (tick == 130) {
            require(visibleTicks >= 20, "Expected drop did not reach the rendered client: " + expectedDrop);
            BiggerReactors.LOGGER.info("MACHINE_DROP_RENDER_CASE_PASS: {} / {} ({} client ticks)", example.id(), expectedDrop, visibleTicks);
            visibleTicks = 0;
            tick = 0;
            caseIndex++;
        }
    }

    private static void verifyHeatedCoolant(Minecraft client) {
        var obsidian = new BlockPos(88, -58, 10);
        var frozen = obsidian.east(4);
        if (++tick == 1) onServer(client, () -> {
            var level = client.getSingleplayerServer().overworld();
            var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
            player.teleportTo(level, 90, -55, 6, Set.of(), 0, 35, false);
            for (var pos : List.of(obsidian, frozen)) {
                var cold = pos.equals(obsidian) ? LiquidObsidian.INSTANCE : FrozenObsidian.INSTANCE;
                var hot = new FluidStack(HeatedCoolant.INSTANCE, 1000);
                hot.applyComponents(CoolantComponents.heated(FluidTransitionRegistry.liquidTransition(cold)));
                var bucket = HeatedCoolantBucket.filled(hot);
                var placer = (HeatedCoolantBucket.BucketWithOrigin) bucket.getItem();
                require(placer.emptyContents(null, level, pos, null, bucket), "Could not place heated coolant");
            }
        });
        if (tick != 100) return;
        require(HeatedFluidSprites.placedSource(obsidian) == LiquidObsidian.INSTANCE, "Client lost the Liquid Obsidian origin");
        require(HeatedFluidSprites.placedSource(frozen) == FrozenObsidian.INSTANCE, "Client lost the Frozen Obsidian origin");
        require(HeatedFluidSprites.placedSource(frozen.below()) == FrozenObsidian.INSTANCE, "Client lost the flowing coolant origin");
        for (var source : List.of(LiquidObsidian.INSTANCE, FrozenObsidian.INSTANCE)) {
            for (boolean flowing : new boolean[]{false, true}) {
                var id = HeatedFluidSprites.texture(source, flowing);
                require(!id.equals(HeatedFluidSprites.fallback(flowing)) && FluidSprites.blockSprite(id).contents().name().equals(id),
                        "Heated coolant sprite did not bake: " + id);
            }
        }
        BiggerReactors.LOGGER.info("HEATED_COOLANT_CLIENT_PASS: both source origins, flowing origin and four heated sprites");
        BiggerReactors.LOGGER.info("MACHINE_DROP_RENDER_PASS: eight real block drops with shaders and validation enabled");
        client.stop();
    }

    private static void place(Minecraft client, GuideStructures.Example example) {
        var origin = new BlockPos(caseIndex * 12, -59, 0);
        var entry = example.parts().entrySet().stream().filter(part -> caseIndex % 2 == 0
                ? part.getValue().getBlock().toString().contains("_terminal")
                : part.getKey().equals(new BlockPos(0, 1, 0))).findFirst().orElseThrow();
        target = origin.offset(entry.getKey());
        expectedDrop = entry.getValue().getBlock();
        onServer(client, () -> {
            var server = client.getSingleplayerServer();
            var level = server.overworld();
            var player = server.getPlayerList().getPlayers().getFirst();
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            boolean west = entry.getKey().getX() == 0;
            player.teleportTo(level, target.getX() + (west ? -2.5 : .5), target.getY(),
                    target.getZ() + (west ? .5 : -2.5), Set.of(), west ? -90 : 0, 22, false);
            example.parts().forEach((pos, state) -> level.setBlockAndUpdate(origin.offset(pos), state));
        });
    }

    private static void onServer(Minecraft client, Runnable work) {
        CompletableFuture.runAsync(work, client.getSingleplayerServer()).join();
    }

    private static void requireShader() throws ReflectiveOperationException {
        var type = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
        var api = type.getMethod("getInstance").invoke(null);
        require(Boolean.TRUE.equals(type.getMethod("isShaderPackInUse").invoke(api)), "An active shader pack is required");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
