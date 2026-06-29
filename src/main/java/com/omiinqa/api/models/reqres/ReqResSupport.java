package com.omiinqa.api.models.reqres;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Nested {@code support} object returned in every ReqRes single-resource response.
 *
 * <p>ReqRes appends a support URL and text to each response as an advertising
 * vehicle.  This class captures those fields so full deserialization succeeds
 * even when the support block is present.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReqResSupport {

    /** URL to the ReqRes support/sponsorship page. */
    private String url;

    /** Human-readable support message. */
    private String text;
}
