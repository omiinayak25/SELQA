package com.omiinqa.api.auth;

import io.restassured.specification.RequestSpecification;

/**
 * Strategy interface for REST Assured request authentication.
 *
 * <p><b>Pattern:</b> Strategy (GoF) — each implementation encapsulates one
 * authentication algorithm so that {@code RequestBuilder} and
 * {@code ApiClient} can vary authentication independently of the request
 * mechanics.  Adding OAuth 2.0 or Digest auth requires only a new
 * implementation class; no existing code changes.</p>
 *
 * <p>Implementations must be stateless and thread-safe so that a single
 * instance can be shared across parallel test threads.</p>
 */
public interface AuthenticationStrategy {

    /**
     * Applies authentication credentials to the supplied {@link RequestSpecification}.
     *
     * <p>Mutates {@code spec} in-place and returns it to enable fluent chaining
     * in {@code RequestBuilder.auth(AuthenticationStrategy)}.</p>
     *
     * @param spec the REST Assured request specification to decorate; never {@code null}
     * @return the same {@code spec} with auth applied, never {@code null}
     */
    RequestSpecification apply(RequestSpecification spec);
}
