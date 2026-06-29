package com.omiinqa.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Factory and lifecycle manager for {@link Span} instances, providing a minimal
 * distributed-tracing abstraction that is OTel-shaped but dependency-free.
 *
 * <p><strong>Why this exists:</strong> OpenTelemetry is the industry standard for
 * distributed tracing, but pulling its SDK into a Selenium test framework adds ~10 MB of
 * transitive dependencies and requires a running collector. {@code Tracer} provides the
 * same conceptual model — named spans, parent-child nesting via a thread-local stack,
 * automatic duration recording — using only the JDK and the SLF4J bridge already present
 * in this framework. When the organisation is ready to adopt OTel, this class can be
 * replaced with a thin adapter over {@code io.opentelemetry.api.trace.Tracer} without
 * touching any existing test code.</p>
 *
 * <p><strong>Nesting model:</strong> Spans are pushed onto a per-thread {@link Deque}
 * (a stack). When a span is started, the top of the stack is its logical parent.
 * When it closes, it is popped, restoring the parent as the active span. This mirrors
 * OTel's context propagation model without requiring a {@code Context} object.</p>
 *
 * <p><strong>Thread safety:</strong> The span stack is {@link ThreadLocal} — each thread
 * maintains its own stack independently. {@link MetricRegistry} calls inside {@link Span#close()}
 * are themselves thread-safe (see {@link MetricRegistry} docs).</p>
 *
 * <p><strong>OTel bridge note:</strong> A future adapter could implement
 * {@code io.opentelemetry.api.trace.Tracer} and delegate {@code spanBuilder(name).startSpan()}
 * to this class, with the OTel {@code Context} stored in the same ThreadLocal stack.</p>
 *
 * <p><strong>Usage:</strong></p>
 * <pre>{@code
 * // Simple span
 * try (Span span = Tracer.startSpan("load-products-page")) {
 *     productsPage.open();
 * }
 *
 * // Nested spans
 * try (Span outer = Tracer.startSpan("checkout-flow")) {
 *     try (Span inner = Tracer.startSpan("fill-shipping-form")) {
 *         checkoutPage.fillShipping(address);
 *     } // inner closes here
 * }   // outer closes here
 * }</pre>
 */
public final class Tracer {

    private static final Logger LOG = LoggerFactory.getLogger(Tracer.class);

    /**
     * Per-thread stack of open spans.
     *
     * <p>{@link ArrayDeque} is used as a stack (LIFO) via {@link Deque#push} and
     * {@link Deque#pop}. It is more efficient than {@link java.util.Stack} which
     * inherits unnecessary synchronisation from {@link java.util.Vector}.</p>
     */
    private static final ThreadLocal<Deque<Span>> STACK =
            ThreadLocal.withInitial(ArrayDeque::new);

    /** Utility class — no instances. */
    private Tracer() {
        throw new UnsupportedOperationException("Tracer is a utility class");
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Starts a new {@link Span} for the current thread and pushes it onto the
     * span stack, making it the current active span.
     *
     * <p>The span is linked to the active {@link CorrelationId} at the time of
     * this call. If no correlation ID has been started, the span still records
     * correctly — it will carry the ID value {@code "unset"}.</p>
     *
     * <p>OTel equivalent:
     * {@code tracer.spanBuilder(name).startSpan()}</p>
     *
     * @param name a short, human-readable operation name (e.g., {@code "login-submit"})
     * @return a new, open {@link Span}; the caller is responsible for closing it
     *         (typically via try-with-resources)
     */
    public static Span startSpan(final String name) {
        final String cid = CorrelationId.get();
        final Deque<Span> stack = STACK.get();
        final Span parent = stack.peek(); // may be null (top-level span)

        if (parent != null) {
            LOG.debug("[SPAN] Starting child span '{}' under parent '{}' cid={}", name, parent.getName(), cid);
        } else {
            LOG.debug("[SPAN] Starting root span '{}' cid={}", name, cid);
        }

        final Span span = new Span(name, cid);
        stack.push(span);
        return span;
    }

    /**
     * Returns the currently active span on this thread, or {@code null} if no
     * span is open.
     *
     * <p>OTel equivalent: {@code Span.current()}</p>
     *
     * @return the most recently started, unclosed {@link Span}, or {@code null}
     */
    public static Span currentSpan() {
        return STACK.get().peek();
    }

    /**
     * Returns the depth of the span stack for the current thread.
     *
     * <p>A depth of 0 means no spans are open; depth 1 means a single root span;
     * depth 2 means a child span is open inside a root span, and so on.</p>
     *
     * @return current nesting depth (0 = no active spans)
     */
    public static int currentDepth() {
        return STACK.get().size();
    }

    /**
     * Clears all open spans from the current thread's stack without closing them.
     *
     * <p>Call this in {@code @AfterMethod} as a safety net in case a test aborted
     * mid-span — it prevents stale spans from polluting the next test on the same
     * thread. Note: durations are NOT recorded for force-cleared spans.</p>
     */
    public static void clearStack() {
        STACK.get().clear();
    }

    // -------------------------------------------------------------------------
    // Package-private — called by Span.close()
    // -------------------------------------------------------------------------

    /**
     * Removes a closed span from the thread's stack.
     *
     * <p>Called by {@link Span#close()} as the last step of span completion.
     * This keeps the stack consistent even if spans are closed out of order
     * (which should not happen in well-written try-with-resources code but
     * may occur in error paths).</p>
     *
     * @param span the span that was just closed
     */
    static void onSpanClosed(final Span span) {
        final Deque<Span> stack = STACK.get();
        if (!stack.isEmpty() && stack.peek() == span) {
            stack.pop();
        } else {
            // Out-of-order close — remove wherever it appears
            stack.remove(span);
            LOG.warn("[SPAN] Out-of-order close detected for span '{}'", span.getName());
        }
    }
}
