package com.omiinqa.observability;

import java.util.Locale;
import java.util.Map;

/**
 * Renders a {@link MetricRegistry} snapshot in the Prometheus text exposition format
 * so the output can be scraped by a Prometheus server and visualised in Grafana.
 *
 * <p><strong>Why this exists:</strong> Prometheus's pull-based scrape model is the
 * industry standard for time-series metrics in CI pipelines. By producing text in the
 * <a href="https://prometheus.io/docs/instrumenting/exposition_formats/#text-based-format">
 * Prometheus text format</a>, this class makes OmiinQA test-execution metrics (API call
 * counts, page-load durations, pass/fail gauges) available to any Prometheus+Grafana
 * or VictoriaMetrics stack with zero additional dependencies — the output is plain UTF-8
 * text that can be written to a file, served from an embedded HTTP endpoint, or pushed
 * to a Pushgateway.</p>
 *
 * <p><strong>Format produced (per metric type):</strong></p>
 * <ul>
 *   <li><strong>Counter</strong>:
 *       <pre>
 * # HELP api_calls_total Number of API calls made
 * # TYPE api_calls_total counter
 * api_calls_total 42.0
 *       </pre>
 *   </li>
 *   <li><strong>Gauge</strong>:
 *       <pre>
 * # HELP drivers_active Current number of active WebDriver instances
 * # TYPE drivers_active gauge
 * drivers_active 3.0
 *       </pre>
 *   </li>
 *   <li><strong>Timer</strong> (exported as Prometheus {@code summary} with
 *       {@code _count} and {@code _sum} suffixes, consistent with the OTel
 *       histogram bridge convention):
 *       <pre>
 * # HELP page_login_ms Login page duration in milliseconds
 * # TYPE page_login_ms summary
 * page_login_ms_count 5.0
 * page_login_ms_sum 1600.0
 * page_login_ms_avg_ms 320.0
 *       </pre>
 *   </li>
 * </ul>
 *
 * <p><strong>Metric name sanitisation:</strong> Prometheus metric names must match
 * {@code [a-zA-Z_:][a-zA-Z0-9_:]*}. This class converts dots and hyphens to underscores
 * to ensure all names emitted by the framework are valid.</p>
 *
 * <p>This class is stateless and thread-safe; each call to {@link #export(MetricRegistry.Snapshot)}
 * creates a new string from the snapshot provided.</p>
 */
public final class PrometheusExporter {

    /** Utility class — no instances. */
    private PrometheusExporter() {
        throw new UnsupportedOperationException("PrometheusExporter is a utility class");
    }

    /**
     * Converts a {@link MetricRegistry.Snapshot} to a Prometheus text exposition string.
     *
     * <p>The returned string always ends with a newline as required by the specification.
     * If the snapshot contains no metrics, the method returns an empty string rather than
     * a header-only document, to avoid confusing Prometheus parsers.</p>
     *
     * @param snapshot the metric snapshot to export; must not be {@code null}
     * @return Prometheus text format string; never {@code null}
     */
    public static String export(final MetricRegistry.Snapshot snapshot) {
        final StringBuilder sb = new StringBuilder();

        // --- Counters ---
        for (final Map.Entry<String, Long> entry : snapshot.counters().entrySet()) {
            final String metricName = sanitise(entry.getKey());
            final long value = entry.getValue();
            sb.append("# HELP ").append(metricName).append(" Counter metric\n");
            sb.append("# TYPE ").append(metricName).append(" counter\n");
            sb.append(metricName).append(' ').append(formatDouble(value)).append('\n');
        }

        // --- Gauges ---
        for (final Map.Entry<String, Long> entry : snapshot.gauges().entrySet()) {
            final String metricName = sanitise(entry.getKey());
            final long value = entry.getValue();
            sb.append("# HELP ").append(metricName).append(" Gauge metric\n");
            sb.append("# TYPE ").append(metricName).append(" gauge\n");
            sb.append(metricName).append(' ').append(formatDouble(value)).append('\n');
        }

        // --- Timers (exported as summary) ---
        for (final Map.Entry<String, MetricRegistry.TimerSnapshot> entry : snapshot.timers().entrySet()) {
            final String metricName = sanitise(entry.getKey());
            final MetricRegistry.TimerSnapshot timer = entry.getValue();
            sb.append("# HELP ").append(metricName).append(" Timer metric (milliseconds)\n");
            sb.append("# TYPE ").append(metricName).append(" summary\n");
            sb.append(metricName).append("_count ").append(formatDouble(timer.getCount())).append('\n');
            sb.append(metricName).append("_sum ").append(
                    String.format(Locale.ROOT, "%.3f", timer.getTotalNanos() / 1_000_000.0)).append('\n');
            sb.append(metricName).append("_avg_ms ").append(
                    String.format(Locale.ROOT, "%.3f", timer.getAvgMs())).append('\n');
        }

        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Sanitises a metric name to comply with the Prometheus naming convention
     * ({@code [a-zA-Z_:][a-zA-Z0-9_:]*}).
     *
     * <p>Dots and hyphens are replaced with underscores since they are the most
     * common non-conforming characters used in Java metric names.</p>
     */
    static String sanitise(final String rawName) {
        return rawName.replace('.', '_').replace('-', '_');
    }

    /** Formats a long as a Prometheus floating-point literal (e.g., {@code 42.0}). */
    private static String formatDouble(final long value) {
        return String.format(Locale.ROOT, "%.1f", (double) value);
    }
}
