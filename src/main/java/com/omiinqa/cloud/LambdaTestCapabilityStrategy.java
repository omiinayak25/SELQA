package com.omiinqa.cloud;

import com.omiinqa.driver.options.OptionsStrategyFactory;
import com.omiinqa.enums.BrowserType;
import org.openqa.selenium.MutableCapabilities;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds W3C-compliant capabilities for LambdaTest Automate.
 *
 * <p><b>LambdaTest W3C capability structure</b> (as of 2024 Automate API):</p>
 * <pre>
 *   {
 *     "browserName": "MicrosoftEdge",   &lt;-- from base browser options
 *     "LT:Options": {
 *       "username":    "&lt;username&gt;",
 *       "accessKey":   "&lt;accessKey&gt;",
 *       "os":          "Windows",
 *       "osVersion":   "11",
 *       "browserVersion": "latest",
 *       "name":        "My Test",         &lt;-- session name
 *       "build":       "CI-Build-42",
 *       "visual":      false,
 *       "network":     false,
 *       "console":     false
 *     }
 *   }
 * </pre>
 *
 * <p>Note: LambdaTest's vendor-namespace key is {@code LT:Options} (mixed case),
 * which differs from BrowserStack's {@code bstack:options} and Sauce Labs'
 * {@code sauce:options}.  The exact casing is critical and is sourced from
 * {@link CloudProvider#LAMBDATEST#vendorOptionsKey()} to avoid typos.</p>
 *
 * <p>Credentials and hub URL are sourced from {@link CloudConfig} which reads
 * from {@link com.omiinqa.config.FrameworkConfig#raw(String,String)}.  Missing
 * credentials produce empty strings, allowing offline capability inspection.</p>
 */
final class LambdaTestCapabilityStrategy implements CloudCapabilityStrategy {

    private final CloudConfig cloudConfig;

    /**
     * Creates a strategy backed by a {@link CloudConfig} scoped to
     * {@link CloudProvider#LAMBDATEST}.
     *
     * @param cloudConfig config facade for LambdaTest keys (must not be {@code null})
     */
    LambdaTestCapabilityStrategy(final CloudConfig cloudConfig) {
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
        // 1. Reuse existing browser-specific capabilities (headless flags, Chrome
        //    sandbox hardening, Firefox preferences, etc.).
        final MutableCapabilities base =
                OptionsStrategyFactory.forBrowser(browser).build(headless);

        // 2. Build the LT:Options vendor namespace map.
        final Map<String, Object> ltOptions = new HashMap<>();
        ltOptions.put("username",        cloudConfig.username());
        ltOptions.put("accessKey",       cloudConfig.accessKey());
        ltOptions.put("os",              nullSafe(osName,         "Windows"));
        ltOptions.put("osVersion",       nullSafe(osVersion,      "11"));
        ltOptions.put("browserVersion",  nullSafe(browserVersion, "latest"));
        ltOptions.put("name",            nullSafe(sessionName,    "OmiinQA Session"));
        ltOptions.put("build",           nullSafe(buildName,      "OmiinQA Build"));
        // Optional diagnostics — off by default to avoid extra cost.
        ltOptions.put("visual",  false);
        ltOptions.put("network", false);
        ltOptions.put("console", false);

        // 3. Merge vendor options into the base capability set.
        //    cloudConfig.vendorOptionsKey() returns "LT:Options" exactly.
        base.setCapability(cloudConfig.vendorOptionsKey(), ltOptions);

        return base;
    }

    @Override
    public CloudProvider provider() {
        return CloudProvider.LAMBDATEST;
    }

    private static String nullSafe(final String value, final String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
