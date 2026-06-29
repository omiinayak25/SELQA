package com.omiinqa.api.models.dummyjson;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated product list returned by {@code GET /products} and
 * {@code GET /products/search?q=...} on DummyJSON.
 *
 * <p>Mirrors DummyJSON's envelope: {@code products}, {@code total},
 * {@code skip}, {@code limit}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DummyJsonProductList {

    /** The product records on the current page. */
    private List<DummyJsonProduct> products;

    /** Total number of products matching the query across all pages. */
    private int total;

    /** Number of records skipped (offset). */
    private int skip;

    /** Maximum records returned per page. */
    private int limit;
}
