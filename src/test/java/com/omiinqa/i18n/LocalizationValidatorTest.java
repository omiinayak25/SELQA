package com.omiinqa.i18n;

import com.omiinqa.exceptions.FrameworkException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link LocalizationValidator}.
 *
 * <p>Verifies assertion methods fire correctly on the sample bundles in
 * {@code src/test/resources/i18n/}. Tests are offline, deterministic, and fast.
 * Groups: {@code i18n}, {@code unit}.
 */
@Test(groups = {"i18n", "unit"})
public class LocalizationValidatorTest {

    private static final Locale EN = Locale.ENGLISH;
    private static final Locale ES = Locale.forLanguageTag("es");
    private static final Locale FR = Locale.FRENCH;
    private static final Locale JA = Locale.JAPANESE; // no bundle on classpath

    private LocalizationBundle bundle;

    @BeforeClass
    public void setUpBundle() {
        bundle = new LocalizationBundle("i18n/messages");
    }

    // -------------------------------------------------------------------------
    // assertTranslated
    // -------------------------------------------------------------------------

    @Test(description = "assertTranslated passes when key is present and non-blank in EN")
    public void assertTranslated_passes_forPresentKeyInEnglish() {
        assertThatCode(() ->
                LocalizationValidator.assertTranslated("login.title", EN, bundle))
                .doesNotThrowAnyException();
    }

    @Test(description = "assertTranslated passes when key is present and non-blank in ES")
    public void assertTranslated_passes_forPresentKeyInSpanish() {
        assertThatCode(() ->
                LocalizationValidator.assertTranslated("cart.empty", ES, bundle))
                .doesNotThrowAnyException();
    }

    @Test(description = "assertTranslated passes for French checkout.button")
    public void assertTranslated_passes_forPresentKeyInFrench() {
        assertThatCode(() ->
                LocalizationValidator.assertTranslated("checkout.button", FR, bundle))
                .doesNotThrowAnyException();
    }

    @Test(description = "assertTranslated fails for a key missing from the bundle")
    public void assertTranslated_fails_forMissingKey() {
        assertThatThrownBy(() ->
                LocalizationValidator.assertTranslated("nonexistent.key", EN, bundle))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("nonexistent.key");
    }

    @Test(description = "assertTranslated throws FrameworkException when bundle arg is null")
    public void assertTranslated_throwsFrameworkException_forNullBundle() {
        assertThatThrownBy(() ->
                LocalizationValidator.assertTranslated("login.title", EN, null))
                .isInstanceOf(FrameworkException.class);
    }

    // -------------------------------------------------------------------------
    // assertAllKeysPresent
    // -------------------------------------------------------------------------

    @Test(description = "assertAllKeysPresent passes when ES has all EN keys")
    public void assertAllKeysPresent_passes_whenSpanishIsComplete() {
        assertThatCode(() ->
                LocalizationValidator.assertAllKeysPresent(EN, ES, bundle))
                .doesNotThrowAnyException();
    }

    @Test(description = "assertAllKeysPresent passes when FR has all EN keys")
    public void assertAllKeysPresent_passes_whenFrenchIsComplete() {
        assertThatCode(() ->
                LocalizationValidator.assertAllKeysPresent(EN, FR, bundle))
                .doesNotThrowAnyException();
    }

    @Test(description = "assertAllKeysPresent fails when target locale is missing keys (JA has no bundle)")
    public void assertAllKeysPresent_fails_whenLocaleHasNoBundle() {
        // Japanese has no bundle, so all EN keys will be reported as missing
        assertThatThrownBy(() ->
                LocalizationValidator.assertAllKeysPresent(EN, JA, bundle))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("missing translations");
    }

    // -------------------------------------------------------------------------
    // assertNoHardcodedDefault
    // -------------------------------------------------------------------------

    @Test(description = "assertNoHardcodedDefault passes for a properly translated key")
    public void assertNoHardcodedDefault_passes_forRealTranslation() {
        assertThatCode(() ->
                LocalizationValidator.assertNoHardcodedDefault("login.title", EN, bundle))
                .doesNotThrowAnyException();
    }

    @Test(description = "assertNoHardcodedDefault fails for a missing key (returns !!key!! marker)")
    public void assertNoHardcodedDefault_fails_forMissingKeyMarker() {
        // The bundle returns "!!missing.key!!" for absent keys; the validator must detect it
        assertThatThrownBy(() ->
                LocalizationValidator.assertNoHardcodedDefault("missing.key", JA, bundle))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("placeholder");
    }

    // -------------------------------------------------------------------------
    // assertBundleExists / assertBundleNotEmpty
    // -------------------------------------------------------------------------

    @Test(description = "assertBundleExists passes for a locale with a bundle on classpath")
    public void assertBundleExists_passes_forExistingBundle() {
        assertThatCode(() ->
                LocalizationValidator.assertBundleExists(EN, bundle))
                .doesNotThrowAnyException();
    }

    @Test(description = "assertBundleExists fails for a locale with no bundle")
    public void assertBundleExists_fails_forMissingBundle() {
        assertThatThrownBy(() ->
                LocalizationValidator.assertBundleExists(JA, bundle))
                .isInstanceOf(AssertionError.class);
    }

    @Test(description = "assertBundleNotEmpty passes for English bundle with many keys")
    public void assertBundleNotEmpty_passes_forEnglish() {
        assertThatCode(() ->
                LocalizationValidator.assertBundleNotEmpty(EN, bundle))
                .doesNotThrowAnyException();
    }

    // -------------------------------------------------------------------------
    // assertLocaleFullyTranslated
    // -------------------------------------------------------------------------

    @Test(description = "assertLocaleFullyTranslated passes for Spanish (complete locale)")
    public void assertLocaleFullyTranslated_passes_forSpanish() {
        assertThatCode(() ->
                LocalizationValidator.assertLocaleFullyTranslated(EN, ES, bundle))
                .doesNotThrowAnyException();
    }

    @Test(description = "assertLocaleFullyTranslated fails for Japanese (no bundle)")
    public void assertLocaleFullyTranslated_fails_forJapanese() {
        assertThatThrownBy(() ->
                LocalizationValidator.assertLocaleFullyTranslated(EN, JA, bundle))
                .isInstanceOf(AssertionError.class);
    }
}
