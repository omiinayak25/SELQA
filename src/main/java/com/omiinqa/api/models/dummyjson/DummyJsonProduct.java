package com.omiinqa.api.models.dummyjson;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DummyJSON product record returned by {@code GET /products/{id}} and
 * in the {@code products} array of {@code GET /products}.
 *
 * <p>Only the fields exercised by the test suite are mapped; the
 * {@link JsonIgnoreProperties} annotation ensures that future DummyJSON API
 * additions do not break deserialization.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DummyJsonProduct {

    /** Server-assigned product identifier. */
    private int id;

    /** Product display title. */
    private String title;

    /** Short product description. */
    private String description;

    /** Price in USD. */
    private double price;

    /** Discount percentage (0–100). */
    private double discountPercentage;

    /** Aggregate rating (0.0–5.0). */
    private double rating;

    /** Available stock count. */
    private int stock;

    /** Product category slug. */
    private String category;

    /** Primary thumbnail URL. */
    private String thumbnail;

    /** All product image URLs. */
    private List<String> images;

    /** Product tags. */
    private List<String> tags;

    /** Brand name. */
    private String brand;

    /** Stock-keeping unit identifier. */
    private String sku;
}
