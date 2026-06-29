package com.omiinqa.i18n;

import com.omiinqa.exceptions.FrameworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe manager for the current test thread's active {@link Locale}.
 *
 * <p><strong>Design pattern:</strong> Thread-Local State. Each test thread carries its own
 * locale independently, which is critical for parallel test execution where different
 * threads may be driving different locale scenarios simultaneously.
 *
 * <p><strong>Why ThreadLocal?</strong> Selenium test suites run concurrently; a shared
 * static Locale would cause race conditions between locale-switching tests. ThreadLocal
 * binds the Locale to the thread's lifecycle, ensuring isolation at zero synchronisation
 * cost for the happy path.
 *
 * <p>Usage pattern:
 * <pre>{@code
 * LocaleManager.setLocale(Locale.FRENCH);
 * try {
 *     // ... run localised test ...
 * } finally {
 *     LocaleManager.reset(); // always clean up in a finally block
 * }
 * }</pre>
 *
 * <p>The supported-locale registry is shared across threads (read-mostly) and is backed by
 * a {@link CopyOnWriteArrayList} so reads are lock-free.
 */
public final class LocaleManager {

    private static final Logger LOG = LoggerFactory.getLogger(LocaleManager.class);

    /** Default locale used when no locale has been set for the current thread. */
    public static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    /** Thread-local storage: each test thread owns its own locale. */
    private static final ThreadLocal<Locale> CURRENT_LOCALE =
            ThreadLocal.withInitial(() -> DEFAULT_LOCALE);

    /**
     * Registry of locales declared as supported by the application under test.
     * Populated by the framework consumer at suite initialisation time.
     */
    private static final List<Locale> SUPPORTED_LOCALES = new CopyOnWriteArrayList<>();

    // Seed the registry with a default set of commonly tested locales.
    static {
        SUPPORTED_LOCALES.add(Locale.ENGLISH);
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("es"));
        SUPPORTED_LOCALES.add(Locale.FRENCH);
    }

    private LocaleManager() {
        throw new UnsupportedOperationException("Utility class — do not instantiate.");
    }

    // -------------------------------------------------------------------------
    // ThreadLocal accessors
    // -------------------------------------------------------------------------

    /**
     * Sets the {@link Locale} for the current thread.
     *
     * @param locale the locale to activate; must not be {@code null}
     * @throws FrameworkException if {@code locale} is {@code null}
     */
    public static void setLocale(final Locale locale) {
        if (locale == null) {
            throw new FrameworkException("Locale must not be null");
        }
        LOG.debug("Setting locale for thread '{}' to '{}'", Thread.currentThread().getName(), locale);
        CURRENT_LOCALE.set(locale);
    }

    /**
     * Returns the locale currently active on the calling thread.
     * Defaults to {@link #DEFAULT_LOCALE} if none has been set.
     *
     * @return the current thread's locale; never {@code null}
     */
    public static Locale getLocale() {
        return CURRENT_LOCALE.get();
    }

    /**
     * Resets the current thread's locale to {@link #DEFAULT_LOCALE} and removes
     * the ThreadLocal binding to prevent memory leaks in thread-pool environments.
     *
     * <p>Always call this in a {@code finally} block (or a TestNG {@code @AfterMethod}).
     */
    public static void reset() {
        LOG.debug("Resetting locale for thread '{}'", Thread.currentThread().getName());
        CURRENT_LOCALE.remove();
    }

    // -------------------------------------------------------------------------
    // Supported-locale registry
    // -------------------------------------------------------------------------

    /**
     * Registers a locale as supported by the application under test.
     * Idempotent: adding the same locale twice has no effect.
     *
     * @param locale the locale to register; must not be {@code null}
     */
    public static void addSupportedLocale(final Locale locale) {
        if (locale == null) {
            throw new FrameworkException("Cannot register a null locale");
        }
        if (!SUPPORTED_LOCALES.contains(locale)) {
            SUPPORTED_LOCALES.add(locale);
            LOG.debug("Registered supported locale: {}", locale);
        }
    }

    /**
     * Replaces the entire supported-locale registry with the given list.
     *
     * @param locales the new list of supported locales; must not be {@code null} or empty
     */
    public static void setSupportedLocales(final List<Locale> locales) {
        if (locales == null || locales.isEmpty()) {
            throw new FrameworkException("Supported locales list must not be null or empty");
        }
        SUPPORTED_LOCALES.clear();
        SUPPORTED_LOCALES.addAll(locales);
        LOG.debug("Supported locales replaced: {}", SUPPORTED_LOCALES);
    }

    /**
     * Returns an unmodifiable view of the supported-locale registry.
     *
     * @return immutable list of supported locales; never {@code null}
     */
    public static List<Locale> getSupportedLocales() {
        return Collections.unmodifiableList(SUPPORTED_LOCALES);
    }

    /**
     * Returns {@code true} if the given locale is in the supported-locale registry.
     *
     * @param locale the locale to check
     * @return {@code true} if supported
     */
    public static boolean isSupported(final Locale locale) {
        return SUPPORTED_LOCALES.contains(locale);
    }

    // -------------------------------------------------------------------------
    // Language / country helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the ISO 639 language code of the current thread's locale (e.g. {@code "en"}).
     *
     * @return language code; never {@code null}
     */
    public static String currentLanguage() {
        return CURRENT_LOCALE.get().getLanguage();
    }

    /**
     * Returns the ISO 3166 country code of the current thread's locale (e.g. {@code "US"},
     * or an empty string if no country is set).
     *
     * @return country code; never {@code null}
     */
    public static String currentCountry() {
        return CURRENT_LOCALE.get().getCountry();
    }

    /**
     * Builds a {@link Locale} from the given BCP-47 language tag and activates it
     * on the current thread. Convenience wrapper over {@link Locale#forLanguageTag(String)}
     * and {@link #setLocale(Locale)}.
     *
     * @param languageTag a BCP-47 language tag such as {@code "es-MX"} or {@code "ar"}
     */
    public static void setLocaleByTag(final String languageTag) {
        if (languageTag == null || languageTag.isBlank()) {
            throw new FrameworkException("Language tag must not be blank");
        }
        setLocale(Locale.forLanguageTag(languageTag));
    }

    /**
     * Returns the display name of the current thread's locale in that locale's own language.
     *
     * @return display name, e.g. {@code "English"} or {@code "français"}
     */
    public static String currentDisplayName() {
        final Locale loc = CURRENT_LOCALE.get();
        return loc.getDisplayName(loc);
    }
}
