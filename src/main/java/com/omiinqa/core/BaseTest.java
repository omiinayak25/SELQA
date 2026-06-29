package com.omiinqa.core;

import com.omiinqa.config.FrameworkConfig;
import com.omiinqa.driver.DriverManager;
import com.omiinqa.listeners.TestListener;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;

/**
 * Abstract base for all UI tests (Template Method for setup/teardown).
 *
 * <p>Owns the per-method driver lifecycle so individual tests never touch
 * {@link DriverManager} directly: a fresh, isolated browser is started before
 * each method and always quit afterwards — even on failure — preventing
 * session leaks in parallel runs. Method-level (not class-level) lifecycle is
 * the safe default for {@code parallel="methods"} execution.</p>
 *
 * <p>The {@link TestListener} is attached here so every subclass inherits
 * logging and failure-screenshot behavior without re-declaring it.</p>
 */
@Listeners(TestListener.class)
public abstract class BaseTest {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final FrameworkConfig config = FrameworkConfig.get();

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        DriverManager.startDriver();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        DriverManager.quitDriver();
    }

    /** Convenience accessor for subclasses needing the raw driver. */
    protected WebDriver driver() {
        return DriverManager.getDriver();
    }

    /** Navigate to a known application URL and return its address. */
    protected String open(final String url) {
        log.info("Navigating to {}", url);
        driver().get(url);
        return url;
    }
}
