package com.omiinqa.data.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Domain model representing a product in the SUT's catalogue.
 *
 * <p><strong>Pattern:</strong> Value Object — used in shopping-cart, search,
 * and checkout test scenarios. {@link BigDecimal} is used for price to avoid
 * floating-point precision errors in monetary comparisons.</p>
 *
 * <p>Deserialises cleanly from JSON via Jackson; the {@code id} field accepts
 * both integer-sourced (from SauceDemo-style numeric IDs) and UUID strings.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    /** Unique product identifier. */
    private String id;

    /** Display name shown in the catalogue and cart. */
    private String name;

    /** Retail price; use {@link BigDecimal} for precision. */
    private BigDecimal price;

    /** Category tag for filtering / sorting tests. */
    private String category;

    /** Short product description. */
    private String description;

    /** URL of the product image (for visual regression tests). */
    private String imageUrl;
}
