package com.omiinqa.api.validator;

import com.omiinqa.exceptions.ApiException;
import io.restassured.response.Response;
import org.assertj.core.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Fluent, AssertJ-backed validator for REST Assured {@link Response} objects.
 *
 * <p><b>Pattern:</b> Fluent Interface / Method Chaining — every assertion
 * returns {@code this} so verifications compose readably at the call site:</p>
 * <pre>{@code
 * ResponseValidator.of(response)
 *     .statusCode(201)
 *     .hasHeader("Content-Type", "application/json")
 *     .bodyJsonPath("data.id", 5)
 *     .responseTimeLessThan(3000, TimeUnit.MILLISECONDS);
 * }</pre>
 *
 * <p><b>Failure semantics:</b> AssertJ failures throw
 * {@link AssertionError}; misuse of the API (e.g., passing a {@code null}
 * response) throws {@link ApiException}.  Test listeners distinguish these
 * two exception types to produce accurate failure vs. error classifications
 * in the Allure report.</p>
 *
 * <p>Instances hold a reference to the response under test and are NOT
 * thread-safe; create one per response.</p>
 */
public final class ResponseValidator {

    private static final Logger LOG = LoggerFactory.getLogger(ResponseValidator.class);

    private final Response response;

    private ResponseValidator(final Response response) {
        if (response == null) {
            throw new ApiException("ResponseValidator requires a non-null Response");
        }
        this.response = response;
    }

    /**
     * Factory method — the preferred way to obtain a {@code ResponseValidator}.
     *
     * @param response the REST Assured response to validate; must not be {@code null}
     * @return a new {@code ResponseValidator} wrapping {@code response}
     * @throws ApiException if {@code response} is {@code null}
     */
    public static ResponseValidator of(final Response response) {
        return new ResponseValidator(response);
    }

    // -----------------------------------------------------------------------
    //  Status
    // -----------------------------------------------------------------------

    /**
     * Asserts that the HTTP status code equals {@code expected}.
     *
     * @param expected the expected HTTP status code (e.g., 200, 201, 404)
     * @return {@code this} for chaining
     */
    public ResponseValidator statusCode(final int expected) {
        LOG.debug("Asserting status code == {}", expected);
        Assertions.assertThat(response.getStatusCode())
                .as("HTTP status code")
                .isEqualTo(expected);
        return this;
    }

    /**
     * Asserts that the HTTP status code falls within [minInclusive, maxInclusive].
     *
     * @param minInclusive lower bound (inclusive)
     * @param maxInclusive upper bound (inclusive)
     * @return {@code this} for chaining
     */
    public ResponseValidator statusCodeBetween(final int minInclusive, final int maxInclusive) {
        LOG.debug("Asserting status code in [{}, {}]", minInclusive, maxInclusive);
        Assertions.assertThat(response.getStatusCode())
                .as("HTTP status code range [%d, %d]", minInclusive, maxInclusive)
                .isBetween(minInclusive, maxInclusive);
        return this;
    }

    /**
     * Asserts that the HTTP status code is a 2xx success code.
     *
     * @return {@code this} for chaining
     */
    public ResponseValidator isSuccess() {
        return statusCodeBetween(200, 299);
    }

    // -----------------------------------------------------------------------
    //  Headers
    // -----------------------------------------------------------------------

    /**
     * Asserts that the response contains a header with the given name.
     *
     * @param headerName the header name (case-insensitive per HTTP spec)
     * @return {@code this} for chaining
     */
    public ResponseValidator hasHeader(final String headerName) {
        LOG.debug("Asserting header '{}' is present", headerName);
        Assertions.assertThat(response.getHeader(headerName))
                .as("Header '%s' present", headerName)
                .isNotNull();
        return this;
    }

    /**
     * Asserts that the response contains a header whose value contains {@code expectedValue}.
     *
     * @param headerName    the header name
     * @param expectedValue the substring that the header value must contain
     * @return {@code this} for chaining
     */
    public ResponseValidator hasHeader(final String headerName, final String expectedValue) {
        LOG.debug("Asserting header '{}' contains '{}'", headerName, expectedValue);
        Assertions.assertThat(response.getHeader(headerName))
                .as("Header '%s' value", headerName)
                .isNotNull()
                .containsIgnoringCase(expectedValue);
        return this;
    }

    /**
     * Asserts that the {@code Content-Type} header contains the given media type.
     *
     * @param mediaType e.g. {@code "application/json"}
     * @return {@code this} for chaining
     */
    public ResponseValidator contentType(final String mediaType) {
        return hasHeader("Content-Type", mediaType);
    }

    // -----------------------------------------------------------------------
    //  Body — JsonPath
    // -----------------------------------------------------------------------

    /**
     * Asserts that the JSON body path {@code jsonPath} resolves to a non-null value.
     *
     * @param jsonPath a REST Assured / Groovy GPath expression (e.g., {@code "data.id"})
     * @return {@code this} for chaining
     */
    public ResponseValidator bodyJsonPathNotNull(final String jsonPath) {
        LOG.debug("Asserting JSON path '{}' is not null", jsonPath);
        Assertions.assertThat((Object) response.jsonPath().get(jsonPath))
                .as("JSON path '%s' must not be null", jsonPath)
                .isNotNull();
        return this;
    }

    /**
     * Asserts that the JSON body path {@code jsonPath} resolves to {@code expectedValue}.
     *
     * @param jsonPath      a GPath expression
     * @param expectedValue the expected value; compared with {@code equals()}
     * @return {@code this} for chaining
     */
    public ResponseValidator bodyJsonPath(final String jsonPath, final Object expectedValue) {
        LOG.debug("Asserting JSON path '{}' == '{}'", jsonPath, expectedValue);
        Assertions.assertThat((Object) response.jsonPath().get(jsonPath))
                .as("JSON path '%s'", jsonPath)
                .isEqualTo(expectedValue);
        return this;
    }

    /**
     * Asserts that the JSON body path {@code jsonPath} resolves to a string
     * containing {@code substring}.
     *
     * @param jsonPath   a GPath expression
     * @param substring  the substring that must appear in the path's string value
     * @return {@code this} for chaining
     */
    public ResponseValidator bodyJsonPathContains(final String jsonPath, final String substring) {
        LOG.debug("Asserting JSON path '{}' contains '{}'", jsonPath, substring);
        final String actual = response.jsonPath().getString(jsonPath);
        Assertions.assertThat(actual)
                .as("JSON path '%s' contains '%s'", jsonPath, substring)
                .isNotNull()
                .contains(substring);
        return this;
    }

    /**
     * Asserts that the body, when interpreted as a JSON array at {@code jsonPath},
     * has the expected size.
     *
     * @param jsonPath      a GPath expression resolving to a list
     * @param expectedSize  the expected list size
     * @return {@code this} for chaining
     */
    public ResponseValidator bodyJsonPathListSize(final String jsonPath, final int expectedSize) {
        LOG.debug("Asserting JSON path '{}' list size == {}", jsonPath, expectedSize);
        Assertions.assertThat(response.jsonPath().getList(jsonPath))
                .as("JSON path '%s' list size", jsonPath)
                .hasSize(expectedSize);
        return this;
    }

    /**
     * Asserts that the response body string is not blank.
     *
     * @return {@code this} for chaining
     */
    public ResponseValidator bodyNotEmpty() {
        LOG.debug("Asserting response body is not empty");
        Assertions.assertThat(response.getBody().asString())
                .as("Response body must not be blank")
                .isNotBlank();
        return this;
    }

    // -----------------------------------------------------------------------
    //  Performance
    // -----------------------------------------------------------------------

    /**
     * Asserts that the response time is less than {@code threshold} in the
     * given {@code unit}.
     *
     * <p>Useful for catching regressions in API response time without a full
     * performance test suite.</p>
     *
     * @param threshold the maximum allowable elapsed time
     * @param unit      the time unit for {@code threshold}
     * @return {@code this} for chaining
     */
    public ResponseValidator responseTimeLessThan(final long threshold, final TimeUnit unit) {
        final long actualMs = response.getTime();
        final long thresholdMs = unit.toMillis(threshold);
        LOG.debug("Asserting response time {}ms < {}ms threshold", actualMs, thresholdMs);
        Assertions.assertThat(actualMs)
                .as("Response time (ms) must be less than %d ms", thresholdMs)
                .isLessThan(thresholdMs);
        return this;
    }

    // -----------------------------------------------------------------------
    //  Schema — delegates to SchemaValidator
    // -----------------------------------------------------------------------

    /**
     * Validates the response body against a JSON Schema file on the classpath.
     *
     * <p>Schema files must reside in {@code src/test/resources/schemas/} which
     * is on the test classpath at runtime.</p>
     *
     * @param schemaClasspathPath classpath-relative path, e.g.
     *                            {@code "schemas/reqres-user-schema.json"}
     * @return {@code this} for chaining
     * @see SchemaValidator#validate(Response, String)
     */
    public ResponseValidator matchesSchema(final String schemaClasspathPath) {
        SchemaValidator.validate(response, schemaClasspathPath);
        return this;
    }

    // -----------------------------------------------------------------------
    //  Raw access
    // -----------------------------------------------------------------------

    /**
     * Returns the wrapped {@link Response} for assertions not covered by this
     * validator's built-in methods.
     *
     * @return the underlying response; never {@code null}
     */
    public Response raw() {
        return response;
    }
}
