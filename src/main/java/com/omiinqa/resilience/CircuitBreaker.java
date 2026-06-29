package com.omiinqa.resilience;

import com.omiinqa.exceptions.FrameworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Circuit Breaker implementation with CLOSED / OPEN / HALF_OPEN state machine.
 *
 * <p><strong>Design pattern:</strong> Circuit Breaker (Michael Nygard / Fowler).
 * Prevents repeated calls to a failing dependency from exhausting thread pools
 * or cascading failures across service boundaries. Instead of waiting for each
 * timeout, the breaker fast-fails once it detects a surge of failures.
 *
 * <p><strong>State transitions:</strong>
 * <ul>
 *   <li>{@link State#CLOSED} (normal) → {@link State#OPEN} when consecutive failure
 *       count reaches {@code failureThreshold}.</li>
 *   <li>{@link State#OPEN} → {@link State#HALF_OPEN} after {@code resetTimeoutMs}
 *       milliseconds have elapsed (as measured by the injected clock).</li>
 *   <li>{@link State#HALF_OPEN} → {@link State#CLOSED} if the probe call succeeds;
 *       → {@link State#OPEN} if the probe call fails.</li>
 * </ul>
 *
 * <p><strong>Testability:</strong> The current-time function is injected via a
 * {@link LongSupplier} so tests can advance time deterministically without real
 * {@link Thread#sleep} calls. Pass {@code System::currentTimeMillis} in production.
 *
 * <p>Usage:
 * <pre>{@code
 * CircuitBreaker breaker = CircuitBreaker.builder()
 *         .failureThreshold(3)
 *         .resetTimeoutMs(5_000)
 *         .clock(System::currentTimeMillis)
 *         .build();
 *
 * String result = breaker.execute(() -> callRemoteService());
 * }</pre>
 *
 * <p>All state transitions are protected by an {@link AtomicReference} CAS loop,
 * making the breaker safe for use across parallel test threads.
 */
public final class CircuitBreaker {

    private static final Logger LOG = LoggerFactory.getLogger(CircuitBreaker.class);

    /**
     * The three states of the circuit breaker state machine.
     */
    public enum State {
        /** Normal operation — calls pass through. Failures increment the counter. */
        CLOSED,
        /** Fast-fail mode — all calls are rejected immediately. */
        OPEN,
        /** Single probe call is allowed to test whether the downstream has recovered. */
        HALF_OPEN
    }

    private final int failureThreshold;
    private final long resetTimeoutMs;
    private final LongSupplier clock;

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    /** Timestamp (from the injected clock) when the circuit was last opened. */
    private final AtomicLong openedAt = new AtomicLong(0L);

    private CircuitBreaker(final Builder builder) {
        this.failureThreshold = builder.failureThreshold;
        this.resetTimeoutMs   = builder.resetTimeoutMs;
        this.clock            = builder.clock;
    }

    // -------------------------------------------------------------------------
    // Execution
    // -------------------------------------------------------------------------

    /**
     * Executes the supplied operation according to the current circuit state.
     *
     * @param operation the operation to execute; must not be {@code null}
     * @param <T>       the result type
     * @return the value produced by {@code operation}
     * @throws FrameworkException if the circuit is OPEN or HALF_OPEN and the
     *                             operation fails, or if {@code operation} is null
     */
    public <T> T execute(final Supplier<T> operation) {
        if (operation == null) {
            throw new FrameworkException("Operation must not be null");
        }

        final State currentState = state.get();

        if (currentState == State.OPEN) {
            // Check whether the reset timeout has elapsed
            final long elapsed = clock.getAsLong() - openedAt.get();
            if (elapsed >= resetTimeoutMs) {
                if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                    LOG.debug("Circuit moving OPEN → HALF_OPEN after {}ms elapsed", elapsed);
                }
            } else {
                throw new FrameworkException(
                        "Circuit is OPEN — fast-failing call. "
                                + (resetTimeoutMs - elapsed) + "ms until half-open probe.");
            }
        }

        // CLOSED or HALF_OPEN: attempt the operation
        try {
            final T result = operation.get();
            onSuccess();
            return result;
        } catch (Exception ex) {
            onFailure(ex);
            throw new FrameworkException(
                    "CircuitBreaker caught exception: " + ex.getMessage(), ex);
        }
    }

    // -------------------------------------------------------------------------
    // State-machine transitions
    // -------------------------------------------------------------------------

    private void onSuccess() {
        final State current = state.get();
        if (current == State.HALF_OPEN) {
            // Probe succeeded — reset fully
            failureCount.set(0);
            state.set(State.CLOSED);
            LOG.debug("Circuit closed after successful HALF_OPEN probe.");
        } else if (current == State.CLOSED) {
            // Reset failure counter on any success in closed state
            failureCount.set(0);
        }
    }

    private void onFailure(final Exception ex) {
        final State current = state.get();
        if (current == State.HALF_OPEN) {
            // Probe failed — re-open
            openedAt.set(clock.getAsLong());
            state.set(State.OPEN);
            LOG.debug("HALF_OPEN probe failed — circuit re-opened: {}", ex.getMessage());
        } else if (current == State.CLOSED) {
            final int failures = failureCount.incrementAndGet();
            LOG.debug("Failure {} of {} threshold", failures, failureThreshold);
            if (failures >= failureThreshold) {
                openedAt.set(clock.getAsLong());
                state.set(State.OPEN);
                LOG.warn("Circuit opened after {} consecutive failures", failures);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Accessors (useful in tests)
    // -------------------------------------------------------------------------

    /**
     * Returns the current state of the circuit.
     *
     * @return the current {@link State}; never {@code null}
     */
    public State getState() { return state.get(); }

    /**
     * Returns the current consecutive failure count.
     *
     * @return failure count; always &gt;= 0
     */
    public int getFailureCount() { return failureCount.get(); }

    /**
     * Resets the circuit breaker to CLOSED state and clears the failure counter.
     * Intended for use between test cases where breaker state must not bleed across.
     */
    public void reset() {
        failureCount.set(0);
        openedAt.set(0L);
        state.set(State.CLOSED);
        LOG.debug("Circuit breaker manually reset to CLOSED.");
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    /**
     * Returns a new builder for {@link CircuitBreaker}.
     *
     * @return a fresh builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link CircuitBreaker}.
     */
    public static final class Builder {

        private int failureThreshold = 5;
        private long resetTimeoutMs = 30_000L;
        private LongSupplier clock = System::currentTimeMillis;

        private Builder() { }

        /**
         * Sets the number of consecutive failures required to open the circuit.
         *
         * @param threshold must be &gt;= 1
         * @return this builder
         */
        public Builder failureThreshold(final int threshold) {
            if (threshold < 1) {
                throw new FrameworkException("failureThreshold must be >= 1");
            }
            this.failureThreshold = threshold;
            return this;
        }

        /**
         * Sets the time that must pass (in milliseconds) before the breaker allows a
         * half-open probe after being opened.
         *
         * @param ms must be &gt;= 0
         * @return this builder
         */
        public Builder resetTimeoutMs(final long ms) {
            if (ms < 0) {
                throw new FrameworkException("resetTimeoutMs must be >= 0");
            }
            this.resetTimeoutMs = ms;
            return this;
        }

        /**
         * Injects the clock function used to measure elapsed time.
         * Use {@code System::currentTimeMillis} in production and a controllable
         * counter (e.g. {@code () -> virtualTime}) in tests.
         *
         * @param clock a {@link LongSupplier} returning the current time in milliseconds;
         *              must not be {@code null}
         * @return this builder
         */
        public Builder clock(final LongSupplier clock) {
            if (clock == null) {
                throw new FrameworkException("Clock supplier must not be null");
            }
            this.clock = clock;
            return this;
        }

        /**
         * Builds the {@link CircuitBreaker}.
         *
         * @return a configured, thread-safe {@link CircuitBreaker}
         */
        public CircuitBreaker build() {
            return new CircuitBreaker(this);
        }
    }
}
