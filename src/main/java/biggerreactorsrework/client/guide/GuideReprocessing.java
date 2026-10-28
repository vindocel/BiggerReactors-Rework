package biggerreactorsrework.client.guide;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import biggerreactorsrework.machine.recipe.ReprocessingRecipe;

/** Native machine process card; follows recipe replacement without a viewer. */
record GuideReprocessing(ResourceLocation id) implements GuideElement {
    public int height(GuideScreen screen,int width){return 100;}
    public void render(GuideScreen screen,GuiGraphics g,int x,int y,int width,int mx,int my){
        var level=Minecraft.getInstance().level;
        var holder=level==null?null:level.getRecipeManager().byKey(id).orElse(null);
        if(holder==null||!(holder.value() instanceof ReprocessingRecipe recipe)){
            screen.fixedText(g,Component.translatable("guide.biggerreactorsrework.recipe_missing"),x,y,width,GuideElements.MUTED);return;
        }
        var options=recipe.ingredient().getItems();
        var input=options.length==0?ItemStack.EMPTY:options[(int)(net.minecraft.Util.getMillis()/1000%options.length)].copyWithCount(recipe.count());
        screen.fixedText(g,Component.translatable("guide.biggerreactorsrework.process.machine"),x,y+14,width,GuideElements.MUTED);
        GuideProcesses.flow(screen,g,x,y+30,width,mx,my,input,
                new ItemStack(biggerreactorsrework.machine.blocks.CyaniteReprocessor.INSTANCE),recipe.result());
        if(recipe.producesFluid()) {
            screen.fluidSlot(g,recipe.fluidResult().getFluid(),x+width-20,y+30,mx,my);
            screen.fixedText(g,recipe.fluidResult().getHoverName(),x,y,width,GuideElements.HEADING);
        } else screen.fixedText(g,recipe.result().getHoverName(),x,y,width,GuideElements.HEADING);
        screen.fixedText(g,Component.literal(recipe.duration()+" t / "+recipe.energy()+" FE/t / "+recipe.totalFluid()+" mB"),x,y+55,width,GuideElements.BODY);
        if(recipe.totalFluid()>0){
            screen.fluidSlot(g,recipe.fluid(),x,y+70,mx,my);
            screen.fixedText(g,Component.literal(recipe.totalFluid()+" mB"),x+23,y+75,width-23,GuideElements.BODY);
        }
    }
}
