package com.omiinqa.api.models.dummyjson;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for {@code POST /auth/login} on DummyJSON.
 *
 * <p>DummyJSON's auth endpoint accepts any of its built-in user credentials
 * and returns a JWT access token for testing protected endpoints.
 * A well-known test credential is {@code username: emilys / password: emilyspass}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DummyJsonLoginRequest {

    /** DummyJSON username (matches a user in the {@code /users} collection). */
    private String username;

    /** Plaintext password for the user. */
    private String password;

    /**
     * Token expiry expressed as a duration string (e.g., {@code "30m"}, {@code "1h"}).
     * Optional; DummyJSON uses a default if omitted.
     */
    private String expiresInMins;
}
