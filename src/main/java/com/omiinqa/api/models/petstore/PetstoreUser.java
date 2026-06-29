package com.omiinqa.api.models.petstore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain model for the {@code User} resource in the Swagger Petstore v2 User API.
 *
 * <p>Maps to the {@code User} schema at
 * <a href="https://petstore.swagger.io/v2/swagger.json">Swagger Petstore v2</a>.
 * Users are managed via {@code POST /user}, {@code GET /user/{username}},
 * {@code PUT /user/{username}}, and {@code DELETE /user/{username}}.</p>
 *
 * <p>Named {@code PetstoreUser} (rather than {@code User}) to avoid collisions
 * with {@code java.lang.reflect.User} and framework user models in other packages
 * inside the same project.</p>
 *
 * <p>Field {@code userStatus} is an integer flag in the Petstore spec
 * ({@code 0 = inactive}, {@code 1 = active}).  The value is stored as-is to
 * match the wire format without an enum, keeping serialisation simple.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PetstoreUser {

    /** Server-assigned user identifier. */
    private long id;

    /** Unique login handle; used as the path parameter for read/update/delete. */
    private String username;

    /** User's given name. */
    private String firstName;

    /** User's family name. */
    private String lastName;

    /** Contact email address. */
    private String email;

    /** Login password (plain text per the public demo API). */
    private String password;

    /** Contact phone number. */
    private String phone;

    /**
     * Account status integer flag.
     * {@code 1} = active, {@code 0} = inactive as defined by the Petstore spec.
     */
    private int userStatus;
}
