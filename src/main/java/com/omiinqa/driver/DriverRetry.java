package com.omiinqa.driver;

import com.omiinqa.exceptions.DriverInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Retries flaky driver creation (Command-ish wrapper around a supplier).
 *
 * <p>Driver startup is one of the least reliable steps in a suite — transient
 * Grid saturation, a node still booting, a port race. Rather than fail a whole
 * test on a startup hiccup, the supplier is retried a bounded number of times
 * with a short backoff before giving up.</p>
 */
public final class DriverRetry {

    private static final Logger LOG = LoggerFactory.getLogger(DriverRetry.class);
    private static final long BACKOFF_MILLIS = 1500L;

    private DriverRetry() {
    }

    public static <T> T withRetry(final Supplier<T> action, final int maxRetries) {
        DriverInitializationException last = null;
        for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
            try {
                return action.get();
            } catch (final DriverInitializationException e) {
                last = e;
                LOG.warn("Driver start attempt {}/{} failed: {}",
                        attempt, maxRetries + 1, e.getMessage());
                sleep();
            }
        }
        throw new DriverInitializationException(
                "Driver could not be started after " + (maxRetries + 1) + " attempts", last);
    }

    private static void sleep() {
        try {
            Thread.sleep(BACKOFF_MILLIS);
        } catch (final InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
