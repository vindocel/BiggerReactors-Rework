package biggerreactorsrework.machine.recipe;

import biggerreactorsrework.BiggerReactors;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.world.level.Level;
import java.util.Comparator;
import java.util.List;

public final class ReprocessingRecipes {
    private static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, BiggerReactors.modid);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, BiggerReactors.modid);
    public static final DeferredHolder<RecipeType<?>, RecipeType<ReprocessingRecipe>> TYPE = TYPES.register("cyanite_reprocessing", () -> new RecipeType<>() { public String toString() { return BiggerReactors.modid + ":cyanite_reprocessing"; } });
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ReprocessingRecipe>> SERIALIZER = SERIALIZERS.register("cyanite_reprocessing", () -> new RecipeSerializer<>(ReprocessingRecipe.CODEC, ReprocessingRecipe.STREAM_CODEC));
    public static void register(IEventBus bus) { TYPES.register(bus); SERIALIZERS.register(bus); }
    private record Snapshot(RecipeMap source,List<RecipeHolder<ReprocessingRecipe>> sorted) { }
    private static Snapshot cache;
    public static synchronized List<RecipeHolder<ReprocessingRecipe>> all(Level level) {
        if (level == null) return List.of();
        var source=RecipeAccess.map(level);
        if(cache!=null&&cache.source()==source)return cache.sorted();
        var sorted=source.byType(TYPE.get()).stream().sorted(Comparator.comparing(r -> r.id().identifier().toString())).toList();
        cache=new Snapshot(source,sorted);return sorted;
    }
    private ReprocessingRecipes() { }
}
