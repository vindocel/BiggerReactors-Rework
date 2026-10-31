package biggerreactorsrework.multiblocks.turbine.simulation;

import net.minecraft.nbt.CompoundTag;
import biggerreactorsrework.util.NbtSerializable;
import biggerreactorsrework.multiblocks.turbine.state.VentState;
import biggerreactorsrework.registries.TurbineCoilRegistry;
import org.joml.Vector4i;

import java.util.ArrayList;

public interface ITurbineSimulation extends NbtSerializable {
    double MAX_RPM = 2200.0;
    
    void reset();
    
    void resize(int x, int y, int z);
    
    void setRotorConfiguration(ArrayList<Vector4i> rotorConfiguration);
    
    void setCoilData(int x, int y, TurbineCoilRegistry.CoilData coilData);
    
    void updateInternalValues();
    
    void setVentState(VentState state);
    
    VentState ventState();
    
    double RPM();
    
    double bladeEfficiencyLastTick();
    
    long flowLastTick();
    
    long nominalFlowRate();
    
    void setNominalFlowRate(long flowRate);
    
    long flowRateLimit();
    
    ITurbineBattery battery();
    
    ITurbineFluidTank fluidTank();
    
    void tick();
    
    void setActive(boolean active);
    boolean active();
    
    void setCoilEngaged(boolean engaged);
    
    boolean coilEngaged();
    
    long FEGeneratedLastTick();
    
    @Deprecated
    long bladeSurfaceArea();
    
    @Deprecated
    double rotorMass();
    
    String debugString();
}
