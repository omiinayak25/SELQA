package com.omiinqa.cloud;

import com.omiinqa.driver.options.OptionsStrategyFactory;
import com.omiinqa.enums.BrowserType;
import org.openqa.selenium.MutableCapabilities;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds W3C-compliant capabilities for Sauce Labs OnDemand.
 *
 * <p><b>Sauce Labs W3C capability structure</b> (as of 2024 Unified Platform):</p>
 * <pre>
 *   {
 *     "browserName": "firefox",         &lt;-- from base browser options
 *     "sauce:options": {
 *       "username":    "&lt;username&gt;",
 *       "accessKey":   "&lt;accessKey&gt;",
 *       "name":        "My Test",         &lt;-- maps to sessionName
 *       "build":       "CI-Build-42",
 *       "browserVersion": "latest",
 *       "platformName":   "Windows 11"   &lt;-- composed from osName + osVersion
 *     }
 *   }
 * </pre>
 *
 * <p>Sauce Labs uses {@code platformName} at the top-level W3C capability
 * (outside the vendor namespace) but the recommended practice is to declare
 * it inside {@code sauce:options} as well for compatibility with older
 * Unified Platform versions.  This implementation places it in
 * {@code sauce:options} only, which works with current endpoints.</p>
 *
 * <p>Credentials and hub URL are sourced from {@link CloudConfig} which reads
 * from {@link com.omiinqa.config.FrameworkConfig#raw(String,String)}.  Missing
 * credentials produce empty strings, allowing offline capability inspection.</p>
 */
final class SauceLabsCapabilityStrategy implements CloudCapabilityStrategy {

    private final CloudConfig cloudConfig;

    /**
     * Creates a strategy backed by a {@link CloudConfig} scoped to
     * {@link CloudProvider#SAUCELABS}.
     *
     * @param cloudConfig config facade for Sauce Labs keys (must not be {@code null})
     */
    SauceLabsCapabilityStrategy(final CloudConfig cloudConfig) {
        this.cloudConfig = cloudConfig;
    }

    @Override
    public MutableCapabilities buildCapabilities(final BrowserType browser,
                                                 final boolean headless,
                                                 final String sessionName,
                                                 final String buildName,
                                                 final String osName,
                                                 final String osVersion,
                                                 final String browserVersion) {
        // 1. Reuse existing browser-specific capabilities (headless, arguments, prefs).
        final MutableCapabilities base =
                OptionsStrategyFactory.forBrowser(browser).build(headless);

        // 2. Build the sauce:options vendor namespace map.
        final Map<String, Object> sauceOptions = new HashMap<>();
        sauceOptions.put("username",        cloudConfig.username());
        sauceOptions.put("accessKey",       cloudConfig.accessKey());
        sauceOptions.put("name",            nullSafe(sessionName,    "OmiinQA Session"));
        sauceOptions.put("build",           nullSafe(buildName,      "OmiinQA Build"));
        sauceOptions.put("browserVersion",  nullSafe(browserVersion, "latest"));

        // Sauce Labs uses a combined "platformName" string such as "Windows 11".
        final String platform = composePlatform(osName, osVersion);
        sauceOptions.put("platformName",    platform);

        // 3. Merge vendor options into the base capability set.
        base.setCapability(cloudConfig.vendorOptionsKey(), sauceOptions);

        return base;
    }

    @Override
    public CloudProvider provider() {
        return CloudProvider.SAUCELABS;
    }

    /**
     * Composes a Sauce Labs {@code platformName} string from separate OS name and
     * version inputs (e.g. {@code "Windows"} + {@code "11"} → {@code "Windows 11"}).
     * Falls back to a sensible default when either part is blank.
     */
    private static String composePlatform(final String osName, final String osVersion) {
        final String name    = (osName    == null || osName.isBlank())    ? "Windows" : osName.trim();
        final String version = (osVersion == null || osVersion.isBlank()) ? "11"      : osVersion.trim();
        return name + " " + version;
    }

    private static String nullSafe(final String value, final String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
