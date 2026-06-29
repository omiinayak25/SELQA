package com.omiinqa.api.models.petstore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the {@code Category} sub-resource embedded inside a Petstore {@link Pet}.
 *
 * <p>Maps to the {@code Category} schema at
 * <a href="https://petstore.swagger.io/v2/swagger.json">Swagger Petstore v2</a>.
 * Used when creating or updating pets to group them by kind (e.g., "Dogs",
 * "Cats").  The {@code id} is server-assigned on the public demo instance and
 * is ignored when it is absent in the response.</p>
 *
 * <p>Lombok {@code @Data} generates getters, setters, {@code equals},
 * {@code hashCode}, and {@code toString}, avoiding boilerplate that would
 * obscure the domain intent.  {@code @Builder} enables readable construction
 * in test fixtures.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Category {

    /**
     * Server-assigned category identifier.
     * May be {@code 0} or absent on the public demo instance.
     */
    private long id;

    /** Human-readable category label, e.g. {@code "Dogs"}. */
    private String name;
}
