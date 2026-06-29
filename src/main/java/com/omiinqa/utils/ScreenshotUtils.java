package com.omiinqa.utils;

import com.omiinqa.constants.FrameworkConstants;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Captures screenshots as files (for the report tree) and as raw bytes (for
 * Allure/Extent attachment). Failure-driven, so it never throws into the
 * caller — a screenshot problem must not mask the original test failure.
 */
public final class ScreenshotUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ScreenshotUtils.class);
    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private ScreenshotUtils() {
    }

    /** @return PNG bytes, or an empty array if capture failed. */
    public static byte[] captureBytes(final WebDriver driver) {
        try {
            return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        } catch (final RuntimeException e) {
            LOG.warn("Screenshot (bytes) capture failed: {}", e.getMessage());
            return new byte[0];
        }
    }

    /**
     * Persist a screenshot under {@code screenshots/} and return its path.
     *
     * @return the written file path, or {@code null} if capture failed
     */
    public static Path capture(final WebDriver driver, final String testName) {
        final byte[] bytes = captureBytes(driver);
        if (bytes.length == 0) {
            return null;
        }
        try {
            Files.createDirectories(FrameworkConstants.SCREENSHOT_DIR);
            final String safeName = testName.replaceAll("[^a-zA-Z0-9._-]", "_");
            final Path target = FrameworkConstants.SCREENSHOT_DIR.resolve(
                    safeName + "_" + LocalDateTime.now().format(STAMP) + ".png");
            Files.write(target, bytes);
            LOG.info("Screenshot saved: {}", target);
            return target;
        } catch (final IOException e) {
            LOG.warn("Could not write screenshot file: {}", e.getMessage());
            return null;
        }
    }
}
