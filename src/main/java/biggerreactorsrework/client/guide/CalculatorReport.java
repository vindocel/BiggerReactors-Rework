package biggerreactorsrework.client.guide;

import biggerreactorsrework.calculator.CalculationResult;
import biggerreactorsrework.calculator.CalculationStatus;
import biggerreactorsrework.calculator.Route;
import biggerreactorsrework.calculator.design.MachineLayout;
import biggerreactorsrework.calculator.design.PlantLayout;
import biggerreactorsrework.client.calculator.CalculatorClient;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static biggerreactorsrework.calculator.CalculationResult.*;

/** Text of a stored result, built only from the snapshot: nothing is recomputed when it is shown. */
final class CalculatorReport {
    record Line(Component text, int color, boolean heading) { }

    static final int WARNING = 0xFF8A4B12;
    static final int ERROR = 0xFF9A2B1F;

    private CalculatorReport() { }

    static String key(String suffix) {
        return "guide.biggerreactorsrework.calculator." + suffix;
    }

    static Component t(String suffix, Object... args) {
        return Component.translatable(key(suffix), args);
    }

    static Locale locale() {
        String code = Minecraft.getInstance().getLanguageManager().getSelected();
        return Locale.forLanguageTag(code.replace('_', '-'));
    }

    static String number(double value, int decimals) {
        var format = NumberFormat.getNumberInstance(locale());
        format.setMaximumFractionDigits(decimals);
        format.setMinimumFractionDigits(decimals);
        return format.format(value);
    }

    static String whole(double value) {
        return number(Math.round(value), 0);
    }

    static String date(long millis) {
        return java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm", locale())
                .format(java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()));
    }

    static Component blockName(String id) {
        var key = ResourceLocation.tryParse(id);
        if (key != null && BuiltInRegistries.BLOCK.containsKey(key)) return BuiltInRegistries.BLOCK.get(key).getName();
        return Component.literal(id);
    }

    static Component fluidName(String id) {
        var key = ResourceLocation.tryParse(id);
        if (key != null && BuiltInRegistries.FLUID.containsKey(key)) return BuiltInRegistries.FLUID.get(key).getFluidType().getDescription();
        return Component.literal(id);
    }

    static Component itemName(String id) {
        var key = ResourceLocation.tryParse(id);
        if (key != null && BuiltInRegistries.ITEM.containsKey(key)) return BuiltInRegistries.ITEM.get(key).getDescription();
        return Component.literal(id);
    }

    static Component material(CalculationResult r) {
        return r.request().liquid() ? fluidName(r.request().material()) : blockName(r.request().material());
    }

    static Component status(CalculationStatus status) {
        return t("status." + status.name().toLowerCase(Locale.ROOT));
    }

    static Component machineName(MachineLayout.Kind kind) {
        return t("machine." + kind.name().toLowerCase(Locale.ROOT));
    }

    static List<Line> lines(CalculationResult r, CalculatorSession.View view) {
        var out = new ArrayList<Line>();
        switch (view) {
            case SUMMARY -> summary(r, out);
            case REACTOR -> reactor(r, out);
            case EXCHANGER -> exchanger(r, out);
            case TURBINE -> turbine(r, out);
            case CONNECTIONS -> connections(r, out);
        }
        return out;
    }

    private static void body(List<Line> out, Component text) {
        out.add(new Line(text, GuideElements.BODY, false));
    }

    private static void heading(List<Line> out, Component text) {
        out.add(new Line(text, GuideElements.HEADING, true));
    }

    private static void muted(List<Line> out, Component text) {
        out.add(new Line(text, GuideElements.MUTED, false));
    }

    static void conditions(List<Line> out, Route route) {
        out.add(new Line(t("conditions.title"), WARNING, true));
        out.add(new Line(t(route == Route.SOLID ? "conditions.text_solid" : "conditions.text"), WARNING, false));
    }

    private static void summary(CalculationResult r, List<Line> out) {
        heading(out, status(r.status()));
        body(out, t("summary.target", whole(r.request().target())));
        body(out, t("summary.predicted", whole(r.value(POWER))));
        if (r.status() == CalculationStatus.BEST_BELOW_TARGET)
            out.add(new Line(t("summary.deficit", whole(r.request().target() - r.value(POWER))), ERROR, false));
        if (r.value(SEARCH_LIMITED) > 0) out.add(new Line(t("search.limited"), WARNING, false));
        muted(out, t("summary.range", whole(r.value(POWER_MIN)), whole(r.value(POWER_MAX))));
        if (r.has(POWER_ALT)) muted(out, t("summary.order", whole(r.value(POWER_ALT))));
        body(out, t("summary.fuel", number(r.value(FUEL), 3)));
        body(out, t("summary.efficiency", whole(r.value(EFFICIENCY))));
        body(out, route(r));
        body(out, t("summary.fuel_choice", itemName(r.request().fuel())));
        conditions(out, r.route());
        muted(out, t("conditions.premises"));
        muted(out, t("conditions.validation", whole(r.value(TICKS))));
        if (!r.missingBlocks().isEmpty()) out.add(new Line(t("rules.missing", String.join(", ", r.missingBlocks())), ERROR, false));
        var catalog = CalculatorClient.catalog();
        if (catalog != null && !catalog.fingerprint().equals(r.fingerprint())) out.add(new Line(t("rules.changed"), ERROR, false));
        heading(out, t("steps.title"));
        steps(r, out);
        heading(out, t("search.title"));
        muted(out, t("search.coverage", whole(r.value(EVALUATED)), number(r.value(ELAPSED_MS) / 1000, 1)));
    }

    static Component route(CalculationResult r) {
        return switch (r.route()) {
            case SOLID -> t("summary.route.solid", blockName(r.request().material()));
            case DIRECT -> t("summary.route.direct", fluidName(r.request().material()), blockName(r.request().coil()));
            case ADVANCED -> t("summary.route.advanced", fluidName(r.request().material()), blockName(r.request().coil()));
        };
    }

    private static void steps(CalculationResult r, List<Line> out) {
        String insertion = whole(r.value(INSERTION));
        if (r.route() == Route.SOLID) {
            body(out, t("steps.build"));
            body(out, t("steps.rods", insertion));
            body(out, t("steps.fuel"));
            body(out, t("steps.solid_start"));
            return;
        }
        body(out, t("steps.build_plant"));
        body(out, t("steps.ports"));
        if (r.route() == Route.DIRECT) {
            body(out, t("steps.charge_direct", whole(r.value(PRIMARY_CHARGE)), whole(r.value(REACTOR_CAPACITY))));
        } else {
            body(out, t("steps.charge_primary", whole(r.value(PRIMARY_CHARGE)), fluidName(r.request().material()), whole(r.value(REACTOR_CAPACITY))));
            body(out, t("steps.charge_water", whole(r.value(WATER_CHARGE)), whole(r.value(EXCHANGER_CAPACITY))));
        }
        body(out, t("steps.fuel"));
        body(out, t("steps.rods", insertion));
        body(out, t("steps.turbine", whole(r.value(TURBINE_FLOW))));
        body(out, t("steps.liquid_start"));
    }

    @Nullable
    static PlantLayout.Placed placed(CalculationResult r, MachineLayout.Kind kind) {
        if (r.plant() == null) return null;
        return r.plant().machines().stream().filter(p -> p.machine().kind() == kind).findFirst().orElse(null);
    }

    private static Component size(MachineLayout m) {
        return t("size", m.width(), m.height(), m.depth());
    }

    private static void reactor(CalculationResult r, List<Line> out) {
        var placed = placed(r, MachineLayout.Kind.REACTOR);
        if (placed == null) return;
        heading(out, machineName(MachineLayout.Kind.REACTOR));
        body(out, size(placed.machine()));
        body(out, t("reactor.columns", whole(r.value(FUEL_COLUMNS)), whole(r.value(FUEL_RODS))));
        if (r.route() == Route.SOLID) body(out, t("reactor.moderator", whole(r.value(MODERATORS)), blockName(r.request().material())));
        else body(out, t("reactor.manifolds", whole(r.value(MANIFOLDS)), whole(r.value(CONTACTS))));
        body(out, t("reactor.rods", whole(r.value(INSERTION))));
        body(out, t("reactor.temperature", whole(r.value(FUEL_TEMPERATURE))));
        if (r.route() == Route.SOLID) {
            body(out, t("reactor.ports_solid"));
            return;
        }
        body(out, t("reactor.capacity", whole(r.value(REACTOR_CAPACITY))));
        body(out, t("reactor.flow", whole(r.value(HOT_FLOW)), whole(100 * r.value(REACTOR_LIMIT))));
        body(out, t("reactor.stocks", whole(r.value(REACTOR_COLD)), whole(r.value(REACTOR_HOT))));
        body(out, t("reactor.ports_liquid"));
        for (var port : placed.machine().ports()) muted(out, portLine(port, placed.offset()));
    }

    private static void exchanger(CalculationResult r, List<Line> out) {
        var placed = placed(r, MachineLayout.Kind.EXCHANGER);
        if (placed == null) {
            muted(out, t("exchanger.none"));
            return;
        }
        heading(out, machineName(MachineLayout.Kind.EXCHANGER));
        body(out, size(placed.machine()));
        body(out, t("exchanger.channels", blockName("biggerreactorsrework:heat_exchanger_condenser_channel"), whole(r.value(CONDENSER)),
                blockName("biggerreactorsrework:heat_exchanger_evaporator_channel"), whole(r.value(EVAPORATOR))));
        body(out, t("exchanger.contacts", whole(r.value(EXCHANGER_CONTACTS))));
        body(out, t("exchanger.capacity", whole(r.value(EXCHANGER_CAPACITY))));
        body(out, t("exchanger.steam", whole(r.value(STEAM)), whole(100 * r.value(EXCHANGER_LIMIT))));
        body(out, t("exchanger.stocks", whole(r.value(EXCHANGER_HOT)), whole(r.value(EXCHANGER_COLD)), whole(r.value(EXCHANGER_WATER)), whole(r.value(EXCHANGER_STEAM))));
        body(out, t("exchanger.ports"));
        for (var port : placed.machine().ports()) muted(out, portLine(port, placed.offset()));
    }

    private static void turbine(CalculationResult r, List<Line> out) {
        var placed = placed(r, MachineLayout.Kind.TURBINE);
        if (placed == null) {
            muted(out, t("turbine.none"));
            return;
        }
        var m = placed.machine();
        heading(out, machineName(MachineLayout.Kind.TURBINE));
        body(out, size(m));
        int arm = (m.width() - 3) / 2;
        body(out, t("turbine.rotor", whole(r.value(SHAFT)), whole(r.value(BLADES)), arm));
        body(out, t("turbine.coils", whole(r.value(COILS)), blockName(r.request().coil())));
        body(out, t("turbine.flow", whole(r.value(TURBINE_FLOW)), whole(r.value(FLOW_LIMIT))));
        body(out, t("turbine.rpm", whole(r.value(RPM))));
        body(out, t("turbine.settings"));
        body(out, t("turbine.capacity", whole(r.value(TURBINE_CAPACITY))));
        body(out, t("turbine.stocks", whole(r.value(TURBINE_STEAM)), whole(r.value(TURBINE_WATER))));
        for (var port : m.ports()) muted(out, portLine(port, placed.offset()));
    }

    private static void connections(CalculationResult r, List<Line> out) {
        conditions(out, r.route());
        if (r.plant() == null) return;
        muted(out, t("connections.origin"));
        if (r.route() == Route.SOLID) {
            muted(out, t("connections.solid"));
            return;
        }
        heading(out, t("connections.pairs"));
        for (var c : r.plant().connections()) {
            var from = r.plant().portPosition(c.from(), c.fromRole());
            var to = r.plant().portPosition(c.to(), c.toRole());
            body(out, t("connections.pair", role(c.fromRole()), position(from), role(c.toRole()), position(to)));
        }
        heading(out, t("connections.charges"));
        if (r.route() == Route.DIRECT) {
            body(out, t("connections.charge_direct", whole(r.value(PRIMARY_CHARGE))));
        } else {
            body(out, t("connections.charge_primary", whole(r.value(PRIMARY_CHARGE)), fluidName(r.request().material())));
            body(out, t("connections.charge_water", whole(r.value(WATER_CHARGE))));
        }
        muted(out, t("connections.no_cost"));
        muted(out, t("connections.conservation", whole(Math.max(r.value(PRIMARY_ERROR), r.value(WATER_ERROR)))));
    }

    static Component role(MachineLayout.Role role) {
        return t("role." + role.name().toLowerCase(Locale.ROOT));
    }

    static Component position(BlockPos pos) {
        return Component.literal("(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")");
    }

    static Component portLine(MachineLayout.Port port, BlockPos offset) {
        return t("port", role(port.role()), position(port.pos().offset(offset)),
                t(port.role().inlet() ? "port.inlet" : "port.outlet"));
    }
}
