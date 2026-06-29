package com.omiinqa.cloud;

import com.omiinqa.enums.BrowserType;
import com.omiinqa.exceptions.DriverInitializationException;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Creates and configures a {@link RemoteWebDriver} pointed at a cloud provider's
 * Selenium hub, using capabilities built by {@link CloudCapabilityFactory}.
 *
 * <p><b>Responsibilities:</b></p>
 * <ol>
 *   <li>Resolve the hub URL from config (with vendor default fallback).</li>
 *   <li>Delegate capability construction to {@link CloudCapabilityFactory}.</li>
 *   <li>Instantiate {@link RemoteWebDriver} and configure {@link LocalFileDetector}
 *       so local file uploads are forwarded to the remote node automatically.</li>
 *   <li>Wrap any connection failure in a {@link DriverInitializationException} with a
 *       meaningful message for diagnostics in CI logs.</li>
 * </ol>
 *
 * <p><b>Lazy connection contract:</b> no network connection is established at
 * construction time.  The driver is created only when
 * {@link #createDriver(BrowserType, String, String, String, String, String)} is
 * called, so the class can be instantiated freely (e.g. in a factory or Spring
 * context) without side-effects.</p>
 *
 * <h2>Config keys read (transitively via {@link CloudConfig})</h2>
 * <pre>
 *   cloud.browserstack.username / accesskey / url
 *   cloud.saucelabs.username    / accesskey / url
 *   cloud.lambdatest.username   / accesskey / url
 * </pre>
 *
 * <h2>Usage example</h2>
 * <pre>{@code
 *   CloudDriverProvider provider = new CloudDriverProvider(CloudProvider.BROWSERSTACK);
 *   RemoteWebDriver driver = provider.createDriver(
 *       BrowserType.CHROME,
 *       "Windows", "11", "latest",
 *       "Login smoke test", "Sprint-42-CI"
 *   );
 * }</pre>
 */
public final class CloudDriverProvider {

    private static final Logger log = LoggerFactory.getLogger(CloudDriverProvider.class);

    private final CloudProvider cloudProvider;
    private final CloudConfig cloudConfig;
    private final CloudCapabilityFactory capabilityFactory;

    /**
     * Creates a provider for the given cloud vendor.  Configuration is sourced
     * from the framework-wide {@link com.omiinqa.config.FrameworkConfig} singleton.
     * No network connection is made at construction time.
     *
     * @param cloudProvider the cloud execution vendor (must not be {@code null})
     */
    public CloudDriverProvider(final CloudProvider cloudProvider) {
        this(cloudProvider, new CloudConfig(cloudProvider), new CloudCapabilityFactory());
    }

    /**
     * Package-private constructor for dependency injection in unit tests.
     * Allows substituting a custom {@link CloudConfig} and
     * {@link CloudCapabilityFactory} without a live config file.
     *
     * @param cloudProvider     the vendor
     * @param cloudConfig       pre-configured config facade
     * @param capabilityFactory capability factory to delegate to
     */
    CloudDriverProvider(final CloudProvider cloudProvider,
                        final CloudConfig cloudConfig,
                        final CloudCapabilityFactory capabilityFactory) {
        if (cloudProvider == null) {
            throw new IllegalArgumentException("CloudProvider must not be null");
        }
        this.cloudProvider     = cloudProvider;
        this.cloudConfig       = cloudConfig;
        this.capabilityFactory = capabilityFactory;
    }

    // ------------------------------------------------------------------ public API

    /**
     * Creates a {@link RemoteWebDriver} connected to the cloud provider's hub
     * using the given browser and session parameters.
     *
     * <p>The hub URL is resolved as follows (highest-priority first):</p>
     * <ol>
     *   <li>JVM system property {@code cloud.<provider>.url}</li>
     *   <li>Config file property {@code cloud.<provider>.url}</li>
     *   <li>Vendor-documented default URL ({@link CloudProvider#defaultHubUrl()})</li>
     * </ol>
     *
     * @param browser        target browser (must not be {@code null})
     * @param osName         target operating system (e.g. {@code "Windows"}, {@code "OS X"})
     * @param osVersion      target OS version (e.g. {@code "11"}, {@code "Ventura"})
     * @param browserVersion specific browser version ({@code "latest"} is recommended)
     * @param sessionName    human-readable session name visible in the cloud dashboard
     * @param buildName      CI build identifier for grouping test sessions
     * @return a live {@link RemoteWebDriver} with {@link LocalFileDetector} configured
     * @throws DriverInitializationException if the hub URL is malformed or the
     *                                       RemoteWebDriver session cannot be established
     */
    public RemoteWebDriver createDriver(final BrowserType browser,
                                        final String osName,
                                        final String osVersion,
                                        final String browserVersion,
                                        final String sessionName,
                                        final String buildName) {
        final String hubUrlString = cloudConfig.hubUrl();
        log.info("Creating {} driver for {} on {}/{} (session='{}')",
                cloudProvider, browser, osName, osVersion, sessionName);

        final URL hubUrl = parseHubUrl(hubUrlString);

        final MutableCapabilities capabilities = capabilityFactory.build(
                cloudProvider, browser,
                osName, osVersion, browserVersion,
                sessionName, buildName);

        try {
            final RemoteWebDriver driver = new RemoteWebDriver(hubUrl, capabilities);
            driver.setFileDetector(new LocalFileDetector());
            log.info("Cloud driver created successfully. Session id: {}",
                    driver.getSessionId());
            return driver;
        } catch (final Exception e) {
            throw new DriverInitializationException(
                    String.format("Failed to create %s RemoteWebDriver for browser %s at %s: %s",
                            cloudProvider, browser, hubUrlString, e.getMessage()), e);
        }
    }

    /**
     * Returns the cloud provider this instance is configured for.
     *
     * @return provider (never {@code null})
     */
    public CloudProvider cloudProvider() {
        return cloudProvider;
    }

    /**
     * Returns the effective hub URL that would be used for driver creation,
     * resolved from config or vendor default — without connecting.
     *
     * @return hub URL string (never {@code null})
     */
    public String resolvedHubUrl() {
        return cloudConfig.hubUrl();
    }

    // ------------------------------------------------------------------ private

    private URL parseHubUrl(final String hubUrlString) {
        try {
            return new URL(hubUrlString);
        } catch (final MalformedURLException e) {
            throw new DriverInitializationException(
                    "Cloud hub URL is malformed: '" + hubUrlString + "'. "
                    + "Set cloud." + cloudProvider.name().toLowerCase() + ".url in config.", e);
        }
    }
}
