package com.omiinqa.listeners;

import com.omiinqa.config.FrameworkConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Re-runs a failed test up to {@code retry.count} times before marking it
 * failed (Strategy plugged into TestNG's retry hook).
 *
 * <p>Retries are a pragmatic shield against genuine UI/network flakiness in a
 * large suite — <em>not</em> a license to hide real defects. The retry budget
 * is configurable and intentionally small (default 2). Every retry is logged
 * so a chronically-retried test is visible and can be quarantined.</p>
 */
public final class RetryAnalyzer implements IRetryAnalyzer {

    private static final Logger LOG = LoggerFactory.getLogger(RetryAnalyzer.class);

    private final int maxRetries = FrameworkConfig.get().retryCount();
    private int attempts;

    @Override
    public boolean retry(final ITestResult result) {
        if (attempts < maxRetries) {
            attempts++;
            LOG.warn("Retry {}/{} for '{}.{}'", attempts, maxRetries,
                    result.getTestClass().getName(), result.getName());
            return true;
        }
        return false;
    }
}
