package com.omiinqa.performance;

import org.assertj.core.api.Assertions;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fluent assertions over captured {@link PerformanceMetrics} against
 * {@link PerformanceBudget}s (performance-smoke layer).
 *
 * <p>Deliberately lightweight: navigation-timing budgets catch gross regressions
 * (a page that suddenly takes 12s) in the functional suite, without standing up a
 * dedicated performance tool. Heavier analysis (Lighthouse) is on the roadmap.</p>
 */
public final class PerformanceValidator {

    private static final Logger LOG = LoggerFactory.getLogger(PerformanceValidator.class);

    private final PerformanceMetrics metrics;

    private PerformanceValidator(final PerformanceMetrics metrics) {
        this.metrics = metrics;
    }

    public static PerformanceValidator of(final WebDriver driver) {
        final PerformanceMetrics m = PerformanceMetrics.capture(driver);
        LOG.info("Performance: load={}ms interactive={}ms ttfb={}ms",
                m.pageLoadMillis(), m.domInteractiveMillis(), m.timeToFirstByteMillis());
        return new PerformanceValidator(m);
    }

    public PerformanceMetrics metrics() {
        return metrics;
    }

    public PerformanceValidator within(final PerformanceBudget budget, final long actual) {
        Assertions.assertThat(budget.isWithin(actual))
                .as("%s %dms within budget %dms", budget.name(), actual, budget.maxMillis())
                .isTrue();
        return this;
    }

    public PerformanceValidator pageLoadWithin(final PerformanceBudget budget) {
        return within(budget, metrics.pageLoadMillis());
    }

    public PerformanceValidator domInteractiveWithin(final PerformanceBudget budget) {
        return within(budget, metrics.domInteractiveMillis());
    }

    public PerformanceValidator firstByteWithin(final PerformanceBudget budget) {
        return within(budget, metrics.timeToFirstByteMillis());
    }
}
