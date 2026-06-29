package com.omiinqa.driver;

import com.omiinqa.config.FrameworkConfig;
import com.omiinqa.driver.options.BrowserOptionsStrategy;
import com.omiinqa.driver.options.OptionsStrategyFactory;
import com.omiinqa.enums.BrowserType;
import com.omiinqa.enums.ExecutionMode;
import com.omiinqa.exceptions.DriverInitializationException;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Creates {@link WebDriver} instances (Factory pattern).
 *
 * <p>The factory owns two orthogonal decisions: <em>which browser</em>
 * (delegated to a {@link BrowserOptionsStrategy}) and <em>where it runs</em>
 * (local vs Grid/remote). Browser binaries are provisioned by
 * WebDriverManager for local runs — never deprecated {@code setProperty}
 * paths.</p>
 *
 * <p>Stateless and side-effect free apart from driver creation, so it is safe
 * to call from many threads simultaneously.</p>
 */
public final class DriverFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DriverFactory.class);

    private DriverFactory() {
    }

    /**
     * Build a driver from the supplied browser/mode, applying the resolved
     * capabilities and the framework's timeout/window policy.
     */
    public static WebDriver create(final BrowserType browser,
                                   final ExecutionMode mode,
                                   final boolean headless) {
        final BrowserOptionsStrategy strategy = OptionsStrategyFactory.forBrowser(browser);
        final MutableCapabilities capabilities = strategy.build(headless);

        final boolean remote = browser.isRemote() || mode != ExecutionMode.LOCAL;
        LOG.info("Creating {} driver [mode={}, headless={}]", browser.key(), mode, headless);

        final WebDriver driver = remote
                ? createRemote(capabilities)
                : createLocal(browser, capabilities);

        applySessionPolicy(driver);
        return driver;
    }

    private static WebDriver createLocal(final BrowserType browser,
                                         final MutableCapabilities capabilities) {
        try {
            return switch (browser) {
                case CHROME, REMOTE_CHROME -> {
                    WebDriverManager.chromedriver().setup();
                    yield new ChromeDriver((ChromeOptions) capabilities);
                }
                case FIREFOX, REMOTE_FIREFOX -> {
                    WebDriverManager.firefoxdriver().setup();
                    yield new FirefoxDriver((FirefoxOptions) capabilities);
                }
                case EDGE, REMOTE_EDGE -> {
                    WebDriverManager.edgedriver().setup();
                    yield new EdgeDriver((EdgeOptions) capabilities);
                }
            };
        } catch (final RuntimeException e) {
            throw new DriverInitializationException(
                    "Failed to start local " + browser.key() + " driver", e);
        }
    }

    private static WebDriver createRemote(final MutableCapabilities capabilities) {
        final String gridUrl = FrameworkConfig.get().gridUrl();
        try {
            final WebDriver driver = new RemoteWebDriver(new URL(gridUrl), capabilities);
            // Enables local-file uploads against a remote node.
            ((RemoteWebDriver) driver).setFileDetector(
                    new org.openqa.selenium.remote.LocalFileDetector());
            return driver;
        } catch (final MalformedURLException e) {
            throw new DriverInitializationException("Invalid Grid URL: " + gridUrl, e);
        } catch (final RuntimeException e) {
            throw new DriverInitializationException(
                    "Failed to start remote driver against " + gridUrl, e);
        }
    }

    private static void applySessionPolicy(final WebDriver driver) {
        final FrameworkConfig config = FrameworkConfig.get();
        driver.manage().timeouts().implicitlyWait(config.implicitTimeout());
        driver.manage().timeouts().pageLoadTimeout(config.pageLoadTimeout());
        driver.manage().timeouts().scriptTimeout(config.scriptTimeout());
        if (config.maximize()) {
            driver.manage().window().maximize();
        }
    }
}
