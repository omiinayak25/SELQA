package com.omiinqa.bdd.steps.domain;

import com.omiinqa.bdd.support.DomainWorld;
import com.omiinqa.i18n.LocalizationBundle;
import com.omiinqa.i18n.LocalizationValidator;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the localization domain.
 *
 * <p>These steps exercise the real {@link LocalizationBundle} and
 * {@link LocalizationValidator} classes against real .properties files
 * on the test classpath — no browser, no fakes, no no-op steps.</p>
 *
 * <p>All step text is prefixed with a localization noun to avoid ambiguity
 * with other domain step classes.</p>
 */
public class LocalizationSteps {

    private static final String BUNDLE_KEY = "locBundle";
    private static final String LOCALE_KEY  = "locLocale";

    // -------------------------------------------------------------------------
    // Bundle / locale setup
    // -------------------------------------------------------------------------

    /**
     * Creates a fresh {@link LocalizationBundle} for the default base name and stores
     * it in the scenario context.
     */
    @Given("a clean localization bundle")
    public void cleanLocalizationBundle() {
        DomainWorld.put(BUNDLE_KEY, new LocalizationBundle());
    }

    /**
     * Activates the given BCP-47 locale tag for subsequent steps.
     *
     * @param tag e.g. "en", "es", "ar", "de", "ja"
     */
    @Given("the localization locale is {string}")
    public void localizationLocaleIs(final String tag) {
        DomainWorld.put(LOCALE_KEY, Locale.forLanguageTag(tag));
    }

    // -------------------------------------------------------------------------
    // Bundle existence checks
    // -------------------------------------------------------------------------

    /**
     * Asserts that a bundle exists on the classpath for the given locale tag.
     */
    @Then("the localization bundle exists for locale {string}")
    public void localizationBundleExistsForLocale(final String tag) {
        final LocalizationBundle bundle = bundle();
        LocalizationValidator.assertBundleExists(Locale.forLanguageTag(tag), bundle);
    }

    /**
     * Asserts that the bundle for the given locale tag is NOT present on the classpath.
     */
    @Then("the localization bundle is absent for locale {string}")
    public void localizationBundleIsAbsentForLocale(final String tag) {
        final LocalizationBundle bundle = bundle();
        final Optional<ResourceBundle> opt = bundle.loadBundle(Locale.forLanguageTag(tag));
        assertThat(opt.isPresent())
                .as("Expected NO bundle for locale [%s] but one was found", tag)
                .isFalse();
    }

    // -------------------------------------------------------------------------
    // Key retrieval / translation assertions
    // -------------------------------------------------------------------------

    /**
     * Asserts that the resolved translation for {@code key} in {@code localeTag} equals
     * the expected value exactly.
     */
    @Then("the localization key {string} for locale {string} is {string}")
    public void localizationKeyForLocaleIs(
            final String key, final String localeTag, final String expected) {
        final LocalizationBundle bundle = bundle();
        final String actual = bundle.get(key, Locale.forLanguageTag(localeTag));
        assertThat(actual)
                .as("Translation of key [%s] for locale [%s]", key, localeTag)
                .isEqualTo(expected);
    }

    /**
     * Asserts that {@code key} resolves to a non-blank, non-placeholder value for the
     * active locale stored in the scenario context.
     */
    @Then("the localization key {string} is translated")
    public void localizationKeyIsTranslated(final String key) {
        final LocalizationBundle bundle = bundle();
        final Locale locale = DomainWorld.get(LOCALE_KEY);
        LocalizationValidator.assertTranslated(key, locale, bundle);
    }

    /**
     * Asserts that the resolved value for the key is present (non-blank) for the locale.
     */
    @Then("the localization key {string} is present for locale {string}")
    public void localizationKeyIsPresentForLocale(final String key, final String localeTag) {
        final LocalizationBundle bundle = bundle();
        assertThat(bundle.isPresent(key, Locale.forLanguageTag(localeTag)))
                .as("Key [%s] should be present for locale [%s]", key, localeTag)
                .isTrue();
    }

    /**
     * Asserts that the resolved value for the key is absent (missing or blank) for the locale.
     */
    @Then("the localization key {string} is absent for locale {string}")
    public void localizationKeyIsAbsentForLocale(final String key, final String localeTag) {
        final LocalizationBundle bundle = bundle();
        assertThat(bundle.isPresent(key, Locale.forLanguageTag(localeTag)))
                .as("Key [%s] should be absent for locale [%s]", key, localeTag)
                .isFalse();
    }

    // -------------------------------------------------------------------------
    // Fallback behaviour
    // -------------------------------------------------------------------------

    /**
     * Asserts that a missing locale falls back to the English value for the given key.
     */
    @Then("the localization key {string} for missing locale {string} falls back to {string}")
    public void localizationFallsBackForMissingLocale(
            final String key, final String missingTag, final String expectedEnValue) {
        final LocalizationBundle bundle = bundle();
        final String actual = bundle.get(key, Locale.forLanguageTag(missingTag));
        assertThat(actual)
                .as("Key [%s] should fall back to English value for missing locale [%s]",
                        key, missingTag)
                .isEqualTo(expectedEnValue);
    }

    /**
     * Asserts that a missing key returns the {@code !!key!!} placeholder marker.
     */
    @Then("the localization key {string} for locale {string} returns the missing-key marker")
    public void localizationKeyReturnsMissingMarker(final String key, final String localeTag) {
        final LocalizationBundle bundle = bundle();
        final String actual = bundle.get(key, Locale.forLanguageTag(localeTag));
        assertThat(actual)
                .as("Key [%s] for locale [%s] should return a !!..!! marker", key, localeTag)
                .matches("^!!.+!!$");
    }

    // -------------------------------------------------------------------------
    // Completeness checks
    // -------------------------------------------------------------------------

    /**
     * Asserts that every key in the English (base) bundle is also present in {@code localeTag}.
     */
    @Then("the localization locale {string} has all keys present against English")
    public void localizationLocaleHasAllKeysAgainstEnglish(final String localeTag) {
        final LocalizationBundle bundle = bundle();
        LocalizationValidator.assertAllKeysPresent(
                Locale.ENGLISH, Locale.forLanguageTag(localeTag), bundle);
    }

    /**
     * Asserts that {@code localeTag} is fully translated (bundle exists, non-empty,
     * all keys present against English).
     */
    @Then("the localization locale {string} is fully translated")
    public void localizationLocaleIsFullyTranslated(final String localeTag) {
        final LocalizationBundle bundle = bundle();
        LocalizationValidator.assertLocaleFullyTranslated(
                Locale.ENGLISH, Locale.forLanguageTag(localeTag), bundle);
    }

    /**
     * Asserts that the set of keys returned for the locale is exactly the expected count.
     */
    @Then("the localization locale {string} has {int} keys")
    public void localizationLocaleHasKeyCount(final String localeTag, final int expectedCount) {
        final LocalizationBundle bundle = bundle();
        final Set<String> keys = bundle.getAllKeys(Locale.forLanguageTag(localeTag));
        assertThat(keys)
                .as("Key count for locale [%s]", localeTag)
                .hasSize(expectedCount);
    }

    /**
     * Asserts that findMissingKeys returns an empty set (no missing keys).
     */
    @Then("the localization locale {string} has no missing keys compared to English")
    public void localizationLocaleHasNoMissingKeys(final String localeTag) {
        final LocalizationBundle bundle = bundle();
        final Set<String> missing = bundle.findMissingKeys(
                Locale.ENGLISH, Locale.forLanguageTag(localeTag));
        assertThat(missing)
                .as("Missing keys for locale [%s] relative to English", localeTag)
                .isEmpty();
    }

    /**
     * Asserts that findMissingKeys returns a non-empty set (some keys are missing).
     */
    @Then("the localization locale {string} has missing keys compared to English")
    public void localizationLocaleHasMissingKeys(final String localeTag) {
        final LocalizationBundle bundle = bundle();
        final Set<String> missing = bundle.findMissingKeys(
                Locale.ENGLISH, Locale.forLanguageTag(localeTag));
        assertThat(missing)
                .as("Expected missing keys for locale [%s] but found none", localeTag)
                .isNotEmpty();
    }

    // -------------------------------------------------------------------------
    // Cache control
    // -------------------------------------------------------------------------

    /**
     * Clears the bundle cache — verifies clearCache() is a no-op on a fresh reload.
     */
    @When("I clear the localization bundle cache")
    public void iClearTheLocalizationBundleCache() {
        bundle().clearCache();
    }

    /**
     * After clearing the cache, asserts that the bundle still loads correctly.
     */
    @Then("the localization bundle for locale {string} still loads after cache clear")
    public void localizationBundleStillLoadsAfterCacheClear(final String localeTag) {
        final LocalizationBundle bundle = bundle();
        final Optional<ResourceBundle> loaded = bundle.loadBundle(Locale.forLanguageTag(localeTag));
        assertThat(loaded.isPresent())
                .as("Bundle for locale [%s] should reload after cache clear", localeTag)
                .isTrue();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private LocalizationBundle bundle() {
        return DomainWorld.service(BUNDLE_KEY, LocalizationBundle::new);
    }
}
