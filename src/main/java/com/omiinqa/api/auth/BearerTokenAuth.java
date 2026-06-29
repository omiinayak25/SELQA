package com.omiinqa.api.auth;

import io.restassured.specification.RequestSpecification;

/**
 * OAuth 2.0 / JWT Bearer-token authentication strategy.
 *
 * <p><b>Pattern:</b> Strategy (GoF) — encapsulates the {@code Authorization: Bearer <token>}
 * header addition.  Using REST Assured's built-in
 * {@code auth().oauth2(token)} ensures the header is set correctly and
 * survives redirect chains.</p>
 *
 * <p>Instances are immutable and thread-safe.  Create one per access token
 * (do not reuse across users) but cache it for the lifetime of a token.</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * String token = bookingService.createToken(user, pass);
 * AuthenticationStrategy auth = new BearerTokenAuth(token);
 * }</pre>
 */
public final class BearerTokenAuth implements AuthenticationStrategy {

    private final String token;

    /**
     * Constructs a Bearer token auth strategy.
     *
     * @param token the raw access token (without the "Bearer " prefix);
     *              must not be {@code null} or blank
     */
    public BearerTokenAuth(final String token) {
        this.token = token;
    }

    /**
     * Adds an {@code Authorization: Bearer &lt;token&gt;} header to {@code spec}.
     *
     * @param spec the request specification to decorate; must not be {@code null}
     * @return the same {@code spec} with Bearer auth applied
     */
    @Override
    public RequestSpecification apply(final RequestSpecification spec) {
        return spec.auth().oauth2(token);
    }
}
