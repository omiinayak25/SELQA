package com.omiinqa.api.models.reqres;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for ReqRes create/update user operations (POST /users, PUT /users/{id}).
 *
 * <p>ReqRes is a hosted fake API; it accepts any JSON body and echoes back
 * the fields.  This POJO captures the two most common fields used in create
 * and update scenarios.  {@link JsonIgnoreProperties} prevents deserialization
 * failures if the server returns additional fields in a future API version.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserRequest {

    /** The user's display name. */
    private String name;

    /** The user's job title or role. */
    private String job;
}
