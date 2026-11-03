package biggerreactorsrework.registries;

import biggerreactorsrework.BiggerReactors;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

/** Persistent, networked origin+profile fingerprint; values never carry client-supplied energy. */
public final class CoolantComponents {
    private static final DeferredRegister.DataComponents TYPES = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE,BiggerReactors.modid);
    public static final Supplier<DataComponentType<String>> ORIGIN = TYPES.registerComponentType("coolant_origin",
            b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));
    public static void register(IEventBus bus) { TYPES.register(bus); }
    public static String origin(DataComponentPatch components){var value=components.get(ORIGIN.get());return value==null || value.isEmpty()?"":value.get();}
    @javax.annotation.Nullable
    public static net.minecraft.world.level.material.Fluid sourceFluid(FluidStack stack) {
        String token=stack.get(ORIGIN.get());
        if(token==null || token.indexOf('@')<=0)return null;
        var key=ResourceLocation.tryParse(token.substring(0,token.indexOf('@')));
        return key!=null && net.minecraft.core.registries.BuiltInRegistries.FLUID.containsKey(key)?net.minecraft.core.registries.BuiltInRegistries.FLUID.get(key):null;
    }
    public static DataComponentPatch heated(FluidTransitionRegistry.FluidTransition transition) {
        return DataComponentPatch.builder().set(ORIGIN.get(), fingerprint(transition)).build();
    }
    // Modified 2026-11-02: version process signatures without changing turbine properties.
    private static String legacyFingerprint(FluidTransitionRegistry.FluidTransition t) {
        return net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(t.liquids.getFirst())+"@"+
                Integer.toHexString(java.util.Objects.hash(t.latentHeat,t.boilingPoint,t.liquidRFMKT,t.gasRFMKT,t.recovery));
    }
    public static String fingerprint(FluidTransitionRegistry.FluidTransition t) {
        return legacyFingerprint(t)+":thermal"+biggerreactorsrework.thermal.ThermalModel.VERSION+":"+t.steamPerBatch;
    }
    public static boolean matchesStoredProfile(String stored,FluidTransitionRegistry.FluidTransition t) {
        if(fingerprint(t).equals(stored))return true;
        String cold=net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(t.liquids.getFirst()).toString();
        String hot=net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(t.gases.getFirst()).toString();
        String expected=switch(cold) {
            case "minecraft:water" -> "biggerreactorsrework:steam@"+Integer.toHexString(java.util.Objects.hash(4d,373.15,.6,.025,.6));
            case "biggerreactorsrework:liquid_obsidian" -> "biggerreactorsrework:heated_coolant@"+Integer.toHexString(java.util.Objects.hash(36d,773.15,2.2,1.8,.71765));
            case "ad_astra:cryo_fuel" -> "biggerreactorsrework:heated_coolant@"+Integer.toHexString(java.util.Objects.hash(30d,773.15,2.2,1.8,.59));
            case "mekanism:sodium" -> "mekanism:superheated_sodium@"+Integer.toHexString(java.util.Objects.hash(40d,1273.15,4d,4d,.95));
            default -> "";
        };
        int separator=expected.indexOf('@');
        if(separator>0 && hot.equals(expected.substring(0,separator)) && stored.equals(cold+expected.substring(separator)))return true;
        return cold.equals("mekanism:sodium") && hot.equals("mekanism:superheated_sodium") && stored.equals(cold+"@"+Integer.toHexString(java.util.Objects.hash(40d,473.15,4d,4d,.95)));
    }
    public static FluidTransitionRegistry.FluidTransition resolve(DataComponentPatch patch) {
        var origin = patch.get(ORIGIN.get());
        if (origin == null || origin.isEmpty() || origin.get().isEmpty()) return null;
        String id = origin.get();
        int split=id.indexOf('@');
        if(split<=0) return null;
        var key=ResourceLocation.tryParse(id.substring(0,split));
        if(key==null || !net.minecraft.core.registries.BuiltInRegistries.FLUID.containsKey(key)) return null;
        var t=FluidTransitionRegistry.liquidTransition(net.minecraft.core.registries.BuiltInRegistries.FLUID.get(key));
        return t!=null && t.genericHot() && matchesStoredProfile(id,t) ? t : null;
    }
    public static Component name(FluidStack stack) {
        var t=resolve(stack.getComponentsPatch());
        if(t==null){
            String origin=stack.get(ORIGIN.get());
            if(origin!=null && origin.contains("@")) {
                var key=ResourceLocation.tryParse(origin.substring(0,origin.indexOf('@')));
                if(key!=null)t=CoolantSync.find(key);
            }
        }
        return t==null ? Component.translatable("fluid.biggerreactorsrework.heated_coolant") :
                Component.translatable("fluid.biggerreactorsrework.heated_coolant.named",new FluidStack(t.liquids.getFirst(),1).getHoverName());
    }
}
