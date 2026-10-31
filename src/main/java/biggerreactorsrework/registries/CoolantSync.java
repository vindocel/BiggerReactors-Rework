package biggerreactorsrework.registries;

// Modified 2026-10-26: clear client authority on disconnect and retain valid empty snapshots.

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.core.networking.SimpleChannel;
import biggerreactorsrework.core.serialization.DataCompound;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import java.util.*;

/** Resolved optional profiles, shared by client tooltips, guide and recipe viewers. */
public final class CoolantSync {
    private static List<FluidTransitionRegistry.FluidTransition> clientProfiles=List.of();
    private static boolean received;
    public static void clearClient(){clientProfiles=List.of();received=false;}
    private static final SimpleChannel CHANNEL=new SimpleChannel(Identifier.fromNamespaceAndPath(BiggerReactors.modid,"coolants"),(data,ctx)->{
        var result=new ArrayList<FluidTransitionRegistry.FluidTransition>();
        for(var value:data.getList("profiles"))if(value instanceof Map<?,?> map){
            var cold=Identifier.tryParse(String.valueOf(map.get("cold")));var hot=Identifier.tryParse(String.valueOf(map.get("hot")));
            if(cold==null || hot==null || !BuiltInRegistries.FLUID.containsKey(cold) || !BuiltInRegistries.FLUID.containsKey(hot))continue;
            var t=new FluidTransitionRegistry.FluidTransition(List.of(BuiltInRegistries.FLUID.getValue(cold)),List.of(BuiltInRegistries.FLUID.getValue(hot)),
                    number(map,"latent"),number(map,"boiling"),number(map,"liquid"),number(map,"gas"),number(map,"turbine"));
            t.recovery=number(map,"recovery");result.add(t);
        }
        clientProfiles=List.copyOf(result); received=true;
        if (net.neoforged.fml.loading.FMLEnvironment.getDist().isClient()) biggerreactorsrework.BiggerReactorsClient.refreshCoolantSprites(clientProfiles);
    },null);
    private static double number(Map<?,?> map,String key){return ((Number)map.get(key)).doubleValue();}
    public static void init(){NeoForge.EVENT_BUS.addListener(CoolantSync::send);}
    public static List<FluidTransitionRegistry.FluidTransition> profiles(){return received || net.neoforged.fml.loading.FMLEnvironment.getDist().isClient() && biggerreactorsrework.BiggerReactorsClient.connected()?clientProfiles:List.copyOf(FluidTransitionRegistry.transitions());}
    public static FluidTransitionRegistry.FluidTransition find(Identifier cold){return profiles().stream().filter(t->BuiltInRegistries.FLUID.getKey(t.liquids.getFirst()).equals(cold)).findFirst().orElse(null);}
    private static void send(OnDatapackSyncEvent event){
        var rows=new ArrayList<Map<String,Object>>();
        for(var t:FluidTransitionRegistry.transitions())rows.add(Map.of("cold",BuiltInRegistries.FLUID.getKey(t.liquids.getFirst()).toString(),"hot",BuiltInRegistries.FLUID.getKey(t.gases.getFirst()).toString(),
                "latent",t.latentHeat,"boiling",t.boilingPoint,"liquid",t.liquidRFMKT,"gas",t.gasRFMKT,"turbine",t.turbineMultiplier,"recovery",t.recovery));
        var data=new DataCompound();data.put("profiles",rows);
        if(event.getPlayer()!=null)CHANNEL.sendToPlayer(event.getPlayer(),data);
        else event.getPlayerList().getPlayers().forEach(p->CHANNEL.sendToPlayer(p,data));
    }
    private CoolantSync(){}
}
