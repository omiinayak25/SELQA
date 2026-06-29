package com.omiinqa.api.models.graphql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a continent resource as returned by the Countries GraphQL API.
 *
 * <p>The Countries API recognises exactly seven continents:
 * Africa (AF), Antarctica (AN), Asia (AS), Europe (EU),
 * North America (NA), Oceania (OC), and South America (SA).</p>
 *
 * <p>Jackson deserialises this POJO from the GraphQL response path
 * {@code data.continents[n]} when the {@code continents} root field is queried.
 * It also appears nested inside {@link Country#getContinent()} when a country
 * query selects the {@code continent} sub-field.</p>
 *
 * @see Country
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Continent {

    /** Two-letter ISO continent code, e.g. {@code "EU"}. */
    private String code;

    /** English name of the continent, e.g. {@code "Europe"}. */
    private String name;

    /**
     * Countries within this continent.
     * This field is only populated when the {@code countries} sub-selection is
     * included in the query (e.g., {@link com.omiinqa.api.graphql.CountriesQueries#CONTINENTS_WITH_COUNTRIES}).
     */
    private List<Country> countries;
}
