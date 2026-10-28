package biggerreactorsrework.client.guide;

import java.util.*;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.Minecraft;
import biggerreactorsrework.util.GuideStructures;
import biggerreactorsrework.machine.recipe.ReprocessingRecipes;

/** Content follows docs/planejamento-guia.md. */
final class GuideContent {
    record Chapter(String id, Supplier<ItemStack> icon, List<GuideElement> elements) { }
    static List<Chapter> chapters() {
        var sections=new LinkedHashMap<String,Chapter>();
        sections().forEach(section->sections.put(section.id(),section));
        return List.of(
            combine("start","reactor_terminal",sections,"start","troubleshooting"),
            combine("materials","uranium_ingot",sections,"materials"),
            combine("reactor","reactor_terminal",sections,"build_reactor","reactor_parts","reactor_panel","active","moderators","efficiency"),
            combine("turbine","turbine_terminal",sections,"turbine_parts","build_turbine","automation"),
            combine("battery","battery_terminal",sections,"battery_parts","build_battery"),
            combine("exchanger","heat_exchanger_terminal",sections,"exchanger"));
    }
    private static Chapter combine(String id,String icon,Map<String,Chapter> sections,String... included) {
        var elements=new ArrayList<GuideElement>();
        for(String key:included) {
            // Subsections remain inside their machine chapter, not separate entries in the contents.
            if(included.length>1)elements.add(new GuideElements.Heading(Component.translatable("guide.biggerreactorsrework.chapter."+key)));
            elements.addAll(sections.get(key).elements());
        }
        return new Chapter(id,()->stack(icon),List.copyOf(elements));
    }
    private static GuideElement limits(String machine,int x,int y,int z,int maxX,int maxY,int maxZ) {
        return new GuideElements.Text(Component.translatable("guide.biggerreactorsrework.limits."+machine,x,y,z,maxX,maxY,maxZ));
    }
    private static List<Chapter> sections() {
        var chapters=new ArrayList<Chapter>();
        { var elements=new ArrayList<GuideElement>();
          elements.add(new GuideElements.Text(Component.translatable("guide.biggerreactorsrework.content.start")));
          elements.add(new GuideElements.Crafting(GuideIds.id("crafting/guide_book")));
          chapters.add(new Chapter("start",()->new ItemStack(BuiltInRegistries.ITEM.get(GuideIds.id("reactor_terminal"))),List.copyOf(elements))); }
        { var elements=new ArrayList<GuideElement>();
          elements.add(new GuideElements.Text(Component.translatable("guide.biggerreactorsrework.content.materials")));
          elements.add(new GuideElements.Heading(Component.translatable("guide.biggerreactorsrework.material.uranium")));
          elements.add(new GuideElements.ItemRow(()->List.of(stack("uranium_ore"),stack("deepslate_uranium_ore"),stack("uranium_chunk")),List.of()));
          elements.add(new GuideElements.Text(Component.translatable("guide.biggerreactorsrework.material.uranium.text")));
          elements.add(new GuideProcesses.Cooking(GuideIds.id("smelting/uranium_chunk"),GuideIds.id("smelting/uranium_ore"),GuideIds.id("smelting/deepslate_uranium_ore")));
          elements.add(new GuideElements.Heading(Component.translatable("guide.biggerreactorsrework.material.graphite")));
          elements.add(new GuideElements.Text(Component.translatable("guide.biggerreactorsrework.material.graphite.text")));
          elements.add(new GuideProcesses.Cooking(GuideIds.id("smelting/graphite_ingot")));
          elements.add(new GuideElements.Heading(Component.translatable("guide.biggerreactorsrework.material.cyanite")));
          elements.add(new GuideElements.Text(Component.translatable("guide.biggerreactorsrework.material.cyanite.text")));
          elements.add(new GuideProcesses.ReactorCycle());
          elements.add(new GuideElements.Heading(Component.translatable("guide.biggerreactorsrework.material.blutonium")));
          elements.add(new GuideElements.Text(Component.translatable("guide.biggerreactorsrework.material.blutonium.text")));
          elements.add(new GuideElements.Crafting(GuideIds.id("crafting/cyanite_reprocessor")));
          addReprocessing(elements);
          elements.add(new GuideElements.Text(Component.translatable("guide.biggerreactorsrework.content.reprocessing")));
          elements.add(new GuideElements.Heading(Component.translatable("guide.biggerreactorsrework.material.ludicrite")));
          elements.add(new GuideElements.Text(Component.translatable("guide.biggerreactorsrework.material.ludicrite.text")));
          elements.add(new GuideElements.Crafting(GuideIds.id("crafting/ludicrite_block_nether_star")));
          var level=Minecraft.getInstance().level;
          if(level!=null && level.getRecipeManager().byKey(GuideIds.id("crafting/ludicrite_block_enderium")).isPresent())
              elements.add(new GuideElements.Crafting(GuideIds.id("crafting/ludicrite_block_enderium")));
          chapters.add(new Chapter("materials",()->new ItemStack(BuiltInRegistries.ITEM.get(GuideIds.id("uranium_ingot"))),List.copyOf(elements))); }
        { var elements=new ArrayList<GuideElement>();
          elements.add(new GuideElements.Text(Component.translatable("guide.biggerreactorsrework.content.reactor_parts")));
          elements.add(new GuideElements.Crafting(GuideIds.id("crafting/reactor/reactor_casing")));
          elements.add(new GuideElements.Crafting(GuideIds.id("crafting/reactor/reactor_glass")));
          elements.add(new GuideElements.Crafting(GuideIds.id("crafting/reactor/reactor_terminal")));
          elements.add(new GuideElements.Crafting(GuideIds.id("crafting/reactor/reactor_fuel_rod")));
          elements.add(new GuideElements.Crafting(GuideIds.id("crafting/reactor/reactor_control_rod")));
          elements.add(new GuideElements.Crafting(GuideIds.id("crafting/reactor/reactor_access_port")));
          elements.add(new GuideElements.Crafting(GuideIds.id("crafting/reactor/reactor_power_tap")));
          elements.add(new GuideElements.Crafting(GuideIds.id("crafting/reactor/reactor_coolant_port")));
          elements.add(new GuideElements.Crafting(GuideIds.id("crafting/reactor/reactor_redstone_port")));
          chapters.add(new Chapter("reactor_parts",()->new ItemStack(BuiltInRegistries.ITEM.get(GuideIds.id("reactor_casing"))),List.copyOf(elements))); }
        { var elements=new ArrayList<GuideElement>();
          elements.add(new GuideElements.Text(Component.translatable("guide.biggerreactorsrework.content.build_reactor")));
          elements.add(limits("reactor",3,3,3,biggerreactorsrework.Config.CONFIG.Reactor.MaxLength,biggerreactorsrework.Config.CONFIG.Reactor.MaxHeight,biggerreactorsrework.Config.CONFIG.Reactor.MaxWidth));
          elements.add(new GuideElements.Text(Component.translatable("guide.biggerreactorsrework.viewer.glass_text")));
          elements.addAll(GuideMultiblock.elements(GuideStructures.reactor(3,false)));
          chapters.add(new Chapter("build_reactor",()->new ItemStack(BuiltInRegistries.ITEM.get(GuideIds.id("reactor_fuel_rod"))),List.copyOf(elements))); }
        { var elements=new ArrayList<GuideElement>();
          elements.add(new GuideElements.Text(Component.translatable("guide.biggerreactorsrework.content.reactor_panel")));
          chapters.add(new Chapter("reactor_panel",()->new ItemStack(BuiltInRegistries.ITEM.get(GuideIds.id("reactor_control_rod"))),List.copyOf(elements))); }
        { var elements=new ArrayList<GuideElement>();
          elements.add(new GuideElements.Text(Component.translatable("guide.biggerreactorsrework.content.moderators")));
          elements.add(propertyTable(true));
          elements.addAll(GuideMultiblock.elements(GuideStructures.reactor(5,false)));
          chapters.add(new Chapter("moderators",()->new ItemStack(BuiltInRegistries.ITEM.get(GuideIds.id("graphite_block"))),List.copyOf(elements))); }
        { var elements=new ArrayList<GuideElement>();
          elements.add(new GuideElements.Text(Component.translatable("guide.biggerreactorsrework.content.active")));
          elements.add(new GuideElements.Text(Component.translatable("guide.biggerreactorsrework.content.active_output_protection")));
          elements.add(new GuideElements.Heading(Component.translatable("guide.biggerreactorsrework.coolants")));
          elements.add(coolantTable());
          elements.addAll(GuideMultiblock.elements(GuideStructures.reactor(5,true)));
          chapters.add(new Chapter("active",()->new ItemStack(BuiltInRegistries.ITEM.get(GuideIds.id("reactor_coolant_port"))),List.copyOf(elements))); }
        { var elements=new ArrayList<GuideElement>();
          elements.add(limits("turbine",5,4,5,biggerreactorsrework.Config.CONFIG.Turbine.MaxLength,biggerreactorsrework.Config.CONFIG.Turbine.MaxHeight,biggerreactorsrework.Config.CONFIG.Turbine.MaxWidth));
          elements.add(new GuideElements.Text(Component.translatable("guide.biggerreactorsrework.content.turbine_parts")));
          elements.add(new GuideElements.Crafting(GuideIds.id("crafting/turbine/turbine_casing")));
          elements.add(new GuideElements.Crafting(GuideIds.id("crafting/turbine/turbine_glass")));
          elements.add(new GuideElements.Crafting(GuideIds.id("crafting/turbine/turbine_terminal")));
          elements.add(new GuideElements.Crafting(GuideIds.id("crafting/turbine/turbine_rotor_bearing")));
          elements.add(new GuideElements.Crafting(GuideIds.id("crafting/turbine/turbine_rotor_shaft")));
          elements.add(new GuideElements.Crafting(GuideIds.id("crafting/turbine/turbine_rotor_blade")));
          elements.add(new GuideElements.Crafting(GuideIds.id("crafting/turbine/turbine_fluid_port")));
          elements.add(new GuideElements.Crafting(GuideIds.id("crafting/turbine/turbine_power_tap")));
          elements.add(new GuideElements.Crafting(GuideIds.id("crafting/turbine/turbine_redstone_port")));
          elements.add(propertyTable(false));
          chapters.add(new Chapter("turbine_parts",()->new ItemStack(BuiltInRegistries.ITEM.get(GuideIds.id("turbine_rotor_bearing"))),List.copyOf(elements))); }
        { var elements=new ArrayList<GuideElement>();
          elements.add(new GuideElements.Text(Component.translatable("guide.biggerreactorsrework.content.build_turbine")));
          elements.add(new GuideElements.Text(Component.translatable("guide.biggerreactorsrework.viewer.glass_text")));
          elements.addAll(GuideMultiblock.elements(GuideStructures.turbine()));
          chapters.add(new Chapter("build_turbine",()->new ItemStack(BuiltInRegistries.ITEM.get(GuideIds.id("turbine_terminal"))),List.copyOf(elements))); }
        { var elements=new ArrayList<GuideElement>();
          elements.add(limits("battery",3,3,3,biggerreactorsrework.Config.CONFIG.Battery.MaxLength,biggerreactorsrework.Config.CONFIG.Battery.MaxHeight,biggerreactorsrework.Config.CONFIG.Battery.MaxWidth));
          elements.add(new GuideElements.Text(Component.translatable("guide.biggerreactorsrework.content.battery_parts",
              biggerreactorsrework.core.client.gui.RenderHelper.formatValue(biggerreactorsrework.Config.CONFIG.Battery.CellCapacity,"FE"),
              biggerreactorsrework.core.client.gui.RenderHelper.formatValue(biggerreactorsrework.Config.CONFIG.Battery.CellTransferRate,"FE/t"))));
          elements.add(new GuideElements.Crafting(GuideIds.id("crafting/battery/battery_casing")));
          elements.add(new GuideElements.Crafting(GuideIds.id("crafting/battery/battery_glass")));
          elements.add(new GuideElements.Crafting(GuideIds.id("crafting/battery/battery_terminal")));
          elements.add(new GuideElements.Crafting(GuideIds.id("crafting/battery/battery_energy_port")));
          elements.add(new GuideElements.Crafting(GuideIds.id("crafting/battery/battery_cell")));
          chapters.add(new Chapter("battery_parts",()->new ItemStack(BuiltInRegistries.ITEM.get(GuideIds.id("battery_cell"))),List.copyOf(elements))); }
        { var elements=new ArrayList<GuideElement>();
          elements.add(new GuideElements.Text(Component.translatable("guide.biggerreactorsrework.content.build_battery")));
          elements.add(new GuideElements.Text(Component.translatable("guide.biggerreactorsrework.viewer.glass_text")));
          elements.addAll(GuideMultiblock.elements(GuideStructures.battery()));
          chapters.add(new Chapter("build_battery",()->new ItemStack(BuiltInRegistries.ITEM.get(GuideIds.id("battery_terminal"))),List.copyOf(elements))); }
        { var elements=new ArrayList<GuideElement>();
          elements.add(limits("exchanger",4,3,3,biggerreactorsrework.Config.CONFIG.HeatExchanger.MaxLength,biggerreactorsrework.Config.CONFIG.HeatExchanger.MaxHeight,biggerreactorsrework.Config.CONFIG.HeatExchanger.MaxWidth));
          elements.add(new GuideElements.Text(Component.translatable("guide.biggerreactorsrework.content.exchanger")));
          elements.add(new GuideElements.Text(Component.translatable("guide.biggerreactorsrework.content.exchanger_output_protection")));
          elements.add(new GuideElements.Crafting(GuideIds.id("crafting/heat_exchanger/casing")));
          elements.add(new GuideElements.Crafting(GuideIds.id("crafting/heat_exchanger/glass")));
          elements.add(new GuideElements.Crafting(GuideIds.id("crafting/heat_exchanger/terminal")));
          elements.add(new GuideElements.Crafting(GuideIds.id("crafting/heat_exchanger/condenser_channel_alt")));
          elements.add(new GuideElements.Crafting(GuideIds.id("crafting/heat_exchanger/evaporator_channel")));
          elements.add(new GuideElements.Crafting(GuideIds.id("crafting/heat_exchanger/fluid_port")));
          elements.add(new GuideElements.Heading(Component.translatable("guide.biggerreactorsrework.exchanges")));
          elements.add(exchangeTable());
          elements.addAll(GuideMultiblock.elements(GuideStructures.exchanger()));
          chapters.add(new Chapter("exchanger",()->new ItemStack(BuiltInRegistries.ITEM.get(GuideIds.id("heat_exchanger_terminal"))),List.copyOf(elements))); }
        { var elements=new ArrayList<GuideElement>();
          elements.add(new GuideElements.Text(Component.translatable("guide.biggerreactorsrework.content.automation")));
          chapters.add(new Chapter("automation",()->new ItemStack(BuiltInRegistries.ITEM.get(GuideIds.id("turbine_redstone_port"))),List.copyOf(elements))); }
        { var elements=new ArrayList<GuideElement>();
          elements.add(new GuideElements.Text(Component.translatable("guide.biggerreactorsrework.content.efficiency")));
          chapters.add(new Chapter("efficiency",()->new ItemStack(BuiltInRegistries.ITEM.get(GuideIds.id("turbine_power_tap"))),List.copyOf(elements))); }
        { var elements=new ArrayList<GuideElement>();
          elements.add(new GuideElements.Text(Component.translatable("guide.biggerreactorsrework.content.troubleshooting")));
          chapters.add(new Chapter("troubleshooting",()->new ItemStack(BuiltInRegistries.ITEM.get(GuideIds.id("reactor_terminal"))),List.copyOf(elements))); }
        return List.copyOf(chapters);
    }
    private static ItemStack stack(String id){return new ItemStack(BuiltInRegistries.ITEM.get(GuideIds.id(id)));}
    private static void addReprocessing(List<GuideElement> elements){
        var recipes=ReprocessingRecipes.all(Minecraft.getInstance().level);
        if(recipes.isEmpty())elements.add(new GuideReprocessing(GuideIds.id("cyanite_reprocessing")));
        else recipes.forEach(holder->elements.add(new GuideReprocessing(holder.id())));
    }
    private static GuideElements.Table coolantTable() {
        return GuideElements.Table.named(List.of(Component.translatable("guide.biggerreactorsrework.coolants.cold"),Component.translatable("guide.biggerreactorsrework.coolants.hot")),
            new float[]{1,1},
            ()->biggerreactorsrework.client.deps.RecipeViewerData.coolants().stream().map(c->List.of(c.input().getHoverName().getString(),c.output().getHoverName().getString())).toList(),
            (cells,column)->{
                for(var c:biggerreactorsrework.client.deps.RecipeViewerData.coolants()){
                    var stack=column==0?c.input():c.output();
                    if(stack.getHoverName().getString().equals(cells.get(column)))return GuideReferences.Target.of(stack);
                }
                return null;
            });
    }
    private static Component circuit(boolean evaporation){
        return Component.translatable("guide.biggerreactorsrework.exchanges."+(evaporation?"evaporator":"condenser"));
    }
    private static GuideElements.Table exchangeTable() {
        return GuideElements.Table.named(List.of(Component.translatable("guide.biggerreactorsrework.exchanges.input"),
                Component.translatable("guide.biggerreactorsrework.exchanges.output"),Component.translatable("guide.biggerreactorsrework.exchanges.circuit")),
            new float[]{1.2f,1.2f,.8f},
            ()->biggerreactorsrework.client.deps.RecipeViewerData.heatExchanges().stream().map(e->List.of(e.input().getHoverName().getString(),
                e.output().getHoverName().getString(),circuit(e.evaporation()).getString())).toList(),
            (cells,column)->{
                if(column>1)return null;
                for(var e:biggerreactorsrework.client.deps.RecipeViewerData.heatExchanges()){
                    var stack=column==0?e.input():e.output();
                    if(stack.getHoverName().getString().equals(cells.get(column)))return GuideReferences.Target.of(stack);
                }
                return null;
            });
    }
    private static String number(double n) { return java.math.BigDecimal.valueOf(n).stripTrailingZeros().toPlainString(); }
    private static GuideElements.Table propertyTable(boolean moderator) {
        var headers=new ArrayList<Component>();headers.add(Component.translatable("guide.biggerreactorsrework.table.material"));
        var tips=new ArrayList<List<Component>>();tips.add(List.of(Component.translatable("guide.biggerreactorsrework.table.material.tip")));
        String[] keys=moderator?new String[]{"absorption","efficiency","moderation","conductivity"}:new String[]{"coil_efficiency","extraction","bonus"};
        String[] symbols=moderator?new String[]{"A","E","M","C"}:new String[]{"E","X","B"};
        for(int i=0;i<keys.length;i++){
            headers.add(Component.literal(symbols[i]));
            tips.add(List.of(Component.translatable("guide.biggerreactorsrework.table."+keys[i]+".title"),Component.translatable("guide.biggerreactorsrework.table."+keys[i]+".tip")));
        }
        return new GuideElements.Table(headers,moderator?new float[]{3,1,1,1,1}:new float[]{3,1,1,1},moderator?GuideContent::moderators:GuideContent::coils,tips,cells->materialReference(moderator,cells));
    }
    private static List<String> materialCells(MaterialTableRows.Entry entry){
        var row=new ArrayList<String>();row.add(entry.name());entry.values().forEach(value->row.add(number(value)));return List.copyOf(row);
    }
    private static List<List<String>> moderators(){return materialEntries(true).stream().map(GuideContent::materialCells).toList();}
    private static List<List<String>> coils(){return materialEntries(false).stream().map(GuideContent::materialCells).toList();}
    private static List<MaterialTableRows.Entry> materialEntries(boolean moderator){
        var entries=new ArrayList<MaterialTableRows.Entry>();
        if(moderator)biggerreactorsrework.registries.ReactorModeratorRegistry.Client.forEach((block,p)->GuideMaterialNames.resolve(block).ifPresent(name->
            entries.add(new MaterialTableRows.Entry(name.material(),name.label(),List.of(p.absorption(),p.heatEfficiency(),p.moderation(),p.heatConductivity()),BuiltInRegistries.BLOCK.getKey(block).toString()))));
        else biggerreactorsrework.registries.TurbineCoilRegistry.Client.getImmutableRegistry().forEach((block,p)->GuideMaterialNames.resolve(block).ifPresent(name->
            entries.add(new MaterialTableRows.Entry(name.material(),name.label(),List.of(p.efficiency,p.extractionRate,p.bonus),BuiltInRegistries.BLOCK.getKey(block).toString()))));
        return MaterialTableRows.byEfficiency(entries,moderator?1:0);
    }
    private static GuideReferences.Target materialReference(boolean moderator,List<String> cells){
        for(var entry:materialEntries(moderator))if(materialCells(entry).equals(cells)){
            var id=net.minecraft.resources.ResourceLocation.tryParse(entry.source());
            if(id==null||!BuiltInRegistries.BLOCK.containsKey(id))return null;
            var block=BuiltInRegistries.BLOCK.get(id);var name=Component.literal(entry.name());
            if(block instanceof net.minecraft.world.level.block.LiquidBlock liquid)return new GuideReferences.Target(ItemStack.EMPTY,liquid.fluid,name);
            return new GuideReferences.Target(new ItemStack(block),null,name);
        }
        return null;
    }
}
