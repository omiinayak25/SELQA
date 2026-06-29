package com.omiinqa.resilience;

import com.omiinqa.exceptions.FrameworkException;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link CircuitBreaker}.
 *
 * <p>Uses an injected clock ({@link AtomicLong} counter) to control time advancement
 * deterministically — no real {@link Thread#sleep} calls. Groups: {@code resilience}, {@code unit}.
 */
@Test(groups = {"resilience", "unit"})
public class CircuitBreakerTest {

    // -------------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------------

    @Test(description = "Circuit starts in CLOSED state")
    public void initialState_isClosed() {
        final CircuitBreaker breaker = CircuitBreaker.builder()
                .failureThreshold(3)
                .resetTimeoutMs(1000)
                .clock(System::currentTimeMillis)
                .build();

        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(breaker.getFailureCount()).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // CLOSED → OPEN transition
    // -------------------------------------------------------------------------

    @Test(description = "Circuit opens after reaching the failure threshold")
    public void circuit_opensAfterFailureThreshold() {
        final CircuitBreaker breaker = CircuitBreaker.builder()
                .failureThreshold(3)
                .resetTimeoutMs(5000)
                .clock(System::currentTimeMillis)
                .build();

        // Cause 3 failures
        for (int i = 0; i < 3; i++) {
            final int fi = i;
            assertThatThrownBy(() -> breaker.execute(() -> {
                throw new RuntimeException("fail " + fi);
            })).isInstanceOf(FrameworkException.class);
        }

        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test(description = "Failure count increments correctly below threshold")
    public void failureCount_incrementsBeforeThreshold() {
        final CircuitBreaker breaker = CircuitBreaker.builder()
                .failureThreshold(5)
                .resetTimeoutMs(5000)
                .clock(System::currentTimeMillis)
                .build();

        assertThatThrownBy(() -> breaker.execute(() -> {
            throw new RuntimeException("fail");
        })).isInstanceOf(FrameworkException.class);

        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(breaker.getFailureCount()).isEqualTo(1);
    }

    // -------------------------------------------------------------------------
    // OPEN → fast-fail
    // -------------------------------------------------------------------------

    @Test(description = "OPEN circuit fast-fails without calling the operation")
    public void openCircuit_fastFails_withoutCallingOperation() {
        final AtomicLong virtualTime = new AtomicLong(0L);

        final CircuitBreaker breaker = CircuitBreaker.builder()
                .failureThreshold(2)
                .resetTimeoutMs(10_000)
                .clock(virtualTime::get)
                .build();

        // Open the circuit
        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> breaker.execute(() -> {
                throw new RuntimeException("open me");
            })).isInstanceOf(FrameworkException.class);
        }
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Time has not advanced — still within reset window
        assertThatThrownBy(() -> breaker.execute(() -> "should not be called"))
                .isInstanceOf(FrameworkException.class)
                .hasMessageContaining("fast-failing");
    }

    // -------------------------------------------------------------------------
    // OPEN → HALF_OPEN after reset timeout
    // -------------------------------------------------------------------------

    @Test(description = "Circuit transitions to HALF_OPEN after reset timeout elapses")
    public void circuit_transitionsToHalfOpen_afterResetTimeout() {
        final AtomicLong virtualTime = new AtomicLong(0L);

        final CircuitBreaker breaker = CircuitBreaker.builder()
                .failureThreshold(2)
                .resetTimeoutMs(1000)
                .clock(virtualTime::get)
                .build();

        // Open the circuit
        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> breaker.execute(() -> {
                throw new RuntimeException("open");
            })).isInstanceOf(FrameworkException.class);
        }

        // Advance virtual time past reset timeout
        virtualTime.set(1500L);

        // Probe call succeeds → circuit closes
        final String result = breaker.execute(() -> "probe success");
        assertThat(result).isEqualTo("probe success");
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    // -------------------------------------------------------------------------
    // HALF_OPEN → OPEN (probe fails)
    // -------------------------------------------------------------------------

    @Test(description = "Failed probe in HALF_OPEN re-opens the circuit")
    public void halfOpenCircuit_reopens_whenProbeFails() {
        final AtomicLong virtualTime = new AtomicLong(0L);

        final CircuitBreaker breaker = CircuitBreaker.builder()
                .failureThreshold(2)
                .resetTimeoutMs(500)
                .clock(virtualTime::get)
                .build();

        // Open the circuit
        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> breaker.execute(() -> {
                throw new RuntimeException("open");
            })).isInstanceOf(FrameworkException.class);
        }

        // Advance time to trigger HALF_OPEN
        virtualTime.set(600L);

        // Probe fails — circuit should re-open
        assertThatThrownBy(() -> breaker.execute(() -> {
            throw new RuntimeException("probe failure");
        })).isInstanceOf(FrameworkException.class);

        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    // -------------------------------------------------------------------------
    // Success resets failure count
    // -------------------------------------------------------------------------

    @Test(description = "Successful call resets failure count in CLOSED state")
    public void successfulCall_resetsFailureCount() {
        final CircuitBreaker breaker = CircuitBreaker.builder()
                .failureThreshold(5)
                .resetTimeoutMs(5000)
                .clock(System::currentTimeMillis)
                .build();

        // One failure
        assertThatThrownBy(() -> breaker.execute(() -> {
            throw new RuntimeException("one fail");
        })).isInstanceOf(FrameworkException.class);

        assertThat(breaker.getFailureCount()).isEqualTo(1);

        // Successful call
        breaker.execute(() -> "ok");
        assertThat(breaker.getFailureCount()).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // Manual reset
    // -------------------------------------------------------------------------

    @Test(description = "reset() returns circuit to CLOSED with zero failures")
    public void reset_returnsToClosed() {
        final CircuitBreaker breaker = CircuitBreaker.builder()
                .failureThreshold(1)
                .resetTimeoutMs(9999)
                .clock(System::currentTimeMillis)
                .build();

        assertThatThrownBy(() -> breaker.execute(() -> {
            throw new RuntimeException("open it");
        })).isInstanceOf(FrameworkException.class);

        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        breaker.reset();
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(breaker.getFailureCount()).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // Builder validation
    // -------------------------------------------------------------------------

    @Test(description = "Builder throws FrameworkException for failureThreshold < 1")
    public void builder_throwsException_forZeroFailureThreshold() {
        assertThatThrownBy(() -> CircuitBreaker.builder().failureThreshold(0))
                .isInstanceOf(FrameworkException.class);
    }

    @Test(description = "Builder throws FrameworkException for negative resetTimeoutMs")
    public void builder_throwsException_forNegativeResetTimeout() {
        assertThatThrownBy(() -> CircuitBreaker.builder().resetTimeoutMs(-1))
                .isInstanceOf(FrameworkException.class);
    }
}
