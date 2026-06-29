package com.omiinqa.api.models.reqres;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated list response envelope returned by ReqRes {@code GET /users?page=N}.
 *
 * <p>ReqRes wraps paginated user lists in an envelope that carries both the
 * page metadata and the data array.  Binding the full envelope allows tests
 * to assert on pagination fields ({@code page}, {@code total}, {@code totalPages})
 * without additional JSON path gymnastics.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReqResListResponse {

    /** Current page number (1-based). */
    private int page;

    /** Number of records per page. */
    @JsonProperty("per_page")
    private int perPage;

    /** Total number of records across all pages. */
    private int total;

    /** Total number of pages. */
    @JsonProperty("total_pages")
    private int totalPages;

    /** The user records on the current page. */
    private List<ReqResUserData> data;

    /** Support / sponsorship block appended to every ReqRes list response. */
    private ReqResSupport support;
}
