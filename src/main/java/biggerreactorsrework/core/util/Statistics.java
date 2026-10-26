package biggerreactorsrework.core.util;

import java.util.*;
import java.util.concurrent.*;

/** Read-only server projections. Bounded worker, no level access and no gameplay mutation. */
public final class Statistics {
    public record Potential(double burn, double converted, double power) { }
    private static final ThreadPoolExecutor WORKER=new ThreadPoolExecutor(1,1,30,TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(16),r->{var t=new Thread(r,"Rework capacity estimate");t.setDaemon(true);return t;});
    public static CompletableFuture<Potential> estimate(java.util.function.Supplier<Potential> work) {
        try { return CompletableFuture.supplyAsync(work,WORKER); }
        catch(RejectedExecutionException busy) { return null; }
    }
    public static Map<String,Object> row(String key,Object value) { return Map.of("key",key,"value",String.valueOf(value)); }
    public static Map<String,Object> rate(String key,double value) { return row(key,String.format(Locale.ROOT,"%.2f mB/t",value)); }
    public static Map<String,Object> quantity(String key,long value) { return row(key,String.format(Locale.ROOT,"%,d mB",value)); }
    public static Map<String,Object> power(String key,double value) { return row(key,String.format(Locale.ROOT,"%.2f FE/t",value)); }
    public static Map<String,Object> pair(biggerreactorsrework.core.fluids.IFluidTankHandler tank) {
        var in=tank.fluidTypeInTank(0);var out=tank.fluidTypeInTank(1);
        return pair(in==net.minecraft.world.level.material.Fluids.EMPTY?net.neoforged.neoforge.fluids.FluidStack.EMPTY:new net.neoforged.neoforge.fluids.FluidStack(in.builtInRegistryHolder(),1,tank.fluidComponentsInTank(0)),
                out==net.minecraft.world.level.material.Fluids.EMPTY?net.neoforged.neoforge.fluids.FluidStack.EMPTY:new net.neoforged.neoforge.fluids.FluidStack(out.builtInRegistryHolder(),1,tank.fluidComponentsInTank(1)));
    }
    public static Map<String,Object> pair(net.neoforged.neoforge.fluids.FluidStack in,net.neoforged.neoforge.fluids.FluidStack out) {
        String cold=net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(in.getFluid()).toString();
        String hot=net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(out.getFluid()).toString();
        var inputOrigin=in.get(biggerreactorsrework.registries.CoolantComponents.ORIGIN.get());
        var origin=out.get(biggerreactorsrework.registries.CoolantComponents.ORIGIN.get());
        return row("pair","@pair:"+cold+"|"+hot+"|"+(origin==null?"":origin)+"|"+(inputOrigin==null?"":inputOrigin));
    }
    private Statistics() { }
}
