package com.omiinqa.api.models.jsonplaceholder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the physical address of a JSONPlaceholder user.
 *
 * <p>This nested object is returned inside every {@code /users} response under
 * the {@code address} key.  It contains street-level fields plus a nested
 * {@link Geo} coordinate object.  All fields are mapped directly from the
 * JSON wire format — the API uses lowercase/camelCase keys that match Java
 * naming conventions, so no {@code @JsonProperty} renaming is required.</p>
 *
 * <p>The {@code @JsonIgnoreProperties(ignoreUnknown = true)} annotation ensures
 * forward compatibility if the API adds new fields in future.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Address {

    /** Street name and house number (e.g., {@code "Kulas Light Apt. 556"}). */
    private String street;

    /** Suite or apartment number (e.g., {@code "Apt. 556"}). */
    private String suite;

    /** City name. */
    private String city;

    /** Zip / postal code (may contain hyphens, e.g., {@code "92998-3874"}). */
    private String zipcode;

    /** Geographic coordinates for this address. */
    private Geo geo;
}
