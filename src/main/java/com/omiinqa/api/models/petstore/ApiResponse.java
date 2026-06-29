package com.omiinqa.api.models.petstore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic API response envelope returned by several Petstore endpoints.
 *
 * <p>The Swagger Petstore v2 uses this three-field structure as the response
 * body for operations that do not return a domain object, such as
 * {@code DELETE /pet/{petId}} and {@code POST /pet/{petId}/uploadFile}.
 * It is also used by some error responses to provide a machine-readable code
 * alongside the human-readable message.</p>
 *
 * <p>Fields:</p>
 * <ul>
 *   <li>{@code code} — HTTP-status-like integer code (e.g., {@code 200}).</li>
 *   <li>{@code type} — category string, typically {@code "unknown"} or {@code "error"}.</li>
 *   <li>{@code message} — free-text description (e.g., the deleted entity ID).</li>
 * </ul>
 *
 * <p>Lombok {@code @Builder} enables readable construction in test assertions;
 * {@link JsonIgnoreProperties} with {@code ignoreUnknown = true} guards against
 * future API additions.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiResponse {

    /**
     * Integer code embedded in the response body.
     * Typically mirrors the HTTP status code but may differ on the public instance.
     */
    private int code;

    /**
     * Response category label, e.g. {@code "unknown"}, {@code "error"},
     * or {@code "success"}.
     */
    private String type;

    /**
     * Human-readable message body, e.g. the string form of a deleted entity ID
     * or an error description.
     */
    private String message;
}
