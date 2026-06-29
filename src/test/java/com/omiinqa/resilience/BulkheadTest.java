package com.omiinqa.resilience;

import com.omiinqa.exceptions.FrameworkException;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link Bulkhead}.
 *
 * <p>Tests cover: normal execution, rejection beyond capacity, concurrent multi-thread
 * rejection, permit release on exception, and builder validation.
 * Groups: {@code resilience}, {@code unit}.
 */
@Test(groups = {"resilience", "unit"})
public class BulkheadTest {

    // -------------------------------------------------------------------------
    // Normal execution
    // -------------------------------------------------------------------------

    @Test(description = "execute returns the operation result when a permit is available")
    public void execute_returnsResult_whenPermitAvailable() {
        final Bulkhead bulkhead = Bulkhead.builder()
                .name("test")
                .maxConcurrentCalls(3)
                .build();

        final String result = bulkhead.execute(() -> "success");
        assertThat(result).isEqualTo("success");
    }

    @Test(description = "availablePermits equals maxConcurrentCalls after all calls complete")
    public void availablePermits_returnsMax_afterCompletedCalls() {
        final Bulkhead bulkhead = Bulkhead.builder()
                .name("permits-test")
                .maxConcurrentCalls(5)
                .build();

        bulkhead.execute(() -> "a");
        bulkhead.execute(() -> "b");

        assertThat(bulkhead.availablePermits()).isEqualTo(5);
    }

    // -------------------------------------------------------------------------
    // Rejection beyond capacity
    // -------------------------------------------------------------------------

    @Test(description = "execute rejects call when bulkhead is saturated (single thread via latch)")
    public void execute_rejectsCall_whenBulkheadIsFull() throws Exception {
        final int maxCalls = 2;
        final Bulkhead bulkhead = Bulkhead.builder()
                .name("rejection-test")
                .maxConcurrentCalls(maxCalls)
                .build();

        final CountDownLatch holdLatch  = new CountDownLatch(1); // keeps permits held
        final CountDownLatch readyLatch = new CountDownLatch(maxCalls); // signals callers are inside

        final ExecutorService executor = Executors.newFixedThreadPool(maxCalls + 1);
        try {
            // Submit exactly maxCalls long-running operations that hold permits open
            for (int i = 0; i < maxCalls; i++) {
                executor.submit(() -> bulkhead.execute(() -> {
                    readyLatch.countDown();
                    try { holdLatch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    return null;
                }));
            }

            // Wait until all permits are held
            readyLatch.await();

            // This call must be rejected (bulkhead full)
            assertThatThrownBy(() -> bulkhead.execute(() -> "should be rejected"))
                    .isInstanceOf(FrameworkException.class)
                    .hasMessageContaining("full");

        } finally {
            holdLatch.countDown(); // release all permits
            executor.shutdownNow();
        }
    }

    // -------------------------------------------------------------------------
    // Permit release on exception
    // -------------------------------------------------------------------------

    @Test(description = "permit is released even when the operation throws an exception")
    public void execute_releasesPermit_whenOperationThrows() {
        final Bulkhead bulkhead = Bulkhead.builder()
                .name("release-on-throw")
                .maxConcurrentCalls(1)
                .build();

        // First call throws — permit must be released
        assertThatThrownBy(() -> bulkhead.execute(() -> {
            throw new RuntimeException("boom");
        })).isInstanceOf(FrameworkException.class);

        // Second call must succeed (permit was released)
        assertThat(bulkhead.availablePermits()).isEqualTo(1);
        final String result = bulkhead.execute(() -> "recovered");
        assertThat(result).isEqualTo("recovered");
    }

    // -------------------------------------------------------------------------
    // Concurrent rejection count
    // -------------------------------------------------------------------------

    @Test(description = "multiple threads beyond capacity are rejected, not queued")
    public void execute_rejectsAllOverCapacity_concurrently() throws Exception {
        final int capacity = 2;
        final int totalThreads = 6;

        final Bulkhead bulkhead = Bulkhead.builder()
                .name("concurrent-rejection")
                .maxConcurrentCalls(capacity)
                .build();

        final CountDownLatch holdLatch  = new CountDownLatch(1);
        final CountDownLatch readyLatch = new CountDownLatch(capacity);
        final CountDownLatch excessLatch = new CountDownLatch(totalThreads - capacity);
        final AtomicInteger rejections  = new AtomicInteger(0);
        final AtomicInteger successes   = new AtomicInteger(0);

        final ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        final List<Future<?>> futures = new ArrayList<>();
        try {
            // Fill capacity slots
            for (int i = 0; i < capacity; i++) {
                futures.add(executor.submit(() -> bulkhead.execute(() -> {
                    readyLatch.countDown();
                    try { holdLatch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    successes.incrementAndGet();
                    return null;
                })));
            }

            readyLatch.await(); // ensure capacity is fully consumed

            // Submit excess threads — all should be rejected. Each signals excessLatch
            // AFTER its execute() attempt so we can guarantee every excess call happened
            // while the two permits are still held (no race with permit release).
            for (int i = 0; i < (totalThreads - capacity); i++) {
                futures.add(executor.submit(() -> {
                    try {
                        bulkhead.execute(() -> "excess");
                    } catch (FrameworkException e) {
                        rejections.incrementAndGet();
                    } finally {
                        excessLatch.countDown();
                    }
                }));
            }

            // Barrier: all excess attempts have completed while capacity is still held.
            excessLatch.await();
        } finally {
            holdLatch.countDown();
            for (final Future<?> f : futures) {
                try { f.get(); } catch (Exception ignored) { }
            }
            executor.shutdownNow();
        }

        assertThat(rejections.get()).isEqualTo(totalThreads - capacity);
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    @Test(description = "getName returns the configured name")
    public void getName_returnsConfiguredName() {
        final Bulkhead bulkhead = Bulkhead.builder().name("my-service").build();
        assertThat(bulkhead.getName()).isEqualTo("my-service");
    }

    @Test(description = "getMaxConcurrentCalls returns the configured capacity")
    public void getMaxConcurrentCalls_returnsCapacity() {
        final Bulkhead bulkhead = Bulkhead.builder().maxConcurrentCalls(7).build();
        assertThat(bulkhead.getMaxConcurrentCalls()).isEqualTo(7);
    }

    // -------------------------------------------------------------------------
    // Builder validation
    // -------------------------------------------------------------------------

    @Test(description = "Builder throws FrameworkException for maxConcurrentCalls < 1")
    public void builder_throwsException_forZeroCapacity() {
        assertThatThrownBy(() -> Bulkhead.builder().maxConcurrentCalls(0))
                .isInstanceOf(FrameworkException.class);
    }

    @Test(description = "Builder throws FrameworkException for blank name")
    public void builder_throwsException_forBlankName() {
        assertThatThrownBy(() -> Bulkhead.builder().name("  "))
                .isInstanceOf(FrameworkException.class);
    }

    @Test(description = "Builder throws FrameworkException for null operation")
    public void execute_throwsFrameworkException_forNullOperation() {
        final Bulkhead bulkhead = Bulkhead.builder().build();
        assertThatThrownBy(() -> bulkhead.execute(null))
                .isInstanceOf(FrameworkException.class);
    }
}
