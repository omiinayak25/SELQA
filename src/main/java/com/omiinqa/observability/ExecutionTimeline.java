package com.omiinqa.observability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-thread named-phase stopwatch that produces a human-readable, ordered timeline.
 *
 * <p><strong>Why this exists:</strong> Test execution pipelines contain multiple
 * distinguishable phases — driver startup, page load, business-action, assertion,
 * teardown — but standard log timestamps only tell you <em>when</em> each message was
 * emitted, not how long each phase took relative to the others. {@code ExecutionTimeline}
 * records those phases with nanosecond precision and renders them as a waterfall-style
 * timeline suitable for pasting into a bug report or CI log.</p>
 *
 * <p><strong>Design:</strong></p>
 * <ul>
 *   <li>ThreadLocal keyed map — each thread manages its own timeline without locking.</li>
 *   <li>The timeline is ordered by insertion (LinkedHashMap) so the waterfall order
 *       always matches the chronological order in which {@link #startPhase} was called.</li>
 *   <li>Immutability on {@link #snapshot()} — callers receive an unmodifiable list of
 *       {@link PhaseEntry} records, preventing after-the-fact mutation.</li>
 *   <li>{@link System#nanoTime()} is used instead of {@link System#currentTimeMillis()}
 *       because it is monotonic and has sub-millisecond resolution — critical for
 *       detecting fast page-object operations that complete in tens of milliseconds.</li>
 * </ul>
 *
 * <p><strong>Usage:</strong></p>
 * <pre>{@code
 * ExecutionTimeline.startPhase("driver-init");
 * // ... do work ...
 * ExecutionTimeline.stopPhase("driver-init");
 *
 * ExecutionTimeline.startPhase("login");
 * // ... do work ...
 * ExecutionTimeline.stopPhase("login");
 *
 * System.out.println(ExecutionTimeline.render());
 * ExecutionTimeline.clear();
 * }</pre>
 *
 * <p>All public methods are safe to call from multiple concurrent threads; each thread
 * sees only its own timeline.</p>
 */
public final class ExecutionTimeline {

    /**
     * Immutable record of a single completed phase.
     *
     * <p>Exposed as a public nested class so calling code can iterate the snapshot
     * and format the data differently (e.g., into an Allure attachment or a JSON payload)
     * without depending on the {@link #render()} string format.</p>
     */
    public static final class PhaseEntry {

        private final String name;
        private final long elapsedNanos;

        PhaseEntry(final String name, final long elapsedNanos) {
            this.name = name;
            this.elapsedNanos = elapsedNanos;
        }

        /** @return the phase label passed to {@link ExecutionTimeline#startPhase(String)} */
        public String getName() {
            return name;
        }

        /** @return elapsed wall time for this phase in nanoseconds */
        public long getElapsedNanos() {
            return elapsedNanos;
        }

        /** @return elapsed time in whole milliseconds (truncated) */
        public long getElapsedMillis() {
            return elapsedNanos / 1_000_000L;
        }

        @Override
        public String toString() {
            return name + " [" + getElapsedMillis() + " ms]";
        }
    }

    // -------------------------------------------------------------------------
    // Internal state — per-thread
    // -------------------------------------------------------------------------

    /** Completed phases, ordered by insertion. */
    private static final ThreadLocal<LinkedHashMap<String, PhaseEntry>> COMPLETED =
            ThreadLocal.withInitial(LinkedHashMap::new);

    /** Phases that have been started but not yet stopped: name -> startNanos. */
    private static final ThreadLocal<Map<String, Long>> IN_FLIGHT =
            ThreadLocal.withInitial(ConcurrentHashMap::new);

    /** Utility class — no instances. */
    private ExecutionTimeline() {
        throw new UnsupportedOperationException("ExecutionTimeline is a utility class");
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Marks the start of a named phase for the current thread.
     *
     * <p>If a phase with the same name is already in-flight, the start time is
     * reset — this matches the behaviour callers expect when a phase name is
     * reused across retries.</p>
     *
     * @param phaseName a short, human-readable label (e.g., {@code "page-load"})
     */
    public static void startPhase(final String phaseName) {
        IN_FLIGHT.get().put(phaseName, System.nanoTime());
    }

    /**
     * Marks the end of a named phase and records the elapsed nanoseconds.
     *
     * <p>If {@link #startPhase(String)} was never called for {@code phaseName},
     * this method silently ignores the call rather than throwing — defensive
     * behaviour is appropriate in an observability layer that must never break
     * the test under observation.</p>
     *
     * @param phaseName the same label passed to {@link #startPhase(String)}
     */
    public static void stopPhase(final String phaseName) {
        final Long startNanos = IN_FLIGHT.get().remove(phaseName);
        if (startNanos == null) {
            return; // never started — ignore silently
        }
        final long elapsed = System.nanoTime() - startNanos;
        COMPLETED.get().put(phaseName, new PhaseEntry(phaseName, elapsed));
    }

    /**
     * Returns an immutable, insertion-ordered snapshot of all completed phases
     * for the current thread.
     *
     * @return unmodifiable list of {@link PhaseEntry}; empty if no phases completed
     */
    public static List<PhaseEntry> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(COMPLETED.get().values()));
    }

    /**
     * Renders the completed phases as a waterfall-style text timeline.
     *
     * <p>Example output:</p>
     * <pre>
     * ─── ExecutionTimeline ───────────────────────
     *   driver-init     :    420 ms
     *   page-load       :   1234 ms
     *   login-action    :    310 ms
     *   assertion       :      5 ms
     * ─────────────────────────────────────────────
     * </pre>
     *
     * @return formatted timeline string; never {@code null}
     */
    public static String render() {
        final List<PhaseEntry> entries = snapshot();
        if (entries.isEmpty()) {
            return "─── ExecutionTimeline (empty) ───";
        }

        // Determine padding for phase name column
        int maxNameLen = 0;
        for (final PhaseEntry e : entries) {
            maxNameLen = Math.max(maxNameLen, e.getName().length());
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("─── ExecutionTimeline ──────────────────────────\n");
        for (final PhaseEntry e : entries) {
            final String name = String.format("%-" + maxNameLen + "s", e.getName());
            sb.append(String.format("  %s : %6d ms%n", name, e.getElapsedMillis()));
        }
        sb.append("────────────────────────────────────────────────");
        return sb.toString();
    }

    /**
     * Clears all in-flight and completed phases for the current thread.
     *
     * <p>Call this in {@code @AfterMethod} to prevent stale phase data from
     * appearing in the next test that runs on the same thread.</p>
     */
    public static void clear() {
        COMPLETED.remove();
        IN_FLIGHT.remove();
    }
}
