package com.omiinqa.resilience;

import com.omiinqa.exceptions.FrameworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

/**
 * Bulkhead concurrency limiter that prevents resource exhaustion under high parallelism.
 *
 * <p><strong>Design pattern:</strong> Bulkhead (from Michael Nygard's "Release It!").
 * Named after the watertight compartments in a ship hull: if one compartment floods it
 * doesn't sink the whole ship. In software, the bulkhead limits how many threads can
 * concurrently enter a resource-constrained section (e.g., a database connection pool,
 * a rate-limited API). Threads exceeding the limit are rejected immediately rather than
 * queued indefinitely, giving the caller a fast, predictable failure path.
 *
 * <p><strong>Implementation:</strong> Backed by a {@link Semaphore} with
 * {@code maxConcurrentCalls} permits. Uses a non-fair semaphore (FIFO not guaranteed)
 * for throughput. {@link Semaphore#tryAcquire()} is used (non-blocking) so the bulkhead
 * rejects rather than queues excess callers — queuing would defeat the purpose.
 *
 * <p>Usage:
 * <pre>{@code
 * Bulkhead bulkhead = Bulkhead.builder()
 *         .maxConcurrentCalls(5)
 *         .name("payment-gateway")
 *         .build();
 *
 * String result = bulkhead.execute(() -> callPaymentGateway());
 * }</pre>
 *
 * <p>This class is thread-safe and may be shared across parallel test threads. Inject a
 * single instance per resource boundary; do not create one per call.
 */
public final class Bulkhead {

    private static final Logger LOG = LoggerFactory.getLogger(Bulkhead.class);

    private final String name;
    private final int maxConcurrentCalls;
    private final Semaphore semaphore;

    private Bulkhead(final Builder builder) {
        this.name               = builder.name;
        this.maxConcurrentCalls = builder.maxConcurrentCalls;
        // Non-fair: maximises throughput by not enforcing FIFO ordering for waiting threads.
        this.semaphore          = new Semaphore(builder.maxConcurrentCalls, false);
    }

    // -------------------------------------------------------------------------
    // Execution
    // -------------------------------------------------------------------------

    /**
     * Executes the supplied operation if a concurrency permit is available.
     *
     * <p>The permit is acquired with {@link Semaphore#tryAcquire()} (non-blocking). If no
     * permit is available the call is rejected immediately with a {@link FrameworkException}.
     * The permit is always released in a {@code finally} block so a failing operation
     * never permanently reduces the available capacity.
     *
     * @param operation the operation to execute; must not be {@code null}
     * @param <T>       the return type
     * @return the value returned by {@code operation}
     * @throws FrameworkException if no concurrency permit is available (bulkhead full)
     *                             or if {@code operation} throws an exception
     */
    public <T> T execute(final Supplier<T> operation) {
        if (operation == null) {
            throw new FrameworkException("Operation must not be null");
        }

        final boolean acquired = semaphore.tryAcquire();
        if (!acquired) {
            final int available = semaphore.availablePermits();
            LOG.warn("Bulkhead '{}' rejected call — 0 permits available (max={})", name, maxConcurrentCalls);
            throw new FrameworkException(
                    "Bulkhead [" + name + "] is full — max concurrent calls ("
                            + maxConcurrentCalls + ") reached. Available permits: " + available);
        }

        LOG.debug("Bulkhead '{}' acquired permit ({} remaining)", name, semaphore.availablePermits());
        try {
            return operation.get();
        } catch (Exception ex) {
            throw new FrameworkException(
                    "Bulkhead [" + name + "] caught exception from operation: " + ex.getMessage(), ex);
        } finally {
            semaphore.release();
            LOG.debug("Bulkhead '{}' released permit ({} remaining)", name, semaphore.availablePermits());
        }
    }

    // -------------------------------------------------------------------------
    // Metrics / accessors
    // -------------------------------------------------------------------------

    /**
     * Returns the number of currently available concurrency permits.
     * A value of 0 means the bulkhead is fully saturated.
     *
     * @return available permits; always in range {@code [0, maxConcurrentCalls]}
     */
    public int availablePermits() {
        return semaphore.availablePermits();
    }

    /**
     * Returns the maximum concurrent calls this bulkhead allows.
     *
     * @return the configured capacity
     */
    public int getMaxConcurrentCalls() {
        return maxConcurrentCalls;
    }

    /**
     * Returns the human-readable name of this bulkhead (useful in log correlation).
     *
     * @return the name; never {@code null}
     */
    public String getName() {
        return name;
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    /**
     * Returns a new builder for {@link Bulkhead}.
     *
     * @return a fresh builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link Bulkhead}.
     */
    public static final class Builder {

        private String name = "default";
        private int maxConcurrentCalls = 10;

        private Builder() { }

        /**
         * Sets a human-readable name for this bulkhead (used in log and error messages).
         *
         * @param name must not be {@code null} or blank
         * @return this builder
         */
        public Builder name(final String name) {
            if (name == null || name.isBlank()) {
                throw new FrameworkException("Bulkhead name must not be blank");
            }
            this.name = name;
            return this;
        }

        /**
         * Sets the maximum number of concurrent calls this bulkhead will allow.
         *
         * @param maxConcurrentCalls must be &gt;= 1
         * @return this builder
         */
        public Builder maxConcurrentCalls(final int maxConcurrentCalls) {
            if (maxConcurrentCalls < 1) {
                throw new FrameworkException("maxConcurrentCalls must be >= 1");
            }
            this.maxConcurrentCalls = maxConcurrentCalls;
            return this;
        }

        /**
         * Builds the {@link Bulkhead}.
         *
         * @return a configured, thread-safe {@link Bulkhead}
         */
        public Bulkhead build() {
            return new Bulkhead(this);
        }
    }
}
