package com.omiinqa.i18n;

import com.omiinqa.exceptions.FrameworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads and caches {@link ResourceBundle} message files for the application under test.
 *
 * <p><strong>Design pattern:</strong> Facade + Cache-Aside. This class is a thin facade
 * over the JDK {@link ResourceBundle} API that adds:
 * <ul>
 *   <li>A per-(baseName, locale) cache so bundles are not repeatedly reloaded from disk
 *       during a test run (important for parallel suites).</li>
 *   <li>Graceful fallback to the {@link LocaleManager#DEFAULT_LOCALE} when a key is
 *       missing in the requested locale.</li>
 *   <li>Structured missing-key detection used by {@link LocalizationValidator}.</li>
 * </ul>
 *
 * <p><strong>Classpath convention:</strong> Message files must live on the classpath at
 * {@code i18n/messages_<lang>.properties}, e.g.:
 * <ul>
 *   <li>{@code src/test/resources/i18n/messages_en.properties}</li>
 *   <li>{@code src/test/resources/i18n/messages_es.properties}</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * LocalizationBundle bundle = new LocalizationBundle("i18n/messages");
 * String title = bundle.get("login.title", Locale.forLanguageTag("es"));
 * }</pre>
 */
public class LocalizationBundle {

    private static final Logger LOG = LoggerFactory.getLogger(LocalizationBundle.class);

    /** Default base name for the framework's own message files. */
    public static final String DEFAULT_BASE_NAME = "i18n/messages";

    private final String baseName;

    /**
     * ConcurrentHashMap used as a thread-safe cache: key = Locale.toLanguageTag(),
     * value = the loaded ResourceBundle (or MISSING sentinel).
     */
    private final ConcurrentHashMap<String, ResourceBundle> cache = new ConcurrentHashMap<>();

    /**
     * Sentinel object stored in the cache to record that a bundle for a given locale
     * could not be found — avoids repeated classpath lookups for the same missing bundle.
     */
    private static final ResourceBundle MISSING_SENTINEL = new ResourceBundle() {
        @Override
        protected Object handleGetObject(final String key) {
            return null;
        }

        @Override
        public Enumeration<String> getKeys() {
            return Collections.emptyEnumeration();
        }
    };

    /**
     * Creates a {@code LocalizationBundle} for the given base name.
     *
     * @param baseName the ResourceBundle base name (e.g. {@code "i18n/messages"});
     *                 must not be {@code null} or blank
     */
    public LocalizationBundle(final String baseName) {
        if (baseName == null || baseName.isBlank()) {
            throw new FrameworkException("Bundle base name must not be blank");
        }
        this.baseName = baseName;
    }

    /**
     * Creates a {@code LocalizationBundle} using the {@link #DEFAULT_BASE_NAME}.
     */
    public LocalizationBundle() {
        this(DEFAULT_BASE_NAME);
    }

    // -------------------------------------------------------------------------
    // Bundle loading
    // -------------------------------------------------------------------------

    /**
     * Loads (or returns a cached) {@link ResourceBundle} for the given locale.
     *
     * @param locale the target locale; must not be {@code null}
     * @return an {@link Optional} containing the bundle, or empty if not found
     */
    public Optional<ResourceBundle> loadBundle(final Locale locale) {
        final String cacheKey = locale.toLanguageTag();
        final ResourceBundle cached = cache.get(cacheKey);
        if (cached != null) {
            return cached == MISSING_SENTINEL ? Optional.empty() : Optional.of(cached);
        }

        try {
            // Use the class-loader that loaded this class so classpath isolation works
            // correctly in Maven Surefire's forked JVMs.
            final ResourceBundle bundle = ResourceBundle.getBundle(
                    baseName, locale, LocalizationBundle.class.getClassLoader(),
                    ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));
            cache.put(cacheKey, bundle);
            LOG.debug("Loaded bundle '{}' for locale '{}'", baseName, locale);
            return Optional.of(bundle);
        } catch (MissingResourceException e) {
            LOG.warn("No bundle found for base='{}' locale='{}': {}", baseName, locale, e.getMessage());
            cache.put(cacheKey, MISSING_SENTINEL);
            return Optional.empty();
        }
    }

    // -------------------------------------------------------------------------
    // Key retrieval
    // -------------------------------------------------------------------------

    /**
     * Retrieves a localised message for the given key and locale.
     *
     * <p>Fallback strategy:
     * <ol>
     *   <li>Attempt to load the bundle for {@code locale}.</li>
     *   <li>If the bundle is missing, fall back to {@link LocaleManager#DEFAULT_LOCALE}.</li>
     *   <li>If the key is absent from the fallback bundle, return the key name surrounded
     *       by {@code !!} markers so that QA logs make missing keys immediately visible
     *       (e.g. {@code "!!missing.key!!"}).</li>
     * </ol>
     *
     * @param key    the message key to look up; must not be {@code null}
     * @param locale the target locale
     * @return the translated string, or a {@code "!!key!!"} marker if not found
     */
    public String get(final String key, final Locale locale) {
        if (key == null) {
            throw new FrameworkException("Message key must not be null");
        }

        final Optional<ResourceBundle> primary = loadBundle(locale);
        if (primary.isPresent()) {
            final ResourceBundle bundle = primary.get();
            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            }
            LOG.debug("Key '{}' not found in bundle for locale '{}', trying default locale", key, locale);
        }

        // Fallback to default locale
        if (!locale.equals(LocaleManager.DEFAULT_LOCALE)) {
            final Optional<ResourceBundle> fallback = loadBundle(LocaleManager.DEFAULT_LOCALE);
            if (fallback.isPresent() && fallback.get().containsKey(key)) {
                LOG.debug("Returning fallback value for key '{}' from default locale", key);
                return fallback.get().getString(key);
            }
        }

        LOG.warn("Key '{}' not found in any bundle for locale '{}'", key, locale);
        return "!!" + key + "!!";
    }

    /**
     * Retrieves a message for the current thread's locale (from {@link LocaleManager}).
     *
     * @param key the message key; must not be {@code null}
     * @return the translated string or {@code "!!key!!"} marker
     */
    public String get(final String key) {
        return get(key, LocaleManager.getLocale());
    }

    // -------------------------------------------------------------------------
    // Missing-key detection
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if the key exists in the bundle for the given locale
     * AND its value is non-blank.
     *
     * @param key    the message key to check
     * @param locale the target locale
     * @return {@code true} if present and non-blank
     */
    public boolean isPresent(final String key, final Locale locale) {
        final Optional<ResourceBundle> bundle = loadBundle(locale);
        if (bundle.isEmpty()) {
            return false;
        }
        if (!bundle.get().containsKey(key)) {
            return false;
        }
        final String value = bundle.get().getString(key);
        return value != null && !value.isBlank();
    }

    /**
     * Returns the complete set of keys defined in the bundle for the given locale.
     * Returns an empty set if no bundle exists for that locale.
     *
     * @param locale the target locale
     * @return immutable set of key names; never {@code null}
     */
    public Set<String> getAllKeys(final Locale locale) {
        final Optional<ResourceBundle> bundle = loadBundle(locale);
        if (bundle.isEmpty()) {
            return Collections.emptySet();
        }
        final Set<String> keys = new HashSet<>();
        bundle.get().keySet().forEach(keys::add);
        return Collections.unmodifiableSet(keys);
    }

    /**
     * Returns the keys that are present in {@code baseLocale} but absent or blank in
     * {@code otherLocale}. An empty set means the translation is complete.
     *
     * @param baseLocale  the reference locale (typically the source language)
     * @param otherLocale the locale to check for missing translations
     * @return set of missing keys; never {@code null}
     */
    public Set<String> findMissingKeys(final Locale baseLocale, final Locale otherLocale) {
        final Set<String> baseKeys  = getAllKeys(baseLocale);
        final Set<String> missing   = new HashSet<>();
        for (final String key : baseKeys) {
            if (!isPresent(key, otherLocale)) {
                missing.add(key);
            }
        }
        return Collections.unmodifiableSet(missing);
    }

    /**
     * Returns the base name of the underlying resource bundle.
     *
     * @return bundle base name; never {@code null}
     */
    public String getBaseName() {
        return baseName;
    }

    /**
     * Clears the internal bundle cache. Useful between test classes that mutate classpath
     * resources (rare) or to force a reload in an environment where properties files are
     * generated at runtime.
     */
    public void clearCache() {
        cache.clear();
        LOG.debug("Bundle cache cleared for base name '{}'", baseName);
    }
}
