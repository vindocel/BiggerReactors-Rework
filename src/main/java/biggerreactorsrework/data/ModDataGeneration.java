package biggerreactorsrework.data;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.items.tools.GuideBookItem;
import biggerreactorsrework.items.ingots.CyaniteIngot;
import biggerreactorsrework.items.ingots.BlutoniumIngot;
import biggerreactorsrework.machine.recipe.ReprocessingRecipe;
import biggerreactorsrework.multiblocks.reactor.blocks.ReactorCasing;
import biggerreactorsrework.multiblocks.turbine.blocks.TurbineCasing;
import biggerreactorsrework.multiblocks.turbine.blocks.TurbineRedstonePort;
import biggerreactorsrework.multiblocks.battery.blocks.BatteryCasing;
import biggerreactorsrework.multiblocks.battery.blocks.BatteryRedstonePort;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.data.loot.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Incremental native datagen; existing IDs and balances are preserved. */
public final class ModDataGeneration {
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath(BiggerReactors.modid,path);}
    public static void gather(GatherDataEvent event){
        var generator=event.getGenerator();var output=generator.getPackOutput();var lookup=event.getLookupProvider();
        generator.addProvider(event.includeServer(),new Recipes(output,lookup));
        generator.addProvider(event.includeServer(),new LootTableProvider(output,Set.of(),List.of(new LootTableProvider.SubProviderEntry(Loot::new,LootContextParamSets.BLOCK)),lookup));
        generator.addProvider(event.includeServer(),new MiningTags(output,lookup,event.getExistingFileHelper()));
    }
    private static final class Recipes extends RecipeProvider {
        Recipes(PackOutput output,CompletableFuture<HolderLookup.Provider> lookup){super(output,lookup);}
        @Override protected void buildRecipes(RecipeOutput output){
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC,GuideBookItem.INSTANCE).requires(Items.BOOK).requires(ReactorCasing.INSTANCE)
                .unlockedBy("has_casing",has(ReactorCasing.INSTANCE)).save(output,id("crafting/guide_book"));
            ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE,TurbineRedstonePort.INSTANCE)
                .pattern("CRC").pattern("RGR").pattern("CRC").define('C',TurbineCasing.INSTANCE).define('R',Items.REDSTONE).define('G',Items.GOLD_INGOT)
                .unlockedBy("has_casing",has(TurbineCasing.INSTANCE)).save(output,id("crafting/turbine/turbine_redstone_port"));
            ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE,BatteryRedstonePort.INSTANCE)
                .pattern("CRC").pattern("RGR").pattern("CRC").define('C',BatteryCasing.INSTANCE).define('R',Items.REDSTONE).define('G',Items.GOLD_INGOT)
                .unlockedBy("has_casing",has(BatteryCasing.INSTANCE)).save(output,id("crafting/battery/battery_redstone_port"));
            output.accept(id("cyanite_reprocessing"),new ReprocessingRecipe(Ingredient.of(CyaniteIngot.INSTANCE),2,new net.minecraft.world.item.ItemStack(BlutoniumIngot.INSTANCE),Fluids.WATER,-1,-1,-1),null);
        }
    }
    private static final class Loot extends BlockLootSubProvider {
        Loot(HolderLookup.Provider lookup){super(Set.of(),FeatureFlags.REGISTRY.allFlags(),lookup);}
        @Override protected void generate(){dropSelf(TurbineRedstonePort.INSTANCE);dropSelf(BatteryRedstonePort.INSTANCE);}
        @Override protected Iterable<Block> getKnownBlocks(){return List.of(TurbineRedstonePort.INSTANCE,BatteryRedstonePort.INSTANCE);}
    }

    private static final class MiningTags extends net.minecraft.data.tags.TagsProvider<Block> {
        MiningTags(PackOutput output,CompletableFuture<HolderLookup.Provider> lookup,net.neoforged.neoforge.common.data.ExistingFileHelper helper){super(output,net.minecraft.core.registries.Registries.BLOCK,lookup,BiggerReactors.modid,helper);}
        private void add(net.minecraft.tags.TagKey<Block> key,String... names){
            for(String name:names)tag(key).add(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK,id(name)));
        }
        @Override protected void addTags(HolderLookup.Provider lookup){
            add(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE,
                "reactor_power_tap",
                "reactor_glass",
                "reactor_manifold",
                "reactor_casing",
                "reactor_redstone_port",
                "reactor_computer_port",
                "reactor_access_port",
                "reactor_terminal",
                "reactor_control_rod",
                "reactor_fuel_rod",
                "reactor_coolant_port",
                "turbine_glass",
                "turbine_rotor_shaft",
                "turbine_terminal",
                "turbine_rotor_blade",
                "turbine_power_tap",
                "turbine_fluid_port",
                "turbine_computer_port",
                "turbine_casing",
                "turbine_rotor_bearing",
                "heat_exchanger_computer_port",
                "heat_exchanger_condenser_channel",
                "heat_exchanger_terminal",
                "heat_exchanger_glass",
                "heat_exchanger_fluid_port",
                "heat_exchanger_evaporator_channel",
                "heat_exchanger_casing",
                "cyanite_reprocessor",
                "uranium_block",
                "raw_uranium_block",
                "ludicrite_block",
                "blutonium_block",
                "uranium_ore",
                "deepslate_uranium_ore",
                "cyanite_block",
                "graphite_block",
                "turbine_redstone_port",
                "battery_casing",
                "battery_glass",
                "battery_terminal",
                "battery_energy_port",
                "battery_redstone_port",
                "battery_cell");
            add(net.minecraft.tags.BlockTags.NEEDS_IRON_TOOL,
                "reactor_power_tap",
                "reactor_glass",
                "reactor_manifold",
                "reactor_casing",
                "reactor_redstone_port",
                "reactor_computer_port",
                "reactor_access_port",
                "reactor_terminal",
                "reactor_control_rod",
                "reactor_fuel_rod",
                "reactor_coolant_port",
                "turbine_glass",
                "turbine_rotor_shaft",
                "turbine_terminal",
                "turbine_rotor_blade",
                "turbine_power_tap",
                "turbine_fluid_port",
                "turbine_computer_port",
                "turbine_casing",
                "turbine_rotor_bearing",
                "heat_exchanger_computer_port",
                "heat_exchanger_condenser_channel",
                "heat_exchanger_terminal",
                "heat_exchanger_glass",
                "heat_exchanger_fluid_port",
                "heat_exchanger_evaporator_channel",
                "heat_exchanger_casing",
                "cyanite_reprocessor",
                "uranium_block",
                "raw_uranium_block",
                "ludicrite_block",
                "blutonium_block",
                "uranium_ore",
                "deepslate_uranium_ore",
                "cyanite_block",
                "graphite_block",
                "turbine_redstone_port",
                "battery_casing",
                "battery_glass",
                "battery_terminal",
                "battery_energy_port",
                "battery_redstone_port",
                "battery_cell");
        }
    }
    private ModDataGeneration(){}
}
