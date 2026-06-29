package com.omiinqa.observability;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PrometheusExporter} — verifies that the Prometheus text
 * exposition format is produced correctly for each metric type. Fully offline.
 */
@Test(groups = {"observability", "unit"})
public class PrometheusExporterTest {

    @BeforeMethod(alwaysRun = true)
    public void before() {
        MetricRegistry.reset();
    }

    @AfterMethod(alwaysRun = true)
    public void cleanup() {
        MetricRegistry.reset();
    }

    // -------------------------------------------------------------------------
    // Counter exposition
    // -------------------------------------------------------------------------

    @Test(groups = {"observability", "unit"})
    public void counterProducesHelpLine() {
        MetricRegistry.incrementCounter("api.calls.total", 5);
        final String output = PrometheusExporter.export(MetricRegistry.snapshot());
        assertThat(output).contains("# HELP api_calls_total");
    }

    @Test(groups = {"observability", "unit"})
    public void counterProducesTypeLine() {
        MetricRegistry.incrementCounter("api.calls.total", 5);
        final String output = PrometheusExporter.export(MetricRegistry.snapshot());
        assertThat(output).contains("# TYPE api_calls_total counter");
    }

    @Test(groups = {"observability", "unit"})
    public void counterProducesValueLine() {
        MetricRegistry.incrementCounter("api.calls.total", 5);
        final String output = PrometheusExporter.export(MetricRegistry.snapshot());
        assertThat(output).contains("api_calls_total 5.0");
    }

    // -------------------------------------------------------------------------
    // Gauge exposition
    // -------------------------------------------------------------------------

    @Test(groups = {"observability", "unit"})
    public void gaugeProducesHelpLine() {
        MetricRegistry.setGauge("drivers.active", 3);
        final String output = PrometheusExporter.export(MetricRegistry.snapshot());
        assertThat(output).contains("# HELP drivers_active");
    }

    @Test(groups = {"observability", "unit"})
    public void gaugeProducesTypeLine() {
        MetricRegistry.setGauge("drivers.active", 3);
        final String output = PrometheusExporter.export(MetricRegistry.snapshot());
        assertThat(output).contains("# TYPE drivers_active gauge");
    }

    @Test(groups = {"observability", "unit"})
    public void gaugeProducesValueLine() {
        MetricRegistry.setGauge("drivers.active", 3);
        final String output = PrometheusExporter.export(MetricRegistry.snapshot());
        assertThat(output).contains("drivers_active 3.0");
    }

    // -------------------------------------------------------------------------
    // Timer exposition
    // -------------------------------------------------------------------------

    @Test(groups = {"observability", "unit"})
    public void timerProducesSummaryTypeLine() {
        MetricRegistry.recordTimer("login.page.ms", 200_000_000L);
        final String output = PrometheusExporter.export(MetricRegistry.snapshot());
        assertThat(output).contains("# TYPE login_page_ms summary");
    }

    @Test(groups = {"observability", "unit"})
    public void timerProducesCountLine() {
        MetricRegistry.recordTimer("login.page.ms", 200_000_000L);
        MetricRegistry.recordTimer("login.page.ms", 300_000_000L);
        final String output = PrometheusExporter.export(MetricRegistry.snapshot());
        assertThat(output).contains("login_page_ms_count 2.0");
    }

    @Test(groups = {"observability", "unit"})
    public void timerProducesSumLine() {
        MetricRegistry.recordTimer("login.page.ms", 200_000_000L);
        final String output = PrometheusExporter.export(MetricRegistry.snapshot());
        // 200_000_000 ns = 200 ms
        assertThat(output).contains("login_page_ms_sum 200.000");
    }

    @Test(groups = {"observability", "unit"})
    public void timerProducesAvgMsLine() {
        MetricRegistry.recordTimer("login.page.ms", 100_000_000L);
        MetricRegistry.recordTimer("login.page.ms", 300_000_000L);
        final String output = PrometheusExporter.export(MetricRegistry.snapshot());
        // avg = (100 + 300) / 2 = 200 ms
        assertThat(output).contains("login_page_ms_avg_ms 200.000");
    }

    // -------------------------------------------------------------------------
    // Name sanitisation
    // -------------------------------------------------------------------------

    @Test(groups = {"observability", "unit"})
    public void dotsInNameAreSanitised() {
        assertThat(PrometheusExporter.sanitise("a.b.c")).isEqualTo("a_b_c");
    }

    @Test(groups = {"observability", "unit"})
    public void hyphensInNameAreSanitised() {
        assertThat(PrometheusExporter.sanitise("my-metric-name")).isEqualTo("my_metric_name");
    }

    // -------------------------------------------------------------------------
    // Empty snapshot
    // -------------------------------------------------------------------------

    @Test(groups = {"observability", "unit"})
    public void emptySnapshotProducesEmptyString() {
        final String output = PrometheusExporter.export(MetricRegistry.snapshot());
        assertThat(output).isEmpty();
    }
}
