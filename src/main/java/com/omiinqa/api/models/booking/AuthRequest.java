package com.omiinqa.api.models.booking;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for {@code POST /auth} on Restful-Booker.
 *
 * <p>Restful-Booker's auth endpoint accepts a username/password pair and
 * returns a short-lived token that must be supplied as a {@code Cookie:
 * token=<value>} header on mutating operations (PUT, PATCH, DELETE).
 * This class models the request side of that exchange.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthRequest {

    /** Restful-Booker admin username (default: {@code admin}). */
    private String username;

    /** Restful-Booker admin password (default: {@code password123}). */
    private String password;
}
