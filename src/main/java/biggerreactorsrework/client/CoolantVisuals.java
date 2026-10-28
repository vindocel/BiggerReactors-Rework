package biggerreactorsrework.client;

import biggerreactorsrework.registries.CoolantComponents;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;

public final class CoolantVisuals {
    public static FluidStack stack(String id,String origin){
        var key=ResourceLocation.tryParse(id);
        if(key==null || !BuiltInRegistries.FLUID.containsKey(key))return FluidStack.EMPTY;
        var fluid=BuiltInRegistries.FLUID.get(key);
        if(fluid==net.minecraft.world.level.material.Fluids.EMPTY)return FluidStack.EMPTY;
        var stack=new FluidStack(fluid,1);
        if(!origin.isEmpty())stack.applyComponents(DataComponentPatch.builder().set(CoolantComponents.ORIGIN.get(),origin).build());
        return stack;
    }
    private CoolantVisuals(){}
}
