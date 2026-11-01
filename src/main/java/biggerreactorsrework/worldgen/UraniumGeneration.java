package biggerreactorsrework.worldgen;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.Config;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Added 2026-11-01: server configuration gates new ore placement without changing existing terrain. */
public final class UraniumGeneration extends PlacementFilter {
    private static final UraniumGeneration INSTANCE = new UraniumGeneration();
    public static final MapCodec<UraniumGeneration> CODEC = MapCodec.unit(INSTANCE);
    private static final DeferredRegister<PlacementModifierType<?>> TYPES =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, BiggerReactors.modid);
    private static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<UraniumGeneration>> TYPE =
            TYPES.register("uranium_generation", () -> () -> CODEC);
    public static void register(IEventBus bus) { TYPES.register(bus); }
    @Override protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos origin) {
        return Config.CONFIG.Worldgen.GenerateUranium;
    }
    @Override public PlacementModifierType<?> type() { return TYPE.get(); }
}
