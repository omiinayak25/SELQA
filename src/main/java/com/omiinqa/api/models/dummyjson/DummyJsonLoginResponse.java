package com.omiinqa.api.models.dummyjson;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response body from {@code POST /auth/login} on DummyJSON.
 *
 * <p>DummyJSON returns the authenticated user's profile alongside a JWT
 * access token and a refresh token.  The {@code accessToken} field is
 * passed as a Bearer token for subsequent authenticated calls such as
 * {@code GET /auth/me}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DummyJsonLoginResponse {

    /** JWT access token — use as {@code Authorization: Bearer <accessToken>}. */
    private String accessToken;

    /** JWT refresh token for extending the session. */
    private String refreshToken;

    /** Numeric user identifier. */
    private int id;

    /** Authenticated user's username. */
    private String username;

    /** Authenticated user's email address. */
    private String email;

    /** Authenticated user's first name. */
    private String firstName;

    /** Authenticated user's last name. */
    private String lastName;

    /** URL to the user's profile picture. */
    private String image;
}
