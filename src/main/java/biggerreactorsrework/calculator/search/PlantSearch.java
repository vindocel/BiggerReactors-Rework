package biggerreactorsrework.calculator.search;

import biggerreactorsrework.Config;
import biggerreactorsrework.calculator.CalculationStatus;
import biggerreactorsrework.calculator.CalculatorCatalog;
import biggerreactorsrework.calculator.CalculatorRequest;
import biggerreactorsrework.calculator.Route;
import biggerreactorsrework.calculator.design.*;
import biggerreactorsrework.calculator.simulation.Bench;
import biggerreactorsrework.calculator.simulation.PlantRun;
import biggerreactorsrework.calculator.simulation.PlantSpec;
import biggerreactorsrework.registries.FluidTransitionRegistry;
import biggerreactorsrework.registries.ReactorModeratorRegistry;
import biggerreactorsrework.registries.TurbineCoilRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/**
 * Finds a plant for a target. Candidates come from fixed families (square reactor bases with a few rod patterns and
 * heights, vertical turbines, two-serpentine exchangers); single machines with ideal supply only pre-select them.
 * Every candidate is then judged as a whole plant with finite charges and real transfers. The smallest plant that
 * meets the target is kept (author's decision of 29/10/2026), with the rod insertion that burns the least fuel, and
 * it is validated again with longer, stricter runs in two controller orders. The lower of the two powers is reported.
 */
public final class PlantSearch {
    public enum Phase { PREPARING, SEARCHING, VALIDATING }

    public interface Progress {
        void update(Phase phase, int done, int total);
    }

    public record Outcome(CalculationStatus status, @Nullable PlantSpec spec, @Nullable PlantLayout plant, @Nullable PlantRun.Result validation,
                          @Nullable PlantRun.Result alternate, int evaluated, boolean searchLimited, long elapsedMillis) { }

    /** Screening power must exceed the target by this much, leaving room for the stricter validation. */
    private static final double SCREEN_MARGIN = 1.01;
    private static final double BENCH_MARGIN = 1.05;

    private final CalculatorRequest request;
    private final Route route;
    private final CalculatorCatalog catalog;
    private final Progress progress;
    private final BooleanSupplier cancelled;
    private final long start = System.currentTimeMillis();
    private int evaluated;
    private int plantEvaluations;
    /** Turbine benches are cheap and bounded by their own search; they count in the report but not in the budget. */
    private int turbineBenches;
    private boolean limited;
    private final java.util.Map<String, List<ReactorDesign>> geometryCache = new java.util.HashMap<>();
    private final java.util.Map<String, TurbineSearch.Choice> turbineCache = new java.util.HashMap<>();

    private PlantSearch(CalculatorRequest request, Route route, CalculatorCatalog catalog, Progress progress, BooleanSupplier cancelled) {
        this.request = request;
        this.route = route;
        this.catalog = catalog;
        this.progress = progress;
        this.cancelled = cancelled;
    }

    public static Outcome run(CalculatorRequest request, Route route, CalculatorCatalog catalog, Progress progress, BooleanSupplier cancelled) {
        var search = new PlantSearch(request, route, catalog, progress, cancelled);
        try {
            return switch (route) {
                case SOLID -> search.solid();
                case DIRECT -> search.liquid(false);
                case ADVANCED -> search.liquid(true);
            };
        } catch (CancellationException e) {
            return search.outcome(CalculationStatus.CANCELLED, null, null, null);
        }
    }

    private Outcome outcome(CalculationStatus status, @Nullable Candidate best, @Nullable PlantRun.Result validation, @Nullable PlantRun.Result alternate) {
        return new Outcome(status, best == null ? null : best.spec, best == null ? null : best.plant, validation, alternate, evaluated + turbineBenches,
                limited, System.currentTimeMillis() - start);
    }

    private boolean budgetLeft() {
        boolean available = plantEvaluations < SearchDomain.MAX_EVALUATIONS && System.currentTimeMillis() - start < SearchDomain.MAX_MILLIS;
        if (!available) limited = true;
        return available;
    }

    /** Preserve time for coupled runs and target recovery instead of spending the whole job on ideal benches. */
    private boolean preparationLeft() {
        boolean available = budgetLeft() && System.currentTimeMillis() - start < SearchDomain.MAX_MILLIS / 2;
        if (!available) limited = true;
        return available;
    }

    /** A plant judged by a screening run. */
    private record Candidate(PlantSpec spec, PlantLayout plant, PlantRun.Result result) {
        double volume() {
            return plant.machines().stream().mapToDouble(p -> (double) p.machine().width() * p.machine().height() * p.machine().depth()).sum();
        }
        long surface() {
            return plant.machines().stream().mapToLong(p -> Compactness.surface(p.machine().width(), p.machine().height(), p.machine().depth())).sum();
        }
        double cost() {
            return plant.machines().stream().mapToDouble(p -> Compactness.cost(p.machine().width(), p.machine().height(), p.machine().depth())).sum();
        }
    }

    private PlantRun.Result screen(PlantSpec spec) {
        evaluated++;
        plantEvaluations++;
        return PlantRun.run(spec, false, PlantRun.Settings.SCREEN, cancelled);
    }

    private boolean meets(PlantRun.Result result) {
        return result.converged() && result.power() >= request.target() * SCREEN_MARGIN;
    }

    /** Balanced compactness first, then literal volume and fuel. No aspect ratio is forbidden. */
    private static final Comparator<Candidate> SMALLEST_FIRST = Comparator.comparingDouble(Candidate::cost).thenComparingDouble(Candidate::volume)
            .thenComparingLong(Candidate::surface).thenComparingDouble(c -> c.result.fuel());
    private static final int COARSE_STEP = 10, MAX_INSERTION = 99, REFINED = 4, SIZES_PER_FAMILY = 4;
    /** The turbine is sized slightly above the target so that the whole plant passes the screening margin. */
    private static final double TURBINE_MARGIN = 1.015;

    private boolean preferred(Candidate a, Candidate b) {
        boolean am = meets(a.result), bm = meets(b.result);
        if (am != bm) return am;
        if (!am) return a.result.power() > b.result.power();
        return SMALLEST_FIRST.compare(a, b) < 0;
    }

    @Nullable
    private Candidate pick(List<Candidate> candidates) {
        Candidate best = null;
        for (var c : candidates) if (best == null || preferred(c, best)) best = c;
        return best;
    }

    // ------------------------------------------------------------------ solid

    private Outcome solid() {
        var moderator = catalog.moderatorProperties().get(request.material());
        Block block = block(request.material());
        if (moderator == null || block == null) return outcome(CalculationStatus.INVALID_INPUT, null, null, null);
        progress.update(Phase.PREPARING, 0, 0);
        var specs = new ArrayList<PlantSpec>();
        for (var design : geometries(ReactorDesign.Pattern.values(), request.target(), 1, 1, block, moderator, null))
            specs.add(new PlantSpec(design, moderator, null, null, null, null, null, 0, 0));
        return validate(refineBest(smallestFirst(specs), specs.size()));
    }

    private static PlantLayout assemble(PlantSpec spec) {
        if (spec.solid()) return PlantAssembler.solid(spec.reactor());
        return spec.exchanger() != null ? PlantAssembler.advanced(spec.reactor(), spec.exchanger(), spec.turbine()) : PlantAssembler.water(spec.reactor(), spec.turbine());
    }

    /**
     * Plants in order of size. Rods fully out give the most output, so a plant that misses the target there is
     * dropped after one run; once a plant meets it, larger ones cannot win and are skipped.
     */
    private List<Candidate> smallestFirst(List<PlantSpec> specs) {
        record Sized(PlantSpec spec, PlantLayout plant, double cost, double volume, long surface) { }
        var sized = new ArrayList<Sized>();
        for (var spec : specs) {
            var plant = assemble(spec);
            if (plant != null) {
                var candidate = new Candidate(spec, plant, null);
                sized.add(new Sized(spec, plant, candidate.cost(), candidate.volume(), candidate.surface()));
            }
        }
        sized.sort(Comparator.comparingDouble(Sized::cost).thenComparingDouble(Sized::volume).thenComparingLong(Sized::surface));
        var plants = new ArrayList<Candidate>();
        double best = Double.MAX_VALUE;
        for (int i = 0; i < sized.size() && budgetLeft(); i++) {
            if (System.currentTimeMillis() - start > SearchDomain.MAX_MILLIS * 3 / 4
                    || plantEvaluations >= SearchDomain.MAX_EVALUATIONS * 2 / 3) { limited = true; break; }
            var entry = sized.get(i);
            if (entry.cost > best) break;
            progress.update(Phase.SEARCHING, i, sized.size() + REFINED);
            var open = screen(entry.spec.withReactor(entry.spec.reactor().withInsertion(0)));
            var first = new Candidate(entry.spec.withReactor(entry.spec.reactor().withInsertion(0)), entry.plant, open);
            if (!meets(open)) {
                if (open.converged()) plants.add(first);
                continue;
            }
            var compact = compactExchanger(shrinkReactor(first));
            var c = coarse(compact.spec, compact.plant, compact);
            plants.add(c);
            best = c.cost();
        }
        return plants;
    }

    /** Refine each interior dimension independently; aspect families only seed the search, never fix the result. */
    private Candidate shrinkReactor(Candidate initial) {
        Candidate best = initial;
        boolean improved = true;
        while (improved && budgetLeft()) {
            improved = false;
            var r = best.spec.reactor();
            var smaller = new ArrayList<PlantSpec>();
            for (int axis = 0; axis < 3; axis++) {
                int x = r.x() - (axis == 0 ? 1 : 0), y = r.y() - (axis == 1 ? 1 : 0), z = r.z() - (axis == 2 ? 1 : 0);
                if (x < 1 || y < 1 || z < 1) continue;
                var reactor = new ReactorDesign(x, y, z, r.pattern(), r.moderator(), 0);
                if (reactor.liquid() && reactor.manifoldColumns() == 0) continue;
                var spec = best.spec.withReactor(reactor);
                smaller.add(spec.solid() ? spec : charges(spec));
            }
            smaller.sort((a, b) -> Compactness.compare(a.reactor().width(), a.reactor().height(), a.reactor().depth(),
                    b.reactor().width(), b.reactor().height(), b.reactor().depth()));
            for (var spec : smaller) {
                if (!budgetLeft()) break;
                var layout = assemble(spec);
                if (layout == null) continue;
                var result = screen(spec);
                if (meets(result)) {
                    best = new Candidate(spec, layout, result);
                    improved = true;
                    break;
                }
            }
        }
        return best;
    }

    /** Fold and shorten the actual coupled heat path, rather than relying on the ideal-supply estimate. */
    private Candidate compactExchanger(Candidate initial) {
        if (initial.spec.exchanger() == null) return initial;
        Candidate best = initial;
        int maxLayers = Math.min(SearchDomain.exchangerLayers(),
                (Math.min(initial.spec.reactor().z(), initial.spec.turbine().side()) + 2) / 3);
        for (int layers = 1; layers <= maxLayers && budgetLeft(); layers++) {
            for (int rows = 2; rows <= SearchDomain.exchangerRows() && budgetLeft(); rows += 2) {
                var shape = new ExchangerDesign(2, rows, layers);
                if (shape.portHeight() > Math.min(initial.spec.reactor().y(), initial.spec.turbine().shaft())) continue;
                var previous = best.spec.exchanger();
                double volume = Compactness.cost(previous.width(), previous.height(), previous.depth());
                int inset = layers == 1 ? 2 : 4;
                int hi = (int) Math.min(SearchDomain.exchangerLength() - (inset - 2),
                        volume / ((long) shape.height() * shape.depth()) - inset);
                if (hi < 2) continue;
                int lo = 2;
                while (lo <= hi && budgetLeft()) {
                    int length = (lo + hi) / 2;
                    var design = new ExchangerDesign(length, rows, layers);
                    var spec = charges(initial.spec.withExchanger(design));
                    var layout = assemble(spec);
                    if (layout == null) break;
                    var result = screen(spec);
                    if (meets(result)) {
                        var candidate = new Candidate(spec, layout, result);
                        if (SMALLEST_FIRST.compare(candidate, best) < 0) best = candidate;
                        hi = length - 1;
                    } else lo = length + 1;
                }
            }
        }
        return best;
    }

    /** Interior height for a base side and aspect, at least {@code min}; -1 when outside the domain. */
    private static int height(int side, double aspect, int min) {
        int height = Math.max(min, (int) Math.round(side * aspect));
        return height > SearchDomain.reactorHeight() ? -1 : height;
    }

    /**
     * Reactor geometries per rod pattern and height ratio: the first sizes, from the smallest whose ideal output
     * covers {@code need} with margin. When nothing covers it, the strongest geometry found is kept.
     */
    private List<ReactorDesign> geometries(ReactorDesign.Pattern[] patterns, double need, int minHeight, int minDepth, @Nullable Block moderator,
                                           @Nullable ReactorModeratorRegistry.ModeratorProperties properties,
                                           @Nullable FluidTransitionRegistry.FluidTransition coolant) {
        String cacheKey = java.util.Arrays.toString(patterns) + "/" + need + "/" + minHeight + "/" + minDepth;
        if (geometryCache.containsKey(cacheKey)) return geometryCache.get(cacheKey);
        var found = new ArrayList<ReactorDesign>();
        ReactorDesign strongest = null;
        double strongestOutput = -1;
        // Square bases and bases one block narrower, so sizes between two squares are tried too.
        for (var pattern : patterns) for (double aspect : SearchDomain.REACTOR_ASPECTS) for (int narrower = 0; narrower <= 1; narrower++) {
            int taken = 0;
            for (int side = Math.max(1, minDepth); side <= SearchDomain.reactorSide(); side++) {
                if (side > SearchDomain.reactorSide() || taken >= SIZES_PER_FAMILY || !preparationLeft()) break;
                int height = height(side, aspect, minHeight);
                if (height < 0) break;
                if (side - narrower < minDepth) continue;
                var design = new ReactorDesign(side, height, side - narrower, pattern, moderator, 0);
                if (design.liquid() && design.manifoldColumns() == 0) continue;
                if (taken > 0) {
                    found.add(design);
                    taken++;
                    continue;
                }
                evaluated++;
                var point = Bench.reactor(design, properties, coolant, 200_000, cancelled);
                // Ideal supply is an upper bound for the plant: without margin here, the plant run decides.
                if (point.output() >= need) {
                    found.add(design);
                    taken++;
                } else if (point.output() > strongestOutput) {
                    strongestOutput = point.output();
                    strongest = design;
                }
            }
        }
        if (found.isEmpty() && strongest != null) found.add(strongest);
        geometryCache.put(cacheKey, found);
        return found;
    }

    /** Lowest fuel meeting the target on a 10 % insertion grid, given the run with the rods fully out. */
    private Candidate coarse(PlantSpec spec, PlantLayout plant, Candidate open) {
        var tried = new ArrayList<Candidate>();
        tried.add(open);
        for (int insertion = COARSE_STEP; insertion <= MAX_INSERTION && budgetLeft(); insertion += COARSE_STEP) {
            var s = spec.withReactor(spec.reactor().withInsertion(insertion));
            var result = screen(s);
            if (result.converged()) tried.add(new Candidate(s, plant, result));
        }
        return pick(tried);
    }

    /** One-percent steps around the coarse insertion; fuel against insertion is not smooth enough to assume more. */
    private Candidate refine(Candidate coarse) {
        var tried = new ArrayList<Candidate>();
        tried.add(coarse);
        int center = (int) Math.round(coarse.spec.reactor().insertion());
        for (int insertion = Math.max(0, center - COARSE_STEP + 1); insertion <= Math.min(MAX_INSERTION, center + COARSE_STEP - 1) && budgetLeft(); insertion++) {
            if (insertion == center) continue;
            var s = coarse.spec.withReactor(coarse.spec.reactor().withInsertion(insertion));
            var result = screen(s);
            if (result.converged()) tried.add(new Candidate(s, coarse.plant, result));
        }
        return pick(tried);
    }

    /** The best coarse plants refined, best first; plants below the target only when none meets it. */
    private List<Candidate> refineBest(List<Candidate> plants, int progressBase) {
        var meeting = new ArrayList<>(plants.stream().filter(c -> meets(c.result)).toList());
        if (meeting.isEmpty()) {
            var below = pick(plants);
            return below == null ? new ArrayList<>() : new ArrayList<>(List.of(below));
        }
        meeting.sort(SMALLEST_FIRST);
        var refined = new ArrayList<Candidate>();
        for (int i = 0; i < Math.min(REFINED, meeting.size()); i++) {
            progress.update(Phase.SEARCHING, progressBase + i, progressBase + REFINED);
            refined.add(refine(meeting.get(i)));
        }
        var best = pick(refined);
        refined.remove(best);
        refined.sort(SMALLEST_FIRST);
        refined.addFirst(best);
        return refined;
    }

    // ------------------------------------------------------------------ liquid

    private Outcome liquid(boolean advanced) {
        var coolant = catalog.coolantProperties().get(request.material());
        var coilData = catalog.coilProperties().get(request.coil());
        Block coil = block(request.coil());
        var water = catalog.water();
        if (coolant == null || coilData == null || coil == null || advanced && water == null) return outcome(CalculationStatus.INVALID_INPUT, null, null, null);
        if (advanced && Config.CONFIG.HeatExchanger.MaxWidth < 4) return outcome(CalculationStatus.SEARCH_LIMIT_REACHED, null, null, null);
        var steam = advanced ? water : coolant;
        progress.update(Phase.PREPARING, 0, 0);
        var first = turbine(coil, coilData, steam, 1, 1);
        if (first == null) return outcome(CalculationStatus.SEARCH_LIMIT_REACHED, null, null, null);
        progress.update(Phase.SEARCHING, 1, 10);

        List<ExchangerDesign> exchangers = List.of();
        double hotPerSteam = 1;
        if (advanced) {
            var choice = exchangers(coolant, water, first);
            exchangers = choice.designs;
            hotPerSteam = choice.hotPerSteam;
            if (exchangers.isEmpty()) return outcome(CalculationStatus.SEARCH_LIMIT_REACHED, null, null, null);
        }
        progress.update(Phase.SEARCHING, 2, 10);

        var plants = new ArrayList<Candidate>();
        var specs = new ArrayList<PlantSpec>();
        for (var exchanger : advanced ? exchangers : java.util.Collections.<ExchangerDesign>singletonList(null)) {
            // Connections constrain the port span, not the exchanger's full height or depth.
            var t = exchanger == null || first.shaft() >= exchanger.portHeight() && first.side() >= exchanger.portDepth()
                    ? first : turbine(coil, coilData, steam, exchanger.portHeight(), exchanger.portDepth() / 2);
            if (t == null) continue;
            if (exchanger != null && t.side() < exchanger.portDepth()) continue;
            double hotNeeded = advanced ? t.flow() * hotPerSteam : t.flow();
            int minHeight = exchanger == null ? 1 : exchanger.portHeight();
            int minDepth = exchanger == null ? 1 : exchanger.portDepth();
            var patterns = new ReactorDesign.Pattern[]{ReactorDesign.Pattern.CHECKER, ReactorDesign.Pattern.GRID2};
            // The ideal reactor capacity is independent of the exchanger's port span. Bench it once, then fit ports.
            for (var seed : geometries(patterns, hotNeeded, 1, 1, null, null, coolant)) {
                var design = new ReactorDesign(seed.x(), Math.max(seed.y(), minHeight), Math.max(seed.z(), minDepth), seed.pattern(), null, 0);
                if (design.y() > SearchDomain.reactorHeight() || design.z() > Config.CONFIG.Reactor.MaxWidth - 2) continue;
                specs.add(charges(new PlantSpec(design, null, coolant, exchanger, advanced ? water : null, t, coilData, 0, 0)));
            }
        }
        plants.addAll(smallestFirst(specs));
        var best = pick(plants);
        if (best != null && !meets(best.result)) {
            var recovered = recoverTarget(best);
            plants.add(meets(recovered.result) ? coarse(recovered.spec, recovered.plant, recovered) : recovered);
        }
        var candidates = refineBest(plants, specs.size());
        if (!candidates.isEmpty()) candidates.set(0, refineCharges(candidates.getFirst()));
        return validate(candidates);
    }

    private record ExchangerChoice(List<ExchangerDesign> designs, double hotPerSteam) { }

    /** Grow from measured closed-loop shortfall; ideal supply must not become an artificial generation ceiling. */
    private Candidate recoverTarget(Candidate initial) {
        Candidate best = initial;
        for (int pass = 0; pass < 10 && !meets(best.result) && budgetLeft(); pass++) {
            var original = best;
            var spec = best.spec;
            var r = spec.reactor();
            int[] size = {r.x(), r.y(), r.z()};
            int[] max = {Config.CONFIG.Reactor.MaxLength - 2, SearchDomain.reactorHeight(), Config.CONFIG.Reactor.MaxWidth - 2};
            double need = (double) r.x() * r.y() * r.z() * Math.min(2, Math.max(1.1, request.target() * SCREEN_MARGIN / Math.max(1, best.result.power())));
            while ((double) size[0] * size[1] * size[2] < need) {
                int axis = -1;
                for (int i = 0; i < 3; i++) if (size[i] < max[i] && (axis < 0 || size[i] < size[axis])) axis = i;
                if (axis < 0) break;
                size[axis]++;
            }
            var grownReactor = new ReactorDesign(size[0], size[1], size[2], r.pattern(), r.moderator(), 0);
            var options = new ArrayList<PlantSpec>();
            options.add(charges(spec.withReactor(grownReactor)));
            if (spec.exchanger() != null) {
                var x = spec.exchanger();
                var shapes = List.of(new ExchangerDesign(x.length() + Math.max(1, x.length() / 4), x.rows(), x.layers()),
                        new ExchangerDesign(x.length(), x.rows() + 2, x.layers()),
                        new ExchangerDesign(x.length(), x.rows(), x.layers() + 1));
                for (var shape : shapes) {
                    if (shape.width() > Config.CONFIG.HeatExchanger.MaxLength || shape.height() > Config.CONFIG.HeatExchanger.MaxHeight
                            || shape.depth() > Config.CONFIG.HeatExchanger.MaxWidth) continue;
                    options.add(charges(spec.withExchanger(shape)));
                    options.add(charges(spec.withReactor(grownReactor).withExchanger(shape)));
                }
            }
            var candidates = new ArrayList<Candidate>();
            for (var option : options) {
                var layout = assemble(option);
                if (layout != null) candidates.add(new Candidate(option, layout, null));
            }
            candidates.sort(Comparator.comparingDouble(Candidate::cost));
            for (var candidate : candidates) {
                if (!budgetLeft()) break;
                var measured = screen(candidate.spec);
                if (!measured.converged()) continue;
                var checked = new Candidate(candidate.spec, candidate.plant, measured);
                if (preferred(checked, best)) best = checked;
            }
            if (best == original) break;
        }
        return meets(best.result) ? compactExchanger(shrinkReactor(best)) : best;
    }

    @Nullable
    private TurbineDesign turbine(Block coil, TurbineCoilRegistry.CoilData coilData, FluidTransitionRegistry.FluidTransition steam, int minShaft, int minArm) {
        long target = (long) Math.ceil(request.target() * TURBINE_MARGIN);
        String key = minShaft + "/" + minArm;
        if (turbineCache.containsKey(key)) return turbineCache.get(key).design();
        var choice = TurbineSearch.design(target, coil, coilData, steam, SearchDomain.turbineArm(), SearchDomain.turbineShaft(), minShaft, minArm, cancelled);
        turbineCache.put(key, choice);
        turbineBenches += choice.benches();
        return choice.design();
    }

    /** Three-dimensional candidates, screened with real heat transfer; no fixed length, height or aspect ratio. */
    private ExchangerChoice exchangers(FluidTransitionRegistry.FluidTransition coolant, FluidTransitionRegistry.FluidTransition water, TurbineDesign turbine) {
        var designs = new ArrayList<ExchangerDesign>();
        double hotPerSteam = 0, needed = turbine.flow() * BENCH_MARGIN * 1.1;
        int lengthLimit = SearchDomain.exchangerLength();
        if (lengthLimit < 2 || SearchDomain.exchangerRows() < 2 || SearchDomain.exchangerLayers() < 1) return new ExchangerChoice(designs, 0);
        var reference = new ExchangerDesign(Math.min(8, lengthLimit), 2);
        var referencePoint = Bench.exchanger(reference, coolant, water, cancelled);
        evaluated++;
        if (referencePoint.output() <= 0) return new ExchangerChoice(designs, 0);
        double contacts = needed / referencePoint.output() * reference.geometry().contact();
        var proposals = new java.util.LinkedHashSet<ExchangerDesign>();
        for (int rows = 2; rows <= SearchDomain.exchangerRows(); rows += 2) {
            for (int layers = 1; layers <= SearchDomain.exchangerLayers(); layers++) {
                int maxLength = lengthLimit - (layers == 1 ? 0 : 2);
                int estimated = Math.max(2, (int) Math.ceil(contacts / ((double) rows * layers)));
                if (maxLength < 2 || estimated > maxLength) continue;
                var design = new ExchangerDesign(estimated, rows, layers);
                if (design.portHeight() > Math.min(SearchDomain.reactorHeight(), SearchDomain.turbineShaft())
                        || design.portDepth() > Math.min(SearchDomain.reactorSide(), 2 * SearchDomain.turbineArm() + 1)) continue;
                proposals.add(design);
            }
        }
        var ordered = new ArrayList<>(proposals);
        ordered.sort(Comparator.comparingDouble((ExchangerDesign d) -> exchangerLowerCost(d, turbine))
                .thenComparingLong(d -> Compactness.surface(d.width(), d.height(), d.depth())));
        // The budget limits work, not dimensions. All configured dimensions participate in the cheap proposal pass.
        for (var proposal : ordered.subList(0, Math.min(12, ordered.size()))) {
            if (!preparationLeft()) break;
            var design = proposal;
            var point = Bench.exchanger(design, coolant, water, cancelled);
            evaluated++;
            int maxLength = lengthLimit - (design.layers() == 1 ? 0 : 2);
            for (int attempt = 0; point.output() < needed && design.length() < maxLength && attempt < 3 && budgetLeft(); attempt++) {
                int length = Math.min(maxLength, Math.max(design.length() + 1,
                        (int) Math.ceil(design.length() * needed / Math.max(1, point.output()))));
                design = new ExchangerDesign(length, design.rows(), design.layers());
                point = Bench.exchanger(design, coolant, water, cancelled);
                evaluated++;
            }
            if (point.output() < needed) continue;
            int lo = 2, hi = design.length();
            while (lo < hi && budgetLeft()) {
                int mid = (lo + hi) / 2;
                var probe = new ExchangerDesign(mid, design.rows(), design.layers());
                var measured = Bench.exchanger(probe, coolant, water, cancelled);
                evaluated++;
                if (measured.output() >= needed) {
                    hi = mid;
                    point = measured;
                } else lo = mid + 1;
            }
            design = new ExchangerDesign(hi, design.rows(), design.layers());
            hotPerSteam = Math.max(hotPerSteam, point.fuel() / point.output());
            if (!designs.contains(design)) designs.add(design);
            // Retain thermal headroom without forcing extra height on the neighbouring machines.
            int longer = Math.min(maxLength, design.length() * 2);
            if (longer > design.length()) {
                var larger = new ExchangerDesign(longer, design.rows(), design.layers());
                if (!designs.contains(larger)) designs.add(larger);
            }
        }
        designs.sort((a, b) -> Compactness.compare(a.width(), a.height(), a.depth(), b.width(), b.height(), b.depth()));
        return new ExchangerChoice(designs, hotPerSteam);
    }

    /** Cheap lower bound including the space a connection demands from its neighbours. */
    private static double exchangerLowerCost(ExchangerDesign d, TurbineDesign turbine) {
        int side = Math.max(turbine.width(), d.portDepth() + 2);
        return Compactness.cost(d.width(), d.height(), d.depth())
                + Compactness.cost(3, d.portHeight() + 2, d.portDepth() + 2)
                + Compactness.cost(side, Math.max(turbine.height(), d.portHeight() + 2), side);
    }

    /** Starting charges: the reactor's cold side full, half the exchanger's hot side, nine tenths of its water side. */
    private static PlantSpec charges(PlantSpec spec) {
        long primary = spec.reactorTankCapacity();
        long water = 0;
        if (spec.exchanger() != null) {
            long channels = spec.exchanger().channelVolume() / 2;
            primary += channels / 2;
            water = channels * 9 / 10;
        }
        return spec.withCharges(primary, water);
    }

    /** Tries a few other charges for the chosen plant and keeps the one with the lowest fuel that still meets the target. */
    private Candidate refineCharges(Candidate chosen) {
        var spec = chosen.spec;
        Candidate best = chosen;
        long reactor = spec.reactorTankCapacity();
        long channels = spec.exchanger() == null ? 0 : spec.exchanger().channelVolume() / 2;
        var options = new ArrayList<long[]>();
        if (spec.exchanger() == null) {
            options.add(new long[]{reactor * 3 / 4, 0});
        } else {
            for (long hot : new long[]{channels / 4, channels * 3 / 4})
                options.add(new long[]{reactor + hot, spec.waterCharge()});
            options.add(new long[]{spec.primaryCharge(), channels * 6 / 10});
        }
        for (var option : options) {
            if (!budgetLeft()) break;
            var s = spec.withCharges(option[0], option[1]);
            var result = screen(s);
            var candidate = new Candidate(s, chosen.plant, result);
            if (result.converged() && preferred(candidate, best)) best = candidate;
        }
        return best;
    }

    // ------------------------------------------------------------------ validation

    /**
     * Validates candidates best first with the strict settings in both controller orders. A plant whose lower power
     * misses the target gets its rods withdrawn a step at a time; the first plant that meets the target wins.
     */
    private Outcome validate(List<Candidate> candidates) {
        Candidate below = null;
        PlantRun.Result belowResult = null, belowAlternate = null;
        int attempt = 0;
        for (var candidate : candidates.subList(0, Math.min(3, candidates.size()))) {
            var spec = candidate.spec;
            for (int step = 0; step < 6; step++) {
                progress.update(Phase.VALIDATING, attempt++, 6);
                var first = PlantRun.run(spec, false, PlantRun.Settings.VALIDATE, cancelled);
                var second = PlantRun.run(spec, true, PlantRun.Settings.VALIDATE, cancelled);
                evaluated += 2;
                if (!first.converged() || !second.converged()) {
                    if (below == null) return outcome(CalculationStatus.NOT_CONVERGED, null, null, null);
                    break;
                }
                var low = first.power() <= second.power() ? first : second;
                var high = low == first ? second : first;
                var validated = new Candidate(spec, candidate.plant, low);
                if (low.power() >= request.target()) return outcome(CalculationStatus.TARGET_MET, validated, low, high);
                if (below == null || low.power() > belowResult.power()) {
                    below = validated;
                    belowResult = low;
                    belowAlternate = high;
                }
                double insertion = spec.reactor().insertion();
                if (insertion <= 0) break;
                spec = spec.withReactor(spec.reactor().withInsertion(Math.max(0, insertion - 2)));
            }
        }
        if (below != null) return outcome(CalculationStatus.BEST_BELOW_TARGET, below, belowResult, belowAlternate);
        return outcome(CalculationStatus.SEARCH_LIMIT_REACHED, null, null, null);
    }

    @Nullable
    private static Block block(String id) {
        var key = ResourceLocation.tryParse(id);
        if (key == null || !BuiltInRegistries.BLOCK.containsKey(key)) return null;
        return BuiltInRegistries.BLOCK.get(key);
    }
}
