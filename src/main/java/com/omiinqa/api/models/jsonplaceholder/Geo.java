package com.omiinqa.api.models.jsonplaceholder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the geographic coordinates (latitude/longitude) nested inside a
 * JSONPlaceholder {@link Address}.
 *
 * <p>Returned as part of the {@code /users} resource under
 * {@code address.geo}.  Both fields are delivered as strings by the API
 * (e.g., {@code "-37.3159"}) rather than numbers, so they are mapped as
 * {@link String} to avoid precision-loss or deserialization errors.</p>
 *
 * <p>Lombok {@code @Data} + {@code @Builder} + {@code @NoArgsConstructor} +
 * {@code @AllArgsConstructor} generate the full value-object contract
 * (getters, setters, equals, hashCode, toString) with zero boilerplate.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Geo {

    /** Latitude as a string, e.g. {@code "-37.3159"}. */
    private String lat;

    /** Longitude as a string, e.g. {@code "81.1496"}. */
    private String lng;
}
