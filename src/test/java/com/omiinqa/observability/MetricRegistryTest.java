package com.omiinqa.observability;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MetricRegistry} — verifies counter, gauge, and timer semantics
 * plus snapshot immutability and concurrent correctness. Fully offline.
 */
@Test(groups = {"observability", "unit"})
public class MetricRegistryTest {

    @AfterMethod(alwaysRun = true)
    public void cleanup() {
        MetricRegistry.reset();
    }

    // -------------------------------------------------------------------------
    // Counter tests
    // -------------------------------------------------------------------------

    @Test(groups = {"observability", "unit"})
    public void newCounterStartsAtZeroInSnapshot() {
        MetricRegistry.incrementCounter("c1");
        // After one increment it should be 1, not start at 0 — this checks the before state
        MetricRegistry.reset();
        assertThat(MetricRegistry.snapshot().counters()).doesNotContainKey("c1");
    }

    @Test(groups = {"observability", "unit"})
    public void singleIncrementByOneRecorded() {
        MetricRegistry.incrementCounter("api.calls");
        assertThat(MetricRegistry.snapshot().counters().get("api.calls")).isEqualTo(1L);
    }

    @Test(groups = {"observability", "unit"})
    public void incrementByDeltaAccumulates() {
        MetricRegistry.incrementCounter("errors", 3);
        MetricRegistry.incrementCounter("errors", 7);
        assertThat(MetricRegistry.snapshot().counters().get("errors")).isEqualTo(10L);
    }

    @Test(groups = {"observability", "unit"})
    public void multipleCountersAreIndependent() {
        MetricRegistry.incrementCounter("x", 5);
        MetricRegistry.incrementCounter("y", 2);
        final MetricRegistry.Snapshot snap = MetricRegistry.snapshot();
        assertThat(snap.counters().get("x")).isEqualTo(5L);
        assertThat(snap.counters().get("y")).isEqualTo(2L);
    }

    @Test(groups = {"observability", "unit"})
    public void counterIsSafeUnderConcurrency() throws InterruptedException {
        final int threads = 10;
        final int incrementsPerThread = 100;
        final CountDownLatch latch = new CountDownLatch(threads);
        final ExecutorService executor = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    MetricRegistry.incrementCounter("concurrent.counter");
                }
                latch.countDown();
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(MetricRegistry.snapshot().counters().get("concurrent.counter"))
                .isEqualTo((long) threads * incrementsPerThread);
    }

    // -------------------------------------------------------------------------
    // Gauge tests
    // -------------------------------------------------------------------------

    @Test(groups = {"observability", "unit"})
    public void setGaugeRecordsAbsoluteValue() {
        MetricRegistry.setGauge("drivers.active", 4);
        assertThat(MetricRegistry.snapshot().gauges().get("drivers.active")).isEqualTo(4L);
    }

    @Test(groups = {"observability", "unit"})
    public void setGaugeOverwritesPreviousValue() {
        MetricRegistry.setGauge("queue.size", 10);
        MetricRegistry.setGauge("queue.size", 7);
        assertThat(MetricRegistry.snapshot().gauges().get("queue.size")).isEqualTo(7L);
    }

    @Test(groups = {"observability", "unit"})
    public void adjustGaugeIncrementsAndDecrements() {
        MetricRegistry.setGauge("g", 10);
        MetricRegistry.adjustGauge("g", -3);
        assertThat(MetricRegistry.snapshot().gauges().get("g")).isEqualTo(7L);
        MetricRegistry.adjustGauge("g", 5);
        assertThat(MetricRegistry.snapshot().gauges().get("g")).isEqualTo(12L);
    }

    // -------------------------------------------------------------------------
    // Timer tests
    // -------------------------------------------------------------------------

    @Test(groups = {"observability", "unit"})
    public void singleTimerSampleRecorded() {
        MetricRegistry.recordTimer("login.ms", 500_000_000L); // 500 ms in nanos
        final MetricRegistry.TimerSnapshot t = MetricRegistry.snapshot().timers().get("login.ms");
        assertThat(t).isNotNull();
        assertThat(t.getCount()).isEqualTo(1L);
        assertThat(t.getTotalNanos()).isEqualTo(500_000_000L);
    }

    @Test(groups = {"observability", "unit"})
    public void timerAverageMsComputedCorrectly() {
        // Record two samples: 100 ms and 300 ms in nanos
        MetricRegistry.recordTimer("page.load", 100_000_000L);
        MetricRegistry.recordTimer("page.load", 300_000_000L);
        final MetricRegistry.TimerSnapshot t = MetricRegistry.snapshot().timers().get("page.load");
        assertThat(t.getCount()).isEqualTo(2L);
        assertThat(t.getAvgMs()).isCloseTo(200.0, org.assertj.core.data.Offset.offset(0.001));
    }

    // -------------------------------------------------------------------------
    // Snapshot immutability
    // -------------------------------------------------------------------------

    @Test(groups = {"observability", "unit"})
    public void snapshotCountersMapIsUnmodifiable() {
        MetricRegistry.incrementCounter("k");
        final MetricRegistry.Snapshot snap = MetricRegistry.snapshot();
        try {
            snap.counters().put("injected", 99L);
        } catch (final UnsupportedOperationException expected) {
            // correct — immutable
        }
        assertThat(MetricRegistry.snapshot().counters()).doesNotContainKey("injected");
    }

    // -------------------------------------------------------------------------
    // Reset
    // -------------------------------------------------------------------------

    @Test(groups = {"observability", "unit"})
    public void resetClearsAllMetrics() {
        MetricRegistry.incrementCounter("r1");
        MetricRegistry.setGauge("r2", 5);
        MetricRegistry.recordTimer("r3", 1000L);
        MetricRegistry.reset();
        final MetricRegistry.Snapshot snap = MetricRegistry.snapshot();
        assertThat(snap.counters()).isEmpty();
        assertThat(snap.gauges()).isEmpty();
        assertThat(snap.timers()).isEmpty();
    }
}
