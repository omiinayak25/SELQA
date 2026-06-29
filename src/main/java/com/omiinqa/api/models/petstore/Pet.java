package com.omiinqa.api.models.petstore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Domain model for the {@code Pet} resource in the Swagger Petstore v2 API.
 *
 * <p>Maps to the {@code Pet} schema at
 * <a href="https://petstore.swagger.io/v2/swagger.json">https://petstore.swagger.io/v2/swagger.json</a>.
 * This POJO is used for both request serialisation (POST/PUT bodies) and response
 * deserialisation (GET responses), achieving symmetry between the read and write
 * models and avoiding the need for separate DTO classes at the test layer.</p>
 *
 * <p><b>Nested types:</b></p>
 * <ul>
 *   <li>{@link Category} — optional grouping (e.g., "Dogs").</li>
 *   <li>{@link Tag} list — zero or more free-form labels.</li>
 *   <li>{@code photoUrls} — list of URL strings; the Petstore spec marks this as required.</li>
 * </ul>
 *
 * <p><b>Status enum values</b> recognised by the Petstore API:
 * {@code available}, {@code pending}, {@code sold}.</p>
 *
 * <p>Lombok annotations keep the class concise; {@link JsonIgnoreProperties}
 * with {@code ignoreUnknown = true} guards against additive server changes.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Pet {

    /**
     * Server-assigned pet identifier.
     * Supply a client-chosen ID when creating or updating; the server may
     * override it on the public demo instance.
     */
    private long id;

    /**
     * Optional category grouping for this pet (e.g., {@code {id:1, name:"Dogs"}}).
     * May be {@code null} when omitted in the request.
     */
    private Category category;

    /**
     * The pet's display name (e.g., {@code "Buddy"}).
     * Required by the Petstore spec.
     */
    private String name;

    /**
     * One or more photo URLs associated with the pet.
     * The Petstore spec marks this field as required, so callers must supply
     * at least one element (an empty list is accepted by the public server).
     */
    private List<String> photoUrls;

    /**
     * Zero or more descriptive tags attached to this pet.
     * May be {@code null} or empty.
     */
    private List<Tag> tags;

    /**
     * Availability status.  Valid values per Petstore spec:
     * {@code available}, {@code pending}, {@code sold}.
     */
    private String status;
}
