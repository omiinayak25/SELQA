package com.omiinqa.intelligence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Offline, deterministic flakiness detector for test suites.
 *
 * <h3>Role</h3>
 * <p>A "flaky" test is one that produces inconsistent outcomes (pass/fail) over
 * repeated runs against the same code. {@code FlakyTestDetector} analyses a
 * run-history (a list of {@code Boolean} pass/fail results per test) and
 * computes a <em>flakiness score</em> in the range {@code [0.0, 1.0]}. Tests
 * whose score exceeds a configurable threshold are flagged.</p>
 *
 * <h3>Algorithm — transition rate</h3>
 * <p>The score for a test with N runs is the number of pass↔fail transitions
 * divided by (N − 1). A perfectly stable test (all pass or all fail) scores
 * {@code 0.0}. A test that alternates on every run scores {@code 1.0}.</p>
 *
 * <pre>
 *   transitions = count of adjacent pairs where result[i] != result[i+1]
 *   score       = transitions / (runs - 1)
 * </pre>
 *
 * <p>A secondary guard requires the history to contain at least one pass AND at
 * least one fail — a test that always fails is not flaky, it is broken.</p>
 *
 * <h3>Fully offline</h3>
 * <p>No driver, no network, no external service. Pure statistical computation on
 * in-memory lists.</p>
 *
 * <h3>Thread safety</h3>
 * <p>Stateless — safe for concurrent use.</p>
 */
public final class FlakyTestDetector {

    private static final Logger log = LoggerFactory.getLogger(FlakyTestDetector.class);

    /** Default flakiness threshold: 20% transition rate is flagged. */
    public static final double DEFAULT_THRESHOLD = 0.20;

    /** Minimum number of runs required to classify a test as flaky. */
    public static final int MIN_RUNS = 3;

    private final double threshold;

    /**
     * Creates a detector with the default threshold ({@value #DEFAULT_THRESHOLD}).
     */
    public FlakyTestDetector() {
        this(DEFAULT_THRESHOLD);
    }

    /**
     * Creates a detector with a custom threshold.
     *
     * @param threshold flakiness score above which a test is flagged; must be
     *                  in {@code (0, 1]}
     * @throws IllegalArgumentException if threshold is out of range
     */
    public FlakyTestDetector(final double threshold) {
        if (threshold <= 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException(
                    "threshold must be in (0, 1]; got " + threshold);
        }
        this.threshold = threshold;
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Computes the flakiness score for a single test's run history.
     *
     * <ul>
     *   <li>Returns {@code 0.0} if {@code results} is {@code null}, empty, or
     *       has fewer than {@value #MIN_RUNS} entries.</li>
     *   <li>Returns {@code 0.0} for a history of all passes or all fails — a
     *       consistently failing test is <em>broken</em>, not flaky.</li>
     * </ul>
     *
     * @param results ordered list of pass ({@code true}) / fail ({@code false})
     *                results, from oldest to newest
     * @return flakiness score in {@code [0.0, 1.0]}
     */
    public double score(final List<Boolean> results) {
        if (results == null || results.size() < MIN_RUNS) {
            return 0.0;
        }

        // Must have mixed outcomes to be flaky (not just broken)
        final boolean hasPass = results.stream().anyMatch(Boolean.TRUE::equals);
        final boolean hasFail = results.stream().anyMatch(Boolean.FALSE::equals);
        if (!hasPass || !hasFail) {
            return 0.0;
        }

        int transitions = 0;
        for (int i = 1; i < results.size(); i++) {
            if (!Objects.equals(results.get(i), results.get(i - 1))) {
                transitions++;
            }
        }

        return (double) transitions / (results.size() - 1);
    }

    /**
     * Returns {@code true} if the given run history is flaky according to the
     * configured threshold.
     *
     * @param results ordered pass/fail history
     * @return {@code true} if {@link #score(List)} exceeds the threshold
     */
    public boolean isFlaky(final List<Boolean> results) {
        return score(results) > threshold;
    }

    /**
     * Analyses a map of test names to their run histories and returns a summary
     * of all tests, sorted by flakiness score descending.
     *
     * @param histories map of test name → ordered pass/fail run history
     * @return an unmodifiable map of test name → {@link FlakinessReport},
     *         ordered by score descending; never {@code null}
     */
    public Map<String, FlakinessReport> analyseAll(final Map<String, List<Boolean>> histories) {
        if (histories == null || histories.isEmpty()) {
            return Collections.emptyMap();
        }

        final List<Map.Entry<String, List<Boolean>>> entries =
                new java.util.ArrayList<>(histories.entrySet());

        // Sort by score descending
        entries.sort((a, b) -> Double.compare(score(b.getValue()), score(a.getValue())));

        final Map<String, FlakinessReport> result = new LinkedHashMap<>();
        for (final Map.Entry<String, List<Boolean>> entry : entries) {
            final double s = score(entry.getValue());
            final boolean flagged = s > threshold;
            result.put(entry.getKey(), new FlakinessReport(entry.getKey(), s, flagged, entry.getValue().size()));
            if (flagged) {
                log.warn("FlakyTestDetector: test '{}' flagged as FLAKY (score={}, threshold={})",
                        entry.getKey(), String.format("%.3f", s), String.format("%.3f", threshold));
            }
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Returns only the tests that are flagged as flaky from the given histories.
     *
     * @param histories map of test name → ordered pass/fail run history
     * @return unmodifiable map of flagged test name → {@link FlakinessReport}
     */
    public Map<String, FlakinessReport> flaggedTests(final Map<String, List<Boolean>> histories) {
        final Map<String, FlakinessReport> all = analyseAll(histories);
        final Map<String, FlakinessReport> flagged = new LinkedHashMap<>();
        all.forEach((name, report) -> {
            if (report.isFlagged()) {
                flagged.put(name, report);
            }
        });
        return Collections.unmodifiableMap(flagged);
    }

    /** Returns the configured flakiness threshold. */
    public double getThreshold() {
        return threshold;
    }

    // =========================================================================
    // Report value object
    // =========================================================================

    /**
     * Immutable summary for a single test's flakiness analysis.
     */
    public static final class FlakinessReport {

        private final String testName;
        private final double flakinessScore;
        private final boolean flagged;
        private final int runCount;

        FlakinessReport(final String testName,
                        final double flakinessScore,
                        final boolean flagged,
                        final int runCount) {
            this.testName = testName;
            this.flakinessScore = flakinessScore;
            this.flagged = flagged;
            this.runCount = runCount;
        }

        /** The test identifier. */
        public String getTestName() { return testName; }

        /** Flakiness score in {@code [0.0, 1.0]}. */
        public double getFlakinessScore() { return flakinessScore; }

        /** {@code true} if the score exceeds the configured threshold. */
        public boolean isFlagged() { return flagged; }

        /** Total number of runs included in the analysis. */
        public int getRunCount() { return runCount; }

        @Override
        public String toString() {
            return String.format("FlakinessReport{test='%s', score=%.3f, flagged=%b, runs=%d}",
                    testName, flakinessScore, flagged, runCount);
        }
    }
}
