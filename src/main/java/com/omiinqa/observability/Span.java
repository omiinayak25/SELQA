package com.omiinqa.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A single unit of work within a distributed trace, modelled on the
 * <a href="https://opentelemetry.io/docs/concepts/signals/traces/#spans">OpenTelemetry Span</a>
 * concept but implemented without any OTel SDK dependency.
 *
 * <p><strong>Why this exists:</strong> Selenium test suites orchestrate multiple
 * sub-operations (navigation, form fill, API call, DB assertion) within a single logical
 * test. Without tracing, diagnosing a 10-second slow test means guessing which step was
 * responsible. A {@code Span} wraps each sub-operation: when it closes, the elapsed time
 * is recorded to {@link MetricRegistry} and emitted to the SLF4J log with the active
 * {@link CorrelationId} — giving a causal chain from the log line back to the metric.</p>
 *
 * <p><strong>OTel compatibility note:</strong> This API intentionally mirrors the OTel
 * Java SDK's {@code Span} interface (name, start/end, parent context via a stack).
 * When the organisation is ready to adopt the OTel SDK, a bridge implementation of this
 * interface can delegate to {@code io.opentelemetry.api.trace.Span} with no changes to
 * existing call sites. The Javadoc comment {@code @OtelBridgeable} on each method marks
 * the OTel equivalent.</p>
 *
 * <p>Instances are obtained from {@link Tracer#startSpan(String)} — do not construct
 * directly.</p>
 *
 * <p><strong>Usage (try-with-resources):</strong></p>
 * <pre>{@code
 * try (Span span = Tracer.startSpan("login-page-load")) {
 *     loginPage.open();
 * }
 * // Span is closed automatically; duration logged + recorded to MetricRegistry
 * }</pre>
 */
public final class Span implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Span.class);

    private final String name;
    private final long startNanos;
    private final String correlationId;
    private boolean closed = false;

    /**
     * Package-private — created only by {@link Tracer#startSpan(String)}.
     *
     * @param name          the operation name (e.g., {@code "login-action"})
     * @param correlationId the active correlation ID at span creation time
     */
    Span(final String name, final String correlationId) {
        this.name = name;
        this.correlationId = correlationId;
        this.startNanos = System.nanoTime();
    }

    /**
     * Returns the operation name of this span.
     *
     * <p>OTel equivalent: {@code Span.getName()}</p>
     *
     * @return span name; never {@code null}
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the nanosecond timestamp at which this span was started.
     *
     * @return {@link System#nanoTime()} value at construction time
     */
    public long getStartNanos() {
        return startNanos;
    }

    /**
     * Ends this span, records the elapsed nanoseconds to {@link MetricRegistry},
     * and logs a structured line including the active {@link CorrelationId}.
     *
     * <p>Calling {@code close()} more than once is a no-op — safe for use in
     * finally blocks alongside try-with-resources.</p>
     *
     * <p>OTel equivalent: {@code Span.end()}</p>
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;

        final long elapsedNanos = System.nanoTime() - startNanos;
        final long elapsedMs = elapsedNanos / 1_000_000L;
        final String metricName = "span." + name.replace('-', '.').replace(' ', '.');

        MetricRegistry.recordTimer(metricName, elapsedNanos);

        LOG.info("[SPAN] correlationId={} span={} durationMs={}", correlationId, name, elapsedMs);

        // Notify Tracer to pop this span from the thread's stack
        Tracer.onSpanClosed(this);
    }

    /**
     * Returns whether this span has already been closed.
     *
     * @return {@code true} if {@link #close()} has been called at least once
     */
    public boolean isClosed() {
        return closed;
    }

    @Override
    public String toString() {
        return "Span{name='" + name + "', correlationId='" + correlationId + "', closed=" + closed + '}';
    }
}
