package com.omiinqa.performance;

/**
 * A named performance budget — the maximum acceptable duration (ms) for a
 * timing metric. Budgets make performance assertions explicit and tunable
 * rather than scattering magic numbers through tests.
 *
 * @param name          human-readable metric name (e.g. "page-load")
 * @param maxMillis     upper bound in milliseconds
 */
public record PerformanceBudget(String name, long maxMillis) {

    /** Sensible default budgets for smoke-level page performance. */
    public static final PerformanceBudget PAGE_LOAD = new PerformanceBudget("page-load", 5_000);
    public static final PerformanceBudget DOM_INTERACTIVE = new PerformanceBudget("dom-interactive", 3_000);
    public static final PerformanceBudget FIRST_BYTE = new PerformanceBudget("ttfb", 1_500);

    public PerformanceBudget {
        if (maxMillis <= 0) {
            throw new IllegalArgumentException("Budget must be positive: " + name);
        }
    }

    public boolean isWithin(final long actualMillis) {
        return actualMillis >= 0 && actualMillis <= maxMillis;
    }
}
