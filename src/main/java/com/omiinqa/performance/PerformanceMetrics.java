package com.omiinqa.performance;

import com.omiinqa.utils.JavaScriptUtils;
import org.openqa.selenium.WebDriver;

/**
 * Reads browser Navigation Timing metrics via the W3C Performance API.
 *
 * <p>A value object captured from {@code window.performance.timing}. All values
 * are milliseconds relative to {@code navigationStart}; {@code -1} means the
 * metric was unavailable. Used by the performance-smoke layer to assert page
 * responsiveness against {@link PerformanceBudget}s without a heavyweight tool.</p>
 */
public record PerformanceMetrics(long pageLoadMillis,
                                 long domInteractiveMillis,
                                 long domContentLoadedMillis,
                                 long timeToFirstByteMillis,
                                 long responseTimeMillis) {

    private static final String SCRIPT = """
            var t = window.performance.timing;
            return {
              load: t.loadEventEnd - t.navigationStart,
              interactive: t.domInteractive - t.navigationStart,
              contentLoaded: t.domContentLoadedEventEnd - t.navigationStart,
              ttfb: t.responseStart - t.navigationStart,
              response: t.responseEnd - t.requestStart
            };
            """;

    /** Capture current navigation timings from the live page. */
    @SuppressWarnings("unchecked")
    public static PerformanceMetrics capture(final WebDriver driver) {
        final Object raw = JavaScriptUtils.execute(driver, SCRIPT);
        if (raw instanceof java.util.Map<?, ?> map) {
            return new PerformanceMetrics(
                    asLong(map.get("load")),
                    asLong(map.get("interactive")),
                    asLong(map.get("contentLoaded")),
                    asLong(map.get("ttfb")),
                    asLong(map.get("response")));
        }
        return new PerformanceMetrics(-1, -1, -1, -1, -1);
    }

    private static long asLong(final Object value) {
        return value instanceof Number n ? n.longValue() : -1L;
    }
}
