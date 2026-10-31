package biggerreactorsrework.client;

import biggerreactorsrework.fluids.HeatedCoolant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

/** The still texture and tint of a fluid, from its fluid model, for screens and renderers. */
public final class FluidSprites {
    private FluidSprites() {
    }

    public static FluidModel model(Fluid fluid) {
        return Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState());
    }

    public static TextureAtlasSprite blockSprite(Identifier id) {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(id);
    }

    public static TextureAtlasSprite still(FluidStack stack) {
        if (stack.getFluid() == HeatedCoolant.INSTANCE) {
            return blockSprite(HeatedFluidSprites.texture(stack, false));
        }
        return model(stack.getFluid()).stillMaterial().sprite();
    }

    /** ARGB tint of the fluid (white when it has none). */
    public static int tint(FluidStack stack) {
        FluidModel model = model(stack.getFluid());
        return model.fluidTintSource() != null ? model.fluidTintSource().colorAsStack(stack) : 0xFFFFFFFF;
    }
}
