package biggerreactorsrework.core.fluids;

import biggerreactorsrework.registries.CoolantComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;

/** Quantized relative proportions of two shared tanks; contains no transferable inventory.
 * Modified 2026-10-26: normalize before rounding so small nonempty stocks cannot disappear.
 */
public record FluidRenderSnapshot(String input,String output,String inputOrigin,String outputOrigin,double inputFill,double outputFill) {
    public static final FluidRenderSnapshot EMPTY=new FluidRenderSnapshot("minecraft:empty","minecraft:empty","","",0,0);
    public static FluidRenderSnapshot capture(IFluidTankHandler tank) {
        long input=Math.max(0,tank.fluidAmountInTank(0)),output=Math.max(0,tank.fluidAmountInTank(1));
        double total=(double)input+output;
        double inputShare=total>0?Math.round(64d*input/total)/64d:0;
        double outputShare=total>0?1-inputShare:0;
        return new FluidRenderSnapshot(BuiltInRegistries.FLUID.getKey(tank.fluidTypeInTank(0)).toString(),
            BuiltInRegistries.FLUID.getKey(tank.fluidTypeInTank(1)).toString(),
            CoolantComponents.origin(tank.fluidComponentsInTank(0)),CoolantComponents.origin(tank.fluidComponentsInTank(1)),
            inputShare,outputShare);
    }
    public CompoundTag save(){var tag=new CompoundTag();tag.putString("input",input);tag.putString("output",output);tag.putString("inputOrigin",inputOrigin);tag.putString("outputOrigin",outputOrigin);tag.putDouble("inputFill",inputFill);tag.putDouble("outputFill",outputFill);return tag;}
    public static FluidRenderSnapshot load(CompoundTag tag){return new FluidRenderSnapshot(tag.getStringOr("input", ""),tag.getStringOr("output", ""),tag.getStringOr("inputOrigin", ""),tag.getStringOr("outputOrigin", ""),tag.getDoubleOr("inputFill", 0D),tag.getDoubleOr("outputFill", 0D));}
}
