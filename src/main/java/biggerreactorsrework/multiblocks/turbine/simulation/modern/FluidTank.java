package biggerreactorsrework.multiblocks.turbine.simulation.modern;

import net.minecraft.world.level.material.Fluid;
import biggerreactorsrework.multiblocks.turbine.simulation.ITurbineFluidTank;
import biggerreactorsrework.registries.FluidTransitionRegistry;
import biggerreactorsrework.util.FluidTransitionTank;

import javax.annotation.Nullable;

public class FluidTank extends FluidTransitionTank implements ITurbineFluidTank {
    public FluidTank() {
        super(true);
    }

    @Override protected boolean selectable(FluidTransitionRegistry.FluidTransition transition) {
        return transition.turbineMultiplier > 0;
    }
    void idealSupply(FluidTransitionRegistry.FluidTransition transition) {
        activeTransition=transition;inFluid=transition.gases.getFirst();outFluid=transition.liquids.getFirst();
        inAmount=perSideCapacity;outAmount=0;
    }
    
    @Override
    public void dumpLiquid() {
        discardedLastTick+=fluidAmountInTank(1);
        dumpTank(1);
    }
    
    @Override
    public void dumpVapor() {
        dumpTank(0);
    }
    
    public long discardedLastTick;
    long flow(long amount, boolean ventExcess){
        sampleFlows();discardedLastTick=0;
        long maxTransitionable = inAmount;
    
        if(!ventExcess){
            maxTransitionable = Math.min(maxTransitionable, perSideCapacity - outAmount);
        }
    
        maxTransitionedLastTick = amount;
        amount = Math.min(maxTransitionable, amount);
        transitionedLastTick = amount;
    
        inAmount -= amount;
        outAmount += amount;
    
        if(ventExcess){
            discardedLastTick=Math.max(0,outAmount-perSideCapacity);
            outAmount = Math.min(outAmount, perSideCapacity);
        }
        
        return transitionedLastTick;
    }
    
    @Nullable
    @Override
    protected FluidTransitionRegistry.FluidTransition selectTransition(Fluid fluid) {
        FluidTransitionRegistry.FluidTransition transition = super.selectTransition(fluid);
        if(transition != null && transition.turbineMultiplier == 0){
            return null;
        }
        return transition;
    }
}
