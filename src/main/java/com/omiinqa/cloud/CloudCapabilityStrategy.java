package com.omiinqa.cloud;

import com.omiinqa.enums.BrowserType;
import org.openqa.selenium.MutableCapabilities;

/**
 * Strategy interface for building provider-specific W3C capability maps.
 *
 * <p><b>Why Strategy:</b> BrowserStack, Sauce Labs, and LambdaTest each require
 * a different vendor-namespace key and a different internal structure for
 * authentication and session metadata.  Encoding those differences in one large
 * {@code switch} block would couple the factory to every provider and make it
 * hard to add a fourth vendor without touching existing logic.</p>
 *
 * <p>Each concrete implementation encapsulates exactly one vendor's
 * requirements, so adding a new provider means adding one class and registering
 * it — existing classes are untouched (Open/Closed Principle).</p>
 *
 * <p>Implementations receive a pre-built base {@link MutableCapabilities} from
 * the existing {@link com.omiinqa.driver.options.OptionsStrategyFactory} so
 * browser-specific arguments (headless flags, Chrome sandbox settings, etc.)
 * are never duplicated.</p>
 *
 * <h2>Contract</h2>
 * <ul>
 *   <li>The returned capabilities must include {@code browserName} (via base merge).</li>
 *   <li>The vendor-namespace key must map to a {@code Map&lt;String,Object&gt;}
 *       containing at least {@code username} and {@code accessKey}.</li>
 *   <li>Implementations must be stateless so a single instance can serve
 *       multiple parallel test threads.</li>
 *   <li>Implementations must never initiate a network connection.</li>
 * </ul>
 */
public interface CloudCapabilityStrategy {

    /**
     * Build fully-formed W3C capabilities for the given browser merged with
     * vendor-specific authentication and session metadata.
     *
     * @param browser        target browser (determines base capability set)
     * @param headless       whether the browser should run without a visible UI
     * @param sessionName    human-readable session name shown in the cloud dashboard
     * @param buildName      CI build name or identifier for grouping sessions
     * @param osName         target operating system name (e.g. {@code "Windows"}, {@code "OS X"})
     * @param osVersion      target OS version (e.g. {@code "11"}, {@code "Ventura"})
     * @param browserVersion specific browser version to request (e.g. {@code "latest"}, {@code "120"})
     * @return merged capabilities ready to pass to {@link org.openqa.selenium.remote.RemoteWebDriver}
     */
    MutableCapabilities buildCapabilities(BrowserType browser,
                                          boolean headless,
                                          String sessionName,
                                          String buildName,
                                          String osName,
                                          String osVersion,
                                          String browserVersion);

    /**
     * Returns the {@link CloudProvider} this strategy targets.
     *
     * @return provider (never {@code null})
     */
    CloudProvider provider();
}
