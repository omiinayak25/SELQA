package com.omiinqa.i18n;

import com.omiinqa.exceptions.FrameworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * AssertJ-based assertion helper for i18n completeness checks.
 *
 * <p><strong>Design pattern:</strong> Static Assertion Facade. All methods are static so
 * test classes can import them like {@code import static ...LocalizationValidator.*}
 * without managing state.
 *
 * <p><strong>Why not just use AssertJ directly?</strong> Raw AssertJ assertions against
 * property files require callers to understand the bundle loading mechanics and key-set
 * comparison algorithms. This facade encapsulates those details behind domain-specific
 * method names ({@code assertTranslated}, {@code assertAllKeysPresent}) that make test
 * intent clear in code review.
 *
 * <p>All assertion methods throw {@link AssertionError} on failure (AssertJ standard),
 * which TestNG and Surefire report as a test failure with a descriptive message.
 *
 * <p>Usage example:
 * <pre>{@code
 * LocalizationBundle bundle = new LocalizationBundle("i18n/messages");
 * LocalizationValidator.assertTranslated("login.title", Locale.forLanguageTag("es"), bundle);
 * LocalizationValidator.assertAllKeysPresent(Locale.ENGLISH, Locale.FRENCH, bundle);
 * }</pre>
 */
public final class LocalizationValidator {

    private static final Logger LOG = LoggerFactory.getLogger(LocalizationValidator.class);

    private LocalizationValidator() {
        throw new UnsupportedOperationException("Utility class — do not instantiate.");
    }

    // -------------------------------------------------------------------------
    // Core assertions
    // -------------------------------------------------------------------------

    /**
     * Asserts that the given key exists in the bundle for the specified locale AND that
     * its value is non-blank. This verifies that the translation team has not left the
     * key empty.
     *
     * <p>Failure message example:
     * {@code "Key [login.title] is missing or blank for locale [es]"}
     *
     * @param key    the message key to verify; must not be {@code null}
     * @param locale the target locale; must not be {@code null}
     * @param bundle the {@link LocalizationBundle} to query; must not be {@code null}
     * @throws AssertionError   if the key is absent or blank
     * @throws FrameworkException if any argument is {@code null}
     */
    public static void assertTranslated(
            final String key,
            final Locale locale,
            final LocalizationBundle bundle) {

        requireNonNull(key, "key");
        requireNonNull(locale, "locale");
        requireNonNull(bundle, "bundle");

        LOG.debug("Asserting translation for key='{}' locale='{}'", key, locale);

        assertThat(bundle.isPresent(key, locale))
                .as("Key [%s] is missing or blank for locale [%s] in bundle [%s]",
                        key, locale.toLanguageTag(), bundle.getBaseName())
                .isTrue();
    }

    /**
     * Asserts that every key present in the {@code baseLocale} bundle also exists (and is
     * non-blank) in the {@code otherLocale} bundle. This is the standard "no missing
     * translations" check run during regression.
     *
     * <p>Failure message lists every missing key so the translation team gets a complete
     * picture in one test run rather than discovering keys one at a time.
     *
     * @param baseLocale  the reference locale (usually the source language, e.g. {@code en})
     * @param otherLocale the locale being validated for completeness
     * @param bundle      the {@link LocalizationBundle} to query
     * @throws AssertionError if any keys from the base locale are absent in the other locale
     */
    public static void assertAllKeysPresent(
            final Locale baseLocale,
            final Locale otherLocale,
            final LocalizationBundle bundle) {

        requireNonNull(baseLocale, "baseLocale");
        requireNonNull(otherLocale, "otherLocale");
        requireNonNull(bundle, "bundle");

        LOG.debug("Asserting all keys from '{}' are present in '{}'", baseLocale, otherLocale);

        final Set<String> missing = bundle.findMissingKeys(baseLocale, otherLocale);

        assertThat(missing)
                .as("Locale [%s] is missing translations for keys that exist in [%s]: %s",
                        otherLocale.toLanguageTag(), baseLocale.toLanguageTag(), missing)
                .isEmpty();
    }

    /**
     * Asserts that the value for the given key and locale does NOT use the
     * {@code "!!key!!"} placeholder format, which indicates an un-translated fallback.
     *
     * <p>This check guards against the scenario where a key exists in the bundle file
     * but was accidentally left with the placeholder value that developers sometimes
     * use as a stub during initial file creation.
     *
     * @param key    the message key to check
     * @param locale the target locale
     * @param bundle the {@link LocalizationBundle} to query
     * @throws AssertionError if the value looks like a hardcoded placeholder
     */
    public static void assertNoHardcodedDefault(
            final String key,
            final Locale locale,
            final LocalizationBundle bundle) {

        requireNonNull(key, "key");
        requireNonNull(locale, "locale");
        requireNonNull(bundle, "bundle");

        final String value = bundle.get(key, locale);

        assertThat(value)
                .as("Key [%s] for locale [%s] appears to be an un-translated placeholder: [%s]",
                        key, locale.toLanguageTag(), value)
                .doesNotMatch("^!!.+!!$");
    }

    /**
     * Asserts that a bundle exists at all for the given locale (i.e. the
     * {@code messages_<lang>.properties} file is on the classpath).
     *
     * @param locale the locale whose bundle must exist
     * @param bundle the {@link LocalizationBundle} to probe
     * @throws AssertionError if no bundle is found for the locale
     */
    public static void assertBundleExists(
            final Locale locale,
            final LocalizationBundle bundle) {

        requireNonNull(locale, "locale");
        requireNonNull(bundle, "bundle");

        assertThat(bundle.loadBundle(locale).isPresent())
                .as("No bundle found for locale [%s] with base name [%s]. "
                                + "Ensure the properties file is on the test classpath.",
                        locale.toLanguageTag(), bundle.getBaseName())
                .isTrue();
    }

    /**
     * Asserts that the set of keys returned for the given locale is not empty.
     * An empty key set usually means the bundle file exists but is completely empty,
     * which is a content-authoring error.
     *
     * @param locale the locale to check
     * @param bundle the {@link LocalizationBundle} to query
     * @throws AssertionError if the key set is empty
     */
    public static void assertBundleNotEmpty(
            final Locale locale,
            final LocalizationBundle bundle) {

        requireNonNull(locale, "locale");
        requireNonNull(bundle, "bundle");

        assertThat(bundle.getAllKeys(locale))
                .as("Bundle for locale [%s] is empty (no keys defined)", locale.toLanguageTag())
                .isNotEmpty();
    }

    /**
     * Compound assertion: verifies that the given locale has a bundle, that the bundle is
     * non-empty, and that all keys from the base locale are present. Combines the three
     * individual assertions for convenience.
     *
     * @param baseLocale  the reference locale
     * @param otherLocale the locale to fully validate
     * @param bundle      the bundle to probe
     */
    public static void assertLocaleFullyTranslated(
            final Locale baseLocale,
            final Locale otherLocale,
            final LocalizationBundle bundle) {

        assertBundleExists(otherLocale, bundle);
        assertBundleNotEmpty(otherLocale, bundle);
        assertAllKeysPresent(baseLocale, otherLocale, bundle);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private static void requireNonNull(final Object value, final String paramName) {
        if (value == null) {
            throw new FrameworkException("Argument '" + paramName + "' must not be null");
        }
    }
}
