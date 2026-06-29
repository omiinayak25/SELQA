package com.omiinqa.bdd.steps.domain;

import com.omiinqa.bdd.support.DomainWorld;
import com.omiinqa.i18n.LocaleManager;
import com.omiinqa.i18n.LocalizationBundle;
import com.omiinqa.i18n.TextDirection;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the internationalization domain.
 *
 * <p>These steps drive the real {@link LocaleManager}, {@link TextDirection},
 * and {@link LocalizationBundle} APIs to exercise locale lifecycle, RTL/LTR
 * direction detection, number formatting, and supported-locale management —
 * all offline, no browser.</p>
 *
 * <p>All step text is prefixed with the "i18n" or "locale" noun to guarantee
 * global uniqueness across the entire test suite.</p>
 */
public class InternationalizationSteps {

    private static final String BUNDLE_KEY    = "i18nBundle";
    private static final String DIRECTION_KEY = "i18nDirection";
    private static final String NUMBER_KEY    = "i18nFormattedNumber";

    // -------------------------------------------------------------------------
    // Teardown
    // -------------------------------------------------------------------------

    /** Resets the thread-local locale after each scenario. */
    @After
    public void resetLocaleAfterScenario() {
        LocaleManager.reset();
    }

    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    /** Creates a fresh LocalizationBundle and registers it in the scenario context. */
    @Given("a clean i18n service")
    public void cleanI18nService() {
        DomainWorld.put(BUNDLE_KEY, new LocalizationBundle());
    }

    // -------------------------------------------------------------------------
    // Locale activation
    // -------------------------------------------------------------------------

    /** Activates a locale on the current thread by BCP-47 tag. */
    @When("I activate locale {string}")
    public void iActivateLocale(final String tag) {
        LocaleManager.setLocaleByTag(tag);
    }

    /** Activates a locale on the current thread by BCP-47 tag (Given form). */
    @Given("the i18n active locale is {string}")
    public void i18nActiveLocaleIs(final String tag) {
        LocaleManager.setLocaleByTag(tag);
    }

    // -------------------------------------------------------------------------
    // Locale read assertions
    // -------------------------------------------------------------------------

    /** Asserts the thread-local locale language matches. */
    @Then("the i18n active language code is {string}")
    public void i18nActiveLanguageCodeIs(final String expectedLang) {
        assertThat(LocaleManager.currentLanguage())
                .as("Active language code")
                .isEqualTo(expectedLang);
    }

    /** Asserts the full thread-local locale tag matches. */
    @Then("the i18n active locale tag is {string}")
    public void i18nActiveLocaleTagIs(final String expectedTag) {
        assertThat(LocaleManager.getLocale().toLanguageTag())
                .as("Active locale tag")
                .isEqualTo(expectedTag);
    }

    // -------------------------------------------------------------------------
    // Locale reset
    // -------------------------------------------------------------------------

    /** Resets the thread-local locale to the default (English). */
    @When("I reset the i18n locale")
    public void iResetTheI18nLocale() {
        LocaleManager.reset();
    }

    /** Asserts that the active locale reverts to English after a reset. */
    @Then("the i18n active locale is the default English locale")
    public void i18nActiveLocaleIsDefaultEnglish() {
        assertThat(LocaleManager.getLocale())
                .as("Default locale after reset")
                .isEqualTo(Locale.ENGLISH);
    }

    // -------------------------------------------------------------------------
    // Supported locale registry
    // -------------------------------------------------------------------------

    /** Registers a locale in the LocaleManager supported list. */
    @When("I register {string} as an i18n supported locale")
    public void iRegisterAsI18nSupportedLocale(final String tag) {
        LocaleManager.addSupportedLocale(Locale.forLanguageTag(tag));
    }

    /** Asserts that a locale is present in the supported list. */
    @Then("the i18n locale {string} is supported")
    public void i18nLocaleIsSupported(final String tag) {
        assertThat(LocaleManager.isSupported(Locale.forLanguageTag(tag)))
                .as("Locale [%s] should be in supported list", tag)
                .isTrue();
    }

    /** Asserts that a locale is NOT present in the supported list. */
    @Then("the i18n locale {string} is not supported")
    public void i18nLocaleIsNotSupported(final String tag) {
        assertThat(LocaleManager.isSupported(Locale.forLanguageTag(tag)))
                .as("Locale [%s] should NOT be in supported list", tag)
                .isFalse();
    }

    /** Asserts that the supported-locale registry contains at least the given count. */
    @Then("the i18n supported locale count is at least {int}")
    public void i18nSupportedLocaleCountIsAtLeast(final int minCount) {
        final List<Locale> supported = LocaleManager.getSupportedLocales();
        assertThat(supported)
                .as("Supported locale count should be >= %d", minCount)
                .hasSizeGreaterThanOrEqualTo(minCount);
    }

    // -------------------------------------------------------------------------
    // Text direction
    // -------------------------------------------------------------------------

    /** Resolves and stores the TextDirection for the given locale tag. */
    @When("I check the i18n text direction for locale {string}")
    public void iCheckI18nTextDirectionForLocale(final String tag) {
        final TextDirection dir = TextDirection.of(Locale.forLanguageTag(tag));
        DomainWorld.put(DIRECTION_KEY, dir);
    }

    /** Asserts the stored direction is RTL. */
    @Then("the i18n text direction is RTL")
    public void i18nTextDirectionIsRtl() {
        final TextDirection dir = DomainWorld.get(DIRECTION_KEY);
        assertThat(dir.isRtl())
                .as("Expected RTL direction but got: %s", dir)
                .isTrue();
    }

    /** Asserts the stored direction is LTR. */
    @Then("the i18n text direction is LTR")
    public void i18nTextDirectionIsLtr() {
        final TextDirection dir = DomainWorld.get(DIRECTION_KEY);
        assertThat(dir.isRtl())
                .as("Expected LTR direction but got: %s", dir)
                .isFalse();
    }

    /** Asserts the HTML attribute of the resolved direction matches the expected value. */
    @Then("the i18n html direction attribute for locale {string} is {string}")
    public void i18nHtmlDirectionAttributeForLocaleIs(final String tag, final String expected) {
        final TextDirection dir = TextDirection.of(Locale.forLanguageTag(tag));
        assertThat(dir.htmlAttribute())
                .as("HTML dir attribute for locale [%s]", tag)
                .isEqualTo(expected);
    }

    /** Direct assertion — locale tag resolves to the expected TextDirection enum name. */
    @Then("the i18n text direction for locale {string} is {string}")
    public void i18nTextDirectionForLocaleIs(final String tag, final String expected) {
        final TextDirection dir = TextDirection.of(Locale.forLanguageTag(tag));
        assertThat(dir.name())
                .as("TextDirection for locale [%s]", tag)
                .isEqualTo(expected);
    }

    // -------------------------------------------------------------------------
    // Number formatting
    // -------------------------------------------------------------------------

    /** Formats a number for the given locale and stores the result. */
    @When("I format the number {double} for locale {string}")
    public void iFormatTheNumberForLocale(final double number, final String tag) {
        final NumberFormat fmt = NumberFormat.getNumberInstance(Locale.forLanguageTag(tag));
        DomainWorld.put(NUMBER_KEY, fmt.format(number));
    }

    /** Asserts the formatted number string equals the expected value. */
    @Then("the i18n formatted number is {string}")
    public void i18nFormattedNumberIs(final String expected) {
        final String actual = DomainWorld.get(NUMBER_KEY);
        assertThat(actual)
                .as("Formatted number")
                .isEqualTo(expected);
    }

    /** Asserts the formatted number string contains the expected decimal separator. */
    @Then("the i18n formatted number contains decimal separator {string}")
    public void i18nFormattedNumberContainsDecimalSeparator(final String sep) {
        final String actual = DomainWorld.get(NUMBER_KEY);
        assertThat(actual)
                .as("Expected decimal separator [%s] in formatted number [%s]", sep, actual)
                .contains(sep);
    }

    // -------------------------------------------------------------------------
    // Translation cross-check via bundle
    // -------------------------------------------------------------------------

    /**
     * Asserts that the bundle resolves a key to the expected value for the given locale,
     * using the i18n bundle stored in the scenario context.
     */
    @Then("the i18n key {string} for locale {string} resolves to {string}")
    public void i18nKeyForLocaleResolvesTo(
            final String key, final String localeTag, final String expected) {
        final LocalizationBundle bundle = DomainWorld.service(BUNDLE_KEY, LocalizationBundle::new);
        final String actual = bundle.get(key, Locale.forLanguageTag(localeTag));
        assertThat(actual)
                .as("i18n key [%s] for locale [%s]", key, localeTag)
                .isEqualTo(expected);
    }

    /**
     * Asserts that switching the thread-local locale causes bundle.get(key) to return
     * the expected locale-specific value.
     */
    @Then("the i18n key {string} resolves in the active locale to {string}")
    public void i18nKeyResolvesInActiveLocaleTo(final String key, final String expected) {
        final LocalizationBundle bundle = DomainWorld.service(BUNDLE_KEY, LocalizationBundle::new);
        final String actual = bundle.get(key);
        assertThat(actual)
                .as("i18n key [%s] in active locale [%s]", key, LocaleManager.getLocale())
                .isEqualTo(expected);
    }
}
