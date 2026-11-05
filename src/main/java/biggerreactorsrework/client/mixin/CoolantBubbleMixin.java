package biggerreactorsrework.client.mixin;

import biggerreactorsrework.fluids.FrozenObsidian;
import biggerreactorsrework.fluids.LiquidObsidian;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.particle.BubbleParticle;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BubbleParticle.class)
abstract class CoolantBubbleMixin {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/material/FluidState;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean rework$keepBubblesInCoolant(FluidState state, TagKey<Fluid> tag, Operation<Boolean> original) {
        var type = state.getFluidType();
        return original.call(state, tag) || type == LiquidObsidian.INSTANCE.getFluidType()
                || type == FrozenObsidian.INSTANCE.getFluidType();
    }
}
