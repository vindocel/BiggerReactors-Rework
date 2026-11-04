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
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.material.*;
import net.neoforged.neoforge.client.event.RegisterSpriteSourcesEvent;
import net.neoforged.neoforge.fluids.FluidStack;

/** Derived client sprites: original animation/detail and an additive warm hue, including cyan textures.
 * Modified 2026-10-25: isolate optional fluid extensions that have no client texture properties.
 */
public final class HeatedFluidSprites implements SpriteSource {
    private static final double FROZEN_LUMA = 0x05 * .3 + 0xEB * .59 + 0xDB * .11;
    private static final MapCodec<HeatedFluidSprites> CODEC=MapCodec.unit(HeatedFluidSprites::new);
    private static final java.util.Set<Identifier> AVAILABLE=java.util.concurrent.ConcurrentHashMap.newKeySet();
    public static void register(RegisterSpriteSourcesEvent event){event.register(Identifier.fromNamespaceAndPath(BiggerReactors.modid,"heated_fluids"),CODEC);}
    @Override public MapCodec<? extends SpriteSource> codec(){return CODEC;}
    private static Identifier id(Fluid source,boolean flowing){var key=BuiltInRegistries.FLUID.getKey(source);return Identifier.fromNamespaceAndPath(BiggerReactors.modid,"heated/"+key.getNamespace()+"/"+key.getPath()+(flowing?"_flow":"_still"));}
    public static Identifier fallback(boolean flowing) {
        return Identifier.fromNamespaceAndPath(BiggerReactors.modid, flowing ? "block/fluid/heated_coolant_flowing" : "block/fluid/heated_coolant_still");
    }
    public static Identifier texture(FluidStack stack,boolean flowing){
        return texture(CoolantComponents.sourceFluid(stack), flowing);
    }
    public static Identifier texture(@javax.annotation.Nullable Fluid source, boolean flowing) {
        if (source == null) return fallback(flowing);
        var candidate = id(source, flowing);
        return AVAILABLE.contains(candidate) ? candidate : fallback(flowing);
    }
    @javax.annotation.Nullable
    public static Fluid placedSource(net.minecraft.core.BlockPos pos) {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level == null) return null;
        var chunk = level.getChunkSource().getChunk(pos.getX() >> 4, pos.getZ() >> 4,
                net.minecraft.world.level.chunk.status.ChunkStatus.FULL, false);
        return CoolantComponents.sourceFluid(biggerreactorsrework.fluids.PlacedCoolantOrigins.get(chunk, pos));
    }
    public static boolean renderPlaced(net.minecraft.client.renderer.block.FluidRenderer renderer, FluidState fluid,
                                       net.minecraft.client.renderer.block.BlockAndTintGetter level, net.minecraft.core.BlockPos pos,
                                       net.minecraft.client.renderer.block.FluidRenderer.Output output,
                                       net.minecraft.world.level.block.state.BlockState block) {
        if (placedSource(pos) == null) return false;
        renderer.tesselate(level, pos, output, block, fluid);
        return true;
    }
    public static boolean missing(java.util.List<biggerreactorsrework.registries.FluidTransitionRegistry.FluidTransition> profiles) {
        return profiles.stream().filter(t->t.genericHot()).anyMatch(t->!AVAILABLE.contains(id(t.liquids.getFirst(),false)));
    }
    public void run(ResourceManager resources,Output output){
        AVAILABLE.clear();
        // Modified 2026-10-27: preload built-in generic outputs before server profiles arrive.
        var eligible=new java.util.HashSet<Fluid>();
        eligible.add(biggerreactorsrework.fluids.LiquidObsidian.INSTANCE);
        eligible.add(biggerreactorsrework.fluids.FrozenObsidian.INSTANCE);
        eligible.add(HeatedCoolant.INSTANCE);
        var cryo=Identifier.fromNamespaceAndPath("ad_astra","cryo_fuel");
        if(BuiltInRegistries.FLUID.containsKey(cryo))eligible.add(BuiltInRegistries.FLUID.getValue(cryo));
        for(var profile:biggerreactorsrework.registries.CoolantSync.profiles())if(profile.genericHot())eligible.addAll(profile.liquids);
        for(var source:eligible) {
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
            if(source==Fluids.EMPTY || !source.isSource(source.defaultFluidState()))return;
            for(boolean flowing:new boolean[]{false,true}) {
                Identifier sourceTexture;
                int sourceTint;
                if(BuiltInRegistries.FLUID.getKey(source).toString().equals("ad_astra:cryo_fuel")) {
                    // Verified against this integration's registered client fluid data: tinted vanilla water.
                    sourceTexture=Identifier.withDefaultNamespace(flowing?"block/water_flow":"block/water_still");
                    sourceTint=-9633798;
                } else if(source==biggerreactorsrework.fluids.FrozenObsidian.INSTANCE) {
                    sourceTexture=Identifier.fromNamespaceAndPath(BiggerReactors.modid,flowing?"block/fluid/frozen_obsidian_flowing":"block/fluid/frozen_obsidian_still");
                    sourceTint=0xFFFFFFFF;
                } else if(source==biggerreactorsrework.fluids.LiquidObsidian.INSTANCE || source==HeatedCoolant.INSTANCE) {
                    sourceTexture=Identifier.fromNamespaceAndPath(BiggerReactors.modid,flowing?"block/fluid/liquid_obsidian_flowing":"block/fluid/liquid_obsidian_still");
                    sourceTint=0xFFFFFFFF;
                } else {
                    // Models are baked after the atlases; profile fluids use the fluid model set of the previous reload.
                    var known=net.minecraft.client.Minecraft.getInstance().getModelManager().getFluidStateModelSet();
                    if(known==null)continue;
                    var model=known.get(source.defaultFluidState());
                    sourceTexture=(flowing?model.flowingMaterial():model.stillMaterial()).sprite().contents().name();
                    sourceTint=model.fluidTintSource()!=null?model.fluidTintSource().colorAsStack(new FluidStack(source,1)):0xFFFFFFFF;
                }
                final var texture=sourceTexture;
                final int tint=sourceTint;
                var resource=resources.getResource(texture.withPath(p->"textures/"+p+".png"));
                if(resource.isEmpty())continue;
                var target=source==HeatedCoolant.INSTANCE ? fallback(flowing) : id(source,flowing);
                AVAILABLE.add(target);
                output.add(target,(SpriteSource.DiscardableLoader)loader->{
                    var original=loader.loadSprite(texture,resource.get());
                    if(original==null){AVAILABLE.remove(target);return null;}
                    try {
                        var input=original.getOriginalImage();var image=new NativeImage(input.getWidth(),input.getHeight(),false);
                        for(int y=0;y<input.getHeight();y++)for(int x=0;x<input.getWidth();x++){
                            int pixel=input.getPixel(x,y),alpha=pixel>>>24;
                            double r=((pixel>>16)&255)*((tint>>16)&255)/255d,g=((pixel>>8)&255)*((tint>>8)&255)/255d,b=(pixel&255)*(tint&255)/255d;
                            double light=Math.max(r,Math.max(g,b));
                            int red=(int)Math.min(255,Math.max(r*.45,light*.8)+48),green=(int)(g*.42),blue=(int)(b*.36);
                            if (source == HeatedCoolant.INSTANCE) {
                                red = (int)Math.min(255, 112 + light * .85);
                                green = (int)(g * .65);
                                blue = (int)(b * .45);
                            }
                            // Added 2026-11-03: preserve the cold texture's luminance in the heated red.
                            if (source == biggerreactorsrework.fluids.FrozenObsidian.INSTANCE) {
                                double scale = (r * .3 + g * .59 + b * .11) / FROZEN_LUMA;
                                red = (int)Math.min(255, 0xEB * scale);
                                green = (int)Math.min(255, 0x05 * scale);
                                blue = (int)Math.min(255, 0x2E * scale);
                            }
                            image.setPixel(x,y,(alpha<<24)|(red<<16)|(green<<8)|blue);
                        }
                        var metadata=resource.get().metadata();
                        return new SpriteContents(target,new FrameSize(original.width(),original.height()),image,
                                metadata.getSection(net.minecraft.client.resources.metadata.animation.AnimationMetadataSection.TYPE),
                                java.util.List.of(),metadata.getSection(net.minecraft.client.resources.metadata.texture.TextureMetadataSection.TYPE));
                    } catch(java.io.IOException unavailable) {
                        AVAILABLE.remove(target);return null;
                    } finally {original.close();}
                });
            }
    }
}
