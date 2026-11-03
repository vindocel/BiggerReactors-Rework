package biggerreactorsrework.multiblocks.reactor.simulation.base;

import biggerreactorsrework.multiblocks.reactor.simulation.*;
import biggerreactorsrework.registries.ReactorModeratorRegistry;
import biggerreactorsrework.core.debug.DebugInfo;
import biggerreactorsrework.core.serialization.DataCompound;
import biggerreactorsrework.thermal.*;
import java.util.*;

// Modified 2026-11-02: fuel-paid thermal simulation; no radiation multipliers or stored waste heat.
public class BaseReactorSimulation implements IReactorSimulation {
    private final SimulationConfiguration configuration;
    private final ThermalModel.Preset preset;
    private final ThermalModel.Geometry geometry;
    private final Rod[][] rods;
    private final List<Column> columns = new ArrayList<>();
    private final FuelTank fuelTank;
    private final Battery battery;
    private final CoolantTank coolantTank;
    private final double efficiency;
    private double electricalFraction;
    private ThermalSnapshot snapshot = ThermalSnapshot.EMPTY;
    private static final int[][] DIRECTIONS={{1,0},{-1,0},{0,1},{0,-1}};

    private static final class Rod implements ControlRod {
        private double insertion;
        public double insertion() { return insertion; }
        public void setInsertion(double value) {
            if(!Double.isFinite(value) || value<0 || value>100) throw new IllegalArgumentException("Insertion must be in [0,100]");
            insertion=value;
        }
    }
    private record Column(Rod rod, int blocks, double covered, int isolated, double[] passive) { }

    public BaseReactorSimulation(SimulationDescription d, SimulationConfiguration configuration) {
        d.ensureValid(); this.configuration=configuration; preset=configuration.thermal(); geometry=d.thermalGeometry();
        rods=new Rod[d.x()][d.z()];
        double totalPassiveWeight=0; long productiveCells=0;
        if(configuration.passivelyCooled()) for(int x=0;x<d.x();x++) for(int y=0;y<d.y();y++) for(int z=0;z<d.z();z++) {
            var p=d.moderatorPropertiesAt(x,y,z);
            if(!d.isControlRodAt(x,z) && p!=null && p.passiveClass()!=ThermalModel.PassiveClass.NONE) productiveCells++;
        }
        for(var column:geometry.columns()) {
            Rod rod=new Rod(); rods[column.x()][column.z()]=rod;
            double[] passive=new double[ThermalModel.PassiveClass.values().length];
            int passiveIsolated=0;
            for(int y=0;y<d.y();y++) {
                int possible=0; double weight=0;
                for(int[] direction:DIRECTIONS) {
                    int nx=column.x()+direction[0],nz=column.z()+direction[1];
                    if(nx>=0 && nx<d.x() && nz>=0 && nz<d.z()) possible++;
                }
                if(possible==0) { passiveIsolated++; continue; }
                for(int[] direction:DIRECTIONS) {
                    int nx=column.x()+direction[0],nz=column.z()+direction[1];
                    if(nx<0 || nx>=d.x() || nz<0 || nz>=d.z() || d.isControlRodAt(nx,nz)) continue;
                    ReactorModeratorRegistry.IModeratorProperties p=d.moderatorPropertiesAt(nx,y,nz);
                    var type=p==null || d.isManifoldAt(nx,y,nz) ? ThermalModel.PassiveClass.NONE : p.passiveClass();
                    double contribution=type.coolingWeight/possible;
                    passive[type.ordinal()]+=contribution; weight+=contribution;
                }
                if(weight==0) passiveIsolated++;
            }
            double covered=configuration.passivelyCooled()?Arrays.stream(passive).sum():column.coveredRods();
            totalPassiveWeight+=covered;
            columns.add(new Column(rod,column.rods(),covered,configuration.passivelyCooled()?passiveIsolated:column.isolatedRods(),passive));
        }
        efficiency=configuration.passivelyCooled()? geometry.rods()==0?0:Math.min(totalPassiveWeight/geometry.rods(),
                (double)(geometry.rods()+productiveCells)/geometry.volume()):geometry.efficiency();
        fuelTank=new FuelTank(Math.multiplyExact(preset.fuelPerRod(),geometry.rods()));
        if(configuration.passivelyCooled()) {
            long shell=(long)(d.x()+2)*(d.y()+2)*(d.z()+2)-geometry.volume();
            battery=new Battery(Math.multiplyExact(shell,configuration.passiveBatteryPerExternalBlock()),configuration); coolantTank=null;
        } else {
            battery=null; coolantTank=new CoolantTank(Math.multiplyExact(geometry.usefulManifolds(),preset.coolantPerManifold()),d.defaultModeratorProperties(),configuration);
        }
    }

    @Override public void tick(boolean active) {
        double usable=fuelTank.usableFuel(),open=0,coveredOpen=0,isolatedOpen=0,rfOpen=0;
        for(var c:columns) {
            double opening=1-c.rod.insertion/100;
            open+=c.blocks*opening; coveredOpen+=c.covered*opening; isolatedOpen+=c.isolated*opening;
            for(var type:ThermalModel.PassiveClass.values()) rfOpen+=c.passive[type.ordinal()]*opening*type.rfPerKJ;
        }
        double requested=active?preset.requestedBurn(usable,geometry.rods(),open):0;
        fuelTank.burn(requested);
        double generated=fuelTank.burnedLastTick()*preset.heatPerFuel();
        double local=open==0?0:generated*(coveredOpen/open);
        var heat=ThermalModel.heat(generated,local,open==0?0:generated*(isolatedOpen/open),efficiency);
        ThermalModel.Conversion conversion;
        double potentialRF=0,discardedRF=0;long acceptedRF=0;
        if(coolantTank!=null) conversion=coolantTank.convertHeat(heat.capturable(),preset);
        else {
            conversion=new ThermalModel.Conversion(0,heat.capturable(),heat.capturable(),0,0,0,0);
            double exact=open==0?0:generated*rfOpen/open;
            if(local>0) exact*=heat.capturable()/local;
            double withFraction=exact+electricalFraction;
            long whole=ThermalModel.wholeUnits(withFraction);
            potentialRF=whole; acceptedRF=battery.generate(whole);discardedRF=whole-acceptedRF;
            electricalFraction=acceptedRF==whole?withFraction-whole:0;
            if(electricalFraction<0 || electricalFraction>=1 || !Double.isFinite(electricalFraction))electricalFraction=0;
        }
        snapshot=new ThermalSnapshot(generated,heat.capturable(),heat.isolated(),heat.structural(),conversion.processed(),
                conversion.useful(),conversion.processLoss(),conversion.missingCoolant(),conversion.blockedOutput(),conversion.rounding(),
                efficiency,geometry.contactEfficiency(),geometry.occupancy(),geometry.rods(),geometry.usefulManifolds(),
                usable,preset.burnPerRod(geometry.rods()),requested,potentialRF,acceptedRF,discardedRF);
    }
    @Override public ThermalSnapshot thermal() { return snapshot; }
    @Override public double burnFraction() { return geometry.rods()==0?0:preset.fill(fuelTank.usableFuel(),geometry.rods()); }
    @Override public int manifoldContacts() { return geometry.contacts(); }
    @Override public IBattery battery() { return battery; }
    @Override public ICoolantTank coolantTank() { return coolantTank; }
    @Override public IFuelTank fuelTank() { return fuelTank; }
    @Override public ControlRod controlRodAt(int x,int z) { return x<0 || x>=rods.length || z<0 || z>=rods[0].length?null:rods[x][z]; }
    @Override public void setAllControlRodInsertions(double value) { for(var c:columns)c.rod.setInsertion(value); }
    /** Deprecated temperature/fertility readings are neutral; productive quantities are in thermal(). */
    @Override public double fertility() { return 1; }
    @Override public double fuelHeat() { return configuration.ambientTemperature(); }
    @Override public double stackHeat() { return configuration.ambientTemperature(); }
    @Override public double ambientTemperature() { return configuration.ambientTemperature(); }
    @Override public DataCompound save() {
        var n=new DataCompound(); n.put("thermalModelVersion",ThermalModel.VERSION);n.put("fuelTank",fuelTank.save());
        if(battery!=null)n.put("battery",battery.save()); if(coolantTank!=null)n.put("coolantTank",coolantTank.save());
        n.put("electricalFraction",electricalFraction);
        n.put("insertions",columns.stream().map(c->c.rod.insertion).toList()); return n;
    }
    @Override public void load(DataCompound n) {
        fuelTank.load(n.getCompound("fuelTank"));
        if(battery!=null)battery.load(n.getCompound("battery"));
        if(coolantTank!=null)coolantTank.load(n.getCompound("coolantTank"));
        electricalFraction=n.getInt("thermalModelVersion")==ThermalModel.VERSION?n.getDouble("electricalFraction"):0;
        if(!Double.isFinite(electricalFraction) || electricalFraction<0 || electricalFraction>=1)electricalFraction=0;
        if(!n.getList("insertions").isEmpty()) {
            var values=n.getList("insertions");
            if(values.size()==columns.size())for(int i=0;i<values.size();i++)if(values.get(i) instanceof Number v && Double.isFinite(v.doubleValue()))
                columns.get(i).rod.setInsertion(Math.max(0,Math.min(100,v.doubleValue())));
        }
        snapshot=ThermalSnapshot.EMPTY;
    }
    @Override public DebugInfo getDebugInfo() {
        var info=new DebugInfo("Thermal simulation v"+ThermalModel.VERSION);
        info.add("Fuel: "+fuelTank.usableFuel());info.add("Burn: "+fuelTank.burnedLastTick());
        info.add("Generated kJ/t: "+snapshot.generated());info.add("Structural efficiency: "+efficiency);return info;
    }
}
