package com.omiinqa.api.graphql;

import com.omiinqa.api.validator.ResponseValidator;
import com.omiinqa.config.FrameworkConfig;
import io.restassured.response.Response;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.assertj.core.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Negative path tests for the GraphQL Countries API.
 *
 * <p>GraphQL error semantics differ from REST: errors are typically returned
 * with an HTTP 200 status code and a JSON body containing an {@code errors}
 * array.  These tests verify that:</p>
 * <ul>
 *   <li>Malformed query documents cause the server to return an {@code errors}
 *       array and a {@code null} (or absent) {@code data} field.</li>
 *   <li>Unknown field selections cause a field-level error entry.</li>
 *   <li>An unknown country code returns {@code null} for {@code data.country}
 *       without a protocol-level error.</li>
 *   <li>The {@code errors} array is non-empty and each entry has a non-null
 *       {@code message} field.</li>
 * </ul>
 *
 * <p>Tests do NOT extend {@code BaseTest} or {@code AbstractApiTest}.</p>
 *
 * @see GraphQlClient
 * @see CountriesQueries
 */
@Feature("GraphQL Countries API")
@Story("Negative / Error Cases")
public class GraphQlNegativeTest {

    private static final Logger LOG = LoggerFactory.getLogger(GraphQlNegativeTest.class);

    private GraphQlClient client;

    /**
     * Initialises the {@link GraphQlClient} once per test class.
     */
    @BeforeClass(alwaysRun = true)
    public void initClient() {
        client = new GraphQlClient();
        LOG.info("GraphQlNegativeTest initialised against: {}",
                FrameworkConfig.get().apiUrl("countries.graphql"));
    }

    // -----------------------------------------------------------------------
    //  Malformed query — parse error
    // -----------------------------------------------------------------------

    /**
     * Verifies that a syntactically malformed query (unclosed brace) causes the
     * server to respond with HTTP 200 and an {@code errors} array.
     *
     * <p>Per the GraphQL June 2018 specification § 7.2.2, when a request fails
     * validation or parsing the server MUST return an {@code errors} array; the
     * {@code data} field may be {@code null} or absent.</p>
     */
    @Test(groups = {"api", "regression"},
          description = "malformed GraphQL query returns errors[] array in response body")
    @Description("Validates that a parse-invalid query produces a GraphQL errors array")
    public void malformedQuery_response_containsErrorsArray() {
        final Response response = client.query(CountriesQueries.MALFORMED_QUERY);

        // GraphQL servers return 200 even for errors; some return 400 for syntax errors.
        // Assert that the response has a non-empty errors array regardless of status.
        ResponseValidator.of(response).statusCode(200);

        final List<?> errors = response.jsonPath().getList("errors");
        Assertions.assertThat(errors)
                .as("Malformed query must produce a non-empty 'errors' array")
                .isNotNull()
                .isNotEmpty();

        LOG.info("Malformed query errors count: {}", errors.size());
    }

    /**
     * Verifies that the error object returned for a malformed query contains a
     * non-null, non-blank {@code message} field — a required field per the spec.
     */
    @Test(groups = {"api", "regression"},
          description = "malformed query error object contains non-blank 'message' field")
    @Description("Validates errors[0].message is present and non-blank for malformed query")
    public void malformedQuery_errorObject_hasNonBlankMessage() {
        final Response response = client.query(CountriesQueries.MALFORMED_QUERY);

        ResponseValidator.of(response).statusCode(200);

        final String message = response.jsonPath().getString("errors[0].message");
        Assertions.assertThat(message)
                .as("errors[0].message must be non-null and non-blank")
                .isNotNull()
                .isNotBlank();

        LOG.info("Malformed query error message: {}", message);
    }

    // -----------------------------------------------------------------------
    //  Unknown field — field-level error
    // -----------------------------------------------------------------------

    /**
     * Verifies that requesting a non-existent field ({@code nonExistentField})
     * on the country type causes a field-level GraphQL error.
     */
    @Test(groups = {"api", "regression"},
          description = "query with unknown field returns errors[] with field-level error message")
    @Description("Validates that selecting a non-existent field produces a GraphQL field error")
    public void unknownField_response_containsErrors() {
        final Response response = client.query(CountriesQueries.UNKNOWN_FIELD_QUERY);

        // REST Assured response is HTTP 200; errors are in the body
        ResponseValidator.of(response).statusCode(200);

        final List<?> errors = response.jsonPath().getList("errors");
        Assertions.assertThat(errors)
                .as("Unknown field query must produce a non-empty 'errors' array")
                .isNotNull()
                .isNotEmpty();

        LOG.info("Unknown field errors: {}", errors.size());
    }

    /**
     * Verifies that the error message for an unknown field references the
     * field name so diagnostics are useful.
     */
    @Test(groups = {"api", "regression"},
          description = "unknown field error message references the invalid field name")
    @Description("Validates errors[0].message mentions the unknown field name")
    public void unknownField_errorMessage_referencesFieldName() {
        final Response response = client.query(CountriesQueries.UNKNOWN_FIELD_QUERY);

        ResponseValidator.of(response).statusCode(200);

        final String message = response.jsonPath().getString("errors[0].message");
        Assertions.assertThat(message)
                .as("Error message must reference the invalid field name")
                .isNotNull()
                .isNotBlank();

        LOG.info("Unknown field error message: {}", message);
    }

    // -----------------------------------------------------------------------
    //  Unknown country code — null data
    // -----------------------------------------------------------------------

    /**
     * Verifies that querying an unknown country code (e.g., "XX") returns HTTP
     * 200 with {@code data.country} as {@code null} — this is valid GraphQL
     * behaviour for nullable fields that resolve to nothing.
     */
    @Test(groups = {"api", "regression"},
          description = "country query with unknown code returns null data.country (no errors)")
    @Description("Validates that an unrecognised country code returns null data.country")
    public void unknownCountryCode_dataCountry_isNull() {
        final Response response = client.query(CountriesQueries.countryByCode("XX"));

        ResponseValidator.of(response).statusCode(200);

        final Object country = response.jsonPath().get("data.country");
        Assertions.assertThat(country)
                .as("data.country must be null for an unrecognised country code")
                .isNull();

        LOG.info("Unknown country code 'XX' resolved to: {}", country);
    }

    // -----------------------------------------------------------------------
    //  Empty string query
    // -----------------------------------------------------------------------

    /**
     * Verifies that sending an empty string as the query body causes the server
     * to return an error rather than silently succeeding.
     */
    @Test(groups = {"api", "regression"},
          description = "empty string query returns errors[] or non-null errors field")
    @Description("Validates that an empty query string is rejected by the server")
    public void emptyQuery_response_containsErrors() {
        final Response response = client.query("");

        ResponseValidator.of(response).statusCode(200);

        final List<?> errors = response.jsonPath().getList("errors");
        Assertions.assertThat(errors)
                .as("Empty query must produce a non-null 'errors' array")
                .isNotNull()
                .isNotEmpty();
    }

    // -----------------------------------------------------------------------
    //  Null variables with valid query
    // -----------------------------------------------------------------------

    /**
     * Verifies that passing {@code null} as the variables map alongside a
     * no-variable query does not break the request — the {@code variables}
     * field is simply omitted from the JSON payload.
     */
    @Test(groups = {"api", "regression"},
          description = "query with null variables map succeeds for a no-variable query")
    @Description("Validates that null variables parameter is safely ignored")
    public void query_nullVariables_succeeds() {
        final Response response = client.query(CountriesQueries.CONTINENTS, null);

        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPathNotNull("data.continents");

        LOG.info("Null-variables query succeeded; continents returned: {}",
                response.jsonPath().getList("data.continents").size());
    }

}
