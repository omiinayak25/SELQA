package com.omiinqa.api.models.petstore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a free-form {@code Tag} that can be attached to a Petstore {@link Pet}.
 *
 * <p>Maps to the {@code Tag} schema at
 * <a href="https://petstore.swagger.io/v2/swagger.json">Swagger Petstore v2</a>.
 * Tags are open-ended labels (e.g., {@code "vaccinated"}, {@code "neutered"})
 * stored in a list on the pet.  A pet may carry zero or more tags.</p>
 *
 * <p>Uses Lombok to eliminate boilerplate; {@link JsonIgnoreProperties} with
 * {@code ignoreUnknown = true} makes deserialization forward-compatible when
 * the public server adds new fields.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tag {

    /** Server-assigned tag identifier. */
    private long id;

    /** Human-readable tag label, e.g. {@code "vaccinated"}. */
    private String name;
}
