package com.omiinqa.resilience;

import com.omiinqa.exceptions.FrameworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Opt-in deterministic fault-injection utility for resilience testing.
 *
 * <p><strong>Purpose:</strong> Chaos engineering verifies that the system under test (and
 * the test framework itself) behaves correctly under adverse conditions. This class
 * injects latency or thrown exceptions into code paths in a <em>fully deterministic</em>
 * manner so test results are reproducible across CI runs.
 *
 * <p><strong>Design pattern:</strong> Decorator / Strategy with an injected counter.
 * Determinism is achieved by using a monotonically-incrementing {@link AtomicInteger}
 * call counter and a configurable {@code injectionEvery} divisor rather than
 * {@link Math#random()}. A fault fires when {@code callCount % injectionEvery == 0}.
 *
 * <p><strong>No real sleeping in tests:</strong> Latency injection delegates to an
 * injected {@code latencyAction} ({@code Consumer<Long>} receiving millis). Substitute a
 * no-op consumer in unit tests and a real {@link Thread#sleep} wrapper in integration tests.
 *
 * <p>Usage:
 * <pre>{@code
 * // Inject a fault every 3rd call; no real sleep (unit test mode).
 * ChaosInjector injector = ChaosInjector.builder()
 *         .injectionEvery(3)
 *         .fault(() -> { throw new RuntimeException("simulated outage"); })
 *         .latencyAction(millis -> {})   // no-op in tests
 *         .latencyMs(100)
 *         .build();
 *
 * String result = injector.maybe(() -> callService(), "service-call");
 * }</pre>
 *
 * <p>This class is clearly a <strong>test utility</strong> and must only be used in
 * {@code src/test} scoped code or explicitly guarded by a feature flag in staging.
 */
public final class ChaosInjector {

    private static final Logger LOG = LoggerFactory.getLogger(ChaosInjector.class);

    /** Monotonic call counter — the sole source of determinism. */
    private final AtomicInteger callCounter;

    /**
     * A fault fires every N-th call ({@code counter % injectionEvery == 0}).
     * Value of {@link Integer#MAX_VALUE} effectively disables injection.
     */
    private final int injectionEvery;

    /** The fault to inject (typically throws an exception). */
    private final Runnable fault;

    /** The latency action; receives milliseconds to simulate. */
    private final Consumer<Long> latencyAction;

    /** Nominal latency to inject when a call triggers the latency path. */
    private final long latencyMs;

    /**
     * When {@code true} the injector injects latency before the fault;
     * when {@code false} it only injects the fault.
     */
    private final boolean injectLatencyFirst;

    private ChaosInjector(final Builder builder) {
        this.callCounter       = new AtomicInteger(0);
        this.injectionEvery    = builder.injectionEvery;
        this.fault             = builder.fault;
        this.latencyAction     = builder.latencyAction;
        this.latencyMs         = builder.latencyMs;
        this.injectLatencyFirst = builder.injectLatencyFirst;
    }

    // -------------------------------------------------------------------------
    // Core API
    // -------------------------------------------------------------------------

    /**
     * Conditionally injects a fault or latency around the given operation.
     *
     * <p>On every N-th call (N = {@code injectionEvery}) the configured fault action
     * is executed before the operation runs. On all other calls the operation runs
     * normally.
     *
     * @param operation   the operation to conditionally corrupt; must not be {@code null}
     * @param description human-readable label for logging (e.g. {@code "checkout-api"})
     * @param <T>         the return type
     * @return the result of {@code operation.get()}
     * @throws RuntimeException as configured in {@link Builder#fault(Runnable)} when
     *                          injection fires
     */
    public <T> T maybe(final Supplier<T> operation, final String description) {
        if (operation == null) {
            throw new FrameworkException("Operation must not be null");
        }
        final int count = callCounter.incrementAndGet();

        if (injectionEvery != Integer.MAX_VALUE && count % injectionEvery == 0) {
            LOG.debug("ChaosInjector triggering fault on call #{} for '{}'", count, description);
            if (injectLatencyFirst) {
                injectLatency();
            }
            fault.run(); // typically throws; fall-through means latency-only injection
        }

        return operation.get();
    }

    /**
     * Overload of {@link #maybe(Supplier, String)} that runs the operation without a
     * fault check — just executes it. Provided for symmetry with void callers.
     *
     * @param operation   the operation to run
     * @param description human-readable label
     */
    public void maybe(final Runnable operation, final String description) {
        maybe(() -> { operation.run(); return null; }, description);
    }

    /**
     * Directly injects the configured latency without running any operation.
     * Useful for wrapping a call site where only latency is wanted.
     */
    public void injectLatency() {
        LOG.debug("ChaosInjector injecting latency of {}ms", latencyMs);
        latencyAction.accept(latencyMs);
    }

    /**
     * Directly fires the configured fault without running any operation.
     * Useful for targeted fault injection at a known call site.
     */
    public void injectFault() {
        LOG.debug("ChaosInjector injecting fault directly");
        fault.run();
    }

    /**
     * Returns the total number of calls that have passed through {@link #maybe}.
     *
     * @return call count; always &gt;= 0
     */
    public int getCallCount() {
        return callCounter.get();
    }

    /**
     * Resets the internal call counter to zero. Useful between independent test cases
     * that share a {@code ChaosInjector} instance.
     */
    public void resetCounter() {
        callCounter.set(0);
        LOG.debug("ChaosInjector call counter reset");
    }

    /**
     * Returns the configured injection frequency (every N-th call).
     *
     * @return the injection divisor
     */
    public int getInjectionEvery() {
        return injectionEvery;
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    /**
     * Returns a new builder for {@link ChaosInjector}.
     *
     * @return a fresh builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link ChaosInjector}.
     */
    public static final class Builder {

        private int injectionEvery = Integer.MAX_VALUE; // disabled by default
        private Runnable fault = () -> {
            throw new FrameworkException("ChaosInjector: simulated fault");
        };
        private Consumer<Long> latencyAction = millis -> {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        };
        private long latencyMs = 0L;
        private boolean injectLatencyFirst = false;

        private Builder() { }

        /**
         * Sets how often the fault fires: every {@code n}-th call.
         * A value of 1 means every call; 2 means every other call, etc.
         *
         * @param n must be &gt;= 1
         * @return this builder
         */
        public Builder injectionEvery(final int n) {
            if (n < 1) {
                throw new FrameworkException("injectionEvery must be >= 1");
            }
            this.injectionEvery = n;
            return this;
        }

        /**
         * Sets the fault to inject. Typically a {@link Runnable} that throws an exception.
         *
         * @param fault must not be {@code null}
         * @return this builder
         */
        public Builder fault(final Runnable fault) {
            if (fault == null) {
                throw new FrameworkException("fault Runnable must not be null");
            }
            this.fault = fault;
            return this;
        }

        /**
         * Overrides the latency action (default: {@link Thread#sleep}).
         * Pass a no-op ({@code millis -> {}}) for unit tests.
         *
         * @param latencyAction a {@code Consumer<Long>} receiving milliseconds
         * @return this builder
         */
        public Builder latencyAction(final Consumer<Long> latencyAction) {
            if (latencyAction == null) {
                throw new FrameworkException("latencyAction must not be null");
            }
            this.latencyAction = latencyAction;
            return this;
        }

        /**
         * Sets the nominal latency in milliseconds to inject.
         *
         * @param ms must be &gt;= 0
         * @return this builder
         */
        public Builder latencyMs(final long ms) {
            if (ms < 0) {
                throw new FrameworkException("latencyMs must be >= 0");
            }
            this.latencyMs = ms;
            return this;
        }

        /**
         * When set to {@code true}, latency is injected before the fault fires on
         * triggered calls. Default is {@code false} (fault only).
         *
         * @param injectLatencyFirst {@code true} to inject latency before the fault
         * @return this builder
         */
        public Builder injectLatencyFirst(final boolean injectLatencyFirst) {
            this.injectLatencyFirst = injectLatencyFirst;
            return this;
        }

        /**
         * Builds the {@link ChaosInjector}.
         *
         * @return a configured {@link ChaosInjector} instance
         */
        public ChaosInjector build() {
            return new ChaosInjector(this);
        }
    }
}
