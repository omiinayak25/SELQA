package com.omiinqa.i18n;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TextDirection} RTL/LTR classification.
 *
 * <p>All tests are offline and deterministic. Groups: {@code i18n}, {@code unit}.
 */
@Test(groups = {"i18n", "unit"})
public class TextDirectionTest {

    // -------------------------------------------------------------------------
    // RTL language classification
    // -------------------------------------------------------------------------

    @DataProvider(name = "rtlLocales")
    public Object[][] rtlLocales() {
        return new Object[][] {
                {Locale.forLanguageTag("ar"),    "Arabic"},
                {Locale.forLanguageTag("he"),    "Hebrew (modern tag)"},
                {new Locale("iw"),               "Hebrew (legacy iw tag)"},
                {Locale.forLanguageTag("fa"),    "Persian / Farsi"},
                {Locale.forLanguageTag("ur"),    "Urdu"},
                {Locale.forLanguageTag("yi"),    "Yiddish"},
                {Locale.forLanguageTag("ar-SA"), "Arabic (Saudi Arabia)"},
                {Locale.forLanguageTag("ar-EG"), "Arabic (Egypt)"},
        };
    }

    @Test(dataProvider = "rtlLocales",
          description = "RTL languages are correctly classified as RTL")
    public void of_locale_returnsRTL(final Locale locale, final String description) {
        assertThat(TextDirection.of(locale))
                .as("Expected RTL for: " + description)
                .isEqualTo(TextDirection.RTL);
    }

    // -------------------------------------------------------------------------
    // LTR language classification
    // -------------------------------------------------------------------------

    @DataProvider(name = "ltrLocales")
    public Object[][] ltrLocales() {
        return new Object[][] {
                {Locale.ENGLISH,                 "English"},
                {Locale.FRENCH,                  "French"},
                {Locale.GERMAN,                  "German"},
                {Locale.JAPANESE,                "Japanese"},
                {Locale.CHINESE,                 "Chinese"},
                {Locale.forLanguageTag("es"),    "Spanish"},
                {Locale.forLanguageTag("pt-BR"), "Portuguese (Brazil)"},
        };
    }

    @Test(dataProvider = "ltrLocales",
          description = "LTR languages are correctly classified as LTR")
    public void of_locale_returnsLTR(final Locale locale, final String description) {
        assertThat(TextDirection.of(locale))
                .as("Expected LTR for: " + description)
                .isEqualTo(TextDirection.LTR);
    }

    // -------------------------------------------------------------------------
    // String tag overload
    // -------------------------------------------------------------------------

    @Test(description = "of(String) classifies 'ar' as RTL")
    public void of_string_classifiesArabicAsRTL() {
        assertThat(TextDirection.of("ar")).isEqualTo(TextDirection.RTL);
    }

    @Test(description = "of(String) classifies 'en-US' as LTR")
    public void of_string_classifiesEnglishAsLTR() {
        assertThat(TextDirection.of("en-US")).isEqualTo(TextDirection.LTR);
    }

    @Test(description = "of(Locale) with null throws IllegalArgumentException")
    public void of_locale_throwsException_forNull() {
        assertThatThrownBy(() -> TextDirection.of((Locale) null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test(description = "of(String) with blank tag throws IllegalArgumentException")
    public void of_string_throwsException_forBlank() {
        assertThatThrownBy(() -> TextDirection.of("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // -------------------------------------------------------------------------
    // Enum helpers
    // -------------------------------------------------------------------------

    @Test(description = "htmlAttribute returns 'rtl' for RTL direction")
    public void htmlAttribute_returnsRtl_forRTL() {
        assertThat(TextDirection.RTL.htmlAttribute()).isEqualTo("rtl");
    }

    @Test(description = "htmlAttribute returns 'ltr' for LTR direction")
    public void htmlAttribute_returnsLtr_forLTR() {
        assertThat(TextDirection.LTR.htmlAttribute()).isEqualTo("ltr");
    }

    @Test(description = "isRtl returns true for RTL and false for LTR")
    public void isRtl_returnsCorrectBoolean() {
        assertThat(TextDirection.RTL.isRtl()).isTrue();
        assertThat(TextDirection.LTR.isRtl()).isFalse();
    }
}
