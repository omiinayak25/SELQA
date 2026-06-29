package com.omiinqa.observability;

import org.slf4j.MDC;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CorrelationId} — verifies ID generation, MDC propagation,
 * cleanup, and thread isolation. Fully offline; no browser, DB, or network required.
 */
@Test(groups = {"observability", "unit"})
public class CorrelationIdTest {

    @AfterMethod(alwaysRun = true)
    public void cleanup() {
        CorrelationId.clear();
    }

    // -------------------------------------------------------------------------
    // start() tests
    // -------------------------------------------------------------------------

    @Test(groups = {"observability", "unit"})
    public void startReturnsNonNullId() {
        final String id = CorrelationId.start();
        assertThat(id).isNotNull().isNotEmpty();
    }

    @Test(groups = {"observability", "unit"})
    public void startIdBeginsWithCidPrefix() {
        final String id = CorrelationId.start();
        assertThat(id).startsWith("cid-");
    }

    @Test(groups = {"observability", "unit"})
    public void startPutsIdIntoMdc() {
        final String id = CorrelationId.start();
        assertThat(MDC.get(CorrelationId.MDC_KEY)).isEqualTo(id);
    }

    @Test(groups = {"observability", "unit"})
    public void consecutiveStartsProduceDifferentIds() {
        final String id1 = CorrelationId.start();
        final String id2 = CorrelationId.start();
        assertThat(id1).isNotEqualTo(id2);
    }

    @Test(groups = {"observability", "unit"})
    public void idContainsMonotonicCounter() {
        // Each call to start() must increment the counter, so IDs must always differ
        final Set<String> ids = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            ids.add(CorrelationId.start());
        }
        assertThat(ids).hasSize(5);
    }

    // -------------------------------------------------------------------------
    // get() tests
    // -------------------------------------------------------------------------

    @Test(groups = {"observability", "unit"})
    public void getReturnsUnsetBeforeStart() {
        // No start() called on fresh thread — must not throw, must return sentinel
        assertThat(CorrelationId.get()).isEqualTo("unset");
    }

    @Test(groups = {"observability", "unit"})
    public void getReturnsActiveIdAfterStart() {
        final String id = CorrelationId.start();
        assertThat(CorrelationId.get()).isEqualTo(id);
    }

    // -------------------------------------------------------------------------
    // clear() tests
    // -------------------------------------------------------------------------

    @Test(groups = {"observability", "unit"})
    public void clearRemovesIdFromThreadLocal() {
        CorrelationId.start();
        CorrelationId.clear();
        assertThat(CorrelationId.get()).isEqualTo("unset");
    }

    @Test(groups = {"observability", "unit"})
    public void clearRemovesIdFromMdc() {
        CorrelationId.start();
        CorrelationId.clear();
        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
    }

    @Test(groups = {"observability", "unit"})
    public void clearIsIdempotent() {
        CorrelationId.start();
        CorrelationId.clear();
        // Second clear must not throw
        CorrelationId.clear();
        assertThat(CorrelationId.get()).isEqualTo("unset");
    }

    // -------------------------------------------------------------------------
    // Thread-isolation tests
    // -------------------------------------------------------------------------

    @Test(groups = {"observability", "unit"})
    public void eachThreadGetsItsOwnId() throws InterruptedException {
        final ConcurrentLinkedQueue<String> ids = new ConcurrentLinkedQueue<>();
        final CountDownLatch latch = new CountDownLatch(3);
        final ExecutorService executor = Executors.newFixedThreadPool(3);

        for (int i = 0; i < 3; i++) {
            executor.submit(() -> {
                try {
                    ids.add(CorrelationId.start());
                } finally {
                    CorrelationId.clear();
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // All three threads produced distinct IDs
        assertThat(ids).hasSize(3);
        assertThat(new HashSet<>(ids)).hasSize(3);
    }

    @Test(groups = {"observability", "unit"})
    public void mdcKeyConstantMatchesExpectedValue() {
        assertThat(CorrelationId.MDC_KEY).isEqualTo("correlationId");
    }
}
