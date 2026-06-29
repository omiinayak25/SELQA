package com.omiinqa.cloud;

import com.omiinqa.enums.BrowserType;
import org.openqa.selenium.MutableCapabilities;

import java.util.EnumMap;
import java.util.Map;

/**
 * Factory that assembles fully-formed W3C capabilities for any combination of
 * cloud provider and browser, using the Strategy pattern to delegate
 * provider-specific merging to the appropriate {@link CloudCapabilityStrategy}.
 *
 * <p><b>Design — why Factory + Strategy:</b></p>
 * <ul>
 *   <li>This class is the single entry point for callers: they hand in a
 *       {@link CloudProvider}, a {@link BrowserType}, and session metadata, and
 *       receive ready-to-use {@link MutableCapabilities}.</li>
 *   <li>The concrete strategy (BrowserStack / Sauce Labs / LambdaTest) is
 *       selected at runtime from an {@code EnumMap}, keeping this class free of
 *       vendor-specific logic (Single Responsibility).</li>
 *   <li>Adding a fourth provider requires only one new strategy class and one
 *       {@code put} into the map — no existing code changes (Open/Closed).</li>
 * </ul>
 *
 * <h2>Usage example</h2>
 * <pre>{@code
 *   MutableCapabilities caps = CloudCapabilityFactory.build(
 *       CloudProvider.BROWSERSTACK,
 *       BrowserType.CHROME,
 *       "Windows", "11", "latest",
 *       "Login smoke test", "Sprint-42-CI"
 *   );
 *   // caps now contains browserName + bstack:options { userName, accessKey, ... }
 * }</pre>
 *
 * <h2>Config keys read (transitively via {@link CloudConfig})</h2>
 * <pre>
 *   cloud.browserstack.username / accesskey / url
 *   cloud.saucelabs.username    / accesskey / url
 *   cloud.lambdatest.username   / accesskey / url
 * </pre>
 *
 * <p>All keys default to empty strings / vendor defaults so this factory can
 * instantiate and build capabilities in offline unit-test environments without
 * real credentials.</p>
 */
public final class CloudCapabilityFactory {

    /** One strategy per provider, lazily populated once per factory instance. */
    private final Map<CloudProvider, CloudCapabilityStrategy> strategies;

    /**
     * Creates a {@code CloudCapabilityFactory} whose strategies read config from
     * the framework-wide {@link com.omiinqa.config.FrameworkConfig} singleton.
     */
    public CloudCapabilityFactory() {
        this.strategies = buildDefaultStrategies();
    }

    /**
     * Package-private constructor for testing: accepts pre-built strategies so
     * unit tests can inject stubs / verify routing without touching
     * {@link com.omiinqa.config.FrameworkConfig}.
     *
     * @param strategies map of provider → strategy to use instead of defaults
     */
    CloudCapabilityFactory(final Map<CloudProvider, CloudCapabilityStrategy> strategies) {
        this.strategies = new EnumMap<>(strategies);
    }

    // ------------------------------------------------------------------ public API

    /**
     * Builds fully-formed W3C capabilities for the given cloud provider and
     * browser, merging base browser options with vendor authentication and
     * session metadata.
     *
     * @param provider       target cloud provider (must not be {@code null})
     * @param browser        target browser (must not be {@code null})
     * @param osName         target operating system name (e.g. {@code "Windows"}, {@code "OS X"});
     *                       {@code null} uses a sensible provider default
     * @param osVersion      target OS version (e.g. {@code "11"}, {@code "Ventura"});
     *                       {@code null} uses a sensible provider default
     * @param browserVersion specific browser version (e.g. {@code "latest"}, {@code "120"});
     *                       {@code null} defaults to {@code "latest"}
     * @param sessionName    human-readable name shown in the cloud dashboard
     * @param buildName      CI build identifier for grouping sessions
     * @return merged {@link MutableCapabilities} ready for
     *         {@link org.openqa.selenium.remote.RemoteWebDriver}
     * @throws IllegalArgumentException if the provider has no registered strategy
     */
    public MutableCapabilities build(final CloudProvider provider,
                                     final BrowserType browser,
                                     final String osName,
                                     final String osVersion,
                                     final String browserVersion,
                                     final String sessionName,
                                     final String buildName) {
        final CloudCapabilityStrategy strategy = resolveStrategy(provider);
        return strategy.buildCapabilities(
                browser, false, sessionName, buildName, osName, osVersion, browserVersion);
    }

    /**
     * Overload that additionally controls the headless flag — useful when cloud
     * providers support headless execution (BrowserStack does as of 2024).
     *
     * @param provider       target cloud provider (must not be {@code null})
     * @param browser        target browser (must not be {@code null})
     * @param headless       whether to run the browser without a visible UI
     * @param osName         target OS name
     * @param osVersion      target OS version
     * @param browserVersion target browser version
     * @param sessionName    session name shown in the cloud dashboard
     * @param buildName      CI build identifier
     * @return merged {@link MutableCapabilities}
     */
    public MutableCapabilities build(final CloudProvider provider,
                                     final BrowserType browser,
                                     final boolean headless,
                                     final String osName,
                                     final String osVersion,
                                     final String browserVersion,
                                     final String sessionName,
                                     final String buildName) {
        final CloudCapabilityStrategy strategy = resolveStrategy(provider);
        return strategy.buildCapabilities(
                browser, headless, sessionName, buildName, osName, osVersion, browserVersion);
    }

    /**
     * Returns the registered {@link CloudCapabilityStrategy} for the given
     * provider, for cases where callers need direct access to strategy metadata.
     *
     * @param provider cloud provider (must not be {@code null})
     * @return the registered strategy (never {@code null})
     * @throws IllegalArgumentException if no strategy is registered for the provider
     */
    public CloudCapabilityStrategy strategyFor(final CloudProvider provider) {
        return resolveStrategy(provider);
    }

    // ------------------------------------------------------------------ private

    private CloudCapabilityStrategy resolveStrategy(final CloudProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("CloudProvider must not be null");
        }
        final CloudCapabilityStrategy strategy = strategies.get(provider);
        if (strategy == null) {
            throw new IllegalArgumentException(
                    "No CloudCapabilityStrategy registered for provider: " + provider);
        }
        return strategy;
    }

    /** Builds the default strategy map using the live {@link CloudConfig} singletons. */
    private static Map<CloudProvider, CloudCapabilityStrategy> buildDefaultStrategies() {
        final EnumMap<CloudProvider, CloudCapabilityStrategy> map =
                new EnumMap<>(CloudProvider.class);

        map.put(CloudProvider.BROWSERSTACK,
                new BrowserStackCapabilityStrategy(new CloudConfig(CloudProvider.BROWSERSTACK)));
        map.put(CloudProvider.SAUCELABS,
                new SauceLabsCapabilityStrategy(new CloudConfig(CloudProvider.SAUCELABS)));
        map.put(CloudProvider.LAMBDATEST,
                new LambdaTestCapabilityStrategy(new CloudConfig(CloudProvider.LAMBDATEST)));

        return map;
    }
}
