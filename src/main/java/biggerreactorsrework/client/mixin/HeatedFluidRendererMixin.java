package biggerreactorsrework.client.mixin;

import biggerreactorsrework.client.FluidSprites;
import biggerreactorsrework.client.HeatedFluidSprites;
import biggerreactorsrework.fluids.HeatedCoolant;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FluidRenderer.class)
abstract class HeatedFluidRendererMixin {
    @WrapOperation(method = "tesselate", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/block/FluidStateModelSet;get(Lnet/minecraft/world/level/material/FluidState;)Lnet/minecraft/client/renderer/block/FluidModel;"))
    private FluidModel rework$selectHeatedModel(FluidStateModelSet models, FluidState state, Operation<FluidModel> original,
                                               BlockAndTintGetter level, BlockPos pos) {
        var model = original.call(models, state);
        if (!HeatedCoolant.INSTANCE.isSame(state.getType())) return model;
        var source = HeatedFluidSprites.placedSource(pos);
        if (source == null) return model;
        return new FluidModel(model.layer(),
                new Material.Baked(FluidSprites.blockSprite(HeatedFluidSprites.texture(source, false)), true),
                new Material.Baked(FluidSprites.blockSprite(HeatedFluidSprites.texture(source, true)), true),
                model.overlayMaterial(), model.fluidTintSource());
    }
}
