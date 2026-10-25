package biggerreactorsrework.client.guide;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import java.util.*;

/** Translated names, common material tags and readable fallback names. */
final class GuideMaterialNames {
    record Name(String material,String label){}
    static Optional<Name> resolve(Block block){
        var id=BuiltInRegistries.BLOCK.getKey(block);
        if(id==null||!BuiltInRegistries.BLOCK.containsKey(id))return Optional.empty();
        if(block instanceof AirBlock)return Optional.of(new Name("air",Component.translatable("block.minecraft.air").getString()));
        if(block instanceof LiquidBlock liquid){
            var fluid=liquid.fluid;var fluidId=BuiltInRegistries.FLUID.getKey(fluid);
            if(fluidId==null||fluid==net.minecraft.world.level.material.Fluids.EMPTY)return Optional.empty();
            String name=translated(fluid.getFluidType().getDescription());
            if(name==null)name=translated(block.getName());
            if(name==null)name=readable(fluidId.getPath());
            return Optional.of(new Name("fluid:"+name.toLowerCase(Locale.ROOT),name));
        }
        if(block.asItem()==Items.AIR)return Optional.empty();
        var material=java.util.stream.Stream.concat(block.builtInRegistryHolder().tags(),block.asItem().builtInRegistryHolder().tags())
            .map(TagKey::location).filter(tag->(tag.getNamespace().equals("c")||tag.getNamespace().equals("forge"))&&tag.getPath().startsWith("storage_blocks/"))
            .map(tag->tag.getPath().substring("storage_blocks/".length())).filter(path->!path.contains("/")).sorted().findFirst();
        if(material.isEmpty()){
            String stem=id.getPath().replaceFirst("^block_","").replaceFirst("_block$","");
            if(Language.getInstance().has("guide.biggerreactorsrework.material_name."+stem))material=Optional.of(stem);
        }
        String name=translated(new ItemStack(block).getHoverName());if(name==null)name=translated(block.getName());
        if(material.isPresent()){
            String key="guide.biggerreactorsrework.material_name."+material.get();
            if(Language.getInstance().has(key))name=Component.translatable("guide.biggerreactorsrework.table.block",Component.translatable(key)).getString();
            else if(name==null)name=Component.translatable("guide.biggerreactorsrework.table.block",readable(material.get())).getString();
        }
        if(name==null)name=readable(id.getPath());
        String identity=material.isPresent()?"block:"+material.get():"block:"+name.toLowerCase(Locale.ROOT);
        return Optional.of(new Name(identity,name));
    }
    private static String translated(Component component){
        if(component.getContents() instanceof TranslatableContents key&&!Language.getInstance().has(key.getKey()))return null;
        String value=component.getString();
        return value.isBlank()||value.matches("^(block|item|fluid)[_.][a-z0-9_.]+$")||value.matches("^[a-z0-9_]+:[a-z0-9_./]+$")?null:value;
    }
    private static String readable(String path){
        boolean molten=path.startsWith("molten_")||path.endsWith("_molten"),block=path.startsWith("block_")||path.endsWith("_block");
        String stem=path.replaceFirst("^(molten_|block_)","").replaceFirst("(_molten|_block)$","");
        String key="guide.biggerreactorsrework.material_name."+stem,name;
        if(Language.getInstance().has(key))name=Component.translatable(key).getString();
        else{var words=new ArrayList<String>();for(String word:stem.split("[_./]+"))if(!word.isEmpty())words.add(Character.toUpperCase(word.charAt(0))+word.substring(1));name=String.join(" ",words);}
        if(molten)return Component.translatable("guide.biggerreactorsrework.table.molten",name).getString();
        return block?Component.translatable("guide.biggerreactorsrework.table.block",name).getString():name;
    }
    private GuideMaterialNames(){}
}
