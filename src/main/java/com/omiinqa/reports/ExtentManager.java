package com.omiinqa.reports;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.omiinqa.config.FrameworkConfig;
import com.omiinqa.constants.FrameworkConstants;

import java.io.IOException;
import java.nio.file.Files;

/**
 * Owns the singleton {@link ExtentReports} aggregator and a per-thread
 * {@link ExtentTest} node (Singleton + ThreadLocal).
 *
 * <p><b>Why ThreadLocal for the test node:</b> Extent's {@code ExtentReports}
 * is thread-safe for creating tests, but each parallel test must log into its
 * own node. Binding the current node to the thread lets step logging stay
 * parameter-free while remaining correct under {@code parallel="methods"}.</p>
 */
public final class ExtentManager {

    private static volatile ExtentReports extent;
    private static final ThreadLocal<ExtentTest> CURRENT = new ThreadLocal<>();

    private ExtentManager() {
    }

    public static ExtentReports getInstance() {
        if (extent == null) {
            synchronized (ExtentManager.class) {
                if (extent == null) {
                    extent = build();
                }
            }
        }
        return extent;
    }

    private static ExtentReports build() {
        try {
            Files.createDirectories(FrameworkConstants.EXTENT_REPORT_DIR);
        } catch (final IOException ignored) {
            // Reporter will surface the path problem if the dir truly cannot be made.
        }
        final String path = FrameworkConfig.get()
                .raw("report.extent.path", "extent-reports/OmiinQA-Report.html");

        final ExtentSparkReporter spark = new ExtentSparkReporter(path);
        spark.config().setTheme(Theme.DARK);
        spark.config().setDocumentTitle("OmiinQA Automation Report");
        spark.config().setReportName("OmiinQA :: Enterprise Selenium Framework");

        final ExtentReports reports = new ExtentReports();
        reports.attachReporter(spark);
        reports.setSystemInfo("Framework", "OmiinQA");
        reports.setSystemInfo("Environment", FrameworkConfig.get().environment().name());
        reports.setSystemInfo("Browser", FrameworkConfig.get().browser().key());
        reports.setSystemInfo("OS", System.getProperty("os.name"));
        reports.setSystemInfo("Java", System.getProperty("java.version"));
        return reports;
    }

    public static ExtentTest createTest(final String name) {
        final ExtentTest test = getInstance().createTest(name);
        CURRENT.set(test);
        return test;
    }

    public static ExtentTest getTest() {
        return CURRENT.get();
    }

    public static void unload() {
        CURRENT.remove();
    }

    /** Flush buffered results to disk — call once at suite end. */
    public static void flush() {
        if (extent != null) {
            extent.flush();
        }
    }
}
