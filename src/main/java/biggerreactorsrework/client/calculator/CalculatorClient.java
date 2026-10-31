package biggerreactorsrework.client.calculator;

import biggerreactorsrework.calculator.CalculationResult;
import biggerreactorsrework.calculator.CalculationStatus;
import biggerreactorsrework.calculator.CalculatorCatalog;
import biggerreactorsrework.calculator.CalculatorRequest;
import biggerreactorsrework.calculator.service.CalculatorNetwork;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Client side of the calculator: the server's catalog and the one calculation in flight. A session id ties every
 * reply to the request that caused it; replies for an older or abandoned session are ignored, so a late result
 * never reopens anything or enters the history.
 */
public final class CalculatorClient {
    public enum Phase { IDLE, PREPARING, SEARCHING, VALIDATING, DONE }

    @Nullable private static CalculatorCatalog catalog;
    private static boolean catalogRequested;
    // Modified 2026-10-30: retry catalog refreshes which were coalesced by the server's request limit.
    private static boolean catalogRefreshPending;
    private static long catalogRequestedAt;
    @Nullable private static String session;
    @Nullable private static CalculatorRequest request;
    private static Phase phase = Phase.IDLE;
    private static int done, total;
    @Nullable private static CalculationResult result;
    @Nullable private static CalculationStatus failure;

    private CalculatorClient() { }

    @Nullable public static CalculatorCatalog catalog() { return catalog; }
    public static Phase phase() { return phase; }
    public static int done() { return done; }
    public static int total() { return total; }
    @Nullable public static CalculationResult result() { return result; }
    @Nullable public static CalculationStatus failure() { return failure; }
    @Nullable public static CalculatorRequest request() { return request; }
    public static boolean running() { return session != null && phase != Phase.DONE && phase != Phase.IDLE; }

    /** Asks for the catalog once per connection; {@link #refreshCatalog()} asks again. */
    public static void requestCatalog() {
        if (catalogRequested && (!catalogRefreshPending || System.nanoTime() - catalogRequestedAt < 1_000_000_000L)) return;
        refreshCatalog();
    }

    public static void refreshCatalog() {
        catalogRequested = true;
        catalogRefreshPending = true;
        catalogRequestedAt = System.nanoTime();
        var tag = new CompoundTag();
        tag.putString("type", CalculatorNetwork.CATALOG);
        CalculatorNetwork.toServer(tag);
    }

    public static void start(CalculatorRequest value) {
        abandon();
        session = UUID.randomUUID().toString();
        request = value;
        phase = Phase.PREPARING;
        done = total = 0;
        result = null;
        failure = null;
        var tag = new CompoundTag();
        tag.putString("type", CalculatorNetwork.START);
        tag.putString("session", session);
        tag.put("request", value.toTag());
        CalculatorNetwork.toServer(tag);
    }

    /** Cancels the job on the server and forgets it, so nothing it sends later is used. */
    public static void abandon() {
        if (session != null && running()) {
            var tag = new CompoundTag();
            tag.putString("type", CalculatorNetwork.CANCEL);
            tag.putString("session", session);
            CalculatorNetwork.toServer(tag);
        }
        session = null;
        request = null;
        phase = Phase.IDLE;
        result = null;
        failure = null;
    }

    public static void disconnected() {
        session = null;
        request = null;
        phase = Phase.IDLE;
        result = null;
        failure = null;
        catalog = null;
        catalogRequested = false;
        catalogRefreshPending = false;
        catalogRequestedAt = 0;
    }

    public static void receive(CompoundTag tag) {
        switch (tag.getStringOr("type", "")) {
            case CalculatorNetwork.CATALOG -> {
                catalog = CalculatorCatalog.fromClientTag(tag.getCompoundOrEmpty("catalog"));
                catalogRefreshPending = false;
            }
            case CalculatorNetwork.PROGRESS -> {
                if (!tag.getStringOr("session", "").equals(session) || !running()) return;
                try {
                    phase = Phase.valueOf(tag.getStringOr("phase", ""));
                } catch (IllegalArgumentException ignored) {
                }
                done = Math.max(0, tag.getIntOr("done", 0));
                total = Math.max(0, tag.getIntOr("total", 0));
            }
            case CalculatorNetwork.RESULT -> {
                if (!tag.getStringOr("session", "").equals(session) || !running()) return;
                var body = tag.getCompoundOrEmpty("result");
                phase = Phase.DONE;
                if (body.contains("failure")) {
                    try {
                        failure = CalculationStatus.valueOf(body.getStringOr("failure", ""));
                    } catch (IllegalArgumentException e) {
                        failure = CalculationStatus.FAILED;
                    }
                    if (failure == CalculationStatus.STALE_CATALOG) refreshCatalog();
                    return;
                }
                var parsed = CalculationResult.fromTag(body);
                if (parsed == null) {
                    failure = CalculationStatus.FAILED;
                    return;
                }
                result = parsed;
                if (!parsed.status().hasDesign()) failure = parsed.status();
                else CalculatorLibrary.get().addRecent(parsed);
            }
            default -> { }
        }
    }
}
