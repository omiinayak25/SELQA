package com.omiinqa.api.auth;

import io.restassured.specification.RequestSpecification;

/**
 * No-operation authentication strategy for public, unauthenticated endpoints.
 *
 * <p><b>Pattern:</b> Null Object — substitutes a do-nothing instance wherever
 * an {@link AuthenticationStrategy} reference is required, eliminating
 * {@code null} checks and making the absence of auth explicit and
 * self-documenting at the call site.</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * RequestSpecification spec = new RequestBuilder()
 *     .baseUri(FrameworkConfig.get().apiUrl("reqres"))
 *     .basePath(ApiEndpoints.REQRES_USERS)
 *     .auth(new NoAuth())
 *     .build();
 * }</pre>
 */
public final class NoAuth implements AuthenticationStrategy {

    /**
     * Returns the specification unchanged; no credentials are added.
     *
     * @param spec the request specification; must not be {@code null}
     * @return the same {@code spec}, unmodified
     */
    @Override
    public RequestSpecification apply(final RequestSpecification spec) {
        return spec;
    }
}
