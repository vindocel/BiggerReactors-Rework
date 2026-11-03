package biggerreactorsrework.util;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import biggerreactorsrework.util.NbtSerializable;
import biggerreactorsrework.registries.FluidTransitionRegistry;
import biggerreactorsrework.core.fluids.IFluidTankHandler;
import biggerreactorsrework.core.util.HeatBody;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class FluidTransitionTank extends HeatBody implements IFluidTankHandler, NbtSerializable {
    
    public final boolean condenser;
    
    public long perSideCapacity;
    protected FluidTransitionRegistry.FluidTransition activeTransition;
    
    public static final int IN_TANK = 0;
    protected Fluid inFluid;
    protected long inAmount = 0;
    
    public static final int OUT_TANK = 1;
    protected Fluid outFluid;
    protected long outAmount = 0;
    
    protected long rfTransferredLastTick;
    protected long transitionedLastTick;
    protected long maxTransitionedLastTick;
    protected DataComponentPatch inComponents = DataComponentPatch.EMPTY;
    protected DataComponentPatch outComponents = DataComponentPatch.EMPTY;
    protected double thermalRemainder;
    private CompoundTag unresolved;
    private double incoming,outgoing;
    public double inputRate,outputRate;
    // Modified 2026-10-26: opt-in limiter; ordinary tanks and turbine behavior are unchanged.
    private double conversionLimit=1;
    private biggerreactorsrework.core.util.OutputBackpressure outputObserver;
    public void setConversionLimit(double limit){conversionLimit=Double.isFinite(limit)?Math.clamp(limit,0,1):0;}
    public void observeOutputWith(biggerreactorsrework.core.util.OutputBackpressure observer){outputObserver=observer;}
    public void sampleFlows(){inputRate=.9*inputRate+.1*incoming;outputRate=.9*outputRate+.1*outgoing;incoming=0;outgoing=0;}
    public boolean hasUnresolvedProfile(){return unresolved!=null;}
    
    public FluidTransitionTank(boolean condenser) {
        this.condenser = condenser;
        setInfinite(true);
    }
    
    @Override
    public int tankCount() {
        return 2;
    }
    
    @Override
    public long tankCapacity(int tank) {
        return Math.max(0, perSideCapacity);
    }
    
    @Nonnull
    @Override
    public Fluid fluidTypeInTank(int tank) {
        if (tank == IN_TANK && inFluid != null) {
            return inFluid;
        }
        if (tank == OUT_TANK && outFluid != null) {
            return outFluid;
        }
        return Fluids.EMPTY;
    }
    
    @Nullable
    @Override
    public DataComponentPatch fluidComponentsInTank(int tank) {
        return tank == IN_TANK ? inComponents : outComponents;
    }
    
    @Override
    public long fluidAmountInTank(int tank) {
        if (tank == IN_TANK) {
            return inAmount;
        }
        if (tank == OUT_TANK) {
            return outAmount;
        }
        return 0;
    }
    
    public long transitionedLastTick() {
        return transitionedLastTick;
    }
    
    public long maxTransitionedLastTick() {
        return maxTransitionedLastTick;
    }
    
    public long rfTransferredLastTick() {
        return rfTransferredLastTick;
    }
    
    @Override
    public boolean fluidValidForTank(int tank, @Nonnull Fluid fluid) {
        if (tank == IN_TANK) {
            return (condenser ? FluidTransitionRegistry.gasTransition(fluid) : FluidTransitionRegistry.liquidTransition(fluid)) != null;
        }
        if (tank == OUT_TANK) {
            return (condenser ? FluidTransitionRegistry.liquidTransition(fluid) : FluidTransitionRegistry.gasTransition(fluid)) != null;
        }
        return false;
    }
    @Override public boolean fluidValidForTank(int tank,Fluid fluid,DataComponentPatch components) {
        if(tank!=IN_TANK)return fluidValidForTank(tank,fluid);
        var transition=condenser?FluidTransitionRegistry.gasTransition(fluid,components):(components.isEmpty()?FluidTransitionRegistry.liquidTransition(fluid):null);
        return transition!=null && selectable(transition);
    }
    
    @Override
    public long fill(@Nonnull Fluid fluid, DataComponentPatch components, long amount, boolean simulate) {
        var selected = condenser ? FluidTransitionRegistry.gasTransition(fluid, components) :
                (components.isEmpty() ? FluidTransitionRegistry.liquidTransition(fluid) : null);
        if(condenser && selected!=null && selected.genericHot())components=biggerreactorsrework.registries.CoolantComponents.heated(selected);
        return fillStack(fluid, components, amount, simulate, selected);
    }
    
    /**
     * Use above fill from IFluidTankHandler unless you need to override the active transition
     * you probably don't, so, don't use this
     */
    @Deprecated
    @SuppressWarnings("DeprecatedIsStillUsed")
    public long fill(@Nonnull Fluid fluid, long amount, boolean simulate, @Nullable FluidTransitionRegistry.FluidTransition transition) {
        return fillStack(fluid, DataComponentPatch.EMPTY, amount, simulate, transition == null ? selectTransition(fluid) : transition);
    }

    protected long fillStack(Fluid fluid, DataComponentPatch components, long amount, boolean simulate,
                             @Nullable FluidTransitionRegistry.FluidTransition transition) {
        if(unresolved!=null)return 0;
        if (amount <= 0 || perSideCapacity <= 0 || transition == null) return 0;
        if (!selectable(transition)) return 0;
        boolean occupied = inAmount > 0 || outAmount > 0;
        if (occupied && (fluid != inFluid || !components.equals(inComponents) || activeTransition == null ||
                !biggerreactorsrework.registries.CoolantComponents.fingerprint(activeTransition).equals(biggerreactorsrework.registries.CoolantComponents.fingerprint(transition)))) return 0;
        long toFill = Math.min(amount, Math.max(0,perSideCapacity-inAmount));
        if (!simulate && toFill > 0) {
            clearSelectionIfEmpty();
            inFluid=fluid; inComponents=components;
            outFluid=condenser ? transition.liquids.getFirst() : transition.gases.getFirst();
            outComponents=!condenser && transition.genericHot() ? biggerreactorsrework.registries.CoolantComponents.heated(transition) : DataComponentPatch.EMPTY;
            activeTransition=transition;
            inAmount+=toFill;incoming+=toFill;
            transitionUpdate();
        }
        return toFill;
    }

    protected boolean selectable(FluidTransitionRegistry.FluidTransition transition) { return true; }
    
    @Override
    public long drain(@Nonnull Fluid fluid, DataComponentPatch components, long amount, boolean simulate) {
        if (amount <= 0) return 0;
        if (activeTransition == null || !components.equals(outComponents)) {
            return 0;
        }
        if (fluid == outFluid || (condenser ? activeTransition.liquids.contains(fluid) : activeTransition.gases.contains(fluid))) {
            long maxDrain = outAmount;
            long toDrain = Math.min(amount, maxDrain);
            if (!simulate) {
                outFluid = fluid;
                outAmount -= toDrain;outgoing+=toDrain;
                if(outputObserver!=null)outputObserver.released(outAmount,perSideCapacity);
                clearSelectionIfEmpty();
            }
            return toDrain;
        }
        return 0;
    }
    
    // Modified 2026-11-02: called only after the shared atomic exchanger has checked both circuits.
    public void resetThermalCounters() { transitionedLastTick=0;maxTransitionedLastTick=0;rfTransferredLastTick=0;thermalRemainder=0; }
    public void processThermalUnits(long units,double heatPerUnit) {
        if(units<0 || units>inAmount || units>Math.max(0,perSideCapacity-outAmount) || unresolved!=null || activeTransition==null)
            throw new IllegalArgumentException("Thermal conversion exceeds available stock");
        inAmount-=units;outAmount+=units;transitionedLastTick=units;maxTransitionedLastTick=units;rfTransferredLastTick=(long)(units*heatPerUnit);thermalRemainder=0;
    }

    // Added 2026-11-02: recover preserved cold/hot stock without admitting its unknown profile into production.
    public Fluid maintenanceFluid(int tank) {
        if(unresolved==null)return fluidTypeInTank(tank);
        var id=ResourceLocation.tryParse(unresolved.getString(tank==0?"inFluid":"outFluid"));
        return id!=null && BuiltInRegistries.FLUID.containsKey(id)?BuiltInRegistries.FLUID.get(id):Fluids.EMPTY;
    }
    public DataComponentPatch maintenanceComponents(int tank) {
        if(unresolved==null)return fluidComponentsInTank(tank);
        String origin=unresolved.getString("origin");
        if(tank==1 && !condenser && maintenanceFluid(tank)==biggerreactorsrework.fluids.HeatedCoolant.INSTANCE)origin=unresolved.getString("profile");
        return !origin.isEmpty() && maintenanceFluid(tank)==biggerreactorsrework.fluids.HeatedCoolant.INSTANCE?
            DataComponentPatch.builder().set(biggerreactorsrework.registries.CoolantComponents.ORIGIN.get(),origin).build():DataComponentPatch.EMPTY;
    }
    public long maintenanceDrain(int tank,Fluid fluid,DataComponentPatch components,long amount,boolean simulate) {
        if(tank<0 || tank>1 || amount<=0 || fluid==Fluids.EMPTY || fluid!=maintenanceFluid(tank) || !components.equals(maintenanceComponents(tank)))return 0;
        long drained=Math.min(amount,tank==0?inAmount:outAmount);
        if(!simulate && drained>0) {
            if(tank==0)inAmount-=drained;else outAmount-=drained;
            if(unresolved!=null) {
                unresolved.putLong(tank==0?"inAmount":"outAmount",tank==0?inAmount:outAmount);
                if(inAmount==0 && outAmount==0)unresolved=null;
            }
            outgoing+=drained;clearSelectionIfEmpty();
        }
        return drained;
    }

    public void dumpTank(int tank) {
        if (tank == IN_TANK) {
            inAmount = 0;
        }
        if (tank == OUT_TANK) {
            outAmount = 0;
            if(outputObserver!=null)outputObserver.released(outAmount,perSideCapacity);
        }
        if(unresolved!=null) {
            if(tank==IN_TANK)unresolved.putLong("inAmount",0);
            if(tank==OUT_TANK)unresolved.putLong("outAmount",0);
            if(unresolved.getLong("inAmount")==0 && unresolved.getLong("outAmount")==0)unresolved=null;
        }
        clearSelectionIfEmpty();
    }

    /** Modified 2026-10-25: fractional heat is not fluid and must not lock an empty pair. */
    protected final void clearSelectionIfEmpty() {
        if(inAmount!=0 || outAmount!=0 || unresolved!=null)return;
        thermalRemainder=0;
        activeTransition=null;inFluid=null;outFluid=null;
        inComponents=DataComponentPatch.EMPTY;outComponents=DataComponentPatch.EMPTY;
        // Preserve this tick's conversion/port counters: draining a product is still real flow.
        transitionUpdate();
    }
    
    public double transferWith(HeatBody body, double rfkt) {
        if (activeTransition == null) {
            rfTransferredLastTick=0;transitionedLastTick=0;maxTransitionedLastTick=0;
            return 0;
        }
        
        rfkt *= (condenser ? activeTransition.gasRFMKT : activeTransition.liquidRFMKT);
        
        double multiplier = (double) inAmount / (double) perSideCapacity;
        rfkt *= Math.max(multiplier, 0.01);
        
        double newTemp = body.temperature() - activeTransition.boilingPoint;
        newTemp *= Math.exp(-rfkt / body.rfPerKelvin());
        newTemp += activeTransition.boilingPoint;
        
        double toTransfer = newTemp - body.temperature();
        toTransfer *= body.rfPerKelvin();
        
        toTransfer = absorbRF(toTransfer);
        
        body.absorbRF(toTransfer);
        return -toTransfer;
    }
    
    @Override
    public double absorbRF(double rf) {
        rfTransferredLastTick = 0;
        transitionedLastTick = 0;
        maxTransitionedLastTick = 0;
        if (activeTransition == null || !Double.isFinite(rf)) return 0;
        if ((rf > 0 && !condenser) || (rf < 0 && condenser)) {
            return 0;
        }
        
        rf = Math.abs(rf)*conversionLimit;
        
        long toTransition = (long) ((rf + thermalRemainder) / activeTransition.latentHeat);
        long maxTransitionable = Math.min(inAmount, perSideCapacity - outAmount);
        
        maxTransitionedLastTick = toTransition;
        var result=biggerreactorsrework.multiblocks.reactor.simulation.base.PhaseChange.accept(rf,activeTransition.latentHeat,maxTransitionable,thermalRemainder);
        toTransition = result.units();
        thermalRemainder = result.remainder();
        transitionedLastTick = toTransition;
        
        inAmount -= toTransition;
        outAmount += toTransition;
        
        rf = result.accepted();
        rfTransferredLastTick = (long) rf;
        if (!condenser) {
            rf *= -1;
        }
        
        return rf;
    }
    
    protected void transitionUpdate() {
    }
    
    public FluidTransitionRegistry.FluidTransition activeTransition() {
        return activeTransition;
    }
    
    @Override
    public CompoundTag serializeNBT() {
        if(unresolved!=null)return unresolved.copy();
        CompoundTag nbt = new CompoundTag();
        if (inFluid != null) {
            nbt.putInt("thermalModelVersion",biggerreactorsrework.thermal.ThermalModel.VERSION);
            nbt.putString("inFluid", BuiltInRegistries.FLUID.getKey(inFluid).toString());
            nbt.putLong("inAmount", inAmount);
            nbt.putString("outFluid", BuiltInRegistries.FLUID.getKey(outFluid).toString());
            nbt.putLong("outAmount", outAmount);
            nbt.putDouble("thermalRemainder",0);
            nbt.putString("profile",biggerreactorsrework.registries.CoolantComponents.fingerprint(activeTransition));
            var origin=inComponents.get(biggerreactorsrework.registries.CoolantComponents.ORIGIN.get());
            if(origin!=null && origin.isPresent()) nbt.putString("origin",origin.get());
        }
        return nbt;
    }
    
    /** Modified 2026-10-26: quarantined saved fluids also prevent destructive inventory merges. */
    public boolean hasStoredContents() {
        return inAmount>0 || outAmount>0 || unresolved!=null &&
                (unresolved.getLong("inAmount")>0 || unresolved.getLong("outAmount")>0);
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        // Modified 2026-10-27: retain invalid legacy stock without reinterpreting its product.
        inAmount=Math.max(0,nbt.getLong("inAmount"));outAmount=Math.max(0,nbt.getLong("outAmount"));
        inFluid=Fluids.EMPTY;outFluid=Fluids.EMPTY;
        inComponents=DataComponentPatch.EMPTY;outComponents=DataComponentPatch.EMPTY;
        unresolved=nbt.contains("inFluid")?nbt.copy():null;
        activeTransition=null;
        transitionUpdate();
        if ((inAmount>0 || outAmount>0) && ((condenser && "minecraft:lava".equals(nbt.getString("inFluid"))) ||
            (!condenser && "biggerreactorsrework:liquid_obsidian".equals(nbt.getString("inFluid")) &&
             "minecraft:lava".equals(nbt.getString("outFluid"))))) return;
        if(nbt.getLong("inAmount")==0 && nbt.getLong("outAmount")==0) {
            inAmount=0;outAmount=0;unresolved=null;clearSelectionIfEmpty();return;
        }
        if (!nbt.contains("inFluid")) {
            return;
        }
        // Modified 2026-10-30: quarantine malformed identifiers without losing their stored amounts.
        ResourceLocation inFluidLocation = ResourceLocation.tryParse(nbt.getString("inFluid"));
        if (inFluidLocation == null) return;
        if (BuiltInRegistries.FLUID.containsKey(inFluidLocation)) {
            Fluid newInFluid = BuiltInRegistries.FLUID.get(inFluidLocation);
            if (newInFluid == null) {
                return;
            }
            FluidTransitionRegistry.FluidTransition newTransition;
            if (condenser) {
                var components=nbt.contains("origin") ? DataComponentPatch.builder().set(biggerreactorsrework.registries.CoolantComponents.ORIGIN.get(),nbt.getString("origin")).build() : DataComponentPatch.EMPTY;
                newTransition = FluidTransitionRegistry.gasTransition(newInFluid,components);
            } else {
                newTransition = FluidTransitionRegistry.liquidTransition(newInFluid);
            }
            if (newTransition == null) {
                return;
            }
            if(!selectable(newTransition))return;
            if(nbt.contains("profile") && !biggerreactorsrework.registries.CoolantComponents.matchesStoredProfile(nbt.getString("profile"),newTransition)) {
                biggerreactorsrework.BiggerReactors.LOGGER.error("Coolant profile changed for stored fluid {}; tank load refused to prevent energy conversion",inFluidLocation);
                return;
            }
            List<Fluid> outFluidList = (condenser ? newTransition.liquids : newTransition.gases);
            Fluid newOutFluid = null;
            ResourceLocation outFluidLocation = ResourceLocation.tryParse(nbt.getString("outFluid"));
            if (outFluidLocation == null && nbt.contains("outFluid")) return;
            if (outFluidLocation != null && BuiltInRegistries.FLUID.containsKey(outFluidLocation)) {
                Fluid oldOutFluid = BuiltInRegistries.FLUID.get(outFluidLocation);
                if (outFluidList.contains(oldOutFluid)) {
                    newOutFluid = oldOutFluid;
                }
            }
            if (newOutFluid == null) {
                newOutFluid = outFluidList.get(0);
            }
            activeTransition = newTransition;
            inFluid = newInFluid;
            outFluid = newOutFluid;
            inComponents=condenser && newTransition.genericHot() ? biggerreactorsrework.registries.CoolantComponents.heated(newTransition) : DataComponentPatch.EMPTY;
            outComponents=!condenser && newTransition.genericHot() ? biggerreactorsrework.registries.CoolantComponents.heated(newTransition) : DataComponentPatch.EMPTY;
            inAmount = Math.max(0, nbt.getLong("inAmount"));
            outAmount = Math.max(0, nbt.getLong("outAmount"));
            thermalRemainder=0;
            unresolved=null;
            transitionUpdate();
            clearSelectionIfEmpty();
        }
    }
    
    @Nullable
    protected FluidTransitionRegistry.FluidTransition selectTransition(Fluid fluid) {
        if (condenser) {
            return FluidTransitionRegistry.gasTransition(fluid);
        } else {
            return FluidTransitionRegistry.liquidTransition(fluid);
        }
    }
}
