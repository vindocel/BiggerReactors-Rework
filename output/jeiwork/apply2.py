root='src/main/java/biggerreactorsrework/client/deps/'
def wr(p,s):
    crlf='\r\n' in open(p,newline='').read()
    if crlf: s=s.replace('\n','\r\n')
    open(p,'w',newline='').write(s)

common_methods='''
    @Override
    public RecipeType<Recipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("%(title)s");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return Stats.WIDTH;
    }

    @Override
    public int getHeight() {
        return %(height)s;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, Stats.slotY(recipe.stats()) + 1).%(add)s;
    }

    @Override
    public void draw(Recipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Stats.draw(guiGraphics, recipe.stats());
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, Recipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        int row = Stats.row(recipe.stats(), mouseX, mouseY);
        if (row >= 0) {
            tooltip.addAll(Stats.tooltip(recipe.stats().get(row)));
        }
    }
'''
def category(pkg, cls, uid, title, terminal, termimport, inputtype, inputimport, add, height, statsexpr, ctorarg):
    imports=sorted(set(i for i in [
        'mezz.jei.api.gui.builder.IRecipeLayoutBuilder','mezz.jei.api.gui.builder.ITooltipBuilder','mezz.jei.api.gui.drawable.IDrawable',
        'mezz.jei.api.gui.ingredient.IRecipeSlotsView','mezz.jei.api.helpers.IGuiHelper','mezz.jei.api.recipe.IFocusGroup',
        'mezz.jei.api.recipe.RecipeIngredientRole','mezz.jei.api.recipe.RecipeType','mezz.jei.api.recipe.category.IRecipeCategory',
        'net.minecraft.client.gui.GuiGraphics','net.minecraft.network.chat.Component','net.minecraft.resources.ResourceLocation',
        'net.minecraft.world.item.ItemStack','biggerreactorsrework.BiggerReactors','biggerreactorsrework.client.deps.RecipeViewerData.Stats',
        termimport, inputimport, ctorarg[2], 'java.util.List'] if i))
    imp=''.join('import %s;\n'%i for i in imports if i)
    return '''package %(pkg)s;

%(imp)s
public class %(cls)s implements IRecipeCategory<%(cls)s.Recipe> {
    private final IDrawable icon;
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(BiggerReactors.modid, "%(uid)s");
    public static final RecipeType<Recipe> RECIPE_TYPE = new RecipeType<>(UID, Recipe.class);

    public %(cls)s(IGuiHelper guiHelper) {
        icon = guiHelper.createDrawableItemStack(new ItemStack(%(terminal)s.INSTANCE));
    }
%(methods)s
    public static class Recipe {
        private final %(inputtype)s input;
        private final %(ctype)s %(cname)s;
        private final List<Stats.Stat> stats;

        public Recipe(%(inputtype)s input, %(ctype)s %(cname)s) {
            this.input = input;
            this.%(cname)s = %(cname)s;
            this.stats = %(statsexpr)s;
        }

        public %(inputtype)s getInput() {
            return input;
        }

        public %(ctype)s get%(Cname)s() {
            return %(cname)s;
        }

        public List<Stats.Stat> stats() {
            return stats;
        }
    }
}
''' % dict(pkg=pkg,imp=imp,cls=cls,uid=uid,terminal=terminal,inputtype=inputtype,ctype=ctorarg[0],cname=ctorarg[1],Cname=ctorarg[1][0].upper()+ctorarg[1][1:],
        statsexpr=statsexpr, methods=common_methods%dict(title=title,height=height,add=add))

MP=('ReactorModeratorRegistry.IModeratorProperties','moderatorProperties','biggerreactorsrework.registries.ReactorModeratorRegistry')
CD=('TurbineCoilRegistry.CoilData','coilData','biggerreactorsrework.registries.TurbineCoilRegistry')
wr(root+'jei/classic/reactor/BlockModeratorCategory.java', category('biggerreactorsrework.client.deps.jei.classic.reactor','BlockModeratorCategory','classic/reactor_moderator_block',
   'jei.biggerreactorsrework.classic.reactor_moderator_block','ReactorTerminal','biggerreactorsrework.multiblocks.reactor.blocks.ReactorTerminal','ItemStack',None,
   'addItemStack(recipe.getInput())','4 * Stats.ROW','Stats.moderator(moderatorProperties)',MP))
wr(root+'jei/classic/reactor/FluidModeratorCategory.java', category('biggerreactorsrework.client.deps.jei.classic.reactor','FluidModeratorCategory','classic/reactor_moderator_fluid',
   'jei.biggerreactorsrework.classic.reactor_moderator_fluid','ReactorTerminal','biggerreactorsrework.multiblocks.reactor.blocks.ReactorTerminal','FluidStack','net.neoforged.neoforge.fluids.FluidStack',
   'addFluidStack(recipe.getInput().getFluid(), 1000)','4 * Stats.ROW','Stats.moderator(moderatorProperties)',MP))
wr(root+'jei/classic/turbine/CoilCategory.java', category('biggerreactorsrework.client.deps.jei.classic.turbine','CoilCategory','classic/turbine_coil',
   'jei.biggerreactorsrework.classic.turbine_coil_block','TurbineTerminal','biggerreactorsrework.multiblocks.turbine.blocks.TurbineTerminal','ItemStack',None,
   'addItemStack(recipe.getInput())','3 * Stats.ROW','Stats.coil(coilData)',CD))
print('ok')
