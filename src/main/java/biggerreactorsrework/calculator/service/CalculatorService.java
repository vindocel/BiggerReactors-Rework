package biggerreactorsrework.calculator.service;

import biggerreactorsrework.BiggerReactors;
import biggerreactorsrework.Config;
import biggerreactorsrework.calculator.*;
import biggerreactorsrework.calculator.design.MachineLayout;
import biggerreactorsrework.calculator.search.PlantSearch;
import biggerreactorsrework.calculator.simulation.PlantRun;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs calculations for players on a small dedicated pool, never on the server thread and never touching a level.
 * The catalog is captured on the server thread before a job starts; the job works on that copy. One job per player,
 * a bounded global queue, a minimum interval between requests, and cancellation on logout, reload and shutdown.
 * Results are sent back from the server thread.
 */
public final class CalculatorService {
    private static final int WORKERS = Math.max(1, Math.min(2, Runtime.getRuntime().availableProcessors() / 2));
    private static final int QUEUE = 8;
    private static final long MIN_INTERVAL_MS = 1000, PROGRESS_INTERVAL_MS = 250;
    private static final int MAX_SESSION = 64;

    private static final ThreadPoolExecutor POOL = new ThreadPoolExecutor(WORKERS, WORKERS, 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(QUEUE), r -> {
        var thread = new Thread(r, "Rework power calculator");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });

    private record Job(String session, AtomicBoolean cancelled, Future<?> future) { }

    private static final Map<UUID, Job> JOBS = new ConcurrentHashMap<>();
    // Modified 2026-10-30: throttle catalog work and reuse snapshots until rules change.
    private static final RequestThrottle LAST_REQUEST = new RequestThrottle(MIN_INTERVAL_MS);
    private static final RequestThrottle LAST_CATALOG = new RequestThrottle(MIN_INTERVAL_MS);
    private static CalculatorCatalog cachedCatalog;
    private static long cachedRevision = -1;

    private CalculatorService() { }

    public static void init() {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            cancel(e.getEntity().getUUID());
            LAST_REQUEST.remove(e.getEntity().getUUID());
            LAST_CATALOG.remove(e.getEntity().getUUID());
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> {
            invalidate();
            LAST_REQUEST.clear();
            LAST_CATALOG.clear();
        });
    }

    /** Data reloaded: running jobs used the old rules, so they stop. */
    public static void invalidate() {
        cancelAll();
        cachedCatalog = null;
    }

    private static void cancelAll() {
        for (var id : List.copyOf(JOBS.keySet())) cancel(id);
    }

    private static void cancel(UUID player) {
        var job = JOBS.remove(player);
        if (job != null) {
            job.cancelled.set(true);
            job.future.cancel(false);
            POOL.remove((Runnable) job.future);
        }
    }

    /** Registry reloads and configuration revisions invalidate the snapshot. Server thread only. */
    private static CalculatorCatalog catalog() {
        long revision = biggerreactorsrework.core.config.ConfigManager.revision();
        if (cachedCatalog == null || revision != cachedRevision) {
            cachedCatalog = CalculatorCatalog.capture();
            cachedRevision = revision;
        }
        return cachedCatalog;
    }

    static void receive(ServerPlayer player, CompoundTag tag) {
        switch (tag.getString("type")) {
            case CalculatorNetwork.CATALOG -> {
                if (!LAST_CATALOG.acquire(player.getUUID(), System.nanoTime())) return;
                var reply = new CompoundTag();
                reply.putString("type", CalculatorNetwork.CATALOG);
                reply.put("catalog", catalog().toClientTag());
                CalculatorNetwork.toPlayer(player, reply);
            }
            case CalculatorNetwork.CANCEL -> {
                var job = JOBS.get(player.getUUID());
                if (job != null && job.session.equals(tag.getString("session"))) cancel(player.getUUID());
            }
            case CalculatorNetwork.START -> start(player, tag);
            default -> { }
        }
    }

    private static void start(ServerPlayer player, CompoundTag tag) {
        String session = tag.getString("session");
        if (session.isEmpty() || session.length() > MAX_SESSION) return;
        if (!LAST_REQUEST.acquire(player.getUUID(), System.nanoTime())) {
            finish(player, session, failure(CalculationStatus.BUSY));
            return;
        }
        var request = CalculatorRequest.fromTag(tag.getCompound("request"));
        if (request == null) {
            finish(player, session, failure(CalculationStatus.INVALID_INPUT));
            return;
        }
        var snapshot = catalog();
        if (!snapshot.fingerprint().equals(request.fingerprint())) {
            finish(player, session, failure(CalculationStatus.STALE_CATALOG));
            return;
        }
        Route route;
        if (!snapshot.fuels().contains(request.fuel())) route = null;
        else if (!request.liquid()) route = snapshot.moderators().contains(request.material()) ? Route.SOLID : null;
        else route = snapshot.coils().contains(request.coil()) ? snapshot.coolantRoute(request.material()) : null;
        if (route == null) {
            finish(player, session, failure(CalculationStatus.INVALID_INPUT));
            return;
        }
        cancel(player.getUUID());
        var cancelled = new AtomicBoolean();
        var server = player.server;
        var id = player.getUUID();
        long[] lastProgress = {0};
        PlantSearch.Progress progress = (phase, done, total) -> {
            long t = System.currentTimeMillis();
            if (t - lastProgress[0] < PROGRESS_INTERVAL_MS) return;
            lastProgress[0] = t;
            var message = new CompoundTag();
            message.putString("type", CalculatorNetwork.PROGRESS);
            message.putString("session", session);
            message.putString("phase", phase.name());
            message.putInt("done", done);
            message.putInt("total", total);
            server.execute(() -> {
                var target = server.getPlayerList().getPlayer(id);
                if (target != null && !cancelled.get()) CalculatorNetwork.toPlayer(target, message);
            });
        };
        try {
            var future = POOL.submit(() -> {
                CompoundTag result;
                try {
                    var outcome = PlantSearch.run(request, route, snapshot, progress, cancelled::get);
                    result = build(request, route, snapshot, outcome).toTag();
                } catch (CancellationException e) {
                    result = failure(CalculationStatus.CANCELLED);
                } catch (RuntimeException e) {
                    BiggerReactors.LOGGER.error("Power calculation failed", e);
                    result = failure(CalculationStatus.FAILED);
                }
                var reply = result;
                server.execute(() -> {
                    var job = JOBS.get(id);
                    if (job == null || !job.session.equals(session) || cancelled.get()) return;
                    JOBS.remove(id);
                    var target = server.getPlayerList().getPlayer(id);
                    if (target != null) finish(target, session, reply);
                });
            });
            JOBS.put(id, new Job(session, cancelled, future));
        } catch (RejectedExecutionException e) {
            finish(player, session, failure(CalculationStatus.BUSY));
        }
    }

    private static CompoundTag failure(CalculationStatus status) {
        var tag = new CompoundTag();
        tag.putString("failure", status.name());
        return tag;
    }

    private static void finish(ServerPlayer player, String session, CompoundTag result) {
        var message = new CompoundTag();
        message.putString("type", CalculatorNetwork.RESULT);
        message.putString("session", session);
        message.put("result", result);
        CalculatorNetwork.toPlayer(player, message);
    }

    /** Turns a search outcome into the stored record: plant, settings, measured values, capacities and counts. */
    public static CalculationResult build(CalculatorRequest request, Route route, CalculatorCatalog catalog, PlantSearch.Outcome outcome) {
        var values = new LinkedHashMap<String, Double>();
        var spec = outcome.spec();
        PlantRun.Result r = outcome.validation();
        if (spec != null && r != null && outcome.status().hasDesign()) {
            values.put(CalculationResult.POWER, r.power());
            values.put(CalculationResult.POWER_MIN, r.powerMin());
            values.put(CalculationResult.POWER_MAX, r.powerMax());
            if (outcome.alternate() != null) values.put(CalculationResult.POWER_ALT, outcome.alternate().power());
            values.put(CalculationResult.FUEL, r.fuel());
            values.put(CalculationResult.EFFICIENCY, r.efficiency());
            values.put(CalculationResult.FUEL_TEMPERATURE, r.fuelTemperature());
            values.put(CalculationResult.DRIFT, r.drift());
            values.put(CalculationResult.TICKS, (double) r.ticks());
            values.put(CalculationResult.WINDOW, (double) r.window());
            var reactor = spec.reactor();
            values.put(CalculationResult.INSERTION, reactor.insertion());
            values.put(CalculationResult.FUEL_COLUMNS, (double) reactor.columns());
            values.put(CalculationResult.FUEL_RODS, (double) reactor.fuelRodBlocks());
            if (reactor.liquid()) {
                values.put(CalculationResult.MANIFOLDS, (double) reactor.manifoldBlocks());
                values.put(CalculationResult.CONTACTS, (double) reactor.contacts());
                values.put(CalculationResult.REACTOR_CAPACITY, (double) spec.reactorTankCapacity());
                values.put(CalculationResult.PRIMARY_CHARGE, (double) spec.primaryCharge());
                values.put(CalculationResult.HOT_FLOW, r.hotFlow());
                values.put(CalculationResult.REACTOR_LIMIT, r.reactorLimit());
                values.put(CalculationResult.PRIMARY_ERROR, (double) r.primaryError());
                values.put(CalculationResult.STEAM, r.steam());
                values.put(CalculationResult.RPM, r.rpm());
                var s = r.stocks();
                values.put(CalculationResult.REACTOR_COLD, (double) s.reactorCold());
                values.put(CalculationResult.REACTOR_HOT, (double) s.reactorHot());
                values.put(CalculationResult.TURBINE_STEAM, (double) s.turbineSteam());
                values.put(CalculationResult.TURBINE_WATER, (double) s.turbineWater());
                var t = spec.turbine();
                values.put(CalculationResult.TURBINE_FLOW, (double) t.flow());
                values.put(CalculationResult.BLADES, (double) t.blades());
                values.put(CalculationResult.COILS, (double) t.coils());
                values.put(CalculationResult.SHAFT, (double) t.shaft());
                values.put(CalculationResult.FLOW_LIMIT, (double) t.flowLimit());
                values.put(CalculationResult.TURBINE_CAPACITY, (double) (((long) t.side() * t.side() * t.shaft() - t.shaft() - t.coils()) * Config.CONFIG.Turbine.TankVolumePerBlock));
                if (spec.exchanger() != null) {
                    var g = spec.exchanger().geometry();
                    values.put(CalculationResult.WATER_CHARGE, (double) spec.waterCharge());
                    values.put(CalculationResult.EXCHANGER_CAPACITY, (double) (spec.exchanger().channelVolume() / 2));
                    values.put(CalculationResult.CONDENSER, (double) g.condenser());
                    values.put(CalculationResult.EVAPORATOR, (double) g.evaporator());
                    values.put(CalculationResult.EXCHANGER_CONTACTS, (double) g.contact());
                    values.put(CalculationResult.EXCHANGER_LIMIT, r.exchangerLimit());
                    values.put(CalculationResult.WATER_ERROR, (double) r.waterError());
                    values.put(CalculationResult.EXCHANGER_HOT, (double) s.exchangerHot());
                    values.put(CalculationResult.EXCHANGER_COLD, (double) s.exchangerCold());
                    values.put(CalculationResult.EXCHANGER_WATER, (double) s.exchangerWater());
                    values.put(CalculationResult.EXCHANGER_STEAM, (double) s.exchangerSteam());
                }
            } else {
                values.put(CalculationResult.MODERATORS, (double) reactor.moderatorBlocks());
            }
        }
        values.put(CalculationResult.EVALUATED, (double) outcome.evaluated());
        values.put(CalculationResult.ELAPSED_MS, (double) outcome.elapsedMillis());
        values.put(CalculationResult.SEARCH_LIMITED, outcome.searchLimited() ? 1.0 : 0.0);
        return new CalculationResult(UUID.randomUUID().toString(), System.currentTimeMillis(), request, route, outcome.status(),
                catalog.fingerprint(), CalculatorCatalog.ENGINE_VERSION, BiggerReactors.modVersion(),
                outcome.status().hasDesign() ? outcome.plant() : null, values, Set.of());
    }

}
