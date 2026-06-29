package com.omiinqa.api.models.petstore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain model for the {@code Order} resource in the Swagger Petstore v2 Store API.
 *
 * <p>Maps to the {@code Order} schema at
 * <a href="https://petstore.swagger.io/v2/swagger.json">Swagger Petstore v2</a>.
 * Orders are placed via {@code POST /store/order} and represent a purchase
 * intent for a specific pet.  The lifecycle runs: {@code placed → approved → delivered}.</p>
 *
 * <p>Key fields:</p>
 * <ul>
 *   <li>{@code id} — server-assigned order identifier.</li>
 *   <li>{@code petId} — foreign key to the {@link Pet} being purchased.</li>
 *   <li>{@code quantity} — number of units ordered.</li>
 *   <li>{@code shipDate} — ISO-8601 date-time string when the order ships.</li>
 *   <li>{@code status} — one of {@code placed}, {@code approved}, {@code delivered}.</li>
 *   <li>{@code complete} — whether the order is finalised.</li>
 * </ul>
 *
 * <p>Lombok {@code @Data} + {@code @Builder} reduce boilerplate; {@link JsonIgnoreProperties}
 * guards against additive server fields in future API versions.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Order {

    /** Server-assigned order identifier. */
    private long id;

    /** Identifier of the pet being ordered; must reference a valid {@link Pet#getId()}. */
    private long petId;

    /** Number of units to order; must be {@code >= 1}. */
    private int quantity;

    /**
     * Requested ship date in ISO-8601 format (e.g., {@code "2025-06-29T10:00:00.000Z"}).
     * The Petstore server stores this verbatim.
     */
    private String shipDate;

    /**
     * Order fulfilment status.  Valid values: {@code placed}, {@code approved},
     * {@code delivered}.
     */
    private String status;

    /** {@code true} when the order has been fully completed. */
    private boolean complete;
}
