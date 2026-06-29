package com.omiinqa.api.auth;

import io.restassured.specification.RequestSpecification;

/**
 * API-key authentication strategy supporting header- and query-parameter delivery.
 *
 * <p><b>Pattern:</b> Strategy (GoF) — the location ({@code HEADER} vs
 * {@code QUERY_PARAM}) is a constructor argument, keeping the strategy
 * open for extension (new locations) without modifying existing callers.</p>
 *
 * <p>Instances are immutable and thread-safe.</p>
 *
 * <p>Usage examples:</p>
 * <pre>{@code
 * // Header-based: X-API-Key: secret123
 * new ApiKeyAuth("X-API-Key", "secret123", ApiKeyAuth.Location.HEADER);
 *
 * // Query-param: ?api_key=secret123
 * new ApiKeyAuth("api_key", "secret123", ApiKeyAuth.Location.QUERY_PARAM);
 * }</pre>
 */
public final class ApiKeyAuth implements AuthenticationStrategy {

    /**
     * Delivery mechanism for the API key.
     */
    public enum Location {
        /** Sent as an HTTP request header. */
        HEADER,
        /** Appended to the URL as a query parameter. */
        QUERY_PARAM
    }

    private final String keyName;
    private final String keyValue;
    private final Location location;

    /**
     * Constructs an API-key auth strategy.
     *
     * @param keyName  the header name or query-parameter name; must not be {@code null}
     * @param keyValue the secret key value; must not be {@code null}
     * @param location where to attach the key ({@link Location#HEADER} or
     *                 {@link Location#QUERY_PARAM})
     */
    public ApiKeyAuth(final String keyName, final String keyValue, final Location location) {
        this.keyName  = keyName;
        this.keyValue = keyValue;
        this.location = location;
    }

    /**
     * Attaches the API key to {@code spec} at the configured location.
     *
     * @param spec the request specification to decorate; must not be {@code null}
     * @return the same {@code spec} with API-key auth applied
     */
    @Override
    public RequestSpecification apply(final RequestSpecification spec) {
        if (location == Location.QUERY_PARAM) {
            return spec.queryParam(keyName, keyValue);
        }
        return spec.header(keyName, keyValue);
    }
}
