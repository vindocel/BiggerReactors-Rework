package biggerreactorsrework.gametest;

import biggerreactorsrework.BiggerReactors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.fml.ModList;

/**
 * Checks that the migrated datapack content (recipes, loot tables) parses on the native platform.
 */
public final class ContentGameTests {
    private ContentGameTests() {
    }

    private static long countOwn(GameTestHelper helper, RecipeType<?> type) {
        //noinspection unchecked,rawtypes
        return helper.getLevel().getServer().getRecipeManager().recipeMap().values().stream()
                .filter(holder -> holder.value().getType() == type)
                .filter(holder -> holder.id().identifier().getNamespace().equals(BiggerReactors.modid))
                .count();
    }

    /**
     * 52 crafting recipe files ship with the mod: 48 always load, 3 computer ports
     * need CC:Tweaked or OpenComputers and 1 ludicrite variant needs Thermal Expansion (modified 2026-10-31).
     * 9 smelting and 9 blasting recipes always load.
     */
    @GameTest(template = "empty_7x7x7")
    public static void allRecipesLoad(GameTestHelper helper) {
        final var mods = ModList.get();
        long expectedCrafting = 48 + (mods.isLoaded("opencomputers") || mods.isLoaded("computercraft") ? 3 : 0) + (mods.isLoaded("thermal_expansion") ? 1 : 0);
        long crafting = countOwn(helper, RecipeType.CRAFTING);
        long smelting = countOwn(helper, RecipeType.SMELTING);
        long blasting = countOwn(helper, RecipeType.BLASTING);
        helper.assertTrue(crafting == expectedCrafting, "crafting recipes: " + crafting + ", expected " + expectedCrafting);
        helper.assertTrue(smelting == 9, "smelting recipes: " + smelting);
        helper.assertTrue(blasting == 9, "blasting recipes: " + blasting);
        helper.succeed();
    }

    private static long countOwnOfType(GameTestHelper helper, String recipeType) {
        return helper.getLevel().getServer().getRecipeManager().recipeMap().values().stream()
                .filter(holder -> holder.id().identifier().getNamespace().equals(BiggerReactors.modid))
                .filter(holder -> {
                    final var key = BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType());
                    return key != null && key.toString().equals(recipeType);
                })
                .count();
    }

    /**
     * Compatibility recipes load only with their mod present: 5 crushing + 3 enriching for Mekanism,
     * 8 inscriber recipes for Applied Energistics 2.
     */
    @GameTest(template = "empty_7x7x7")
    public static void compatRecipesLoad(GameTestHelper helper) {
        final var mods = ModList.get();
        final long crushing = countOwnOfType(helper, "mekanism:crushing");
        final long enriching = countOwnOfType(helper, "mekanism:enriching");
        final long inscriber = countOwnOfType(helper, "ae2:inscriber");
        helper.assertTrue(crushing == (mods.isLoaded("mekanism") ? 5 : 0), "mekanism crushing recipes: " + crushing);
        helper.assertTrue(enriching == (mods.isLoaded("mekanism") ? 3 : 0), "mekanism enriching recipes: " + enriching);
        helper.assertTrue(inscriber == (mods.isLoaded("ae2") ? 8 : 0), "ae2 inscriber recipes: " + inscriber);
        helper.succeed();
    }

    /**
     * Every block of the mod that drops itself or something else has a loot table that parses.
     */
    @GameTest(template = "empty_7x7x7")
    public static void blockLootTablesLoad(GameTestHelper helper) {
        final var registries = helper.getLevel().getServer().reloadableRegistries();
        int checked = 0;
        for (var block : BuiltInRegistries.BLOCK) {
            final var id = BuiltInRegistries.BLOCK.getKey(block);
            if (!id.getNamespace().equals(BiggerReactors.modid)) {
                continue;
            }
            final var lootKey = block.getLootTable();
            if (lootKey.isEmpty() || lootKey.get().identifier().getPath().equals("empty")) {
                continue;
            }
            final var table = registries.getLootTable(ResourceKey.create(Registries.LOOT_TABLE, lootKey.get().identifier()));
            if (table == LootTable.EMPTY) {
                // fluid blocks have no drops; everything else must have a table
                if (!(block instanceof net.minecraft.world.level.block.LiquidBlock)) {
                    helper.fail("missing or invalid loot table for " + id);
                }
                continue;
            }
            checked++;
        }
        helper.assertTrue(checked >= 30, "loot tables checked: " + checked);
        helper.succeed();
    }
    /** Added 2026-11-01: map renderers draw MapColor.NONE as black or empty. */
    @GameTest(template="empty_7x7x7")
    public static void everySolidBlockDeclaresAMapColor(GameTestHelper h) {
        var missing = net.minecraft.core.registries.BuiltInRegistries.BLOCK.entrySet().stream()
                .filter(e -> e.getKey().identifier().getNamespace().equals(biggerreactorsrework.BiggerReactors.modid))
                .filter(e -> !(e.getValue() instanceof net.minecraft.world.level.block.LiquidBlock))
                .filter(e -> e.getValue().defaultMapColor() == net.minecraft.world.level.material.MapColor.NONE)
                .map(e -> e.getKey().identifier().getPath()).toList();
        h.assertTrue(missing.isEmpty(), "blocks without a map color: " + missing);
        h.succeed();
    }
}
