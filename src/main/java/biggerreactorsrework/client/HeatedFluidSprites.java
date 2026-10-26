package biggerreactorsrework.client;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.fluids.HeatedCoolant;
import biggerreactorsrework.registries.CoolantComponents;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.*;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.material.*;
import net.neoforged.neoforge.client.event.RegisterSpriteSourceTypesEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/** Derived client sprites: original animation/detail and an additive warm hue, including cyan textures.
 * Modified 2026-10-25: isolate optional fluid extensions that have no client texture properties.
 */
public final class HeatedFluidSprites implements SpriteSource {
    private static SpriteSourceType TYPE;
    private static final java.util.Set<ResourceLocation> AVAILABLE=java.util.concurrent.ConcurrentHashMap.newKeySet();
    public static void register(RegisterSpriteSourceTypesEvent event){TYPE=new SpriteSourceType(MapCodec.unit(HeatedFluidSprites::new));event.register(ResourceLocation.fromNamespaceAndPath(BiggerReactors.modid,"heated_fluids"),TYPE);}
    public SpriteSourceType type(){return TYPE;}
    private static ResourceLocation id(Fluid source,boolean flowing){var key=BuiltInRegistries.FLUID.getKey(source);return ResourceLocation.fromNamespaceAndPath(BiggerReactors.modid,"heated/"+key.getNamespace()+"/"+key.getPath()+(flowing?"_flow":"_still"));}
    public static Fluid source(FluidStack stack){
        String token=stack.get(CoolantComponents.ORIGIN.get());
        if(token==null || !token.contains("@"))return Fluids.EMPTY;
        var key=ResourceLocation.tryParse(token.substring(0,token.indexOf('@')));
        return key!=null && BuiltInRegistries.FLUID.containsKey(key)?BuiltInRegistries.FLUID.get(key):Fluids.EMPTY;
    }
    public static ResourceLocation texture(FluidStack stack,boolean flowing){
        var source=source(stack);var candidate=id(source,flowing);
        return AVAILABLE.contains(candidate)?candidate:ResourceLocation.fromNamespaceAndPath(BiggerReactors.modid,"block/fluid/liquid_obsidian_still");
    }
    public void run(ResourceManager resources,Output output){
        AVAILABLE.clear();
        for(var source:BuiltInRegistries.FLUID) {
            try {
                addSource(resources,output,source);
            } catch (RuntimeException unavailable) {
                // Some mods register fluids without client properties. Their texture lookup must
                // not fail the atlas reload and trigger another NeoForge loading pass.
                BiggerReactors.LOGGER.warn("Cannot derive heated coolant texture for {}: {}; using fallback",BuiltInRegistries.FLUID.getKey(source),unavailable.toString());
            }
        }
    }
    private void addSource(ResourceManager resources,Output output,Fluid source) {
            if(source==Fluids.EMPTY || source==HeatedCoolant.INSTANCE || !source.isSource(source.defaultFluidState()))return;
            var extension=IClientFluidTypeExtensions.of(source);
            for(boolean flowing:new boolean[]{false,true}) {
                var texture=flowing?extension.getFlowingTexture(new FluidStack(source,1)):extension.getStillTexture(new FluidStack(source,1));
                if(texture==null)continue;
                var resource=resources.getResource(texture.withPath(p->"textures/"+p+".png"));
                if(resource.isEmpty())continue;
                var target=id(source,flowing);int tint=extension.getTintColor(new FluidStack(source,1));
                AVAILABLE.add(target);
                output.add(target,loader->{
                    var original=loader.loadSprite(texture,resource.get());
                    if(original==null)return null;
                    try {
                        var input=original.getOriginalImage();var image=new NativeImage(input.getWidth(),input.getHeight(),false);
                        for(int y=0;y<input.getHeight();y++)for(int x=0;x<input.getWidth();x++){
                            int pixel=input.getPixelRGBA(x,y),alpha=pixel>>>24;
                            double r=(pixel&255)*((tint>>16)&255)/255d,g=((pixel>>8)&255)*((tint>>8)&255)/255d,b=((pixel>>16)&255)*(tint&255)/255d;
                            double light=Math.max(r,Math.max(g,b));
                            int red=(int)Math.min(255,Math.max(r*.45,light*.8)+48),green=(int)(g*.42),blue=(int)(b*.36);
                            image.setPixelRGBA(x,y,(alpha<<24)|(blue<<16)|(green<<8)|red);
                        }
                        return new SpriteContents(target,new FrameSize(original.width(),original.height()),image,original.metadata());
                    } finally {original.close();}
                });
            }
    }
}
