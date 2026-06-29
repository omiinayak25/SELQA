package com.omiinqa.api.auth;

import io.restassured.specification.RequestSpecification;

/**
 * HTTP Basic authentication strategy.
 *
 * <p><b>Pattern:</b> Strategy (GoF) — encapsulates Basic auth so the caller
 * is shielded from the REST Assured {@code auth().preemptive().basic(...)}
 * API detail.  Preemptive mode is used because many servers close the
 * connection before issuing a 401 challenge, wasting a round-trip.</p>
 *
 * <p>Instances are immutable and thread-safe.</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * AuthenticationStrategy auth = new BasicAuth("admin", "password123");
 * RequestSpecification spec = new RequestBuilder()
 *     .baseUri(baseUrl)
 *     .auth(auth)
 *     .build();
 * }</pre>
 */
public final class BasicAuth implements AuthenticationStrategy {

    private final String username;
    private final String password;

    /**
     * Constructs a Basic auth strategy.
     *
     * @param username the HTTP Basic username; must not be {@code null}
     * @param password the HTTP Basic password; must not be {@code null}
     */
    public BasicAuth(final String username, final String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Applies HTTP Basic credentials to {@code spec} in preemptive mode.
     *
     * @param spec the request specification to decorate; must not be {@code null}
     * @return the same {@code spec} with Basic auth applied
     */
    @Override
    public RequestSpecification apply(final RequestSpecification spec) {
        return spec.auth().preemptive().basic(username, password);
    }
}
