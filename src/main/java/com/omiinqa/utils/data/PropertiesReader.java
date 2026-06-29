package com.omiinqa.utils.data;

import com.omiinqa.exceptions.DataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Typed, classpath-based {@code .properties} file reader.
 *
 * <p><strong>Pattern:</strong> Immutable Value Object — the properties are
 * loaded once at construction time and never mutated. Each instance is bound
 * to a single file, which makes the source of truth explicit in test code
 * and prevents cross-file key collisions.</p>
 *
 * <p>Unlike the framework's {@code ConfigManager} (which drives infrastructure
 * configuration), this reader is for <em>test data</em> property files — e.g.
 * environment-specific test account credentials or feature-flag overrides that
 * must not be embedded in source.</p>
 *
 * <p><strong>Usage:</strong>
 * <pre>{@code
 *   PropertiesReader props = new PropertiesReader("testdata/test-accounts.properties");
 *   String username = props.get("admin.username");
 *   int timeout     = props.getInt("retry.count");
 *   boolean enabled = props.getBoolean("feature.darkmode", false);
 * }</pre>
 * </p>
 */
public final class PropertiesReader {

    private static final Logger log = LoggerFactory.getLogger(PropertiesReader.class);

    private final Properties props;
    private final String source;

    /**
     * Loads the given classpath resource as a {@link Properties} file.
     *
     * @param classpath classpath-relative path, e.g. {@code "testdata/test-accounts.properties"}
     * @throws DataException if the resource cannot be found or read
     */
    public PropertiesReader(final String classpath) {
        this.source = classpath;
        this.props = load(classpath);
        log.debug("Loaded {} properties from '{}'", props.size(), classpath);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns the raw string value for the given key.
     *
     * @param key property key
     * @return value; never {@code null}
     * @throws DataException if the key is absent
     */
    public String get(final String key) {
        String value = props.getProperty(key);
        if (value == null) {
            throw new DataException(
                    "Property key '" + key + "' not found in '" + source + "'");
        }
        return value;
    }

    /**
     * Returns the value for the given key, or {@code defaultValue} if absent.
     *
     * @param key          property key
     * @param defaultValue fallback value
     * @return resolved value or default
     */
    public String get(final String key, final String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    /**
     * Returns the value parsed as an {@code int}.
     *
     * @param key property key
     * @return integer value
     * @throws DataException if the key is absent or the value is not a valid integer
     */
    public int getInt(final String key) {
        String raw = get(key);
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new DataException(
                    "Property '" + key + "' = '" + raw + "' is not a valid integer in '"
                            + source + "'", e);
        }
    }

    /**
     * Returns the value parsed as a {@code long}.
     *
     * @param key property key
     * @return long value
     * @throws DataException if the key is absent or the value is not a valid long
     */
    public long getLong(final String key) {
        String raw = get(key);
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            throw new DataException(
                    "Property '" + key + "' = '" + raw + "' is not a valid long in '"
                            + source + "'", e);
        }
    }

    /**
     * Returns the value parsed as a {@code boolean}.
     * Recognised truthy strings: {@code "true"}, {@code "yes"}, {@code "1"} (case-insensitive).
     *
     * @param key          property key
     * @param defaultValue fallback if key is absent
     * @return boolean value
     */
    public boolean getBoolean(final String key, final boolean defaultValue) {
        String raw = props.getProperty(key);
        if (raw == null) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(raw.trim())
                || "yes".equalsIgnoreCase(raw.trim())
                || "1".equals(raw.trim());
    }

    /**
     * Returns the value parsed as a {@code double}.
     *
     * @param key property key
     * @return double value
     * @throws DataException if the key is absent or the value is not a valid double
     */
    public double getDouble(final String key) {
        String raw = get(key);
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            throw new DataException(
                    "Property '" + key + "' = '" + raw + "' is not a valid double in '"
                            + source + "'", e);
        }
    }

    /**
     * Returns {@code true} if the given key exists in the file.
     *
     * @param key property key
     * @return {@code true} if present
     */
    public boolean contains(final String key) {
        return props.containsKey(key);
    }

    /**
     * Exposes all loaded properties for iteration.
     *
     * @return an unmodifiable snapshot of the properties
     */
    public Properties asProperties() {
        Properties copy = new Properties();
        copy.putAll(props);
        return copy;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private static Properties load(final String classpath) {
        InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(classpath);
        if (is == null) {
            is = PropertiesReader.class.getResourceAsStream("/" + classpath);
        }
        if (is == null) {
            throw new DataException("Classpath resource not found: '" + classpath + "'");
        }
        final InputStream resolvedStream = is;
        Properties p = new Properties();
        try (resolvedStream) {
            p.load(resolvedStream);
        } catch (IOException e) {
            throw new DataException("Failed to load properties from '" + classpath + "'", e);
        }
        return p;
    }
}
