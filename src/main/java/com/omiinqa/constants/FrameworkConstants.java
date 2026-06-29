package com.omiinqa.constants;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Immutable, compile-time framework constants: filesystem layout and the
 * canonical configuration keys. Centralizing key names here prevents the
 * "magic string typo" class of bugs across the config layer.
 *
 * <p>Final class with a private constructor — this is a constant holder, not
 * a type to be instantiated or extended (KISS / YAGNI).</p>
 */
public final class FrameworkConstants {

    private FrameworkConstants() {
        throw new AssertionError("Constant holder — do not instantiate.");
    }

    // ---- Filesystem ----
    public static final Path PROJECT_ROOT = Paths.get(System.getProperty("user.dir"));
    public static final Path SCREENSHOT_DIR = PROJECT_ROOT.resolve("screenshots");
    public static final Path LOG_DIR = PROJECT_ROOT.resolve("logs");
    public static final Path TEST_DATA_DIR =
            PROJECT_ROOT.resolve("src/test/resources/testdata");
    public static final Path EXTENT_REPORT_DIR = PROJECT_ROOT.resolve("extent-reports");
    public static final Path ALLURE_RESULTS_DIR =
            PROJECT_ROOT.resolve("target/allure-results");

    public static final String BASE_CONFIG = "config/config.properties";
    public static final String ENV_CONFIG_DIR = "config/env/";

    // ---- Canonical configuration keys ----
    public static final String KEY_BROWSER = "browser";
    public static final String KEY_HEADLESS = "headless";
    public static final String KEY_EXECUTION_MODE = "execution.mode";
    public static final String KEY_GRID_URL = "grid.url";
    public static final String KEY_ENV = "env";

    public static final String KEY_TIMEOUT_IMPLICIT = "timeout.implicit";
    public static final String KEY_TIMEOUT_EXPLICIT = "timeout.explicit";
    public static final String KEY_TIMEOUT_PAGELOAD = "timeout.pageload";
    public static final String KEY_TIMEOUT_SCRIPT = "timeout.script";
    public static final String KEY_TIMEOUT_POLLING = "timeout.polling.millis";

    public static final String KEY_RETRY_COUNT = "retry.count";
    public static final String KEY_RETRY_DRIVER_COUNT = "retry.driver.count";

    public static final String KEY_MAXIMIZE = "browser.maximize";
    public static final String KEY_WINDOW_WIDTH = "browser.window.width";
    public static final String KEY_WINDOW_HEIGHT = "browser.window.height";

    public static final String KEY_SCREENSHOT_ON_FAILURE = "screenshot.on.failure";
    public static final String KEY_SCREENSHOT_ON_SUCCESS = "screenshot.on.success";
}
