package com.omiinqa.reports.export;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TestNG {@link ITestListener} that aggregates per-test outcomes into a
 * {@link TestRunResult} for downstream export.
 *
 * <h2>Observer Pattern</h2>
 * <p>This class is registered as a TestNG listener (observer). The TestNG engine
 * (subject) calls the {@code onTest*} callbacks as each test transitions through
 * its lifecycle. {@code ResultsAggregator} records those transitions without
 * knowing which specific tests will run, satisfying the open/closed principle.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>TestNG can invoke listener callbacks concurrently in parallel-method mode.
 * All mutable state uses thread-safe primitives:
 * <ul>
 *   <li>{@link CopyOnWriteArrayList} for the result list (reads dominate once a
 *       suite is done)</li>
 *   <li>{@link AtomicInteger} for the three counters</li>
 *   <li>{@code volatile} for the start timestamp</li>
 * </ul>
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>
 *   // testng.xml
 *   &lt;listeners&gt;
 *     &lt;listener class-name="com.omiinqa.reports.export.ResultsAggregator"/&gt;
 *   &lt;/listeners&gt;
 *
 *   // Programmatic access (e.g. from a suite-finish listener):
 *   TestRunResult run = aggregator.buildResult(ctx);
 *   new ReportExporterFacade("reports/export").exportAll(run);
 * </pre>
 *
 * @see TestRunResult
 * @see ReportExporterFacade
 */
public class ResultsAggregator implements ITestListener {

    private static final Logger LOG = LoggerFactory.getLogger(ResultsAggregator.class);

    private final CopyOnWriteArrayList<TestCaseResult> results = new CopyOnWriteArrayList<>();
    private final AtomicInteger passed  = new AtomicInteger(0);
    private final AtomicInteger failed  = new AtomicInteger(0);
    private final AtomicInteger skipped = new AtomicInteger(0);

    /** Epoch millis recorded when {@link #onStart(ITestContext)} fires. */
    private volatile long startedAt = 0L;

    /**
     * Records the suite start timestamp.
     *
     * @param context the TestNG context for the suite being started
     */
    @Override
    public void onStart(final ITestContext context) {
        startedAt = System.currentTimeMillis();
        LOG.info("ResultsAggregator: suite '{}' started at {}", context.getName(), startedAt);
    }

    /**
     * Increments the passed counter and records a {@code PASSED} result.
     *
     * @param result the TestNG result for the passing test
     */
    @Override
    public void onTestSuccess(final ITestResult result) {
        passed.incrementAndGet();
        results.add(buildCase(result, "PASSED", null));
        LOG.debug("PASSED: {}", result.getName());
    }

    /**
     * Increments the failed counter and records a {@code FAILED} result,
     * capturing the throwable message when present.
     *
     * @param result the TestNG result for the failing test
     */
    @Override
    public void onTestFailure(final ITestResult result) {
        failed.incrementAndGet();
        final String errorMsg = extractError(result);
        results.add(buildCase(result, "FAILED", errorMsg));
        LOG.warn("FAILED: {} — {}", result.getName(), errorMsg);
    }

    /**
     * Increments the skipped counter and records a {@code SKIPPED} result.
     *
     * @param result the TestNG result for the skipped test
     */
    @Override
    public void onTestSkipped(final ITestResult result) {
        skipped.incrementAndGet();
        results.add(buildCase(result, "SKIPPED", null));
        LOG.debug("SKIPPED: {}", result.getName());
    }

    /**
     * Builds a {@link TestRunResult} snapshot from current aggregated state.
     *
     * <p>This method is safe to call from {@code onFinish(ITestContext)} or
     * any point after the suite completes. The returned object captures the
     * state at the moment of the call; subsequent test callbacks will not
     * retroactively change the snapshot.</p>
     *
     * @param ctx the TestNG context, used to extract suite name and XML parameters
     * @return a fully populated {@link TestRunResult}
     */
    public TestRunResult buildResult(final ITestContext ctx) {
        final long finishedAt = System.currentTimeMillis();
        final long totalDurationMs = finishedAt - startedAt;

        return TestRunResult.builder()
                .suiteName(ctx.getName())
                .totalPassed(passed.get())
                .totalFailed(failed.get())
                .totalSkipped(skipped.get())
                .totalDurationMs(totalDurationMs)
                .testCases(new ArrayList<>(results))
                .environmentMetadata(extractEnvMetadata(ctx))
                .startedAt(startedAt)
                .finishedAt(finishedAt)
                .build();
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private static TestCaseResult buildCase(final ITestResult result,
                                            final String status,
                                            final String errorMessage) {
        final long duration = result.getEndMillis() - result.getStartMillis();
        return TestCaseResult.builder()
                .name(result.getName())
                .className(result.getTestClass().getName())
                .status(status)
                .durationMs(duration)
                .errorMessage(errorMessage)
                .build();
    }

    private static String extractError(final ITestResult result) {
        if (result.getThrowable() != null) {
            final String msg = result.getThrowable().getMessage();
            return (msg != null && !msg.isEmpty()) ? msg : result.getThrowable().getClass().getSimpleName();
        }
        return null;
    }

    private static Map<String, String> extractEnvMetadata(final ITestContext ctx) {
        try {
            if (ctx.getCurrentXmlTest() != null) {
                final Map<String, String> params = ctx.getCurrentXmlTest().getAllParameters();
                if (params != null && !params.isEmpty()) {
                    return Collections.unmodifiableMap(new HashMap<>(params));
                }
            }
        } catch (final Exception ex) {
            LOG.debug("Could not read XML test parameters: {}", ex.getMessage());
        }
        return Collections.emptyMap();
    }
}
