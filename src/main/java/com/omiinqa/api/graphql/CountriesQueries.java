package com.omiinqa.api.graphql;

/**
 * Catalogue of GraphQL query strings and builder methods for the Countries API.
 *
 * <p><b>Design rationale:</b> Centralising query literals in one place prevents
 * duplication across test classes, gives reviewers a single location to validate
 * GraphQL syntax, and makes schema-driven updates (field renames, new selections)
 * a single-point change.  Constants are used for fixed queries; static factory
 * methods are used for queries that require inline arguments (e.g., country code)
 * so callers remain readable without string interpolation scattered throughout
 * test files.</p>
 *
 * <p>All queries target {@code https://countries.trevorblades.com/graphql}
 * (the Countries API by Trevor Blades) and are compatible with its published
 * schema as of 2025.</p>
 *
 * <p>This class is a utility constants holder — it is {@code final} and cannot
 * be instantiated.</p>
 *
 * @see GraphQlClient
 * @see GraphQlRequest
 */
public final class CountriesQueries {

    private CountriesQueries() {
        // Utility class — no instantiation.
    }

    // -----------------------------------------------------------------------
    //  Country queries
    // -----------------------------------------------------------------------

    /**
     * Fetches name, capital, currency, phone code, native name, and emoji for a
     * country identified by its ISO 3166-1 alpha-2 code.
     *
     * <p>Uses a GraphQL variable ({@code $code: ID!}) so the same document can
     * be sent with different codes without string interpolation — suitable for
     * data-driven test with {@link GraphQlClient#query(String, java.util.Map)}.</p>
     *
     * <p>Example variables: {@code Map.of("code", "US")}</p>
     */
    public static final String COUNTRY_BY_CODE_WITH_VARIABLE =
            "query CountryByCode($code: ID!) {" +
            "  country(code: $code) {" +
            "    name" +
            "    native" +
            "    capital" +
            "    currency" +
            "    phone" +
            "    emoji" +
            "    languages {" +
            "      code" +
            "      name" +
            "    }" +
            "  }" +
            "}";

    /**
     * Fetches all fields for the United States (inline code, no variables).
     * Used for positive single-country assertions.
     */
    public static final String COUNTRY_US =
            "{ country(code: \"US\") { name native capital currency phone emoji" +
            "    languages { code name }" +
            "  }" +
            "}";

    /**
     * Fetches all fields for India (inline code, no variables).
     */
    public static final String COUNTRY_IN =
            "{ country(code: \"IN\") { name native capital currency phone emoji" +
            "    languages { code name }" +
            "  }" +
            "}";

    /**
     * Fetches all fields for Brazil (inline code, no variables).
     */
    public static final String COUNTRY_BR =
            "{ country(code: \"BR\") { name native capital currency phone emoji" +
            "    languages { code name }" +
            "  }" +
            "}";

    // -----------------------------------------------------------------------
    //  Dynamic country query builder
    // -----------------------------------------------------------------------

    /**
     * Builds an inline country query for the given ISO alpha-2 code.
     *
     * <p>Use this method when a data provider drives multiple country codes
     * and variables are not desirable (e.g., negative code tests).</p>
     *
     * @param code the ISO 3166-1 alpha-2 country code (e.g., {@code "DE"})
     * @return a GraphQL query string selecting name, capital, currency, and emoji
     */
    public static String countryByCode(final String code) {
        return "{ country(code: \"" + code + "\") { name capital currency emoji } }";
    }

    // -----------------------------------------------------------------------
    //  Continent queries
    // -----------------------------------------------------------------------

    /**
     * Fetches all continents with their ISO code and name.
     * The Countries API returns exactly 7 continents.
     */
    public static final String CONTINENTS =
            "{ continents { code name } }";

    /**
     * Fetches all continents with nested countries (code + name only) to
     * validate the continent–country relationship.
     */
    public static final String CONTINENTS_WITH_COUNTRIES =
            "{ continents { code name countries { code name } } }";

    // -----------------------------------------------------------------------
    //  Countries filtered by continent
    // -----------------------------------------------------------------------

    /**
     * Builds a query that fetches countries belonging to a specific continent,
     * filtered using the {@code filter} argument on the {@code countries} field.
     *
     * <p>Example: {@code countriesByContinent("EU")} returns all European countries.</p>
     *
     * @param continentCode the ISO continent code (e.g., {@code "EU"}, {@code "AS"})
     * @return a GraphQL query string selecting country code, name, and capital
     */
    public static String countriesByContinent(final String continentCode) {
        return "{ countries(filter: { continent: { eq: \"" + continentCode + "\" } })" +
               "  { code name capital } }";
    }

    // -----------------------------------------------------------------------
    //  Language queries
    // -----------------------------------------------------------------------

    /**
     * Fetches all languages registered in the Countries API with their ISO code,
     * name, and native name.
     */
    public static final String LANGUAGES =
            "{ languages { code name native } }";

    // -----------------------------------------------------------------------
    //  Alias query
    // -----------------------------------------------------------------------

    /**
     * Fetches two countries in a single request using GraphQL field aliases.
     * Demonstrates that the Countries API correctly resolves aliased fields.
     */
    public static final String ALIAS_TWO_COUNTRIES =
            "{ usa: country(code: \"US\") { name capital }" +
            "  germany: country(code: \"DE\") { name capital } }";

    // -----------------------------------------------------------------------
    //  Negative / error-inducing queries
    // -----------------------------------------------------------------------

    /**
     * A deliberately malformed GraphQL document (unclosed brace) that must
     * cause the server to return an {@code errors} array.
     */
    public static final String MALFORMED_QUERY =
            "{ country(code: \"US\") { name capital";

    /**
     * A syntactically valid query that requests a non-existent field
     * ({@code nonExistentField}) on the country type.  The server must
     * return a field-level error.
     */
    public static final String UNKNOWN_FIELD_QUERY =
            "{ country(code: \"US\") { nonExistentField } }";
}
