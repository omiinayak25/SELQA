package com.omiinqa.cloud;

import com.omiinqa.config.FrameworkConfig;

/**
 * Typed accessor facade over the {@code cloud.*} configuration namespace.
 *
 * <p><b>Why a separate class:</b> {@link FrameworkConfig} is a general-purpose
 * facade that must remain unmodified (Foundation Contract).  {@code CloudConfig}
 * acts as a narrow, cloud-specific sub-facade that translates raw
 * {@code FrameworkConfig.raw(key, default)} calls into intention-revealing
 * methods.  Tests and production code read {@code CloudConfig.for(provider)}
 * without knowing the key names, making key renames a one-line change here.</p>
 *
 * <h2>Config keys</h2>
 * <pre>
 *   cloud.browserstack.username   – BrowserStack username          (default: "")
 *   cloud.browserstack.accesskey  – BrowserStack access key        (default: "")
 *   cloud.browserstack.url        – BrowserStack hub URL override  (default: vendor default)
 *
 *   cloud.saucelabs.username      – Sauce Labs username            (default: "")
 *   cloud.saucelabs.accesskey     – Sauce Labs access key         (default: "")
 *   cloud.saucelabs.url           – Sauce Labs hub URL override    (default: vendor default)
 *
 *   cloud.lambdatest.username     – LambdaTest username            (default: "")
 *   cloud.lambdatest.accesskey    – LambdaTest access key          (default: "")
 *   cloud.lambdatest.url          – LambdaTest hub URL override    (default: vendor default)
 * </pre>
 *
 * <p>All keys fall back to an empty string (credentials) or the vendor's
 * documented default URL so the class instantiates cleanly in offline/CI
 * environments that lack real credentials.</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 *   CloudConfig cfg = new CloudConfig(CloudProvider.BROWSERSTACK);
 *   String user = cfg.username();       // reads cloud.browserstack.username
 *   String hubUrl = cfg.hubUrl();       // reads cloud.browserstack.url or falls back to default
 * }</pre>
 */
public final class CloudConfig {

    private final CloudProvider provider;
    private final FrameworkConfig frameworkConfig;

    /**
     * Creates a {@code CloudConfig} for the given provider backed by the
     * framework-wide singleton {@link FrameworkConfig}.
     *
     * @param provider the cloud provider whose keys to resolve (must not be {@code null})
     */
    public CloudConfig(final CloudProvider provider) {
        this(provider, FrameworkConfig.get());
    }

    /**
     * Package-private constructor for unit-test injection of a custom
     * {@link FrameworkConfig} (avoids touching classpath resources in tests).
     *
     * @param provider       the cloud provider
     * @param frameworkConfig config facade to delegate key lookups to
     */
    CloudConfig(final CloudProvider provider, final FrameworkConfig frameworkConfig) {
        if (provider == null) {
            throw new IllegalArgumentException("CloudProvider must not be null");
        }
        this.provider        = provider;
        this.frameworkConfig = frameworkConfig;
    }

    /**
     * Returns the cloud provider this config instance is scoped to.
     *
     * @return provider (never {@code null})
     */
    public CloudProvider provider() {
        return provider;
    }

    /**
     * Returns the username credential for the provider.
     * Reads {@code <prefix>.username}; defaults to {@code ""} when absent.
     *
     * @return username, possibly empty when no credentials are configured
     */
    public String username() {
        return frameworkConfig.raw(provider.usernameKey(), "");
    }

    /**
     * Returns the access key credential for the provider.
     * Reads {@code <prefix>.accesskey}; defaults to {@code ""} when absent.
     *
     * @return access key, possibly empty when no credentials are configured
     */
    public String accessKey() {
        return frameworkConfig.raw(provider.accessKeyKey(), "");
    }

    /**
     * Returns the effective hub URL for the provider.
     * Reads {@code <prefix>.url}; falls back to the vendor's documented
     * default ({@link CloudProvider#defaultHubUrl()}) when absent.
     *
     * @return hub URL (never {@code null})
     */
    public String hubUrl() {
        return frameworkConfig.raw(provider.urlKey(), provider.defaultHubUrl());
    }

    /**
     * Returns the W3C vendor-namespace key for this provider (e.g.
     * {@code bstack:options}, {@code sauce:options}, {@code LT:Options}).
     *
     * @return vendor options key (never {@code null})
     */
    public String vendorOptionsKey() {
        return provider.vendorOptionsKey();
    }
}
