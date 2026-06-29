package com.omiinqa.api.models.reqres;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Nested {@code data} object within a ReqRes single-user response.
 *
 * <p>ReqRes wraps individual user records inside a {@code data} envelope
 * alongside a {@code support} object.  This class models the inner record.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReqResUserData {

    /** Numeric user identifier assigned by ReqRes. */
    private int id;

    /** User's email address. */
    private String email;

    /** User's first name. */
    @JsonProperty("first_name")
    private String firstName;

    /** User's last name. */
    @JsonProperty("last_name")
    private String lastName;

    /** URL to the user's avatar image. */
    private String avatar;
}
