package com.omiinqa.observability;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Tracer} and {@link Span} — verifies span lifecycle, nesting,
 * duration recording in {@link MetricRegistry}, correlation ID propagation, and
 * stack cleanup. Fully offline.
 */
@Test(groups = {"observability", "unit"})
public class TracerTest {

    @BeforeMethod(alwaysRun = true)
    public void before() {
        CorrelationId.start();
        MetricRegistry.reset();
        Tracer.clearStack();
    }

    @AfterMethod(alwaysRun = true)
    public void cleanup() {
        Tracer.clearStack();
        CorrelationId.clear();
        MetricRegistry.reset();
    }

    // -------------------------------------------------------------------------
    // Basic span lifecycle
    // -------------------------------------------------------------------------

    @Test(groups = {"observability", "unit"})
    public void startSpanReturnsOpenSpan() {
        final Span span = Tracer.startSpan("test-op");
        try {
            assertThat(span).isNotNull();
            assertThat(span.isClosed()).isFalse();
        } finally {
            span.close();
        }
    }

    @Test(groups = {"observability", "unit"})
    public void spanNameMatchesInput() {
        try (final Span span = Tracer.startSpan("my-operation")) {
            assertThat(span.getName()).isEqualTo("my-operation");
        }
    }

    @Test(groups = {"observability", "unit"})
    public void spanIsClosedAfterTryWithResources() {
        Span span;
        try (final Span s = Tracer.startSpan("twr-test")) {
            span = s;
        }
        assertThat(span.isClosed()).isTrue();
    }

    @Test(groups = {"observability", "unit"})
    public void closeIsIdempotent() {
        final Span span = Tracer.startSpan("idempotent-close");
        span.close();
        // Second close must not throw or double-record
        span.close();
        assertThat(span.isClosed()).isTrue();
    }

    // -------------------------------------------------------------------------
    // Duration recorded to MetricRegistry
    // -------------------------------------------------------------------------

    @Test(groups = {"observability", "unit"})
    public void closedSpanRecordsDurationToMetricRegistry() {
        try (final Span ignored = Tracer.startSpan("login-action")) {
            // nothing
        }
        // metric name: span.login.action (hyphens -> dots in Span.close)
        final MetricRegistry.TimerSnapshot timer =
                MetricRegistry.snapshot().timers().get("span.login.action");
        assertThat(timer).isNotNull();
        assertThat(timer.getCount()).isEqualTo(1L);
        assertThat(timer.getTotalNanos()).isPositive();
    }

    @Test(groups = {"observability", "unit"})
    public void multipleSpansRecordIndependentTimers() {
        try (final Span ignored = Tracer.startSpan("phase-one")) { /* empty */ }
        try (final Span ignored = Tracer.startSpan("phase-two")) { /* empty */ }

        final MetricRegistry.Snapshot snap = MetricRegistry.snapshot();
        assertThat(snap.timers()).containsKey("span.phase.one");
        assertThat(snap.timers()).containsKey("span.phase.two");
    }

    @Test(groups = {"observability", "unit"})
    public void repeatedSpanNameAccumulatesSamples() {
        try (final Span ignored = Tracer.startSpan("repeated")) { /* empty */ }
        try (final Span ignored = Tracer.startSpan("repeated")) { /* empty */ }

        final MetricRegistry.TimerSnapshot timer =
                MetricRegistry.snapshot().timers().get("span.repeated");
        assertThat(timer.getCount()).isEqualTo(2L);
    }

    // -------------------------------------------------------------------------
    // Nesting / stack behaviour
    // -------------------------------------------------------------------------

    @Test(groups = {"observability", "unit"})
    public void nestedSpansProduceCorrectDepth() {
        try (final Span outer = Tracer.startSpan("outer")) {
            assertThat(Tracer.currentDepth()).isEqualTo(1);
            try (final Span inner = Tracer.startSpan("inner")) {
                assertThat(Tracer.currentDepth()).isEqualTo(2);
            }
            // After inner closes, depth returns to 1
            assertThat(Tracer.currentDepth()).isEqualTo(1);
        }
        assertThat(Tracer.currentDepth()).isEqualTo(0);
    }

    @Test(groups = {"observability", "unit"})
    public void currentSpanReturnsActiveSpan() {
        try (final Span span = Tracer.startSpan("active-check")) {
            assertThat(Tracer.currentSpan()).isSameAs(span);
        }
    }

    @Test(groups = {"observability", "unit"})
    public void currentSpanIsNullWhenNoSpanOpen() {
        assertThat(Tracer.currentSpan()).isNull();
    }

    @Test(groups = {"observability", "unit"})
    public void afterAllNestedSpansCloseDepthIsZero() {
        try (final Span a = Tracer.startSpan("a")) {
            try (final Span b = Tracer.startSpan("b")) {
                try (final Span c = Tracer.startSpan("c")) {
                    assertThat(Tracer.currentDepth()).isEqualTo(3);
                }
            }
        }
        assertThat(Tracer.currentDepth()).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // CorrelationId propagation
    // -------------------------------------------------------------------------

    @Test(groups = {"observability", "unit"})
    public void spanCarriesActiveCorrelationId() {
        final String cid = CorrelationId.get();
        final Span span = Tracer.startSpan("cid-test");
        try {
            // The span's toString must contain the active correlation ID
            assertThat(span.toString()).contains(cid);
        } finally {
            span.close();
        }
    }

    // -------------------------------------------------------------------------
    // clearStack
    // -------------------------------------------------------------------------

    @Test(groups = {"observability", "unit"})
    public void clearStackResetsDepthToZero() {
        Tracer.startSpan("leaked-span"); // intentionally not closed
        Tracer.clearStack();
        assertThat(Tracer.currentDepth()).isEqualTo(0);
    }
}
