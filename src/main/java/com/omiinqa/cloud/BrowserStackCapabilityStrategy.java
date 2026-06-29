package com.omiinqa.cloud;

import com.omiinqa.driver.options.OptionsStrategyFactory;
import com.omiinqa.enums.BrowserType;
import org.openqa.selenium.MutableCapabilities;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds W3C-compliant capabilities for BrowserStack Automate.
 *
 * <p><b>BrowserStack W3C capability structure</b> (as of 2024 Automate API):</p>
 * <pre>
 *   {
 *     "browserName": "chrome",          &lt;-- from base browser options
 *     "bstack:options": {
 *       "userName":    "&lt;username&gt;",
 *       "accessKey":   "&lt;accessKey&gt;",
 *       "os":          "Windows",
 *       "osVersion":   "11",
 *       "browserVersion": "latest",
 *       "sessionName": "My Test",
 *       "buildName":   "CI-Build-42"
 *     }
 *   }
 * </pre>
 *
 * <p>Credentials and the hub URL are sourced from {@link CloudConfig} which in
 * turn reads them from {@link com.omiinqa.config.FrameworkConfig#raw(String,String)}.
 * When credentials are absent (offline / unit-test mode) the options map will
 * contain empty strings — capabilities can still be built and inspected.</p>
 */
final class BrowserStackCapabilityStrategy implements CloudCapabilityStrategy {

    private final CloudConfig cloudConfig;

    /**
     * Creates a strategy backed by a {@link CloudConfig} scoped to
     * {@link CloudProvider#BROWSERSTACK}.
     *
     * @param cloudConfig config facade for BrowserStack keys (must not be {@code null})
     */
    BrowserStackCapabilityStrategy(final CloudConfig cloudConfig) {
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
        // 1. Start from the browser-specific base capabilities (Chrome flags,
        //    Firefox prefs, etc.) so we reuse existing hardening without duplication.
        final MutableCapabilities base =
                OptionsStrategyFactory.forBrowser(browser).build(headless);

        // 2. Build the bstack:options vendor namespace map.
        final Map<String, Object> bstackOptions = new HashMap<>();
        bstackOptions.put("userName",       cloudConfig.username());
        bstackOptions.put("accessKey",      cloudConfig.accessKey());
        bstackOptions.put("os",             nullSafe(osName,         "Windows"));
        bstackOptions.put("osVersion",      nullSafe(osVersion,      "11"));
        bstackOptions.put("browserVersion", nullSafe(browserVersion, "latest"));
        bstackOptions.put("sessionName",    nullSafe(sessionName,    "OmiinQA Session"));
        bstackOptions.put("buildName",      nullSafe(buildName,      "OmiinQA Build"));

        // 3. Merge vendor options into the base capability set.
        base.setCapability(cloudConfig.vendorOptionsKey(), bstackOptions);

        return base;
    }

    @Override
    public CloudProvider provider() {
        return CloudProvider.BROWSERSTACK;
    }

    private static String nullSafe(final String value, final String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
