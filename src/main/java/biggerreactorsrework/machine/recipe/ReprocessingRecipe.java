package biggerreactorsrework.machine.recipe;

import biggerreactorsrework.Config;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/** Native machine recipe. Optional costs retain the server configuration. */
public record ReprocessingRecipe(Ingredient ingredient, int count, ItemStack result, Fluid fluid,
                                int ticks, int energyPerTick, int fluidPerTick, FluidStack fluidResult, int fluidPerOperation) implements Recipe<SingleRecipeInput> {
    // Modified 2026-10-27: preserve legacy item recipes and add exclusive fluid output and batch input.
    public ReprocessingRecipe(Ingredient ingredient,int count,ItemStack result,Fluid fluid,int ticks,int energyPerTick,int fluidPerTick) {
        this(ingredient,count,result,fluid,ticks,energyPerTick,fluidPerTick,FluidStack.EMPTY,-1);
    }
    public ReprocessingRecipe {
        if (count < 1 || count > 64 || (result.isEmpty() == fluidResult.isEmpty()) || fluidPerOperation < -1 || ticks == 0 || ticks < -1 || energyPerTick < -1 || fluidPerTick < -1)
            throw new IllegalArgumentException("Invalid reprocessing recipe quantities");
        result = result.copy();
        fluidResult = fluidResult.copy();
    }
    public static final MapCodec<ReprocessingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(ReprocessingRecipe::ingredient),
            Codec.intRange(1, 64).optionalFieldOf("input_count", 1).forGetter(ReprocessingRecipe::count),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("result", ItemStack.EMPTY).forGetter(ReprocessingRecipe::result),
            BuiltInRegistries.FLUID.byNameCodec().optionalFieldOf("fluid", Fluids.WATER).forGetter(ReprocessingRecipe::fluid),
            Codec.intRange(-1, Integer.MAX_VALUE).optionalFieldOf("ticks", -1).forGetter(ReprocessingRecipe::ticks),
            Codec.intRange(-1, Integer.MAX_VALUE).optionalFieldOf("energy_per_tick", -1).forGetter(ReprocessingRecipe::energyPerTick),
            Codec.intRange(-1, Integer.MAX_VALUE).optionalFieldOf("fluid_per_tick", -1).forGetter(ReprocessingRecipe::fluidPerTick),
            FluidStack.CODEC.optionalFieldOf("fluid_result", FluidStack.EMPTY).forGetter(ReprocessingRecipe::fluidResult),
            Codec.intRange(-1, Integer.MAX_VALUE).optionalFieldOf("fluid_per_operation", -1).forGetter(ReprocessingRecipe::fluidPerOperation)
    ).apply(i, ReprocessingRecipe::new));

    public int duration() { return ticks < 0 ? Config.CONFIG.CyaniteReprocessor.TotalWorkTime : ticks; }
    public int energy() { return energyPerTick < 0 ? Config.CONFIG.CyaniteReprocessor.EnergyConsumptionPerTick : energyPerTick; }
    public int water() { return fluidPerTick < 0 ? Config.CONFIG.CyaniteReprocessor.WaterConsumptionPerTick : fluidPerTick; }
    public boolean producesFluid() { return !fluidResult.isEmpty(); }
    @Override public FluidStack fluidResult() { return fluidResult.copy(); }
    public long totalFluid() { return fluidPerOperation >= 0 ? fluidPerOperation : (long) water()*duration(); }
    public int fluidCostAt(int progress) { return fluidPerOperation >= 0 ? (int)(((long)fluidPerOperation*(progress+1))/duration()-((long)fluidPerOperation*progress)/duration()) : water(); }
    @Override public ItemStack result() { return result.copy(); }
    @Override public boolean matches(SingleRecipeInput input, Level level) { return ingredient.test(input.item()) && input.item().getCount() >= count; }
    @Override public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) { return result.copy(); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return result.copy(); }
    // Modified 2026-10-26: machine recipes are displayed by the dedicated viewers, not the crafting book.
    @Override public boolean isSpecial() { return true; }
    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public NonNullList<Ingredient> getIngredients() { return NonNullList.of(Ingredient.EMPTY, ingredient); }
    @Override public RecipeSerializer<?> getSerializer() { return ReprocessingRecipes.SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ReprocessingRecipes.TYPE.get(); }

    public static final class Serializer implements RecipeSerializer<ReprocessingRecipe> {
        @Override public MapCodec<ReprocessingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ReprocessingRecipe> streamCodec() {
            return StreamCodec.of((b, r) -> {
                Ingredient.CONTENTS_STREAM_CODEC.encode(b, r.ingredient);
                b.writeVarInt(r.count);
                ItemStack.OPTIONAL_STREAM_CODEC.encode(b, r.result);
                b.writeResourceLocation(BuiltInRegistries.FLUID.getKey(r.fluid));
                b.writeInt(r.ticks); b.writeInt(r.energyPerTick); b.writeInt(r.fluidPerTick);
                FluidStack.OPTIONAL_STREAM_CODEC.encode(b,r.fluidResult);b.writeInt(r.fluidPerOperation);
            }, b -> new ReprocessingRecipe(Ingredient.CONTENTS_STREAM_CODEC.decode(b), b.readVarInt(),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(b), BuiltInRegistries.FLUID.get(b.readResourceLocation()), b.readInt(), b.readInt(), b.readInt(),FluidStack.OPTIONAL_STREAM_CODEC.decode(b),b.readInt()));
        }
    }
}
