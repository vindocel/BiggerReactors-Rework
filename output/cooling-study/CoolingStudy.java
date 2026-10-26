import biggerreactorsrework.Config;
import biggerreactorsrework.core.util.HeatBody;
import biggerreactorsrework.multiblocks.reactor.simulation.*;
import biggerreactorsrework.multiblocks.reactor.simulation.cpu.TimeSlicedReactorSimulation;
import biggerreactorsrework.registries.ReactorModeratorRegistry.ModeratorProperties;
import biggerreactorsrework.registries.FluidTransitionRegistry;
import biggerreactorsrework.registries.FluidTransitionRegistry.TransitionProperties;
import biggerreactorsrework.util.FluidTransitionTank;
import com.google.gson.GsonBuilder;
import java.nio.file.*;
import java.util.*;

/** Read-only numerical study of the existing compiled simulation, outside Minecraft.
 * No block, registry, world, mod sources or gameplay settings are changed.
 * HX geometry and tick order reproduce HeatExchangerMultiblockController.
 */
public class CoolingStudy {
  static final int WARMUP=160000, SAMPLE=20000;
  static final class Tank extends FluidTransitionTank {
    Tank(boolean condenser,int n,double latent,double boiling,double liquid,double gas) {
      super(condenser);
      perSideCapacity=n*Config.CONFIG.HeatExchanger.ChannelTankVolumePerBlock;
      activeTransition=new FluidTransitionRegistry.FluidTransition(List.of(),List.of(),latent,boiling,liquid,gas,0);
    }
    void full(){inAmount=perSideCapacity;}
    long put(long n){long v=Math.min(n,perSideCapacity-inAmount);inAmount+=v;return v;}
    long take(long n){long v=Math.min(n,outAmount);outAmount-=v;return v;}
    long out(){return outAmount;}
    long in(){return inAmount;}
  }
  static final class Exchanger {
    final int n;
    final Tank hot,water;
    final HeatBody cond=new HeatBody(),evap=new HeatBody(),air=new HeatBody(),ambient=new HeatBody();
    Exchanger(int n) {
      this.n=n;
      hot=new Tank(true,n,15,1273.15,1.9,1);
      water=new Tank(false,n,4,373.15,.6,.025);
      var c=Config.CONFIG.HeatExchanger;
      cond.setRfPerKelvin(n*c.ChannelFEPerKelvinUnitVolume);
      evap.setRfPerKelvin(n*c.ChannelFEPerKelvinUnitVolume);
      air.setRfPerKelvin((16*(n+2)-2*n)*c.AirFEPerKelvinUnitVolume);
      for(var b:List.of(cond,evap,air,ambient))b.setTemperature(293.15);
      ambient.setInfinite(true);
      water.full();
    }
    void tick() {
      var c=Config.CONFIG.HeatExchanger;
      hot.transferWith(cond,n*c.ChannelInternalSurfaceArea);
      cond.transferWith(air,(3*n+2)*c.AirFEPerKelvinMetreSquared);
      cond.transferWith(evap,n*c.ChannelFEPerKelvinMetreSquared);
      evap.transferWith(air,(3*n+2)*c.AirFEPerKelvinMetreSquared);
      water.transferWith(evap,n*c.ChannelInternalSurfaceArea);
      air.transferWith(ambient,(16*n+64)*c.AmbientFEPerKelvinMetreSquared);
    }
  }
  static TimeSlicedReactorSimulation reactor(int columns) {
    var d=new SimulationDescription();d.setSize(3,3,3);
    d.setDefaultIModeratorProperties(new ModeratorProperties(.1,.25,1.1,.05));
    for(int x=0;x<3;x++)for(int z=0;z<3;z++) {
      boolean fuel=columns==1 ? x==1&&z==1 : columns==5 ? (x+z)%2==0 : (x+z)%2==1;
      if(fuel)d.setControlRod(x,z,true);
      else if(columns!=1 || Math.abs(x-1)+Math.abs(z-1)==1)for(int y=0;y<3;y++)d.setManifold(x,y,z,true);
    }
    var s=new TimeSlicedReactorSimulation(d,new SimulationConfiguration(293.15,false));
    s.coolantTank().setModeratorProperties(new ModeratorProperties(.3,.7,1.35,.75));
    s.coolantTank().setTransitionProperties(new TransitionProperties(15,1273.15,1.9,1,0));
    s.coolantTank().insertLiquid(s.coolantTank().perSideCapacity());
    return s;
  }
  static Map<String,Object> run(int columns,int length,boolean isolated) {
    var s=isolated?null:reactor(columns);
    var hx=length==0?null:new Exchanger(length);
    double burn=0,reacted=0,returned=0,steam=0,heat=0,burnStart=0,burnEnd=0;
    double fuelTemperatureStart=0,casingTemperatureStart=0,inventoryMin=Double.POSITIVE_INFINITY,inventoryMax=0;
    for(int i=0;i<WARMUP+SAMPLE;i++) {
      if(s!=null) {
        s.fuelTank().extractWaste(Long.MAX_VALUE,false);
        s.fuelTank().insertFuel(Long.MAX_VALUE,false);
        if(hx==null) {
          s.coolantTank().extractVapor(s.coolantTank().vaporAmount());
          s.coolantTank().insertLiquid(s.coolantTank().perSideCapacity()-s.coolantTank().liquidAmount());
        }
        s.tick(true);
        if(hx!=null) {
          long transferred=hx.hot.put(s.coolantTank().vaporAmount());
          s.coolantTank().extractVapor(transferred);
        }
      }
      if(hx!=null) {
        if(isolated)hx.hot.full();
        hx.water.full();hx.water.take(Long.MAX_VALUE);
        hx.tick();
        if(s!=null) {
          long transferred=hx.hot.take(s.coolantTank().perSideCapacity()-s.coolantTank().liquidAmount());
          s.coolantTank().insertLiquid(transferred);
        } else hx.hot.take(Long.MAX_VALUE);
      }
      if(s!=null&&hx!=null) {
        long inventory=s.coolantTank().liquidAmount()+s.coolantTank().vaporAmount()+hx.hot.in()+hx.hot.out();
        inventoryMin=Math.min(inventoryMin,inventory);inventoryMax=Math.max(inventoryMax,inventory);
      }
      if(i==WARMUP&&s!=null){fuelTemperatureStart=s.fuelHeat();casingTemperatureStart=s.stackHeat();}
      if(i>=WARMUP) {
        if(s!=null){burn+=s.fuelTank().burnedLastTick();reacted+=s.coolantTank().transitionedLastTick();heat+=s.coolantTank().rfTransferredLastTick();}
        if(hx!=null){returned+=hx.hot.transitionedLastTick();steam+=hx.water.transitionedLastTick();}
        if(s!=null&&i<WARMUP+1000)burnStart+=s.fuelTank().burnedLastTick()/1000;
        if(s!=null&&i>=WARMUP+SAMPLE-1000)burnEnd+=s.fuelTank().burnedLastTick()/1000;
      }
    }
    var out=new LinkedHashMap<String,Object>();
    out.put("case",isolated?"hx_ideal_supply":length==0?"reactor_ideal_supply":"coupled_closed_primary");
    out.put("fuel_columns",columns);out.put("hx_dimensions",length==0?"none":"4x4x"+(length+2));
    out.put("channels_each",length);out.put("inter_channel_contact_faces",length);
    if(s!=null) {
      out.put("fuel_capacity_mB",s.fuelTank().capacity());out.put("burn_mB_s",burn/SAMPLE*20);
      out.put("burn_first1000_mB_s",burnStart*20);out.put("burn_last1000_mB_s",burnEnd*20);
      out.put("reactor_lava_mB_s",reacted/SAMPLE*20);out.put("reactor_heat_FE_t",heat/SAMPLE);
      out.put("fuel_C",s.fuelHeat()-273.15);out.put("casing_C",s.stackHeat()-273.15);
      out.put("fuel_temperature_change_K",s.fuelHeat()-fuelTemperatureStart);
      out.put("casing_temperature_change_K",s.stackHeat()-casingTemperatureStart);
      out.put("reactor_cold_mB",s.coolantTank().liquidAmount());out.put("reactor_hot_mB",s.coolantTank().vaporAmount());
      out.put("reactor_each_tank_capacity_mB",s.coolantTank().perSideCapacity());
      if(hx!=null){out.put("closed_primary_min_total_mB",inventoryMin);out.put("closed_primary_max_total_mB",inventoryMax);}
    }
    if(hx!=null) {
      out.put("hx_return_obsidian_mB_s",returned/SAMPLE*20);out.put("steam_mB_s",steam/SAMPLE*20);
      out.put("hx_hot_in_mB",hx.hot.in());out.put("hx_cold_out_mB",hx.hot.out());
      out.put("hx_cond_C",hx.cond.temperature()-273.15);out.put("hx_evap_C",hx.evap.temperature()-273.15);
      out.put("hx_each_tank_capacity_mB",hx.hot.perSideCapacity);
    }
    return out;
  }
  public static void main(String[] args)throws Exception {
    Locale.setDefault(Locale.ROOT);
    var results=new ArrayList<Map<String,Object>>();
    for(int length:new int[]{2,3,4,5,6})results.add(run(0,length,true));
    for(int columns:new int[]{1,4,5}) {
      results.add(run(columns,0,false));
      for(int length:new int[]{2,3,4,6})results.add(run(columns,length,false));
    }
    var report=new LinkedHashMap<String,Object>();
    report.put("model","existing production simulation, default Config, 20 game ticks per second; no new rework controller");
    report.put("warmup_ticks",WARMUP);report.put("sample_ticks",SAMPLE);report.put("results",results);
    String json=new GsonBuilder().setPrettyPrinting().create().toJson(report);
    Files.writeString(Path.of("output/cooling-study/results.json"),json);
    for(var row:results)System.out.println(row);
  }
}
