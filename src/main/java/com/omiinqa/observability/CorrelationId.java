package com.omiinqa.observability;

import org.slf4j.MDC;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Per-thread correlation ID generator and propagator.
 *
 * <p><strong>Why this exists:</strong> In parallel test execution, multiple threads write
 * to the same log sink simultaneously. Without a stable, unique token per test thread,
 * log lines from different tests interleave and become impossible to group in Kibana,
 * Grafana Loki, or any centralized log aggregator. {@code CorrelationId} solves this
 * by assigning a deterministic, monotonically-increasing ID at the start of every test
 * thread and surfacing it through SLF4J MDC so that <em>every</em> log statement
 * automatically carries it — no manual parameter threading required.</p>
 *
 * <p><strong>Design decisions:</strong></p>
 * <ul>
 *   <li>ThreadLocal storage — each thread owns its own ID; no synchronization on reads.</li>
 *   <li>Deterministic format — {@code <sanitised-thread-name>-<monotonic-counter>}.
 *       Math.random and wall-clock timestamps are intentionally avoided: they produce
 *       non-reproducible IDs that make log replay and bug reproduction harder.</li>
 *   <li>MDC key {@code "correlationId"} — matches the key expected by the log4j2
 *       pattern layout used elsewhere in this framework, so no config change is needed.</li>
 *   <li>OTel bridge note — the ID can be injected as a W3C {@code traceparent}
 *       header if this framework is later wired to an OpenTelemetry SDK exporter.</li>
 * </ul>
 *
 * <p><strong>Usage:</strong></p>
 * <pre>{@code
 * // At test start (or in a @BeforeMethod / TestListener):
 * CorrelationId.start();
 *
 * // Anywhere in the test or page objects — MDC is already populated:
 * String cid = CorrelationId.get();
 *
 * // At test end (or in @AfterMethod):
 * CorrelationId.clear();
 * }</pre>
 *
 * <p>This class is stateless beyond its ThreadLocal; it is safe to use from multiple
 * concurrent threads without any external synchronisation.</p>
 */
public final class CorrelationId {

    /** MDC key consumed by the log4j2 pattern layout across this framework. */
    public static final String MDC_KEY = "correlationId";

    /**
     * Global counter incremented once per {@link #start()} call.
     * Using {@link AtomicLong} keeps the increment lock-free while guaranteeing
     * uniqueness across all threads in the JVM.
     */
    private static final AtomicLong COUNTER = new AtomicLong(0);

    /** Per-thread storage of the active correlation ID string. */
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    /** Utility class — no instances. */
    private CorrelationId() {
        throw new UnsupportedOperationException("CorrelationId is a utility class");
    }

    /**
     * Generates a new correlation ID for the current thread, stores it in a
     * {@link ThreadLocal}, and publishes it to {@link MDC} so every subsequent
     * log statement on this thread automatically includes it.
     *
     * <p>If called again before {@link #clear()}, the existing ID is replaced,
     * which is intentional: test retries should produce a fresh ID.</p>
     *
     * @return the newly generated correlation ID
     */
    public static String start() {
        final String id = buildId();
        CURRENT.set(id);
        MDC.put(MDC_KEY, id);
        return id;
    }

    /**
     * Returns the correlation ID currently active on this thread, or
     * {@code "unset"} if {@link #start()} has not been called.
     *
     * @return the active correlation ID; never {@code null}
     */
    public static String get() {
        final String id = CURRENT.get();
        return id != null ? id : "unset";
    }

    /**
     * Removes the correlation ID from the current thread's {@link ThreadLocal}
     * and from {@link MDC}.
     *
     * <p>Must be called at the end of every test (e.g., {@code @AfterMethod}) to
     * prevent stale IDs from leaking into subsequent tests that reuse the same
     * thread-pool thread.</p>
     */
    public static void clear() {
        CURRENT.remove();
        MDC.remove(MDC_KEY);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a deterministic ID from the current thread name (sanitised to be
     * log-safe) and a global monotonic counter.
     */
    private static String buildId() {
        final String threadName = Thread.currentThread().getName()
                .replaceAll("[^A-Za-z0-9_\\-]", "_")
                .replaceAll("_{2,}", "_");
        final long seq = COUNTER.incrementAndGet();
        return "cid-" + threadName + "-" + seq;
    }
}
