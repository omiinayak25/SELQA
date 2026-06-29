package com.omiinqa.resilience;

import com.omiinqa.exceptions.FrameworkException;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ChaosInjector}.
 *
 * <p>All tests are offline and deterministic. Latency action is always a no-op;
 * determinism is achieved via the call-counter modulus. Groups: {@code resilience}, {@code unit}.
 */
@Test(groups = {"resilience", "unit"})
public class ChaosInjectorTest {

    /** No-op latency action — prevents any real sleep in unit tests. */
    private static final java.util.function.Consumer<Long> NO_OP_LATENCY = millis -> { };

    // -------------------------------------------------------------------------
    // Normal pass-through (no injection triggered)
    // -------------------------------------------------------------------------

    @Test(description = "maybe passes through normally when injection does not fire")
    public void maybe_passesThrough_whenInjectionDoesNotFire() {
        final ChaosInjector injector = ChaosInjector.builder()
                .injectionEvery(5)       // fires on 5th, 10th, ...
                .latencyAction(NO_OP_LATENCY)
                .build();

        // Calls 1, 2, 3 — no injection
        final AtomicInteger opCount = new AtomicInteger(0);
        for (int i = 0; i < 3; i++) {
            injector.maybe(() -> { opCount.incrementAndGet(); return null; }, "test");
        }
        assertThat(opCount.get()).isEqualTo(3);
        assertThat(injector.getCallCount()).isEqualTo(3);
    }

    // -------------------------------------------------------------------------
    // Deterministic fault injection
    // -------------------------------------------------------------------------

    @Test(description = "fault fires exactly on the Nth call (injectionEvery=3)")
    public void maybe_injectsFault_onEveryNthCall() {
        final AtomicInteger faultCount = new AtomicInteger(0);

        final ChaosInjector injector = ChaosInjector.builder()
                .injectionEvery(3)
                .fault(() -> faultCount.incrementAndGet())   // count but do NOT throw
                .latencyAction(NO_OP_LATENCY)
                .build();

        // Run 9 calls: faults on calls 3, 6, 9
        for (int i = 0; i < 9; i++) {
            injector.maybe(() -> "ok", "test-" + i);
        }

        assertThat(faultCount.get()).isEqualTo(3);
    }

    @Test(description = "fault on every 1st call means every call triggers injection")
    public void maybe_injectsEveryCall_whenInjectionEveryIsOne() {
        final AtomicInteger faultCount = new AtomicInteger(0);

        final ChaosInjector injector = ChaosInjector.builder()
                .injectionEvery(1)
                .fault(() -> faultCount.incrementAndGet())   // count but do NOT throw
                .latencyAction(NO_OP_LATENCY)
                .build();

        for (int i = 0; i < 5; i++) {
            injector.maybe(() -> "ok", "test");
        }

        assertThat(faultCount.get()).isEqualTo(5);
    }

    @Test(description = "fault that throws is propagated as RuntimeException through maybe()")
    public void maybe_propagatesThrownFault() {
        final ChaosInjector injector = ChaosInjector.builder()
                .injectionEvery(1)   // throw on first call
                .fault(() -> { throw new IllegalStateException("chaos!"); })
                .latencyAction(NO_OP_LATENCY)
                .build();

        assertThatThrownBy(() -> injector.maybe(() -> "unreachable", "test"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("chaos!");
    }

    // -------------------------------------------------------------------------
    // Latency injection
    // -------------------------------------------------------------------------

    @Test(description = "injectLatency calls the latency action with the configured millis")
    public void injectLatency_callsLatencyAction_withConfiguredMillis() {
        final List<Long> recorded = new ArrayList<>();

        final ChaosInjector injector = ChaosInjector.builder()
                .latencyMs(250)
                .latencyAction(recorded::add)
                .build();

        injector.injectLatency();
        injector.injectLatency();

        assertThat(recorded).containsExactly(250L, 250L);
    }

    @Test(description = "injectLatencyFirst causes latency to fire before the fault on triggered call")
    public void injectLatencyFirst_firesLatencyBeforeFault() {
        final List<String> order = new ArrayList<>();

        final ChaosInjector injector = ChaosInjector.builder()
                .injectionEvery(1)
                .injectLatencyFirst(true)
                .latencyMs(10)
                .latencyAction(ms -> order.add("latency"))
                .fault(() -> { order.add("fault"); throw new RuntimeException("boom"); })
                .build();

        assertThatThrownBy(() -> injector.maybe(() -> "ok", "ordered"))
                .isInstanceOf(RuntimeException.class);

        assertThat(order).containsExactly("latency", "fault");
    }

    // -------------------------------------------------------------------------
    // Direct injectFault
    // -------------------------------------------------------------------------

    @Test(description = "injectFault fires the fault Runnable directly")
    public void injectFault_firesConfiguredFault() {
        final AtomicInteger count = new AtomicInteger(0);

        final ChaosInjector injector = ChaosInjector.builder()
                .fault(() -> count.incrementAndGet())
                .latencyAction(NO_OP_LATENCY)
                .build();

        injector.injectFault();
        assertThat(count.get()).isEqualTo(1);
    }

    // -------------------------------------------------------------------------
    // Counter reset
    // -------------------------------------------------------------------------

    @Test(description = "resetCounter resets call count to zero and alters injection cadence")
    public void resetCounter_resetsToZero() {
        final AtomicInteger faultCount = new AtomicInteger(0);

        final ChaosInjector injector = ChaosInjector.builder()
                .injectionEvery(3)
                .fault(() -> faultCount.incrementAndGet())
                .latencyAction(NO_OP_LATENCY)
                .build();

        // Calls 1, 2 — no fault yet
        injector.maybe(() -> "a", "a");
        injector.maybe(() -> "b", "b");
        assertThat(faultCount.get()).isEqualTo(0);

        injector.resetCounter();

        // After reset, calls 1, 2 again — no fault
        injector.maybe(() -> "c", "c");
        injector.maybe(() -> "d", "d");
        assertThat(faultCount.get()).isEqualTo(0);
        assertThat(injector.getCallCount()).isEqualTo(2);
    }

    // -------------------------------------------------------------------------
    // Builder validation
    // -------------------------------------------------------------------------

    @Test(description = "Builder throws FrameworkException for injectionEvery < 1")
    public void builder_throwsException_forZeroInjectionEvery() {
        assertThatThrownBy(() -> ChaosInjector.builder().injectionEvery(0))
                .isInstanceOf(FrameworkException.class);
    }

    @Test(description = "Builder throws FrameworkException for negative latencyMs")
    public void builder_throwsException_forNegativeLatencyMs() {
        assertThatThrownBy(() -> ChaosInjector.builder().latencyMs(-1))
                .isInstanceOf(FrameworkException.class);
    }

    @Test(description = "maybe throws FrameworkException when operation is null")
    public void maybe_throwsFrameworkException_forNullOperation() {
        final ChaosInjector injector = ChaosInjector.builder()
                .latencyAction(NO_OP_LATENCY)
                .build();

        assertThatThrownBy(() -> injector.maybe((java.util.function.Supplier<Object>) null, "test"))
                .isInstanceOf(FrameworkException.class);
    }
}
