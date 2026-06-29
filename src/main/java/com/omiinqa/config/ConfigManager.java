package com.omiinqa.config;

import com.omiinqa.constants.FrameworkConstants;
import com.omiinqa.enums.Environment;
import com.omiinqa.exceptions.ConfigurationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Thread-safe, lazily-initialized configuration provider (Singleton pattern).
 *
 * <p><b>Why Singleton:</b> configuration is global, immutable read-only state
 * loaded once per JVM. A single instance avoids re-reading files on every
 * lookup while remaining safe for parallel TestNG threads, which only ever
 * read.</p>
 *
 * <p><b>Resolution precedence</b> (highest wins):</p>
 * <ol>
 *   <li>JVM system properties / {@code -Dkey=value} (CI & local overrides)</li>
 *   <li>Environment overlay {@code config/env/<env>.properties}</li>
 *   <li>Base {@code config/config.properties}</li>
 * </ol>
 *
 * <p>The {@code env} itself is resolved first (from {@code -Denv} or the base
 * file) so the correct overlay can be chosen.</p>
 */
public final class ConfigManager {

    private static volatile ConfigManager instance;

    private final Properties properties;
    private final Environment environment;

    private ConfigManager() {
        this.properties = new Properties();
        loadBase();
        this.environment = resolveEnvironment();
        loadEnvironmentOverlay(this.environment);
    }

    /** Double-checked locking keeps first-touch lazy yet parallel-safe. */
    public static ConfigManager get() {
        if (instance == null) {
            synchronized (ConfigManager.class) {
                if (instance == null) {
                    instance = new ConfigManager();
                }
            }
        }
        return instance;
    }

    private void loadBase() {
        loadInto(FrameworkConstants.BASE_CONFIG, true);
    }

    private Environment resolveEnvironment() {
        final String sys = System.getProperty(FrameworkConstants.KEY_ENV);
        final String fromFile = properties.getProperty(FrameworkConstants.KEY_ENV);
        return Environment.from(sys != null ? sys : fromFile);
    }

    private void loadEnvironmentOverlay(final Environment env) {
        // Overlay is optional — absence is not fatal, base values remain.
        loadInto(FrameworkConstants.ENV_CONFIG_DIR + env.fileName(), false);
    }

    private void loadInto(final String classpathResource, final boolean required) {
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(classpathResource)) {
            if (in == null) {
                if (required) {
                    throw new ConfigurationException(
                            "Required configuration not found on classpath: " + classpathResource);
                }
                return;
            }
            properties.load(in);
        } catch (final IOException e) {
            throw new ConfigurationException(
                    "Failed to load configuration: " + classpathResource, e);
        }
    }

    // ----------------------------------------------------------------- lookups

    /**
     * Resolve a key honoring precedence. System properties always win so a
     * {@code -Dbrowser=firefox} on the Maven command line overrides files.
     *
     * @throws ConfigurationException when the key is absent everywhere
     */
    public String get(final String key) {
        final String sys = System.getProperty(key);
        if (sys != null && !sys.isBlank()) {
            return sys.trim();
        }
        final String value = properties.getProperty(key);
        if (value == null) {
            throw new ConfigurationException("Missing configuration key: " + key);
        }
        return value.trim();
    }

    public String get(final String key, final String defaultValue) {
        final String sys = System.getProperty(key);
        if (sys != null && !sys.isBlank()) {
            return sys.trim();
        }
        final String value = properties.getProperty(key);
        return value == null ? defaultValue : value.trim();
    }

    public int getInt(final String key) {
        return parseInt(key, get(key));
    }

    public int getInt(final String key, final int defaultValue) {
        final String raw = get(key, null);
        return raw == null ? defaultValue : parseInt(key, raw);
    }

    public long getLong(final String key, final long defaultValue) {
        final String raw = get(key, null);
        try {
            return raw == null ? defaultValue : Long.parseLong(raw);
        } catch (final NumberFormatException e) {
            throw new ConfigurationException("Config key '" + key + "' is not a long: " + raw, e);
        }
    }

    public boolean getBoolean(final String key, final boolean defaultValue) {
        final String raw = get(key, null);
        return raw == null ? defaultValue : Boolean.parseBoolean(raw);
    }

    public Environment environment() {
        return environment;
    }

    private int parseInt(final String key, final String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (final NumberFormatException e) {
            throw new ConfigurationException("Config key '" + key + "' is not an int: " + raw, e);
        }
    }
}
