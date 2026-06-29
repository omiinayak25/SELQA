package com.omiinqa.resilience;

import com.omiinqa.exceptions.FrameworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Deterministic functional retry with configurable backoff and retry-on predicate.
 *
 * <p><strong>Design pattern:</strong> Decorator / Strategy. The caller supplies the
 * operation as a {@link Supplier} and the retry policy is composed separately from the
 * operation itself, keeping retry logic out of business/test code.
 *
 * <p><strong>Backoff modes:</strong>
 * <ul>
 *   <li>{@link BackoffMode#FIXED} — the same delay on every retry attempt.</li>
 *   <li>{@link BackoffMode#EXPONENTIAL} — delay doubles on each attempt:
 *       {@code delay × 2^(attempt-1)}.</li>
 * </ul>
 *
 * <p><strong>Testability:</strong> The sleep behaviour is provided via an injected
 * {@code sleepAction} ({@code LongConsumer} receiving millis), so tests can substitute a
 * no-op consumer and assert the computed delays without any real {@link Thread#sleep}.
 *
 * <p>Usage:
 * <pre>{@code
 * RetryPolicy<String> policy = RetryPolicy.<String>builder()
 *         .maxAttempts(3)
 *         .delay(Duration.ofMillis(500))
 *         .backoff(RetryPolicy.BackoffMode.EXPONENTIAL)
 *         .retryOn(ex -> ex instanceof IOException)
 *         .sleepAction(millis -> {}) // no-op in unit tests
 *         .build();
 *
 * String result = policy.execute(() -> riskyOperation());
 * }</pre>
 *
 * <p>If all attempts are exhausted the last exception is re-thrown, wrapped in a
 * {@link FrameworkException}.
 */
public final class RetryPolicy<T> {

    private static final Logger LOG = LoggerFactory.getLogger(RetryPolicy.class);

    /**
     * Controls how the inter-retry delay grows across attempts.
     */
    public enum BackoffMode {
        /** Same delay for every retry. */
        FIXED,
        /** Delay doubles on each subsequent retry attempt. */
        EXPONENTIAL
    }

    private final int maxAttempts;
    private final long baseDelayMs;
    private final BackoffMode backoffMode;
    private final Predicate<Exception> retryOn;
    private final java.util.function.LongConsumer sleepAction;

    private RetryPolicy(final Builder<T> builder) {
        this.maxAttempts  = builder.maxAttempts;
        this.baseDelayMs  = builder.baseDelayMs;
        this.backoffMode  = builder.backoffMode;
        this.retryOn      = builder.retryOn;
        this.sleepAction  = builder.sleepAction;
    }

    // -------------------------------------------------------------------------
    // Execution
    // -------------------------------------------------------------------------

    /**
     * Executes the supplied operation, retrying on eligible exceptions up to
     * {@link #getMaxAttempts()} times.
     *
     * @param operation the operation to execute; must not be {@code null}
     * @return the value returned by the operation on a successful attempt
     * @throws FrameworkException wrapping the last exception if all attempts fail,
     *                             or if the exception type is not retryable
     */
    public T execute(final Supplier<T> operation) {
        if (operation == null) {
            throw new FrameworkException("Operation must not be null");
        }

        Exception lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                LOG.debug("RetryPolicy attempt {}/{}", attempt, maxAttempts);
                return operation.get();
            } catch (Exception ex) {
                lastException = ex;
                if (!retryOn.test(ex)) {
                    LOG.debug("Exception not retryable, giving up: {}", ex.getMessage());
                    throw new FrameworkException(
                            "Non-retryable exception on attempt " + attempt + ": " + ex.getMessage(), ex);
                }
                if (attempt < maxAttempts) {
                    final long delay = computeDelay(attempt);
                    LOG.debug("Attempt {} failed ({}), sleeping {}ms before retry",
                            attempt, ex.getMessage(), delay);
                    sleepAction.accept(delay);
                }
            }
        }

        throw new FrameworkException(
                "All " + maxAttempts + " attempts exhausted. Last error: "
                        + (lastException != null ? lastException.getMessage() : "unknown"),
                lastException);
    }

    /**
     * Computes the delay (in milliseconds) before the next attempt.
     *
     * @param completedAttempts the number of attempts that have already run (1-based)
     * @return the delay in milliseconds
     */
    long computeDelay(final int completedAttempts) {
        if (backoffMode == BackoffMode.EXPONENTIAL) {
            // 2^(completedAttempts-1) × baseDelay, capped at Long.MAX_VALUE / 2 to avoid overflow
            final long multiplier = 1L << Math.min(completedAttempts - 1, 62);
            return baseDelayMs * multiplier;
        }
        return baseDelayMs;
    }

    // -------------------------------------------------------------------------
    // Accessors (used by tests)
    // -------------------------------------------------------------------------

    /** @return the maximum number of attempts this policy will make. */
    public int getMaxAttempts() { return maxAttempts; }

    /** @return the base delay in milliseconds. */
    public long getBaseDelayMs() { return baseDelayMs; }

    /** @return the configured backoff mode. */
    public BackoffMode getBackoffMode() { return backoffMode; }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    /**
     * Returns a new builder for {@code RetryPolicy<T>}.
     *
     * @param <T> the return type of the operation
     * @return a fresh builder
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * Fluent builder for {@link RetryPolicy}.
     *
     * @param <T> the return type of the operation being wrapped
     */
    public static final class Builder<T> {

        private int maxAttempts = 3;
        private long baseDelayMs = 500L;
        private BackoffMode backoffMode = BackoffMode.FIXED;
        private Predicate<Exception> retryOn = ex -> true;
        private java.util.function.LongConsumer sleepAction = millis -> {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new FrameworkException("Retry sleep interrupted", ie);
            }
        };

        private Builder() { }

        /**
         * Sets the maximum number of attempts (including the first attempt).
         *
         * @param maxAttempts must be &gt;= 1
         * @return this builder
         */
        public Builder<T> maxAttempts(final int maxAttempts) {
            if (maxAttempts < 1) {
                throw new FrameworkException("maxAttempts must be >= 1");
            }
            this.maxAttempts = maxAttempts;
            return this;
        }

        /**
         * Sets the base delay between retry attempts.
         *
         * @param delay must not be {@code null} or negative
         * @return this builder
         */
        public Builder<T> delay(final Duration delay) {
            if (delay == null || delay.isNegative()) {
                throw new FrameworkException("Delay must not be null or negative");
            }
            this.baseDelayMs = delay.toMillis();
            return this;
        }

        /**
         * Sets the backoff mode (fixed or exponential).
         *
         * @param mode the mode; must not be {@code null}
         * @return this builder
         */
        public Builder<T> backoff(final BackoffMode mode) {
            if (mode == null) {
                throw new FrameworkException("BackoffMode must not be null");
            }
            this.backoffMode = mode;
            return this;
        }

        /**
         * Sets the predicate that decides whether an exception is retryable.
         * Default: all exceptions are retried.
         *
         * @param predicate a {@link Predicate} over the caught exception
         * @return this builder
         */
        public Builder<T> retryOn(final Predicate<Exception> predicate) {
            if (predicate == null) {
                throw new FrameworkException("retryOn predicate must not be null");
            }
            this.retryOn = predicate;
            return this;
        }

        /**
         * Overrides the sleep action (default: {@link Thread#sleep}).
         * Inject a no-op ({@code millis -> {}}) in unit tests to avoid real delays.
         *
         * @param sleepAction a {@code LongConsumer} receiving the sleep duration in ms
         * @return this builder
         */
        public Builder<T> sleepAction(final java.util.function.LongConsumer sleepAction) {
            if (sleepAction == null) {
                throw new FrameworkException("sleepAction must not be null");
            }
            this.sleepAction = sleepAction;
            return this;
        }

        /**
         * Builds the {@link RetryPolicy}.
         *
         * @return a configured, immutable {@link RetryPolicy}
         */
        public RetryPolicy<T> build() {
            return new RetryPolicy<>(this);
        }
    }
}
