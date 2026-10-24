package biggerreactorsrework.client.deps.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.Config;
import biggerreactorsrework.client.deps.RecipeViewerData;
import biggerreactorsrework.client.deps.RecipeViewers;
import biggerreactorsrework.machine.blocks.CyaniteReprocessor;
import biggerreactorsrework.client.deps.jei.classic.reactor.BlockModeratorCategory;
import biggerreactorsrework.client.deps.jei.classic.reactor.FluidModeratorCategory;
import biggerreactorsrework.client.deps.jei.classic.turbine.CoilCategory;
import biggerreactorsrework.multiblocks.reactor.blocks.ReactorTerminal;
import biggerreactorsrework.multiblocks.turbine.blocks.TurbineTerminal;

import javax.annotation.Nonnull;
import java.util.List;

@JeiPlugin
public class BiggerReactorsJEIPlugin implements IModPlugin {

    @Nonnull
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(BiggerReactors.modid, "jei_plugin");
    }

    /** Skipped when EMI is installed, which has its own plugin. */
    private static boolean enabled() {
        return !RecipeViewers.emi();
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        JeiBridge.setRuntime(runtime);
    }

    @Override
    public void onRuntimeUnavailable() {
        JeiBridge.setRuntime(null);
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        if (!enabled()) {
            return;
        }
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new CyaniteReprocessingCategory(guiHelper));
        registration.addRecipeCategories(new BlockModeratorCategory(guiHelper));
        registration.addRecipeCategories(new FluidModeratorCategory(guiHelper));
        registration.addRecipeCategories(new CoilCategory(guiHelper));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        if (!enabled()) {
            return;
        }
        registration.addRecipeCatalyst(new ItemStack(CyaniteReprocessor.INSTANCE), CyaniteReprocessingCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ReactorTerminal.INSTANCE), BlockModeratorCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ReactorTerminal.INSTANCE), FluidModeratorCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(TurbineTerminal.INSTANCE), CoilCategory.RECIPE_TYPE);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (!enabled() || !Config.CONFIG.EnableJEIIntegration) {
            return;
        }
        registration.addRecipes(CyaniteReprocessingCategory.RECIPE_TYPE, List.of(new CyaniteReprocessingCategory.Recipe()));
        registration.addRecipes(CoilCategory.RECIPE_TYPE, RecipeViewerData.coils().stream()
                .map(c -> new CoilCategory.Recipe(c.block(), c.data())).toList());
        registration.addRecipes(FluidModeratorCategory.RECIPE_TYPE, RecipeViewerData.moderatorFluids().stream()
                .map(m -> new FluidModeratorCategory.Recipe(new FluidStack(m.fluid(), 1000), m.properties())).toList());
        registration.addRecipes(BlockModeratorCategory.RECIPE_TYPE, RecipeViewerData.moderatorBlocks().stream()
                .map(m -> new BlockModeratorCategory.Recipe(m.block(), m.properties())).toList());
    }
}
