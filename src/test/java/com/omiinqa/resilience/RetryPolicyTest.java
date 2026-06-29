package com.omiinqa.resilience;

import com.omiinqa.exceptions.FrameworkException;
import org.testng.annotations.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link RetryPolicy}.
 *
 * <p>All tests are offline and deterministic: sleepAction is always a no-op so there
 * are no real delays. Groups: {@code resilience}, {@code unit}.
 */
@Test(groups = {"resilience", "unit"})
public class RetryPolicyTest {

    /** No-op sleep: prevents any real Thread.sleep in tests. */
    private static final java.util.function.LongConsumer NO_OP_SLEEP = millis -> { };

    // -------------------------------------------------------------------------
    // Successful execution
    // -------------------------------------------------------------------------

    @Test(description = "execute returns value immediately when operation succeeds on first attempt")
    public void execute_returnsValue_onFirstAttemptSuccess() {
        final RetryPolicy<String> policy = RetryPolicy.<String>builder()
                .maxAttempts(3)
                .sleepAction(NO_OP_SLEEP)
                .build();

        final String result = policy.execute(() -> "hello");
        assertThat(result).isEqualTo("hello");
    }

    @Test(description = "execute succeeds after N failures followed by a success")
    public void execute_succeeds_afterNFailures() {
        final AtomicInteger attempts = new AtomicInteger(0);

        final RetryPolicy<String> policy = RetryPolicy.<String>builder()
                .maxAttempts(4)
                .delay(Duration.ofMillis(10))
                .sleepAction(NO_OP_SLEEP)
                .build();

        final String result = policy.execute(() -> {
            if (attempts.incrementAndGet() < 3) {
                throw new RuntimeException("transient failure");
            }
            return "recovered";
        });

        assertThat(result).isEqualTo("recovered");
        assertThat(attempts.get()).isEqualTo(3);
    }

    // -------------------------------------------------------------------------
    // Exhaustion
    // -------------------------------------------------------------------------

    @Test(description = "execute throws FrameworkException when all attempts are exhausted")
    public void execute_throwsFrameworkException_whenAllAttemptsExhausted() {
        final RetryPolicy<String> policy = RetryPolicy.<String>builder()
                .maxAttempts(3)
                .sleepAction(NO_OP_SLEEP)
                .build();

        assertThatThrownBy(() -> policy.execute(() -> {
            throw new RuntimeException("always fails");
        }))
                .isInstanceOf(FrameworkException.class)
                .hasMessageContaining("attempts exhausted");
    }

    @Test(description = "attempt counter reaches maxAttempts before giving up")
    public void execute_makesExactlyMaxAttempts_beforeExhaustion() {
        final AtomicInteger callCount = new AtomicInteger(0);

        final RetryPolicy<Void> policy = RetryPolicy.<Void>builder()
                .maxAttempts(5)
                .sleepAction(NO_OP_SLEEP)
                .build();

        assertThatThrownBy(() -> policy.execute(() -> {
            callCount.incrementAndGet();
            throw new RuntimeException("fail");
        })).isInstanceOf(FrameworkException.class);

        assertThat(callCount.get()).isEqualTo(5);
    }

    // -------------------------------------------------------------------------
    // retryOn predicate
    // -------------------------------------------------------------------------

    @Test(description = "non-retryable exception is not retried and is re-thrown immediately")
    public void execute_doesNotRetry_forNonRetryableException() {
        final AtomicInteger callCount = new AtomicInteger(0);

        final RetryPolicy<Void> policy = RetryPolicy.<Void>builder()
                .maxAttempts(5)
                .retryOn(ex -> ex instanceof IOException)
                .sleepAction(NO_OP_SLEEP)
                .build();

        assertThatThrownBy(() -> policy.execute(() -> {
            callCount.incrementAndGet();
            throw new RuntimeException("non-retryable");
        })).isInstanceOf(FrameworkException.class)
                .hasMessageContaining("Non-retryable");

        // Only 1 attempt because the exception type is not retryable
        assertThat(callCount.get()).isEqualTo(1);
    }

    // -------------------------------------------------------------------------
    // Backoff sequence (deterministic delay verification)
    // -------------------------------------------------------------------------

    @Test(description = "FIXED backoff produces the same delay on every attempt")
    public void computeDelay_fixed_returnsSameDelay() {
        final RetryPolicy<Void> policy = RetryPolicy.<Void>builder()
                .maxAttempts(4)
                .delay(Duration.ofMillis(200))
                .backoff(RetryPolicy.BackoffMode.FIXED)
                .sleepAction(NO_OP_SLEEP)
                .build();

        assertThat(policy.computeDelay(1)).isEqualTo(200L);
        assertThat(policy.computeDelay(2)).isEqualTo(200L);
        assertThat(policy.computeDelay(3)).isEqualTo(200L);
    }

    @Test(description = "EXPONENTIAL backoff doubles delay on each attempt")
    public void computeDelay_exponential_doublesOnEachAttempt() {
        final RetryPolicy<Void> policy = RetryPolicy.<Void>builder()
                .maxAttempts(5)
                .delay(Duration.ofMillis(100))
                .backoff(RetryPolicy.BackoffMode.EXPONENTIAL)
                .sleepAction(NO_OP_SLEEP)
                .build();

        // attempt 1 → 100ms, 2 → 200ms, 3 → 400ms, 4 → 800ms
        assertThat(policy.computeDelay(1)).isEqualTo(100L);
        assertThat(policy.computeDelay(2)).isEqualTo(200L);
        assertThat(policy.computeDelay(3)).isEqualTo(400L);
        assertThat(policy.computeDelay(4)).isEqualTo(800L);
    }

    @Test(description = "sleep is called with the correct delay on each retry")
    public void execute_callsSleep_withCorrectDelay() {
        final List<Long> recordedDelays = new ArrayList<>();

        final RetryPolicy<String> policy = RetryPolicy.<String>builder()
                .maxAttempts(3)
                .delay(Duration.ofMillis(50))
                .backoff(RetryPolicy.BackoffMode.FIXED)
                .sleepAction(recordedDelays::add)
                .build();

        final AtomicInteger count = new AtomicInteger(0);
        policy.execute(() -> {
            if (count.incrementAndGet() < 3) {
                throw new RuntimeException("retry me");
            }
            return "done";
        });

        // 2 retries → 2 sleep calls
        assertThat(recordedDelays).hasSize(2).containsOnly(50L);
    }

    // -------------------------------------------------------------------------
    // Builder validation
    // -------------------------------------------------------------------------

    @Test(description = "Builder throws FrameworkException when maxAttempts is zero")
    public void builder_throwsException_forZeroMaxAttempts() {
        assertThatThrownBy(() -> RetryPolicy.builder().maxAttempts(0))
                .isInstanceOf(FrameworkException.class);
    }

    @Test(description = "Builder throws FrameworkException when delay is negative")
    public void builder_throwsException_forNegativeDelay() {
        assertThatThrownBy(() -> RetryPolicy.builder().delay(Duration.ofMillis(-1)))
                .isInstanceOf(FrameworkException.class);
    }
}
