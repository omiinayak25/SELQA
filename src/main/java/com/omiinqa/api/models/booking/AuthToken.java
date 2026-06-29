package com.omiinqa.api.models.booking;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response from {@code POST /auth} on Restful-Booker.
 *
 * <p>The token field is required for all write operations on the Restful-Booker
 * API.  It must be delivered as a {@code Cookie} header ({@code token=<value>})
 * rather than as a Bearer token — an unusual pattern that the framework
 * accommodates via {@link com.omiinqa.api.builder.RequestBuilder#cookie(String, String)}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthToken {

    /** The session token to pass as {@code Cookie: token=<value>}. */
    private String token;
}
