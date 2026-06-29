package com.omiinqa.i18n;

import com.omiinqa.exceptions.FrameworkException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link LocaleManager}.
 *
 * <p>All tests are offline and deterministic. No browser or network calls are made.
 * Each test resets ThreadLocal state in {@link #resetLocaleAfterEach()} to prevent
 * cross-test contamination.
 */
@Test(groups = {"i18n", "unit"})
public class LocaleManagerTest {

    @AfterMethod(alwaysRun = true)
    public void resetLocaleAfterEach() {
        LocaleManager.reset();
        // Restore default supported locales so tests are independent
        LocaleManager.setSupportedLocales(List.of(
                Locale.ENGLISH,
                Locale.forLanguageTag("es"),
                Locale.FRENCH));
    }

    // -------------------------------------------------------------------------
    // setLocale / getLocale
    // -------------------------------------------------------------------------

    @Test(description = "getLocale returns DEFAULT_LOCALE when nothing has been set")
    public void getLocale_returnsDefault_whenNotSet() {
        assertThat(LocaleManager.getLocale()).isEqualTo(LocaleManager.DEFAULT_LOCALE);
    }

    @Test(description = "setLocale stores the locale on the current thread")
    public void setLocale_storesLocale_forCurrentThread() {
        LocaleManager.setLocale(Locale.FRENCH);
        assertThat(LocaleManager.getLocale()).isEqualTo(Locale.FRENCH);
    }

    @Test(description = "setLocale with null throws FrameworkException")
    public void setLocale_throwsException_forNullLocale() {
        assertThatThrownBy(() -> LocaleManager.setLocale(null))
                .isInstanceOf(FrameworkException.class)
                .hasMessageContaining("must not be null");
    }

    @Test(description = "reset restores the locale to DEFAULT_LOCALE")
    public void reset_restoresDefault() {
        LocaleManager.setLocale(Locale.JAPANESE);
        LocaleManager.reset();
        assertThat(LocaleManager.getLocale()).isEqualTo(LocaleManager.DEFAULT_LOCALE);
    }

    // -------------------------------------------------------------------------
    // setLocaleByTag
    // -------------------------------------------------------------------------

    @Test(description = "setLocaleByTag accepts a valid BCP-47 tag")
    public void setLocaleByTag_acceptsValidTag() {
        LocaleManager.setLocaleByTag("de-DE");
        assertThat(LocaleManager.getLocale().getLanguage()).isEqualTo("de");
        assertThat(LocaleManager.getLocale().getCountry()).isEqualTo("DE");
    }

    @Test(description = "setLocaleByTag with blank string throws FrameworkException")
    public void setLocaleByTag_throwsException_forBlankTag() {
        assertThatThrownBy(() -> LocaleManager.setLocaleByTag("  "))
                .isInstanceOf(FrameworkException.class);
    }

    // -------------------------------------------------------------------------
    // Language / country helpers
    // -------------------------------------------------------------------------

    @Test(description = "currentLanguage returns the language code of the active locale")
    public void currentLanguage_returnsLanguageCode() {
        LocaleManager.setLocale(Locale.forLanguageTag("es"));
        assertThat(LocaleManager.currentLanguage()).isEqualTo("es");
    }

    @Test(description = "currentCountry returns empty string for a language-only locale")
    public void currentCountry_returnsEmpty_forLanguageOnlyLocale() {
        LocaleManager.setLocale(Locale.forLanguageTag("es"));
        assertThat(LocaleManager.currentCountry()).isEmpty();
    }

    @Test(description = "currentCountry returns country code when country is set")
    public void currentCountry_returnsCountry_whenSet() {
        LocaleManager.setLocale(Locale.forLanguageTag("fr-FR"));
        assertThat(LocaleManager.currentCountry()).isEqualTo("FR");
    }

    // -------------------------------------------------------------------------
    // Supported locales registry
    // -------------------------------------------------------------------------

    @Test(description = "isSupported returns true for a registered locale")
    public void isSupported_returnsTrue_forRegisteredLocale() {
        assertThat(LocaleManager.isSupported(Locale.ENGLISH)).isTrue();
    }

    @Test(description = "isSupported returns false for an unregistered locale")
    public void isSupported_returnsFalse_forUnregisteredLocale() {
        assertThat(LocaleManager.isSupported(Locale.JAPANESE)).isFalse();
    }

    @Test(description = "addSupportedLocale registers a new locale")
    public void addSupportedLocale_registersLocale() {
        LocaleManager.addSupportedLocale(Locale.GERMAN);
        assertThat(LocaleManager.isSupported(Locale.GERMAN)).isTrue();
    }

    @Test(description = "getSupportedLocales returns an unmodifiable list")
    public void getSupportedLocales_returnsUnmodifiableList() {
        final var locales = LocaleManager.getSupportedLocales();
        assertThatThrownBy(() -> locales.add(Locale.CHINESE))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test(description = "setSupportedLocales with empty list throws FrameworkException")
    public void setSupportedLocales_throwsException_forEmptyList() {
        assertThatThrownBy(() -> LocaleManager.setSupportedLocales(List.of()))
                .isInstanceOf(FrameworkException.class);
    }
}
