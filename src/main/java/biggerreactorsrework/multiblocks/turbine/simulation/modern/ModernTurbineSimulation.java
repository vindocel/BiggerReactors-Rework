package biggerreactorsrework.multiblocks.turbine.simulation.modern;

import net.minecraft.nbt.CompoundTag;
import biggerreactorsrework.Config;
import biggerreactorsrework.multiblocks.turbine.simulation.ITurbineBattery;
import biggerreactorsrework.multiblocks.turbine.simulation.ITurbineFluidTank;
import biggerreactorsrework.multiblocks.turbine.simulation.ITurbineSimulation;
import biggerreactorsrework.multiblocks.turbine.state.VentState;
import biggerreactorsrework.registries.TurbineCoilRegistry;
import org.joml.Vector4i;

import java.util.ArrayList;

// Modified 2026-11-03: generation is a fixed FE per steam mB from the coil material, limited by blade and coil
// capacity; rotor speed only ramps the start-up, the rotor hazard and the overspeed ceiling.
public class ModernTurbineSimulation implements ITurbineSimulation {
    public static final double NOMINAL_RPM = 1800;
    /** Ticks to reach nominal speed with any steam (20 s), and to stop from it without steam (30 s). */
    private static final double SPIN_UP_TICKS = 400, SPIN_DOWN_TICKS = 600;
    
    private int x, y, z;
    
    private boolean active = false;
    
    private long coilSize;
    private double coilFESum;
    private long coilCapacity;
    
    private double rotorCapacityPerRPM;
    
    private long maxFlowRate = -1;
    private long maxMaxFlowRate = 0;
    
    private int rotorShafts;
    private double rotorAxialMass;
    private double rotorMass;
    private double linearBladeMetersPerRevolution = 0;
    
    private VentState ventState = VentState.OVERFLOW;
    private boolean coilEngaged = true;
    
    private double rpm = 0;
    private double feRemainder = 0;
    private final FluidTank fluidTank = new FluidTank();
    private final Battery battery = new Battery();
    
    private double energyGeneratedLastTick;
    private double rotorEfficiencyLastTick;
    
    @Override
    public void reset() {
        rpm = 0;
        feRemainder = 0;
    }
    
    @Override
    public void resize(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        coilSize = 0;
        coilFESum = 0;
        coilCapacity = 0;
        maxMaxFlowRate = (((long) x * z) - 1 /* bearing*/) * Config.CONFIG.Turbine.FlowRatePerBlock;
    }
    
    @Override
    public void setRotorConfiguration(ArrayList<Vector4i> rotorConfiguration) {
        rotorMass = 0;
        linearBladeMetersPerRevolution = 0;
        for (Vector4i vector4i : rotorConfiguration) {
            linearBladeMetersPerRevolution += rangeFromZeroSum(vector4i.x);
            linearBladeMetersPerRevolution += rangeFromZeroSum(vector4i.y);
            linearBladeMetersPerRevolution += rangeFromZeroSum(vector4i.z);
            linearBladeMetersPerRevolution += rangeFromZeroSum(vector4i.w);
            rotorMass += vector4i.x + vector4i.y + vector4i.z + vector4i.w;
        }
        
        rotorCapacityPerRPM = linearBladeMetersPerRevolution * Config.CONFIG.Turbine.BladeSteamPerKilometre;
        rotorCapacityPerRPM /= 1000; // metre / kilometre
        
        rotorShafts = rotorConfiguration.size();
        
        rotorAxialMass = rotorShafts * Config.CONFIG.Turbine.RotorAxialMassPerShaft;
        rotorAxialMass += linearBladeMetersPerRevolution * Config.CONFIG.Turbine.RotorAxialMassPerBlade;
        
        rotorCapacityPerRPM *= 2 * Math.PI;
        
        rotorMass *= Config.CONFIG.Turbine.RotorAxialMassPerBlade;
        rotorMass += (double) rotorShafts * Config.CONFIG.Turbine.RotorAxialMassPerShaft;
        
        if (maxFlowRate == -1) {
            setNominalFlowRate((long) (rotorCapacityPerRPM * 1800));
        }
    }
    
    private long rangeFromZeroSum(long upper) {
        return ((1 + upper) * upper) / 2;
    }
    
    @Override
    public void setCoilData(int x, int y, TurbineCoilRegistry.CoilData coilData) {
        coilFESum += coilData.fePerMB();
        coilCapacity += coilData.steamPerTick();
        coilSize++;
    }
    
    @Override
    public void updateInternalValues() {
        battery.setCapacity((coilSize + 1) * Config.CONFIG.Turbine.BatterySizePerCoilBlock);
        fluidTank.perSideCapacity = (((long) x * y * z) - ((long) rotorShafts + coilSize)) * Config.CONFIG.Turbine.TankVolumePerBlock;
    }
    
    @Override
    public void setVentState(VentState state) {
        ventState = state;
    }
    
    @Override
    public VentState ventState() {
        return ventState;
    }
    
    @Override
    public double RPM() {
        return rpm;
    }
    
    @Override
    public double bladeEfficiencyLastTick() {
        return rotorEfficiencyLastTick;
    }
    
    @Override
    public long flowLastTick() {
        return fluidTank.transitionedLastTick();
    }
    
    @Override
    public long nominalFlowRate() {
        return Math.min(maxFlowRate, flowRateLimit());
    }
    
    @Override
    public void setNominalFlowRate(long flowRate) {
        maxFlowRate = flowRate;
        maxFlowRate = Math.min(flowRateLimit(), Math.max(0, flowRate));
    }
    
    @Override
    public long flowRateLimit() {
        // Modified 2026-11-03: allow steam above the nominal load so the rotor can reach the governor.
        return Math.min(maxMaxFlowRate, (long) Math.ceil(nominalSteamCapacity() * MAX_RPM / NOMINAL_RPM));
    }

    public double nominalSteamCapacity() {
        return coilCapacity > 0 ? Math.min(bladeCapacity(), coilCapacity) : bladeCapacity();
    }

    public double steadyRPM(double flow) {
        double capacity = nominalSteamCapacity();
        if (capacity <= 0 || flow <= 0) return 0;
        double load = flow * steamFactor(fluidTank.activeTransition()) / capacity;
        return Math.min(MAX_RPM, load * (coilEngaged && coilCapacity > 0 ? NOMINAL_RPM : MAX_RPM));
    }

    /** The original grid efficiency curve, applied to the current coil's FE per mB. */
    public static double speedEfficiency(double speed) {
        if (speed <= 0) return 0;
        double frequency = Config.CONFIG.Turbine.EffectiveGridFrequency;
        double peak = frequency * 60;
        double minimum = peak / Math.pow(2, Config.CONFIG.Turbine.EfficiencyPeaks - 0.5);
        if (speed < minimum) return 0.5;
        if (speed > peak) {
            double excess = speed - peak;
            return Math.max(0, 1 - excess * excess / (8 * frequency * peak));
        }
        double harmonic = 1 - 2 * Math.log(speed / peak) / Math.log(2);
        return 0.75 - 0.25 * Math.cos(harmonic * Math.PI);
    }
    
    @Override
    public ITurbineBattery battery() {
        return battery;
    }
    
    @Override
    public ITurbineFluidTank fluidTank() {
        return fluidTank;
    }
    
    /** Average FE per steam mB of the coils, for this turbine's steam. */
    public double fePerMB() {
        var transition = fluidTank.activeTransition();
        return coilSize <= 0 ? 0 : coilFESum / coilSize * steamFactor(transition);
    }

    /** Steam the blades take at nominal speed. */
    public double bladeCapacity() {
        return rotorCapacityPerRPM * NOMINAL_RPM;
    }

    public long coilCapacity() {
        return coilCapacity;
    }

    /** Steam whose torque equals the reference steam's: other vapors scale by latent heat and turbine multiplier. */
    public static double steamFactor(@javax.annotation.Nullable biggerreactorsrework.registries.FluidTransitionRegistry.FluidTransition transition) {
        return transition == null ? 1 : Math.max(0, transition.latentHeat * transition.turbineMultiplier / 10);
    }

    /** FE/t once the rotor has settled at the speed driven by this flow. */
    public double steadyPower(double flow) {
        double admitted = Math.min(flow, flowRateLimit());
        return powerAtSpeed(admitted, steadyRPM(admitted));
    }

    private double powerAtSpeed(double flow, double speed) {
        double useful = Math.min(Math.max(0, flow), nominalSteamCapacity());
        return coilEngaged ? useful * fePerMB() * speedEfficiency(speed) : 0;
    }

    @Override
    public void tick() {
        double flow;
        if (active) {
            flow = fluidTank.flow(Math.min(maxFlowRate, flowRateLimit()), ventState != VentState.CLOSED);
        } else {
            fluidTank.flow(0, ventState != VentState.CLOSED);
            flow = 0;
        }
        if (ventState == VentState.ALL) {
            fluidTank.dumpLiquid();
        }
        
        double target = steadyRPM(flow);
        if (rpm < target) {
            rpm = Math.min(target, rpm + NOMINAL_RPM / SPIN_UP_TICKS);
        } else if (rpm > target) {
            rpm = Math.max(target, rpm - NOMINAL_RPM / SPIN_DOWN_TICKS);
        }

        rotorEfficiencyLastTick = flow > 0 ? Math.min(flow, nominalSteamCapacity()) / flow * speedEfficiency(rpm) : 0;
        if (coilEngaged) {
            double generated = powerAtSpeed(flow, rpm) + feRemainder;
            long whole = (long) Math.floor(generated);
            feRemainder = generated - whole;
            energyGeneratedLastTick = whole;
            if (whole > 0) {
                battery.generate(whole);
            }
        } else {
            energyGeneratedLastTick = 0;
            feRemainder = 0;
        }
    }
    
    @Override
    public void setActive(boolean active) {
        this.active = active;
    }
    
    @Override
    public boolean active() {
        return active;
    }
    
    @Override
    public void setCoilEngaged(boolean engaged) {
        coilEngaged = engaged;
    }
    
    @Override
    public boolean coilEngaged() {
        return coilEngaged;
    }
    
    @Override
    public long FEGeneratedLastTick() {
        return (long) energyGeneratedLastTick;
    }
    
    @Override
    public long bladeSurfaceArea() {
        return 0;
    }
    
    @Override
    public double rotorMass() {
        return rotorAxialMass;
    }

    /** Inlet full and outlet empty, as in {@link #capacityStudy()}; for calculations outside the level only. */
    public void idealSupply(biggerreactorsrework.registries.FluidTransitionRegistry.FluidTransition transition) {
        fluidTank.idealSupply(transition);
    }

    /** Intake capacity and power at the selected steady flow. */
    public java.util.function.Supplier<biggerreactorsrework.core.util.Statistics.Potential> capacityStudy() {
        double efficient = flowRateLimit();
        double flow = Math.min(maxFlowRate, efficient);
        double power = steadyPower(flow);
        return () -> new biggerreactorsrework.core.util.Statistics.Potential(efficient, flow, power);
    }
    
    @Override
    public String debugString() {
        return "";
    }
    
    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.put("fluidTank", fluidTank.serializeNBT());
        nbt.put("battery", battery.serializeNBT());
        nbt.putInt("ventState", ventState.toInt());
        nbt.putDouble("rpm", rpm);
        nbt.putDouble("feRemainder", feRemainder);
        nbt.putLong("maxFlowRate", maxFlowRate);
        nbt.putBoolean("coilEngaged", coilEngaged);
        nbt.putBoolean("active", active);
        return nbt;
    }
    
    @Override
    public void deserializeNBT(CompoundTag nbt) {
        fluidTank.deserializeNBT(nbt.getCompoundOrEmpty("fluidTank"));
        battery.deserializeNBT(nbt.getCompoundOrEmpty("battery"));
        // Modified 2026-10-30: invalid saved controls cannot prevent the machine loading.
        int vent = nbt.getIntOr("ventState", 0);
        ventState = vent >= 0 && vent <= 2 ? VentState.fromInt(vent) : VentState.OVERFLOW;
        rpm = Math.min(MAX_RPM, biggerreactorsrework.core.util.SavedValues.nonNegative(nbt.getDoubleOr("rpm", 0D), 0));
        feRemainder = Math.min(1, biggerreactorsrework.core.util.SavedValues.nonNegative(nbt.getDoubleOr("feRemainder", 0D), 0));
        maxFlowRate = Math.max(0, nbt.getLongOr("maxFlowRate", 0L));
        coilEngaged = nbt.getBooleanOr("coilEngaged", false);
        active = nbt.getBooleanOr("active", false);
    }
}
