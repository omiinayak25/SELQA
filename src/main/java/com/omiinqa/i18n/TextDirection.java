package com.omiinqa.i18n;

import java.util.Locale;
import java.util.Set;

/**
 * Represents the text-rendering direction required by a locale's primary script.
 *
 * <p><strong>Why this matters for QA:</strong> Right-to-left (RTL) locales require the
 * UI layout to mirror left-to-right layouts. Automated tests must assert that RTL-specific
 * CSS ({@code dir="rtl"}, {@code text-align: right}) is applied for Arabic, Hebrew, and
 * Farsi users, and that it is absent for LTR locales.
 *
 * <p>Use {@link #of(Locale)} to classify any locale at runtime without hard-coding
 * language codes inside test logic.
 *
 * <p><strong>Supported RTL languages:</strong> Arabic ({@code ar}), Hebrew ({@code he},
 * legacy tag {@code iw}), Persian/Farsi ({@code fa}), Urdu ({@code ur}),
 * Yiddish ({@code yi}), Sindhi ({@code sd}), and Thaana ({@code dv}).
 */
public enum TextDirection {

    /**
     * Left-to-Right — the default for Latin, Cyrillic, and most scripts.
     */
    LTR,

    /**
     * Right-to-Left — required by Arabic, Hebrew, Persian, and related scripts.
     */
    RTL;

    /**
     * ISO 639 language codes whose primary script runs right-to-left.
     * "iw" is the legacy Java tag for Hebrew (ISO 639 deprecated in favour of "he").
     */
    private static final Set<String> RTL_LANGUAGES = Set.of(
            "ar",  // Arabic
            "he",  // Hebrew (modern BCP-47)
            "iw",  // Hebrew (legacy Java Locale)
            "fa",  // Persian / Farsi
            "ur",  // Urdu
            "yi",  // Yiddish
            "sd",  // Sindhi
            "dv"   // Dhivehi / Thaana
    );

    /**
     * Classifies the given locale as {@link #LTR} or {@link #RTL} based on its
     * primary language code.
     *
     * <p>The classification is based on the language sub-tag only (the first segment
     * of the BCP-47 tag). Region sub-tags (e.g. {@code ar-SA} vs {@code ar-EG}) do
     * not change directionality.
     *
     * @param locale the locale to classify; must not be {@code null}
     * @return {@link #RTL} if the locale's language is known to be right-to-left,
     *         {@link #LTR} otherwise
     * @throws IllegalArgumentException if {@code locale} is {@code null}
     */
    public static TextDirection of(final Locale locale) {
        if (locale == null) {
            throw new IllegalArgumentException("Locale must not be null");
        }
        final String lang = locale.getLanguage();
        return RTL_LANGUAGES.contains(lang) ? RTL : LTR;
    }

    /**
     * Convenience overload that accepts a BCP-47 language tag string.
     *
     * @param languageTag a BCP-47 tag such as {@code "ar"}, {@code "he-IL"}, or {@code "en-US"}
     * @return {@link #RTL} or {@link #LTR}
     */
    public static TextDirection of(final String languageTag) {
        if (languageTag == null || languageTag.isBlank()) {
            throw new IllegalArgumentException("Language tag must not be blank");
        }
        return of(Locale.forLanguageTag(languageTag));
    }

    /**
     * Returns the HTML {@code dir} attribute value corresponding to this direction.
     *
     * @return {@code "rtl"} or {@code "ltr"} (lower-case, as expected by HTML)
     */
    public String htmlAttribute() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Returns {@code true} if this direction is {@link #RTL}.
     *
     * @return {@code true} for RTL locales
     */
    public boolean isRtl() {
        return this == RTL;
    }
}
