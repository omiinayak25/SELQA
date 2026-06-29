package com.omiinqa.reference.catalog;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * A product in the reference catalog domain. Immutable by convention once added
 * to the catalog; updates are out of scope for this reference slice.
 *
 * <p>Fields: {@code id} (auto-assigned), {@code name}, {@code category},
 * {@code brand}, {@code price} (non-negative {@link BigDecimal}),
 * {@code rating} (0.0–5.0), {@code inStock}, {@code tags}.</p>
 */
@Data
@Builder
public class Product {

    private long id;
    private String name;
    private String category;
    private String brand;
    private BigDecimal price;
    private double rating;
    private boolean inStock;

    @Builder.Default
    private List<String> tags = List.of();
}
