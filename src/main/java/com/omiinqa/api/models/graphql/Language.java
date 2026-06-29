package com.omiinqa.api.models.graphql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a language resource as returned by the Countries GraphQL API.
 *
 * <p>The Countries API's {@code Language} type uses BCP 47 / ISO 639-1 codes.
 * Jackson deserialises this POJO from elements of the {@code data.languages}
 * array (global language list) and from the nested {@code languages} field
 * within a {@link Country} object.</p>
 *
 * @see Country
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Language {

    /** ISO 639-1 or ISO 639-3 language code, e.g. {@code "en"} or {@code "zho"}. */
    private String code;

    /** English name of the language, e.g. {@code "English"}. */
    private String name;

    /**
     * Native (endonym) name of the language, e.g. {@code "English"} for English,
     * {@code "Deutsch"} for German. ({@code native} is a Java keyword, so mapped.)
     */
    @JsonProperty("native")
    private String nativeName;

    /**
     * Whether the language is written right-to-left.
     * {@code true} for Arabic, Hebrew, etc.
     */
    private Boolean rtl;
}
