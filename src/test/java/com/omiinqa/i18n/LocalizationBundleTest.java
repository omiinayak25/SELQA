package com.omiinqa.i18n;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link LocalizationBundle}.
 *
 * <p>Loads the sample bundles in {@code src/test/resources/i18n/}:
 * {@code messages_en.properties}, {@code messages_es.properties}, {@code messages_fr.properties}.
 *
 * <p>All tests are offline (no browser/network). Groups: {@code i18n}, {@code unit}.
 */
@Test(groups = {"i18n", "unit"})
public class LocalizationBundleTest {

    private static final Locale EN = Locale.ENGLISH;
    private static final Locale ES = Locale.forLanguageTag("es");
    private static final Locale FR = Locale.FRENCH;

    private LocalizationBundle bundle;

    @BeforeClass
    public void setUpBundle() {
        bundle = new LocalizationBundle("i18n/messages");
    }

    // -------------------------------------------------------------------------
    // Bundle loading
    // -------------------------------------------------------------------------

    @Test(description = "loadBundle returns a present Optional for English")
    public void loadBundle_returnsPresentOptional_forEnglish() {
        final Optional<ResourceBundle> result = bundle.loadBundle(EN);
        assertThat(result).isPresent();
    }

    @Test(description = "loadBundle returns a present Optional for Spanish")
    public void loadBundle_returnsPresentOptional_forSpanish() {
        assertThat(bundle.loadBundle(ES)).isPresent();
    }

    @Test(description = "loadBundle returns a present Optional for French")
    public void loadBundle_returnsPresentOptional_forFrench() {
        assertThat(bundle.loadBundle(FR)).isPresent();
    }

    @Test(description = "loadBundle returns empty for an unsupported locale")
    public void loadBundle_returnsEmpty_forUnsupportedLocale() {
        // Japanese bundle does not exist in test resources
        assertThat(bundle.loadBundle(Locale.JAPANESE)).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Key retrieval
    // -------------------------------------------------------------------------

    @Test(description = "get(key, EN) returns English translation for login.title")
    public void get_returnsEnglishTranslation_forLoginTitle() {
        assertThat(bundle.get("login.title", EN)).isEqualTo("Sign In");
    }

    @Test(description = "get(key, ES) returns Spanish translation for login.title")
    public void get_returnsSpanishTranslation_forLoginTitle() {
        assertThat(bundle.get("login.title", ES)).isEqualTo("Iniciar sesión");
    }

    @Test(description = "get(key, FR) returns French translation for checkout.button")
    public void get_returnsFrenchTranslation_forCheckoutButton() {
        assertThat(bundle.get("checkout.button", FR)).isEqualTo("Passer à la caisse");
    }

    @Test(description = "get returns !!key!! marker for a missing key in a valid locale")
    public void get_returnsMissingMarker_forUnknownKey() {
        final String result = bundle.get("nonexistent.key", EN);
        assertThat(result).startsWith("!!")
                .endsWith("!!")
                .contains("nonexistent.key");
    }

    @Test(description = "get falls back to English when the key is missing in the requested locale")
    public void get_fallsBackToEnglish_whenKeyMissingInLocale() {
        // messages_es.properties and messages_fr.properties have all keys.
        // We use loadBundle directly on Japanese (no bundle) to force fallback.
        // For this test we verify the English value is returned for Japanese (no bundle).
        final String result = bundle.get("login.title", Locale.JAPANESE);
        // Should fall back to English value
        assertThat(result).isEqualTo("Sign In");
    }

    // -------------------------------------------------------------------------
    // isPresent
    // -------------------------------------------------------------------------

    @Test(description = "isPresent returns true for a key that exists and is non-blank")
    public void isPresent_returnsTrue_forExistingNonBlankKey() {
        assertThat(bundle.isPresent("cart.empty", EN)).isTrue();
        assertThat(bundle.isPresent("cart.empty", ES)).isTrue();
        assertThat(bundle.isPresent("cart.empty", FR)).isTrue();
    }

    @Test(description = "isPresent returns false for a nonexistent key")
    public void isPresent_returnsFalse_forNonexistentKey() {
        assertThat(bundle.isPresent("does.not.exist", EN)).isFalse();
    }

    // -------------------------------------------------------------------------
    // getAllKeys
    // -------------------------------------------------------------------------

    @Test(description = "getAllKeys returns a non-empty set for English bundle")
    public void getAllKeys_returnsNonEmptySet_forEnglish() {
        final Set<String> keys = bundle.getAllKeys(EN);
        assertThat(keys).isNotEmpty()
                .contains("login.title", "cart.empty", "checkout.button");
    }

    @Test(description = "getAllKeys returns an empty set for an unsupported locale")
    public void getAllKeys_returnsEmpty_forUnsupportedLocale() {
        assertThat(bundle.getAllKeys(Locale.JAPANESE)).isEmpty();
    }

    // -------------------------------------------------------------------------
    // findMissingKeys
    // -------------------------------------------------------------------------

    @Test(description = "findMissingKeys returns empty set when ES has all EN keys")
    public void findMissingKeys_returnsEmpty_whenSpanishIsComplete() {
        assertThat(bundle.findMissingKeys(EN, ES)).isEmpty();
    }

    @Test(description = "findMissingKeys returns empty set when FR has all EN keys")
    public void findMissingKeys_returnsEmpty_whenFrenchIsComplete() {
        assertThat(bundle.findMissingKeys(EN, FR)).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Cache and misc
    // -------------------------------------------------------------------------

    @Test(description = "getBaseName returns the configured base name")
    public void getBaseName_returnsConfiguredBaseName() {
        assertThat(bundle.getBaseName()).isEqualTo("i18n/messages");
    }

    @Test(description = "clearCache allows re-loading bundles without errors")
    public void clearCache_doesNotBreakSubsequentLoads() {
        bundle.clearCache();
        assertThat(bundle.loadBundle(EN)).isPresent();
    }
}
