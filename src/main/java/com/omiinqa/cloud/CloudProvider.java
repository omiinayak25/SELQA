package com.omiinqa.cloud;

/**
 * Enumeration of supported cloud cross-browser execution providers.
 *
 * <p><b>Why an enum:</b> each vendor has a fixed set of associated constants —
 * the config-key prefix used to look up credentials, the default hub URL when
 * none is overridden in config, and the W3C vendor-namespace key that must
 * appear in the capability set.  Encoding all three per-variant values here
 * keeps the rest of the cloud package free of vendor-specific {@code if/switch}
 * chains and makes adding a fourth provider a two-line change.</p>
 *
 * <h2>Config keys consumed (read via {@code FrameworkConfig.raw})</h2>
 * <pre>
 *   cloud.browserstack.username   – BrowserStack username
 *   cloud.browserstack.accesskey  – BrowserStack access key
 *   cloud.browserstack.url        – Hub URL (optional, defaults to {@value #BSTACK_DEFAULT_URL})
 *
 *   cloud.saucelabs.username      – Sauce Labs username
 *   cloud.saucelabs.accesskey     – Sauce Labs access key
 *   cloud.saucelabs.url           – Hub URL (optional, defaults to US West datacenter)
 *
 *   cloud.lambdatest.username     – LambdaTest username
 *   cloud.lambdatest.accesskey    – LambdaTest access key
 *   cloud.lambdatest.url          – Hub URL (optional, defaults to {@value #LT_DEFAULT_URL})
 * </pre>
 *
 * <p>All keys are read with a blank-string default so the framework can be
 * instantiated and capabilities built without real credentials — tests that
 * only inspect capability objects will compile and pass offline.</p>
 */
public enum CloudProvider {

    /**
     * BrowserStack — {@code bstack:options} W3C namespace.
     * Default hub: {@code https://hub-cloud.browserstack.com/wd/hub}
     */
    BROWSERSTACK(
            "cloud.browserstack",
            "https://hub-cloud.browserstack.com/wd/hub",
            "bstack:options"),

    /**
     * Sauce Labs — {@code sauce:options} W3C namespace.
     * Default hub: US West datacenter OnDemand endpoint.
     */
    SAUCELABS(
            "cloud.saucelabs",
            "https://ondemand.us-west-1.saucelabs.com/wd/hub",
            "sauce:options"),

    /**
     * LambdaTest — {@code LT:Options} W3C namespace.
     * Default hub: {@code https://hub.lambdatest.com/wd/hub}
     */
    LAMBDATEST(
            "cloud.lambdatest",
            "https://hub.lambdatest.com/wd/hub",
            "LT:Options");

    // ------------------------------------------------------------------ fields

    /** Config-key prefix, e.g. {@code cloud.browserstack}. */
    private final String configPrefix;

    /** Hub URL used when {@code <prefix>.url} is absent in config. */
    private final String defaultHubUrl;

    /** W3C vendor-namespace key placed inside the capabilities map. */
    private final String vendorOptionsKey;

    // ------------------------------------------------------------------ ctor

    CloudProvider(final String configPrefix,
                  final String defaultHubUrl,
                  final String vendorOptionsKey) {
        this.configPrefix    = configPrefix;
        this.defaultHubUrl   = defaultHubUrl;
        this.vendorOptionsKey = vendorOptionsKey;
    }

    // ------------------------------------------------------------------ accessors

    /**
     * Returns the config-key prefix for this provider, e.g.
     * {@code cloud.browserstack}.  Append {@code .username},
     * {@code .accesskey}, or {@code .url} to form complete keys.
     *
     * @return config-key prefix (never {@code null})
     */
    public String configPrefix() {
        return configPrefix;
    }

    /**
     * Returns the vendor's documented hub URL used when no override is present
     * in the project configuration.
     *
     * @return default hub URL (never {@code null})
     */
    public String defaultHubUrl() {
        return defaultHubUrl;
    }

    /**
     * Returns the W3C vendor-namespace key that wraps provider-specific
     * options inside the capability map (e.g. {@code bstack:options}).
     *
     * @return vendor options key (never {@code null})
     */
    public String vendorOptionsKey() {
        return vendorOptionsKey;
    }

    /**
     * Convenience helper: returns the config key for the username credential.
     *
     * @return {@code <prefix>.username}
     */
    public String usernameKey() {
        return configPrefix + ".username";
    }

    /**
     * Convenience helper: returns the config key for the access-key credential.
     *
     * @return {@code <prefix>.accesskey}
     */
    public String accessKeyKey() {
        return configPrefix + ".accesskey";
    }

    /**
     * Convenience helper: returns the config key for the hub URL override.
     *
     * @return {@code <prefix>.url}
     */
    public String urlKey() {
        return configPrefix + ".url";
    }
}
