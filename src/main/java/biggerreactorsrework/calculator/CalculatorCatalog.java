package biggerreactorsrework.calculator;

import biggerreactorsrework.Config;
import biggerreactorsrework.registries.CoolantComponents;
import biggerreactorsrework.registries.FluidTransitionRegistry;
import biggerreactorsrework.registries.ReactorModeratorRegistry;
import biggerreactorsrework.registries.TurbineCoilRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import javax.annotation.Nullable;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * The materials the server accepts right now, captured on the server thread: solid moderators that can be placed,
 * reactor coolants with their route, and turbine coils. The fingerprint covers every property and configuration
 * value the calculation reads, so a result can tell when its rules changed.
 */
public record CalculatorCatalog(List<String> fuels, List<String> moderators, List<Coolant> coolants, List<String> coils, String fingerprint,
                                Map<String, ReactorModeratorRegistry.ModeratorProperties> moderatorProperties,
                                Map<String, FluidTransitionRegistry.FluidTransition> coolantProperties,
                                Map<String, TurbineCoilRegistry.CoilData> coilProperties,
                                @Nullable FluidTransitionRegistry.FluidTransition water) {
    /** Bumped whenever the search or the virtual plant changes in a way that changes results. */
    public static final int ENGINE_VERSION = 3;

    public record Coolant(String id, Route route) { }

    public static CalculatorCatalog capture() {
        var moderatorProperties = new TreeMap<String, ReactorModeratorRegistry.ModeratorProperties>();
        ReactorModeratorRegistry.snapshot().forEach((block, properties) -> {
            if (block.defaultBlockState().isAir() || block.asItem() == Items.AIR) return;
            moderatorProperties.put(key(block), properties);
        });
        var water = FluidTransitionRegistry.liquidTransition(Fluids.WATER);
        var coolants = new ArrayList<Coolant>();
        var coolantProperties = new TreeMap<String, FluidTransitionRegistry.FluidTransition>();
        for (var transition : FluidTransitionRegistry.transitions()) {
            for (Fluid liquid : transition.liquids) {
                Route route = route(transition, water);
                if (route == null) continue;
                String id = BuiltInRegistries.FLUID.getKey(liquid).toString();
                coolantProperties.put(id, transition);
                coolants.add(new Coolant(id, route));
            }
        }
        coolants.sort(Comparator.comparing(Coolant::id));
        var coilProperties = new TreeMap<String, TurbineCoilRegistry.CoilData>();
        TurbineCoilRegistry.snapshot().forEach((block, data) -> {
            if (block.asItem() != Items.AIR) coilProperties.put(key(block), data);
        });
        String fingerprint = fingerprint(moderatorProperties, coolantProperties, coilProperties, water);
        return new CalculatorCatalog(captureFuels(), List.copyOf(moderatorProperties.keySet()), List.copyOf(coolants), List.copyOf(coilProperties.keySet()),
                fingerprint, Map.copyOf(moderatorProperties), Map.copyOf(coolantProperties), Map.copyOf(coilProperties), water);
    }

    /**
     * The mod's own fuels, uranium and blutonium (author's decision of 29/10/2026). Both become the same fuel inside
     * the reactor, so the choice is recorded with the result but does not change the simulation.
     */
    private static List<String> captureFuels() {
        return List.of(BuiltInRegistries.ITEM.getKey(biggerreactorsrework.items.ingots.UraniumIngot.INSTANCE).toString(),
                BuiltInRegistries.ITEM.getKey(biggerreactorsrework.items.ingots.BlutoniumIngot.INSTANCE).toString());
    }

    /** Direct when the hot product drives the turbine, through the exchanger when water can take its heat. */
    @Nullable
    private static Route route(FluidTransitionRegistry.FluidTransition transition, @Nullable FluidTransitionRegistry.FluidTransition water) {
        if (transition.turbineMultiplier > 0) return Route.DIRECT;
        if (water != null && water.turbineMultiplier > 0 && transition.boilingPoint > water.boilingPoint) return Route.ADVANCED;
        return null;
    }

    private static String key(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    @Nullable
    public Route coolantRoute(String id) {
        return coolants.stream().filter(c -> c.id().equals(id)).map(Coolant::route).findFirst().orElse(null);
    }

    private static String fingerprint(Map<String, ReactorModeratorRegistry.ModeratorProperties> moderators,
                                      Map<String, FluidTransitionRegistry.FluidTransition> coolants,
                                      Map<String, TurbineCoilRegistry.CoilData> coils, @Nullable FluidTransitionRegistry.FluidTransition water) {
        var text = new StringBuilder("engine=").append(ENGINE_VERSION).append('\n');
        moderators.forEach((id, p) -> text.append("m ").append(id).append(' ').append(p.absorption).append(' ').append(p.heatEfficiency)
                .append(' ').append(p.moderation).append(' ').append(p.heatConductivity).append('\n'));
        coolants.forEach((id, t) -> text.append("c ").append(id).append(' ').append(CoolantComponents.fingerprint(t)).append(' ').append(t.recovery).append('\n'));
        if (water != null) text.append("w ").append(CoolantComponents.fingerprint(water)).append(' ').append(water.recovery).append('\n');
        coils.forEach((id, c) -> text.append("k ").append(id).append(' ').append(c.efficiency).append(' ').append(c.bonus).append(' ').append(c.extractionRate).append('\n'));
        for (Object section : new Object[]{Config.CONFIG.Reactor, Config.CONFIG.Reactor.ModeSpecific, Config.CONFIG.Turbine, Config.CONFIG.HeatExchanger})
            for (var field : section.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || !(field.getType().isPrimitive())) continue;
                try {
                    field.setAccessible(true);
                    text.append("cfg ").append(section.getClass().getSimpleName()).append('.').append(field.getName()).append('=').append(field.get(section)).append('\n');
                } catch (ReflectiveOperationException ignored) {
                }
            }
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(text.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 16);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** What the client needs to fill the form: ids, routes and the fingerprint. */
    public CompoundTag toClientTag() {
        var tag = new CompoundTag();
        tag.put("fuels", strings(fuels));
        tag.put("moderators", strings(moderators));
        var list = new ListTag();
        for (var c : coolants) {
            var entry = new CompoundTag();
            entry.putString("id", c.id());
            entry.putString("route", c.route().name());
            list.add(entry);
        }
        tag.put("coolants", list);
        tag.put("coils", strings(coils));
        tag.putString("fingerprint", fingerprint);
        return tag;
    }

    private static ListTag strings(List<String> values) {
        var list = new ListTag();
        values.forEach(v -> list.add(StringTag.valueOf(v)));
        return list;
    }

    /** Client-side view of {@link #toClientTag()}; property maps stay empty on the client. */
    public static CalculatorCatalog fromClientTag(CompoundTag tag) {
        var fuels = new ArrayList<String>();
        tag.getList("fuels", Tag.TAG_STRING).forEach(t -> fuels.add(t.getAsString()));
        var moderators = new ArrayList<String>();
        tag.getList("moderators", Tag.TAG_STRING).forEach(t -> moderators.add(t.getAsString()));
        var coolants = new ArrayList<Coolant>();
        for (var t : tag.getList("coolants", Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag) t;
            try {
                coolants.add(new Coolant(entry.getString("id"), Route.valueOf(entry.getString("route"))));
            } catch (IllegalArgumentException ignored) {
            }
        }
        var coils = new ArrayList<String>();
        tag.getList("coils", Tag.TAG_STRING).forEach(t -> coils.add(t.getAsString()));
        return new CalculatorCatalog(List.copyOf(fuels), List.copyOf(moderators), List.copyOf(coolants), List.copyOf(coils), tag.getString("fingerprint"),
                Map.of(), Map.of(), Map.of(), null);
    }
}
