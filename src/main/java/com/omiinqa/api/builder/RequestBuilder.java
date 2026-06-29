package com.omiinqa.api.builder;

import com.omiinqa.api.auth.AuthenticationStrategy;
import com.omiinqa.api.auth.NoAuth;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import java.util.HashMap;
import java.util.Map;

/**
 * Fluent builder for REST Assured {@link RequestSpecification} objects.
 *
 * <p><b>Pattern:</b> Builder (GoF) — shields callers from the complexity of
 * REST Assured's {@code RequestSpecification} DSL, enforces consistent
 * defaults (JSON content-type, logging), and makes it impossible to
 * accidentally share mutable specification state across tests.  Each
 * {@link #build()} call produces a fresh, independent specification.</p>
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 * RequestSpecification spec = new RequestBuilder()
 *     .baseUri(FrameworkConfig.get().apiUrl("reqres"))
 *     .basePath(ApiEndpoints.REQRES_USER_BY_ID)
 *     .pathParam("id", 2)
 *     .auth(new BearerTokenAuth(token))
 *     .contentType(ContentType.JSON)
 *     .build();
 * Response response = ApiClient.get(spec);
 * }</pre>
 *
 * <p>Instances of this builder are NOT thread-safe; create a new builder per
 * request.</p>
 */
public final class RequestBuilder {

    private String baseUri;
    private String basePath                = "";
    private ContentType contentType        = ContentType.JSON;
    private Object body;
    private AuthenticationStrategy auth   = new NoAuth();

    private final Map<String, String>  headers     = new HashMap<>();
    private final Map<String, Object>  queryParams = new HashMap<>();
    private final Map<String, Object>  pathParams  = new HashMap<>();
    private final Map<String, String>  cookies     = new HashMap<>();

    // -----------------------------------------------------------------------
    //  Fluent setters
    // -----------------------------------------------------------------------

    /**
     * Sets the base URI (scheme + host, optionally port).
     *
     * @param baseUri e.g. {@code "https://reqres.in/api"}; must not be {@code null}
     * @return {@code this} for chaining
     */
    public RequestBuilder baseUri(final String baseUri) {
        this.baseUri = baseUri;
        return this;
    }

    /**
     * Sets the resource path appended to the base URI.
     *
     * @param basePath e.g. {@code "/users/{id}"}
     * @return {@code this} for chaining
     */
    public RequestBuilder basePath(final String basePath) {
        this.basePath = basePath;
        return this;
    }

    /**
     * Adds a single request header.
     *
     * @param name  header name; must not be {@code null}
     * @param value header value
     * @return {@code this} for chaining
     */
    public RequestBuilder header(final String name, final String value) {
        headers.put(name, value);
        return this;
    }

    /**
     * Adds multiple request headers at once.
     *
     * @param headers map of header name → value; must not be {@code null}
     * @return {@code this} for chaining
     */
    public RequestBuilder headers(final Map<String, String> headers) {
        this.headers.putAll(headers);
        return this;
    }

    /**
     * Adds a single query parameter.
     *
     * @param name  parameter name; must not be {@code null}
     * @param value parameter value
     * @return {@code this} for chaining
     */
    public RequestBuilder queryParam(final String name, final Object value) {
        queryParams.put(name, value);
        return this;
    }

    /**
     * Adds multiple query parameters at once.
     *
     * @param params map of param name → value; must not be {@code null}
     * @return {@code this} for chaining
     */
    public RequestBuilder queryParams(final Map<String, Object> params) {
        this.queryParams.putAll(params);
        return this;
    }

    /**
     * Registers a path parameter for template substitution (e.g., {@code {id}}).
     *
     * @param name  the template token name without braces
     * @param value the value to substitute
     * @return {@code this} for chaining
     */
    public RequestBuilder pathParam(final String name, final Object value) {
        pathParams.put(name, value);
        return this;
    }

    /**
     * Registers multiple path parameters for template substitution.
     *
     * @param params map of token name → value; must not be {@code null}
     * @return {@code this} for chaining
     */
    public RequestBuilder pathParams(final Map<String, Object> params) {
        this.pathParams.putAll(params);
        return this;
    }

    /**
     * Adds a cookie to the request.
     *
     * @param name  cookie name; must not be {@code null}
     * @param value cookie value
     * @return {@code this} for chaining
     */
    public RequestBuilder cookie(final String name, final String value) {
        cookies.put(name, value);
        return this;
    }

    /**
     * Sets the request body.  REST Assured will serialize the object using
     * Jackson when {@code contentType} is {@link ContentType#JSON}.
     *
     * @param body the request body; a POJO, a {@code Map}, or a raw string
     * @return {@code this} for chaining
     */
    public RequestBuilder body(final Object body) {
        this.body = body;
        return this;
    }

    /**
     * Overrides the default {@link ContentType#JSON} content type.
     *
     * @param contentType the desired content type; must not be {@code null}
     * @return {@code this} for chaining
     */
    public RequestBuilder contentType(final ContentType contentType) {
        this.contentType = contentType;
        return this;
    }

    /**
     * Applies an {@link AuthenticationStrategy}.  Defaults to {@link NoAuth}
     * if never called.
     *
     * @param auth the strategy to apply; must not be {@code null}
     * @return {@code this} for chaining
     */
    public RequestBuilder auth(final AuthenticationStrategy auth) {
        this.auth = auth;
        return this;
    }

    // -----------------------------------------------------------------------
    //  Terminal operation
    // -----------------------------------------------------------------------

    /**
     * Constructs and returns a fully configured {@link RequestSpecification}.
     *
     * <p>Each call produces a new specification; the builder state is not reset
     * after {@code build()}, enabling partial re-use (e.g., setting the base
     * URI once and varying only path params).</p>
     *
     * @return a ready-to-use {@code RequestSpecification}
     * @throws IllegalStateException if {@code baseUri} has not been set
     */
    public RequestSpecification build() {
        if (baseUri == null || baseUri.isBlank()) {
            throw new IllegalStateException("baseUri must be set before calling build()");
        }

        RequestSpecification spec = RestAssured.given()
                .baseUri(baseUri)
                .basePath(basePath)
                .contentType(contentType);

        if (!headers.isEmpty())     { spec = spec.headers(headers); }
        if (!queryParams.isEmpty()) { spec = spec.queryParams(queryParams); }
        if (!pathParams.isEmpty())  { spec = spec.pathParams(pathParams); }
        if (!cookies.isEmpty())     { spec = spec.cookies(cookies); }
        if (body != null)           { spec = spec.body(body); }

        spec = auth.apply(spec);
        return spec;
    }
}
