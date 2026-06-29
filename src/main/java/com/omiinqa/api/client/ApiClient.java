package com.omiinqa.api.client;

import com.omiinqa.config.FrameworkConfig;
import com.omiinqa.exceptions.ApiException;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Central REST Assured wrapper providing static, HTTP-verb-aligned methods
 * for every interaction between the framework and an API under test.
 *
 * <p><b>Design rationale:</b> A single entry point for all HTTP calls ensures
 * that cross-cutting concerns — request/response logging, Allure attachment,
 * and exception translation — are applied uniformly without duplicating filter
 * setup in every service or test class.  Static methods are chosen over an
 * instance-based singleton because service classes hold no per-call state and
 * static imports ({@code import static ApiClient.*}) keep test code concise.</p>
 *
 * <p><b>Filters applied on every request:</b></p>
 * <ol>
 *   <li>{@link AllureRestAssured} — attaches request/response bodies to the
 *       Allure report step for traceability.</li>
 *   <li>{@link RequestLoggingFilter} and {@link ResponseLoggingFilter} at
 *       {@link LogDetail#ALL}, writing to an SLF4J-backed {@link PrintStream}
 *       so output flows through Log4j2 rather than to raw stdout in CI.</li>
 * </ol>
 *
 * <p><b>Base URL resolution:</b> callers must obtain the base URI from
 * {@code FrameworkConfig.get().apiUrl(key)} before constructing the
 * {@link RequestSpecification} (typically via {@link com.omiinqa.api.builder.RequestBuilder}).
 * This class does not impose a fixed base URL so it can serve any API key
 * registered in {@code config.properties}.</p>
 *
 * <p>Instances of this class are never created; all methods are static.</p>
 */
public final class ApiClient {

    private static final Logger LOG = LoggerFactory.getLogger(ApiClient.class);

    static {
        // Disable REST Assured's default URL-encoding so explicit path params
        // are not double-encoded when they contain special characters.
        RestAssured.urlEncodingEnabled = false;
    }

    private ApiClient() {
        // Utility class — no instantiation.
    }

    // -----------------------------------------------------------------------
    //  HTTP verbs
    // -----------------------------------------------------------------------

    /**
     * Executes an HTTP GET request.
     *
     * @param spec a fully configured {@link RequestSpecification} (base URI,
     *             path, headers, query/path params, auth); must not be {@code null}
     * @return the server's {@link Response}; never {@code null}
     * @throws ApiException if the request cannot be sent due to a connectivity
     *                      or configuration error
     */
    public static Response get(final RequestSpecification spec) {
        return execute(spec, "GET", null);
    }

    /**
     * Executes an HTTP POST request.
     *
     * @param spec a fully configured {@link RequestSpecification}; must not be {@code null}
     * @return the server's {@link Response}; never {@code null}
     * @throws ApiException if the request cannot be sent
     */
    public static Response post(final RequestSpecification spec) {
        return execute(spec, "POST", null);
    }

    /**
     * Executes an HTTP PUT request.
     *
     * @param spec a fully configured {@link RequestSpecification}; must not be {@code null}
     * @return the server's {@link Response}; never {@code null}
     * @throws ApiException if the request cannot be sent
     */
    public static Response put(final RequestSpecification spec) {
        return execute(spec, "PUT", null);
    }

    /**
     * Executes an HTTP PATCH request.
     *
     * @param spec a fully configured {@link RequestSpecification}; must not be {@code null}
     * @return the server's {@link Response}; never {@code null}
     * @throws ApiException if the request cannot be sent
     */
    public static Response patch(final RequestSpecification spec) {
        return execute(spec, "PATCH", null);
    }

    /**
     * Executes an HTTP DELETE request.
     *
     * @param spec a fully configured {@link RequestSpecification}; must not be {@code null}
     * @return the server's {@link Response}; never {@code null}
     * @throws ApiException if the request cannot be sent
     */
    public static Response delete(final RequestSpecification spec) {
        return execute(spec, "DELETE", null);
    }

    /**
     * Executes an HTTP GET against a known {@code apiKey} (resolved through
     * {@link FrameworkConfig}) and path, without requiring callers to build a
     * {@link RequestSpecification} when no extra configuration is needed.
     *
     * @param apiKey the key understood by {@code FrameworkConfig.get().apiUrl()} (e.g., "reqres")
     * @param path   the resource path (e.g., {@code "/users/2"})
     * @return the server's {@link Response}; never {@code null}
     * @throws ApiException if the request cannot be sent
     */
    public static Response getFromApi(final String apiKey, final String path) {
        final String baseUri = FrameworkConfig.get().apiUrl(apiKey);
        final RequestSpecification spec = withFilters(RestAssured.given()
                .baseUri(baseUri)
                .basePath(path));
        return execute(spec, "GET", path);
    }

    // -----------------------------------------------------------------------
    //  Internal execution pipeline
    // -----------------------------------------------------------------------

    /**
     * Attaches cross-cutting logging and Allure filters to a specification
     * and executes the named HTTP method.
     *
     * <p>Filters are added here (rather than in {@code RequestBuilder}) to
     * guarantee they are always present regardless of how the spec was
     * constructed.</p>
     */
    private static Response execute(final RequestSpecification spec,
                                    final String method,
                                    final String path) {
        try {
            final RequestSpecification prepared = withFilters(spec);
            LOG.debug("Executing {} {}", method, path != null ? path : "<spec-defined path>");

            return switch (method) {
                case "GET"    -> prepared.when().get();
                case "POST"   -> prepared.when().post();
                case "PUT"    -> prepared.when().put();
                case "PATCH"  -> prepared.when().patch();
                case "DELETE" -> prepared.when().delete();
                default -> throw new ApiException("Unsupported HTTP method: " + method);
            };
        } catch (final ApiException ae) {
            throw ae;
        } catch (final Exception ex) {
            throw new ApiException("Failed to execute " + method + " request: " + ex.getMessage(), ex);
        }
    }

    /**
     * Decorates a {@link RequestSpecification} with the standard suite of
     * filters: {@link AllureRestAssured} for report attachment and
     * {@link RequestLoggingFilter}/{@link ResponseLoggingFilter} writing to
     * an SLF4J-aware {@link PrintStream}.
     */
    private static RequestSpecification withFilters(final RequestSpecification spec) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintStream ps = new PrintStream(baos, true, StandardCharsets.UTF_8) {
            @Override
            public void flush() {
                super.flush();
                final String content = baos.toString(StandardCharsets.UTF_8).trim();
                if (!content.isEmpty()) {
                    LOG.debug("[REST-ASSURED]\n{}", content);
                    baos.reset();
                }
            }
        };

        return spec
                .filter(new AllureRestAssured())
                .filter(new RequestLoggingFilter(LogDetail.ALL, ps))
                .filter(new ResponseLoggingFilter(LogDetail.ALL, ps));
    }
}
