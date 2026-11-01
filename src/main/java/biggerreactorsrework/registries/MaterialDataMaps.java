package biggerreactorsrework.registries;

// Modified 2026-10-26: preserve configurable recovery and generic-hot origin semantics.

import biggerreactorsrework.BiggerReactors;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import java.util.List;
import java.util.Map;

/** Native data-pack overlays. Final resolved snapshots retain the existing sync authority. */
public final class MaterialDataMaps {
    private static final Codec<Double> NONNEGATIVE=Codec.DOUBLE.validate(n->Double.isFinite(n)&&n>=0?DataResult.success(n):DataResult.error(()->"Expected a finite nonnegative number"));
    private static final Codec<Double> POSITIVE=NONNEGATIVE.validate(n->n>0?DataResult.success(n):DataResult.error(()->"Expected a positive number"));
    private static final Codec<Double> UNIT=NONNEGATIVE.validate(n->n<=1?DataResult.success(n):DataResult.error(()->"Expected a number in [0,1]"));
    public record Moderator(double absorption,double efficiency,double moderation,double conductivity) {
        public static final Codec<Moderator> CODEC=RecordCodecBuilder.create(i->i.group(
            UNIT.fieldOf("absorption").forGetter(Moderator::absorption),UNIT.fieldOf("efficiency").forGetter(Moderator::efficiency),
            NONNEGATIVE.validate(n->n>=1?DataResult.success(n):DataResult.error(()->"Moderation must be >= 1")).fieldOf("moderation").forGetter(Moderator::moderation),
            NONNEGATIVE.fieldOf("conductivity").forGetter(Moderator::conductivity)).apply(i,Moderator::new));
        ReactorModeratorRegistry.ModeratorProperties properties(){return new ReactorModeratorRegistry.ModeratorProperties(absorption,efficiency,moderation,conductivity);}
    }
    public record Coil(double efficiency,double extractionRate,double bonus) {
        public static final Codec<Coil> CODEC=RecordCodecBuilder.create(i->i.group(
            POSITIVE.fieldOf("efficiency").forGetter(Coil::efficiency),POSITIVE.fieldOf("extractionRate").forGetter(Coil::extractionRate),
            POSITIVE.validate(n->n<2?DataResult.success(n):DataResult.error(()->"Bonus must be < 2")).fieldOf("bonus").forGetter(Coil::bonus)).apply(i,Coil::new));
    }
    public record Transition(ExtraCodecs.TagOrElementLocation gas,double latentHeat,double boilingPoint,double liquidThermalConductivity,double gasThermalConductivity,double turbineMultiplier,double recovery) {
        public static final Codec<Transition> CODEC=RecordCodecBuilder.create(i->i.group(
            ExtraCodecs.TAG_OR_ELEMENT_ID.fieldOf("gas").forGetter(Transition::gas),POSITIVE.fieldOf("latentHeat").forGetter(Transition::latentHeat),
            POSITIVE.fieldOf("boilingPoint").forGetter(Transition::boilingPoint),NONNEGATIVE.fieldOf("liquidThermalConductivity").forGetter(Transition::liquidThermalConductivity),
            NONNEGATIVE.fieldOf("gasThermalConductivity").forGetter(Transition::gasThermalConductivity),NONNEGATIVE.fieldOf("turbineMultiplier").forGetter(Transition::turbineMultiplier),
            POSITIVE.validate(n->n<=1?DataResult.success(n):DataResult.error(()->"Recovery must be in (0,1]")).optionalFieldOf("recovery",1d).forGetter(Transition::recovery)).apply(i,Transition::new));
    }
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath(BiggerReactors.modid,path);}
    public static final DataMapType<Block,Moderator> MODERATOR=DataMapType.builder(id("reactor_moderator"),Registries.BLOCK,Moderator.CODEC).build();
    // Modified 2026-11-01: per-item generic fuel yields, zero explicitly disables a matching item/tag.
    public record ReactorFuel(long amount) {
        public static final Codec<ReactorFuel> CODEC = Codec.DOUBLE.validate(n -> Double.isFinite(n) && n == Math.rint(n) && n >= 0 && n <= Long.MAX_VALUE / Integer.MAX_VALUE
                        ? DataResult.success(n) : DataResult.error(() -> "Fuel yield must be an integer within the safe range"))
                .xmap(Double::longValue, Long::doubleValue)
                .fieldOf("amount").xmap(ReactorFuel::new, ReactorFuel::amount).codec();
    }
    public static final DataMapType<net.minecraft.world.item.Item, ReactorFuel> REACTOR_FUEL =
            DataMapType.builder(id("reactor_fuel"), Registries.ITEM, ReactorFuel.CODEC).synced(ReactorFuel.CODEC, false).build();
    public static final DataMapType<Fluid,Moderator> FLUID_MODERATOR=DataMapType.builder(id("reactor_fluid_moderator"),Registries.FLUID,Moderator.CODEC).build();
    public static final DataMapType<Block,Coil> COIL=DataMapType.builder(id("turbine_coil"),Registries.BLOCK,Coil.CODEC).build();
    public static final DataMapType<Fluid,Transition> TRANSITION=DataMapType.builder(id("fluid_transition"),Registries.FLUID,Transition.CODEC).build();
    public static void register(RegisterDataMapTypesEvent event){event.register(REACTOR_FUEL);event.register(MODERATOR);event.register(FLUID_MODERATOR);event.register(COIL);event.register(TRANSITION);}
    static void moderators(Map<Block,ReactorModeratorRegistry.ModeratorProperties> target){
        BuiltInRegistries.FLUID.holders().forEach(holder->{var data=holder.getData(FLUID_MODERATOR);if(data!=null)target.put(holder.value().defaultFluidState().createLegacyBlock().getBlock(),data.properties());});
        BuiltInRegistries.BLOCK.holders().forEach(holder->{var data=holder.getData(MODERATOR);if(data!=null)target.put(holder.value(),data.properties());});
    }
    static void coils(Map<Block,TurbineCoilRegistry.CoilData> target){
        BuiltInRegistries.BLOCK.holders().forEach(holder->{var d=holder.getData(COIL);if(d!=null)target.put(holder.value(),new TurbineCoilRegistry.CoilData(d.efficiency,d.bonus,d.extractionRate));});
    }
    static void transitions(Map<Fluid,FluidTransitionRegistry.FluidTransition> liquids,Map<Fluid,FluidTransitionRegistry.FluidTransition> gases){
        BuiltInRegistries.FLUID.holders().sorted(java.util.Comparator.comparing(h->h.key().location())).forEach(holder->{
            var d=holder.getData(TRANSITION);if(d==null)return;
            var outputs=new java.util.ArrayList<Fluid>();
            if(d.gas.tag()) BuiltInRegistries.FLUID.getTag(net.minecraft.tags.TagKey.create(Registries.FLUID,d.gas.id())).ifPresent(tag->tag.forEach(h->{if(h.value().isSource(h.value().defaultFluidState()))outputs.add(h.value());}));
            else if(BuiltInRegistries.FLUID.containsKey(d.gas.id())) outputs.add(BuiltInRegistries.FLUID.get(d.gas.id()));
            if(outputs.isEmpty()){BiggerReactors.LOGGER.error("Ignoring transition with missing gas: {}",d.gas);return;}
            outputs.sort(java.util.Comparator.comparing(BuiltInRegistries.FLUID::getKey));
            var transition=new FluidTransitionRegistry.FluidTransition(List.of(holder.value()),List.copyOf(outputs),d.latentHeat,d.boilingPoint,d.liquidThermalConductivity,d.gasThermalConductivity,d.turbineMultiplier);
            transition.recovery=d.recovery;
            var previous=liquids.put(holder.value(),transition);
            if(previous!=null)previous.gases.forEach(gas->{if(gases.get(gas)==previous)gases.remove(gas);});
            outputs.stream().filter(gas->gas!=biggerreactorsrework.fluids.HeatedCoolant.INSTANCE).forEach(gas->gases.put(gas,transition));
        });
    }
    private MaterialDataMaps(){}
}
