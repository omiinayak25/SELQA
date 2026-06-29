package com.omiinqa.reports.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Aggregate result for a complete TestNG suite run.
 *
 * <p>Produced by {@link ResultsAggregator#buildResult(org.testng.ITestContext)} after
 * all tests in a suite have completed. Carries rolled-up counters, the ordered list of
 * individual {@link TestCaseResult} objects, freeform environment metadata (e.g.
 * browser name, target environment), and wall-clock timestamps supplied by the caller.
 *
 * <p><strong>Timestamp contract:</strong> {@code startedAt} and {@code finishedAt} are
 * epoch-millisecond values passed in by the aggregator — they are <em>never</em>
 * computed inside this class. This keeps the value object side-effect-free and
 * trivially testable with deterministic fixtures.</p>
 *
 * <p>Typical usage:
 * <pre>
 *   TestRunResult run = TestRunResult.builder()
 *       .suiteName("Regression")
 *       .totalPassed(42).totalFailed(1).totalSkipped(2)
 *       .totalDurationMs(120_000L)
 *       .testCases(aggregator.getSnapshot())
 *       .environmentMetadata(Map.of("browser", "chrome", "env", "staging"))
 *       .startedAt(startEpoch)
 *       .finishedAt(finishEpoch)
 *       .build();
 * </pre>
 * </p>
 *
 * @see TestCaseResult
 * @see ResultsAggregator
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestRunResult {

    /** TestNG suite name, sourced from {@code ITestContext.getName()}. */
    private String suiteName;

    /** Count of tests that ended with {@code ITestResult.SUCCESS}. */
    private int totalPassed;

    /** Count of tests that ended with {@code ITestResult.FAILURE}. */
    private int totalFailed;

    /** Count of tests that ended with {@code ITestResult.SKIP}. */
    private int totalSkipped;

    /**
     * Sum of all individual {@link TestCaseResult#getDurationMs()} values,
     * or equivalently {@code finishedAt - startedAt}.
     */
    private long totalDurationMs;

    /** Ordered list of per-test results collected during the run. */
    private List<TestCaseResult> testCases;

    /**
     * Arbitrary key-value pairs describing the execution environment.
     * Populated from TestNG XML {@code &lt;parameter&gt;} elements when available.
     * Examples: {@code browser=chrome}, {@code env=staging}.
     */
    private Map<String, String> environmentMetadata;

    /**
     * Epoch milliseconds at which {@link ResultsAggregator#onStart} was called.
     * Set by the aggregator; never computed here.
     */
    private long startedAt;

    /**
     * Epoch milliseconds at which {@link ResultsAggregator#buildResult} was called.
     * Set by the aggregator; never computed here.
     */
    private long finishedAt;
}
