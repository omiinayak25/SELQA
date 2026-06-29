package com.omiinqa.driver;

import com.omiinqa.config.FrameworkConfig;
import com.omiinqa.enums.BrowserType;
import com.omiinqa.enums.ExecutionMode;
import com.omiinqa.exceptions.DriverInitializationException;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parallel-safe WebDriver lifecycle holder (Singleton + ThreadLocal).
 *
 * <p><b>Why ThreadLocal:</b> TestNG runs methods across a thread pool. A shared
 * static {@code WebDriver} would have threads stamping on each other's session.
 * Binding the driver to the running thread gives every parallel test its own
 * isolated browser while keeping the access API global and parameter-free.</p>
 *
 * <p>Contract: {@link #startDriver()} once per test (typically in a
 * {@code @BeforeMethod}), {@link #getDriver()} anywhere in that thread, and
 * {@link #quitDriver()} in an {@code @AfterMethod}. {@link #quitDriver()} always
 * calls {@code remove()} so threads are never leaked back to the pool carrying a
 * dead session — a classic parallel-suite memory leak.</p>
 */
public final class DriverManager {

    private static final Logger LOG = LoggerFactory.getLogger(DriverManager.class);

    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    private DriverManager() {
    }

    /** Start a driver for the current thread using configured browser/mode. */
    public static WebDriver startDriver() {
        final FrameworkConfig config = FrameworkConfig.get();
        return startDriver(config.browser(), config.executionMode(), config.headless());
    }

    /** Start a driver with an explicit browser/mode (used by cross-browser tests). */
    public static WebDriver startDriver(final BrowserType browser,
                                        final ExecutionMode mode,
                                        final boolean headless) {
        if (DRIVER.get() != null) {
            LOG.warn("Driver already active on thread '{}' — reusing existing session",
                    Thread.currentThread().getName());
            return DRIVER.get();
        }
        final WebDriver driver = DriverRetry.withRetry(
                () -> DriverFactory.create(browser, mode, headless),
                FrameworkConfig.get().driverRetryCount());
        DRIVER.set(driver);
        LOG.debug("Driver bound to thread '{}'", Thread.currentThread().getName());
        return driver;
    }

    /**
     * @return the driver bound to the current thread
     * @throws DriverInitializationException if no driver has been started
     */
    public static WebDriver getDriver() {
        final WebDriver driver = DRIVER.get();
        if (driver == null) {
            throw new DriverInitializationException(
                    "No WebDriver on thread '" + Thread.currentThread().getName()
                            + "'. Did you call DriverManager.startDriver()?");
        }
        return driver;
    }

    public static boolean hasDriver() {
        return DRIVER.get() != null;
    }

    /** Quit the driver and detach it from the thread (idempotent). */
    public static void quitDriver() {
        final WebDriver driver = DRIVER.get();
        if (driver != null) {
            try {
                driver.quit();
            } catch (final RuntimeException e) {
                LOG.warn("Error while quitting driver: {}", e.getMessage());
            } finally {
                DRIVER.remove();
                LOG.debug("Driver released from thread '{}'",
                        Thread.currentThread().getName());
            }
        }
    }
}
