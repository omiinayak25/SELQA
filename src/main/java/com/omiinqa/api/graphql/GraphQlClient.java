package com.omiinqa.api.graphql;

import com.omiinqa.api.builder.RequestBuilder;
import com.omiinqa.api.client.ApiClient;
import com.omiinqa.config.FrameworkConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Facade over the GraphQL Countries API at {@code https://countries.trevorblades.com}.
 *
 * <p><b>Design pattern:</b> Facade (GoF) — encapsulates the mechanics of
 * constructing a GraphQL-over-HTTP POST request (JSON body with {@code query}
 * and optional {@code variables} fields, {@code application/json} content type,
 * single endpoint) so that test classes and service layers interact with a
 * clean, intention-revealing API rather than low-level REST Assured plumbing.</p>
 *
 * <p><b>Protocol notes:</b> The Countries API is a standard GraphQL endpoint.
 * Every operation is an HTTP POST to the root path ({@code /}).  The request
 * body is a JSON object serialised from {@link GraphQlRequest}; successful
 * responses return HTTP 200 with a JSON body whose top-level keys are
 * {@code data} (for successful field resolution) and/or {@code errors} (for
 * field-level or document-level errors).</p>
 *
 * <p><b>Base URL resolution:</b> The base URI is read from
 * {@link FrameworkConfig#apiUrl(String)} using the key {@code "countries.graphql"},
 * which maps to the property {@code api.countries.graphql.url} in
 * {@code config.properties}. This keeps the concrete URL out of test code and
 * makes environment switching trivial.</p>
 *
 * <p><b>Thread safety:</b> Instances are stateless after construction and may
 * be shared across test methods within the same test class; however, because
 * {@link RequestBuilder} is not thread-safe, each {@code query()} invocation
 * creates its own builder instance.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * GraphQlClient client = new GraphQlClient();
 *
 * // Simple query
 * Response r1 = client.query(CountriesQueries.CONTINENTS);
 *
 * // Parameterised query with variables
 * Response r2 = client.query(
 *     CountriesQueries.COUNTRY_BY_CODE,
 *     Map.of("code", "US"));
 * }</pre>
 *
 * @see GraphQlRequest
 * @see CountriesQueries
 * @see com.omiinqa.api.validator.ResponseValidator
 */
public final class GraphQlClient {

    private static final Logger LOG = LoggerFactory.getLogger(GraphQlClient.class);

    /** GraphQL services expose a single endpoint — the root path. */
    private static final String GRAPHQL_PATH = "/";

    private final String baseUri;

    /**
     * Constructs a client whose base URI is resolved from
     * {@link FrameworkConfig#apiUrl(String)} with the key {@code "countries.graphql"}.
     *
     * <p>This is the preferred constructor for production test use.</p>
     */
    public GraphQlClient() {
        this.baseUri = FrameworkConfig.get().apiUrl("countries.graphql");
        LOG.debug("GraphQlClient initialised against {}", this.baseUri);
    }

    /**
     * Constructs a client with an explicit base URI.
     *
     * <p>Use this constructor for contract tests or when the server address
     * must be overridden without changing {@code config.properties}.</p>
     *
     * @param baseUri the fully-qualified base URI (scheme + host, no trailing slash);
     *                must not be {@code null} or blank
     */
    public GraphQlClient(final String baseUri) {
        this.baseUri = baseUri;
        LOG.debug("GraphQlClient initialised against explicit URI {}", baseUri);
    }

    // -----------------------------------------------------------------------
    //  Public API
    // -----------------------------------------------------------------------

    /**
     * Executes a GraphQL query or mutation without variables.
     *
     * <p>The query string is wrapped in a {@link GraphQlRequest} POJO and
     * serialised as JSON by Jackson.  The {@code variables} field is omitted
     * from the payload (see {@link GraphQlRequest}'s {@code @JsonInclude}
     * annotation).</p>
     *
     * @param query the GraphQL query document string; must not be {@code null}
     * @return the raw REST Assured {@link Response} (HTTP 200 for both
     *         successful data responses and field-level GraphQL errors)
     * @throws com.omiinqa.exceptions.ApiException if the HTTP transport fails
     */
    public Response query(final String query) {
        LOG.debug("Executing GraphQL query (no variables): {}", abbreviated(query));
        return post(GraphQlRequest.builder().query(query).build());
    }

    /**
     * Executes a GraphQL query or mutation with named variables.
     *
     * <p>Variables are bound to the GraphQL document's variable declarations
     * (e.g., {@code query CountryQuery($code: ID!) { country(code: $code) { name } }})
     * and passed in the {@code "variables"} JSON field.</p>
     *
     * @param query     the GraphQL query document string; must not be {@code null}
     * @param variables a map of variable name → value that will be serialised
     *                  into the {@code "variables"} JSON field; {@code null} is
     *                  equivalent to calling {@link #query(String)}
     * @return the raw REST Assured {@link Response}
     * @throws com.omiinqa.exceptions.ApiException if the HTTP transport fails
     */
    public Response query(final String query, final Map<String, Object> variables) {
        LOG.debug("Executing GraphQL query with {} variable(s): {}",
                variables == null ? 0 : variables.size(), abbreviated(query));
        return post(GraphQlRequest.builder().query(query).variables(variables).build());
    }

    // -----------------------------------------------------------------------
    //  Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Builds and dispatches the REST Assured POST request for the given
     * {@link GraphQlRequest}.
     *
     * <p>Content type is set to {@code application/json} because GraphQL over
     * HTTP mandates JSON-encoded bodies.  The path is always {@code /} since
     * the Countries API does not use operation-specific endpoints.</p>
     *
     * @param request the fully-constructed request body POJO
     * @return the server's raw {@link Response}
     */
    private Response post(final GraphQlRequest request) {
        return ApiClient.post(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(GRAPHQL_PATH)
                        .contentType(ContentType.JSON)
                        .body(request)
                        .build());
    }

    /**
     * Abbreviates a query string for safe, readable log output.
     *
     * @param query the full query string
     * @return the first 100 characters followed by {@code "..."} if longer
     */
    private static String abbreviated(final String query) {
        if (query == null) return "<null>";
        return query.length() > 100 ? query.substring(0, 100) + "..." : query;
    }
}
