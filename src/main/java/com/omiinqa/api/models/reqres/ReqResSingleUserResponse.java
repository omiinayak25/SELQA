package com.omiinqa.api.models.reqres;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Top-level envelope returned by ReqRes {@code GET /users/{id}}.
 *
 * <p>ReqRes wraps user records in a two-key envelope:</p>
 * <pre>
 * {
 *   "data": { ... },
 *   "support": { ... }
 * }
 * </pre>
 * <p>This class models that envelope so that {@code response.as(ReqResSingleUserResponse.class)}
 * deserializes the entire payload in one call, keeping test assertions on
 * typed fields rather than raw JSON paths.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReqResSingleUserResponse {

    /** The user record. */
    private ReqResUserData data;

    /** Support / sponsorship block always present in ReqRes responses. */
    private ReqResSupport support;
}
