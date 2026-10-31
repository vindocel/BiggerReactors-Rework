package biggerreactorsrework.client.guide;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import java.util.Arrays;
import biggerreactorsrework.items.ingots.UraniumIngot;
import biggerreactorsrework.items.ingots.CyaniteIngot;
import biggerreactorsrework.multiblocks.reactor.blocks.ReactorAccessPort;

/** Acquisition processes, distinct from storage conversions. */
final class GuideProcesses {
    static void flow(GuideScreen screen, GuiGraphicsExtractor g, int x, int y, int width, int mx, int my,
                     ItemStack input, ItemStack machine, ItemStack output) {
        int start=x+Math.max(0,(width-114)/2);
        screen.itemSlot(g,input,start,y,mx,my);
        GuideElements.arrow(g,start+25,y+5);
        screen.itemSlot(g,machine,start+48,y,mx,my);
        GuideElements.arrow(g,start+73,y+5);
        screen.itemSlot(g,output,start+96,y,mx,my);
    }
    record Cooking(Identifier... ids) implements GuideElement {
        public int height(GuideScreen screen,int width){return 61;}
        public void render(GuideScreen screen,GuiGraphicsExtractor g,int x,int y,int width,int mx,int my){
            var level=Minecraft.getInstance().level;
            var recipes=Arrays.stream(ids).map(id->level==null?null:biggerreactorsrework.machine.recipe.RecipeAccess.byKey(level,net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,id)).orElse(null))
                .filter(holder->holder!=null && holder.value() instanceof AbstractCookingRecipe).toList();
            if(recipes.isEmpty()){
                screen.fixedText(g,Component.translatable("guide.biggerreactorsrework.recipe_missing"),x,y,width,GuideElements.MUTED);return;
            }
            long cycle=net.minecraft.util.Util.getMillis()/1500;
            var recipe=(AbstractCookingRecipe)recipes.get((int)(cycle%recipes.size())).value();
            var ingredients=biggerreactorsrework.machine.recipe.RecipeAccess.stacks(recipe.input());
            var input=ingredients.length==0?ItemStack.EMPTY:ingredients[(int)(cycle/recipes.size()%ingredients.length)];
            var output=biggerreactorsrework.machine.recipe.RecipeAccess.result(level.registryAccess(),recipe);
            screen.fixedText(g,output.getHoverName(),x,y,width,GuideElements.HEADING);
            flow(screen,g,x,y+15,width,mx,my,input,new ItemStack(Items.FURNACE),output);
            screen.fixedText(g,Component.translatable("guide.biggerreactorsrework.process.smelting",recipe.cookingTime()),x,y+42,width,GuideElements.MUTED);
        }
    }
    /** Diagram of reactor operation, not a fixed-count crafting recipe. */
    static final class ReactorCycle implements GuideElement {
        public int height(GuideScreen screen,int width){return 49;}
        public void render(GuideScreen screen,GuiGraphicsExtractor g,int x,int y,int width,int mx,int my){
            screen.fixedText(g,Component.translatable("guide.biggerreactorsrework.process.reactor"),x,y,width,GuideElements.HEADING);
            flow(screen,g,x,y+15,width,mx,my,new ItemStack(UraniumIngot.INSTANCE),new ItemStack(ReactorAccessPort.INSTANCE),new ItemStack(CyaniteIngot.INSTANCE));
        }
    }
    private GuideProcesses(){}
}
