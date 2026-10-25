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
    public static final DeferredHolder<RecipeSerializer<?>, ReprocessingRecipe.Serializer> SERIALIZER = SERIALIZERS.register("cyanite_reprocessing", ReprocessingRecipe.Serializer::new);
    public static void register(IEventBus bus) { TYPES.register(bus); SERIALIZERS.register(bus); }
    private record Snapshot(java.util.Collection<RecipeHolder<?>> source,List<RecipeHolder<ReprocessingRecipe>> sorted) { }
    private static final java.util.Map<RecipeManager,Snapshot> CACHE = new java.util.WeakHashMap<>();
    public static synchronized List<RecipeHolder<ReprocessingRecipe>> all(Level level) {
        if (level == null) return List.of();
        var manager=level.getRecipeManager();var source=manager.getRecipes();var cached=CACHE.get(manager);
        if(cached!=null&&cached.source()==source)return cached.sorted();
        var sorted=manager.getAllRecipesFor(TYPE.get()).stream().sorted(Comparator.comparing(r -> r.id().toString())).toList();
        CACHE.put(manager,new Snapshot(source,sorted));return sorted;
    }
    private ReprocessingRecipes() { }
}
