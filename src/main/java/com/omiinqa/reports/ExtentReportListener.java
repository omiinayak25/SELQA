package com.omiinqa.reports;

import com.aventstack.extentreports.Status;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * Bridges the TestNG lifecycle into Extent (Adapter pattern).
 *
 * <p>Registered alongside {@link com.omiinqa.listeners.TestListener}; this one
 * is solely responsible for the Extent HTML report so reporting concerns stay
 * separate from logging/screenshot concerns (Single Responsibility). Flushes on
 * suite finish so the report is always written even if the JVM exits promptly.</p>
 */
public final class ExtentReportListener implements ITestListener {

    @Override
    public void onTestStart(final ITestResult result) {
        ExtentManager.createTest(
                result.getTestClass().getRealClass().getSimpleName() + " :: " + result.getName());
    }

    @Override
    public void onTestSuccess(final ITestResult result) {
        ExtentManager.getTest().log(Status.PASS, "Test passed");
        ExtentManager.unload();
    }

    @Override
    public void onTestFailure(final ITestResult result) {
        final Throwable t = result.getThrowable();
        ExtentManager.getTest().log(Status.FAIL,
                t == null ? "Test failed" : t.toString());
        ExtentManager.unload();
    }

    @Override
    public void onTestSkipped(final ITestResult result) {
        ExtentManager.getTest().log(Status.SKIP, "Test skipped");
        ExtentManager.unload();
    }

    @Override
    public void onFinish(final ITestContext context) {
        ExtentManager.flush();
    }
}
