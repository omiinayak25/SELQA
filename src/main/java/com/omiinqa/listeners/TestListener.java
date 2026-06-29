package com.omiinqa.listeners;

import com.omiinqa.driver.DriverManager;
import com.omiinqa.config.FrameworkConfig;
import com.omiinqa.utils.ScreenshotUtils;
import io.qameta.allure.Allure;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.ByteArrayInputStream;

/**
 * Cross-cutting test lifecycle hook (Observer over the TestNG run).
 *
 * <p>One place for what should happen around <em>every</em> test regardless of
 * layer: structured logging with a per-test MDC token (so parallel logs stay
 * readable), and failure evidence — a screenshot attached to Allure — captured
 * only when a UI driver is present. API/DB tests reuse the same listener and
 * simply skip the screenshot step.</p>
 */
public final class TestListener implements ITestListener {

    private static final Logger LOG = LoggerFactory.getLogger(TestListener.class);

    @Override
    public void onTestStart(final ITestResult result) {
        MDC.put("testName", result.getName());
        LOG.info("▶ START  {}.{}", result.getTestClass().getRealClass().getSimpleName(),
                result.getName());
    }

    @Override
    public void onTestSuccess(final ITestResult result) {
        LOG.info("✔ PASS   {} ({} ms)", result.getName(), durationMillis(result));
        if (FrameworkConfig.get().screenshotOnSuccess() && DriverManager.hasDriver()) {
            attachScreenshot("success-" + result.getName());
        }
        MDC.remove("testName");
    }

    @Override
    public void onTestFailure(final ITestResult result) {
        LOG.error("x FAIL   {} ({} ms): {}", result.getName(), durationMillis(result),
                result.getThrowable() == null ? "—" : result.getThrowable().getMessage());
        if (FrameworkConfig.get().screenshotOnFailure() && DriverManager.hasDriver()) {
            attachScreenshot("failure-" + result.getName());
        }
        MDC.remove("testName");
    }

    @Override
    public void onTestSkipped(final ITestResult result) {
        LOG.warn("➜ SKIP   {}", result.getName());
        MDC.remove("testName");
    }

    @Override
    public void onFinish(final ITestContext context) {
        LOG.info("Suite '{}' finished: {} passed, {} failed, {} skipped",
                context.getName(),
                context.getPassedTests().size(),
                context.getFailedTests().size(),
                context.getSkippedTests().size());
    }

    private void attachScreenshot(final String name) {
        try {
            final WebDriver driver = DriverManager.getDriver();
            final byte[] png = ScreenshotUtils.captureBytes(driver);
            if (png.length > 0) {
                Allure.addAttachment(name, "image/png",
                        new ByteArrayInputStream(png), ".png");
                ScreenshotUtils.capture(driver, name);
            }
        } catch (final RuntimeException e) {
            LOG.warn("Failed to attach screenshot '{}': {}", name, e.getMessage());
        }
    }

    private long durationMillis(final ITestResult result) {
        return result.getEndMillis() - result.getStartMillis();
    }
}
