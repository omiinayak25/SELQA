package com.omiinqa.config;

import com.omiinqa.constants.FrameworkConstants;
import com.omiinqa.enums.BrowserType;
import com.omiinqa.enums.Environment;
import com.omiinqa.enums.ExecutionMode;

import java.time.Duration;

/**
 * Strongly-typed, read-only view over {@link ConfigManager} (Facade pattern).
 *
 * <p>Callers depend on intention-revealing methods — {@code config.browser()},
 * {@code config.explicitTimeout()} — instead of raw string keys and manual
 * parsing. This isolates the rest of the framework from the config source and
 * gives one place to evolve defaults.</p>
 */
public final class FrameworkConfig {

    private static final FrameworkConfig INSTANCE = new FrameworkConfig();

    private final ConfigManager cfg;

    private FrameworkConfig() {
        this.cfg = ConfigManager.get();
    }

    public static FrameworkConfig get() {
        return INSTANCE;
    }

    // ---- Execution ----
    public BrowserType browser() {
        return BrowserType.from(cfg.get(FrameworkConstants.KEY_BROWSER, "chrome"));
    }

    public boolean headless() {
        return cfg.getBoolean(FrameworkConstants.KEY_HEADLESS, true);
    }

    public ExecutionMode executionMode() {
        return ExecutionMode.from(cfg.get(FrameworkConstants.KEY_EXECUTION_MODE, "local"));
    }

    public String gridUrl() {
        return cfg.get(FrameworkConstants.KEY_GRID_URL, "http://localhost:4444/wd/hub");
    }

    public Environment environment() {
        return cfg.environment();
    }

    // ---- Timeouts ----
    public Duration implicitTimeout() {
        return Duration.ofSeconds(cfg.getInt(FrameworkConstants.KEY_TIMEOUT_IMPLICIT, 0));
    }

    public Duration explicitTimeout() {
        return Duration.ofSeconds(cfg.getInt(FrameworkConstants.KEY_TIMEOUT_EXPLICIT, 15));
    }

    public Duration pageLoadTimeout() {
        return Duration.ofSeconds(cfg.getInt(FrameworkConstants.KEY_TIMEOUT_PAGELOAD, 30));
    }

    public Duration scriptTimeout() {
        return Duration.ofSeconds(cfg.getInt(FrameworkConstants.KEY_TIMEOUT_SCRIPT, 20));
    }

    public Duration pollingInterval() {
        return Duration.ofMillis(cfg.getLong(FrameworkConstants.KEY_TIMEOUT_POLLING, 500));
    }

    // ---- Retry ----
    public int retryCount() {
        return cfg.getInt(FrameworkConstants.KEY_RETRY_COUNT, 2);
    }

    public int driverRetryCount() {
        return cfg.getInt(FrameworkConstants.KEY_RETRY_DRIVER_COUNT, 2);
    }

    // ---- Window ----
    public boolean maximize() {
        return cfg.getBoolean(FrameworkConstants.KEY_MAXIMIZE, true);
    }

    public int windowWidth() {
        return cfg.getInt(FrameworkConstants.KEY_WINDOW_WIDTH, 1920);
    }

    public int windowHeight() {
        return cfg.getInt(FrameworkConstants.KEY_WINDOW_HEIGHT, 1080);
    }

    // ---- Evidence ----
    public boolean screenshotOnFailure() {
        return cfg.getBoolean(FrameworkConstants.KEY_SCREENSHOT_ON_FAILURE, true);
    }

    public boolean screenshotOnSuccess() {
        return cfg.getBoolean(FrameworkConstants.KEY_SCREENSHOT_ON_SUCCESS, false);
    }

    // ---- Application URLs ----
    public String appUrl(final String appKey) {
        return cfg.get("app." + appKey + ".url");
    }

    public String apiUrl(final String apiKey) {
        return cfg.get("api." + apiKey + ".url");
    }

    /** Escape hatch for ad-hoc keys without widening the typed surface. */
    public String raw(final String key) {
        return cfg.get(key);
    }

    public String raw(final String key, final String defaultValue) {
        return cfg.get(key, defaultValue);
    }
}
