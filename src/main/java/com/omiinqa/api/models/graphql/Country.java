package com.omiinqa.api.models.graphql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a country resource as returned by the Countries GraphQL API.
 *
 * <p>Fields align with the {@code Country} type in the Countries API schema
 * ({@code https://countries.trevorblades.com/graphql}).  Only fields
 * commonly queried in the test suite are mapped; unknown fields are silently
 * ignored via {@link JsonIgnoreProperties} so that schema additions in the
 * remote API do not break deserialization.</p>
 *
 * <p>Jackson deserialises this POJO from the GraphQL response path
 * {@code data.country} (single lookup) or elements of
 * {@code data.countries} (list lookup).</p>
 *
 * @see Continent
 * @see Language
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Country {

    /** ISO 3166-1 alpha-2 country code, e.g. {@code "US"}. */
    private String code;

    /** English name of the country, e.g. {@code "United States"}. */
    private String name;

    /** Native/local name of the country ({@code native} is a Java keyword, so mapped). */
    @JsonProperty("native")
    private String nativeName;

    /** Capital city name, e.g. {@code "Washington D.C."}. */
    private String capital;

    /** Comma-separated currency codes, e.g. {@code "USD,USN,USS"}. */
    private String currency;

    /**
     * Country calling code without the leading {@code +}, e.g. {@code "1"}.
     * Note: some countries share a calling code (e.g., US and CA both use {@code "1"}).
     */
    private String phone;

    /** Unicode flag emoji for the country, e.g. {@code "🇺🇸"}. */
    private String emoji;

    /** Languages officially used in the country. */
    private List<Language> languages;

    /** The continent this country belongs to. */
    private Continent continent;
}
