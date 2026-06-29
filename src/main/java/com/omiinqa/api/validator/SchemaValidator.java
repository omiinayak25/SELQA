package com.omiinqa.api.validator;

import com.omiinqa.exceptions.ApiException;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSON Schema validation helper that delegates to REST Assured's
 * {@link JsonSchemaValidator} (backed by the Everit JSON Schema library).
 *
 * <p><b>Design rationale:</b> Extracting schema validation into its own class
 * keeps {@link ResponseValidator} focused on field-level assertions and gives
 * the schema concern a dedicated home.  Both classes are used together:</p>
 * <pre>{@code
 * ResponseValidator.of(response)
 *     .statusCode(200)
 *     .matchesSchema("schemas/reqres-user-schema.json");
 * // — or directly:
 * SchemaValidator.validate(response, "schemas/booking-schema.json");
 * }</pre>
 *
 * <p>Schema files must reside in {@code src/test/resources/schemas/} and be
 * on the test classpath.  Draft-04 through draft-07 are supported by
 * the underlying library.</p>
 *
 * <p>Instances of this class are never created; all methods are static.</p>
 */
public final class SchemaValidator {

    private static final Logger LOG = LoggerFactory.getLogger(SchemaValidator.class);

    private SchemaValidator() {
        // Utility class — no instantiation.
    }

    /**
     * Validates {@code response}'s JSON body against the schema at
     * {@code schemaClasspathPath}.
     *
     * <p>Validation failures surface as {@link AssertionError} from Hamcrest's
     * {@code assertThat}, keeping them consistent with REST Assured's own
     * assertion style and distinguishable from {@link ApiException} (setup errors)
     * in the Allure report.</p>
     *
     * @param response            the REST Assured response whose body will be
     *                            validated; must not be {@code null}
     * @param schemaClasspathPath classpath-relative path to the JSON Schema file,
     *                            e.g. {@code "schemas/reqres-user-schema.json"};
     *                            must not be {@code null} or blank
     * @throws ApiException    if {@code response} is {@code null} or the schema
     *                         path is blank
     * @throws AssertionError  if the response body does not conform to the schema
     */
    public static void validate(final Response response, final String schemaClasspathPath) {
        if (response == null) {
            throw new ApiException("SchemaValidator requires a non-null Response");
        }
        if (schemaClasspathPath == null || schemaClasspathPath.isBlank()) {
            throw new ApiException("Schema classpath path must not be blank");
        }

        LOG.debug("Validating response body against schema: {}", schemaClasspathPath);
        response.then()
                .assertThat()
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath(schemaClasspathPath));
        LOG.debug("Schema validation passed for: {}", schemaClasspathPath);
    }
}
