package com.omiinqa.observability;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * In-process, thread-safe metric store supporting counters, gauges, and timers.
 *
 * <p><strong>Why this exists:</strong> Traditional test frameworks report pass/fail counts
 * but give no insight into <em>operational</em> characteristics: how many API calls were
 * made, how long each page action took in aggregate, or what the current state of a shared
 * resource counter is. {@code MetricRegistry} bridges this gap by acting as a lightweight,
 * zero-dependency equivalent of Micrometer or Dropwizard Metrics — scoped to a single JVM
 * process and designed to feed {@link PrometheusExporter} for Grafana dashboards.</p>
 *
 * <p><strong>Design:</strong></p>
 * <ul>
 *   <li><strong>Counters</strong> ({@link LongAdder}) — monotonically increasing; optimised
 *       for high-frequency increment under contention (striped internally by the JVM).</li>
 *   <li><strong>Gauges</strong> ({@link AtomicLong}) — can go up or down; models current
 *       state (e.g., active WebDriver instances, items in cart).</li>
 *   <li><strong>Timers</strong> — store a sample count and total nanoseconds, both as
 *       {@link LongAdder}; the average duration is derived at {@link #snapshot()} time
 *       and expressed in milliseconds for readability.</li>
 *   <li>All maps are {@link ConcurrentHashMap} — safe for concurrent test threads without
 *       external locking.</li>
 *   <li>{@link #snapshot()} returns an immutable deep-copy so callers can safely iterate
 *       and export while the registry continues to be updated by other threads.</li>
 * </ul>
 *
 * <p><strong>Usage:</strong></p>
 * <pre>{@code
 * MetricRegistry.incrementCounter("api.calls", 1);
 * MetricRegistry.setGauge("drivers.active", 3);
 * MetricRegistry.recordTimer("login.duration.ms", 320);
 *
 * MetricRegistry.Snapshot snap = MetricRegistry.snapshot();
 * long calls = snap.counters().get("api.calls");  // 1
 * }</pre>
 *
 * <p>This class is a pure utility (all-static); it is deliberately a singleton registry
 * following the same pattern as SLF4J's {@code LoggerFactory} — tests share one registry
 * so cross-cutting dashboards can aggregate across all test classes without injection.</p>
 */
public final class MetricRegistry {

    // -------------------------------------------------------------------------
    // Internal metric stores
    // -------------------------------------------------------------------------

    private static final ConcurrentHashMap<String, LongAdder> COUNTERS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicLong> GAUGES = new ConcurrentHashMap<>();

    /** Paired adders: total nanos and sample count for each timer name. */
    private static final ConcurrentHashMap<String, LongAdder> TIMER_TOTAL_NANOS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, LongAdder> TIMER_COUNTS = new ConcurrentHashMap<>();

    /** Utility class — no instances. */
    private MetricRegistry() {
        throw new UnsupportedOperationException("MetricRegistry is a utility class");
    }

    // -------------------------------------------------------------------------
    // Counter API
    // -------------------------------------------------------------------------

    /**
     * Increments a named counter by {@code delta}.
     *
     * <p>The counter is created on first use. Counters are monotonically
     * increasing — they cannot be decremented.</p>
     *
     * @param name  metric name (e.g., {@code "api.calls.total"})
     * @param delta amount to add; must be positive
     */
    public static void incrementCounter(final String name, final long delta) {
        COUNTERS.computeIfAbsent(name, k -> new LongAdder()).add(delta);
    }

    /**
     * Increments a named counter by 1.
     *
     * @param name metric name
     */
    public static void incrementCounter(final String name) {
        incrementCounter(name, 1L);
    }

    // -------------------------------------------------------------------------
    // Gauge API
    // -------------------------------------------------------------------------

    /**
     * Sets a gauge to an absolute value.
     *
     * <p>Gauges model current state and may increase or decrease freely.</p>
     *
     * @param name  metric name (e.g., {@code "drivers.active"})
     * @param value the new absolute value
     */
    public static void setGauge(final String name, final long value) {
        GAUGES.computeIfAbsent(name, k -> new AtomicLong()).set(value);
    }

    /**
     * Adds {@code delta} (positive or negative) to an existing gauge, or creates
     * it with {@code delta} as its initial value.
     *
     * @param name  metric name
     * @param delta amount to add (negative to decrement)
     */
    public static void adjustGauge(final String name, final long delta) {
        GAUGES.computeIfAbsent(name, k -> new AtomicLong()).addAndGet(delta);
    }

    // -------------------------------------------------------------------------
    // Timer API
    // -------------------------------------------------------------------------

    /**
     * Records a single duration sample for the named timer.
     *
     * <p>Internally stores the total nanoseconds and sample count. The
     * {@link Snapshot} exposes the average in milliseconds.</p>
     *
     * @param name      timer name (e.g., {@code "page.login.ms"})
     * @param durationNanos elapsed duration in nanoseconds
     */
    public static void recordTimer(final String name, final long durationNanos) {
        TIMER_TOTAL_NANOS.computeIfAbsent(name, k -> new LongAdder()).add(durationNanos);
        TIMER_COUNTS.computeIfAbsent(name, k -> new LongAdder()).add(1);
    }

    // -------------------------------------------------------------------------
    // Snapshot
    // -------------------------------------------------------------------------

    /**
     * Returns a point-in-time, immutable snapshot of all registered metrics.
     *
     * <p>The snapshot is safe to iterate and export (e.g., via {@link PrometheusExporter})
     * while the registry continues to receive updates from other threads — no data from
     * after the snapshot call will appear in the returned maps.</p>
     *
     * @return immutable {@link Snapshot}
     */
    public static Snapshot snapshot() {
        // Deep-copy counters
        final Map<String, Long> counters = new HashMap<>();
        COUNTERS.forEach((k, v) -> counters.put(k, v.sum()));

        // Deep-copy gauges
        final Map<String, Long> gauges = new HashMap<>();
        GAUGES.forEach((k, v) -> gauges.put(k, v.get()));

        // Compute timer averages (milliseconds)
        final Map<String, TimerSnapshot> timers = new HashMap<>();
        TIMER_COUNTS.forEach((name, countAdder) -> {
            final long count = countAdder.sum();
            final LongAdder totalNanos = TIMER_TOTAL_NANOS.get(name);
            final long total = totalNanos != null ? totalNanos.sum() : 0L;
            final double avgMs = count > 0 ? (total / (double) count) / 1_000_000.0 : 0.0;
            timers.put(name, new TimerSnapshot(count, total, avgMs));
        });

        return new Snapshot(
                Collections.unmodifiableMap(counters),
                Collections.unmodifiableMap(gauges),
                Collections.unmodifiableMap(timers));
    }

    /**
     * Resets all metrics to zero.
     *
     * <p>Intended for use between test suites when the same JVM process runs
     * multiple suites sequentially and metrics should not bleed across.</p>
     */
    public static void reset() {
        COUNTERS.clear();
        GAUGES.clear();
        TIMER_TOTAL_NANOS.clear();
        TIMER_COUNTS.clear();
    }

    // -------------------------------------------------------------------------
    // Value objects
    // -------------------------------------------------------------------------

    /**
     * Immutable snapshot of all metrics at a single point in time.
     */
    public static final class Snapshot {

        private final Map<String, Long> counters;
        private final Map<String, Long> gauges;
        private final Map<String, TimerSnapshot> timers;

        Snapshot(final Map<String, Long> counters,
                 final Map<String, Long> gauges,
                 final Map<String, TimerSnapshot> timers) {
            this.counters = counters;
            this.gauges = gauges;
            this.timers = timers;
        }

        /** @return immutable map of counter name to current cumulative value */
        public Map<String, Long> counters() {
            return counters;
        }

        /** @return immutable map of gauge name to current value */
        public Map<String, Long> gauges() {
            return gauges;
        }

        /** @return immutable map of timer name to {@link TimerSnapshot} */
        public Map<String, TimerSnapshot> timers() {
            return timers;
        }
    }

    /**
     * Immutable statistics for a single timer.
     */
    public static final class TimerSnapshot {

        private final long count;
        private final long totalNanos;
        private final double avgMs;

        TimerSnapshot(final long count, final long totalNanos, final double avgMs) {
            this.count = count;
            this.totalNanos = totalNanos;
            this.avgMs = avgMs;
        }

        /** @return number of samples recorded */
        public long getCount() {
            return count;
        }

        /** @return sum of all sample durations in nanoseconds */
        public long getTotalNanos() {
            return totalNanos;
        }

        /** @return arithmetic mean of all sample durations in milliseconds */
        public double getAvgMs() {
            return avgMs;
        }
    }
}
