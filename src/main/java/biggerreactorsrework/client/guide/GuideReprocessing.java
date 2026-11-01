package biggerreactorsrework.client.guide;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import biggerreactorsrework.machine.recipe.ReprocessingRecipe;

/** Native machine process card; follows recipe replacement without a viewer. */
// Modified 2026-11-01: inputs side by side, one output slot, and labelled requirements.
record GuideReprocessing(Identifier id) implements GuideElement {
    public int height(GuideScreen screen,int width){return 100;}
    public void render(GuideScreen screen,GuiGraphicsExtractor g,int x,int y,int width,int mx,int my){
        var level=Minecraft.getInstance().level;
        var holder=level==null?null:biggerreactorsrework.machine.recipe.RecipeAccess.byKey(level,net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,id)).orElse(null);
        if(holder==null||!(holder.value() instanceof ReprocessingRecipe recipe)){
            screen.fixedText(g,Component.translatable("guide.biggerreactorsrework.recipe_missing"),x,y,width,GuideElements.MUTED);return;
        }
        var options=biggerreactorsrework.machine.recipe.RecipeAccess.stacks(recipe.ingredient());
        var input=options.length==0?ItemStack.EMPTY:options[(int)(net.minecraft.util.Util.getMillis()/1000%options.length)].copyWithCount(recipe.count());
        boolean usesFluid=recipe.totalFluid()>0;
        screen.fixedText(g,recipe.producesFluid()?recipe.fluidResult().getHoverName():recipe.result().getHoverName(),x,y,width,GuideElements.HEADING);
        screen.fixedText(g,Component.translatable("guide.biggerreactorsrework.process.machine"),x,y+12,width,GuideElements.MUTED);

        int row=y+26,slot=x+Math.max(0,(width-(usesFluid?134:114))/2);
        screen.itemSlot(g,input,slot,row,mx,my);
        if(usesFluid){slot+=20;screen.fluidSlot(g,recipe.fluid(),slot,row,mx,my);}
        GuideElements.arrow(g,slot+25,row+5);
        screen.itemSlot(g,new ItemStack(biggerreactorsrework.machine.blocks.CyaniteReprocessor.INSTANCE),slot+48,row,mx,my);
        GuideElements.arrow(g,slot+73,row+5);
        if(recipe.producesFluid())screen.fluidSlot(g,recipe.fluidResult().getFluid(),slot+96,row,mx,my);
        else screen.itemSlot(g,recipe.result(),slot+96,row,mx,my);

        int line=y+49;
        if(usesFluid){screen.fixedText(g,Component.translatable("guide.biggerreactorsrework.process.fluid",recipe.fluid().getFluidType().getDescription(),recipe.totalFluid()),x,line,width,GuideElements.BODY);line+=11;}
        screen.fixedText(g,Component.translatable("guide.biggerreactorsrework.process.energy",(long)recipe.energy()*recipe.duration(),recipe.energy()),x,line,width,GuideElements.BODY);line+=11;
        String seconds=recipe.duration()%20==0?Integer.toString(recipe.duration()/20):Double.toString(recipe.duration()/20d);
        screen.fixedText(g,Component.translatable("guide.biggerreactorsrework.process.time",seconds),x,line,width,GuideElements.BODY);line+=11;
        if(recipe.producesFluid())screen.fixedText(g,Component.translatable("guide.biggerreactorsrework.process.produces",recipe.fluidResult().getAmount()),x,line,width,GuideElements.BODY);
    }
}
