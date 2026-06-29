package com.omiinqa.api.models.reqres;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response body returned by ReqRes after a successful create (POST /users) or
 * update (PUT/PATCH /users/{id}) operation.
 *
 * <p>The server echoes back the submitted fields and appends a generated
 * {@code id} and an ISO-8601 {@code createdAt} or {@code updatedAt}
 * timestamp.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserResponse {

    /** Server-assigned user identifier. */
    private String id;

    /** The user's display name (echoed from request). */
    private String name;

    /** The user's job title (echoed from request). */
    private String job;

    /** ISO-8601 creation timestamp set by the server on POST. */
    @JsonProperty("createdAt")
    private String createdAt;

    /** ISO-8601 update timestamp set by the server on PUT/PATCH. */
    @JsonProperty("updatedAt")
    private String updatedAt;
}
