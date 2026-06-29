package com.omiinqa.reference.catalog;

import com.omiinqa.reference.core.DomainException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Composable filter pipeline for the in-memory product catalog.
 *
 * <p>All criteria are applied with AND semantics. Null/absent criteria are
 * ignored (treated as "any"). Order of results matches catalog insertion order.</p>
 *
 * <p>Error codes (asserted by scenarios):
 * {@code FILTER_BAD_RANGE} — minPrice > maxPrice.</p>
 */
public class FilterService {

    private final CatalogService catalog;

    public FilterService(final CatalogService catalog) {
        this.catalog = catalog;
    }

    /**
     * Filter products by the supplied criteria (all AND-combined).
     *
     * @param category  if non-null, keep only products in this category (case-insensitive)
     * @param brand     if non-null, keep only products from this brand (case-insensitive)
     * @param minPrice  if non-null, lower bound (inclusive)
     * @param maxPrice  if non-null, upper bound (inclusive)
     * @param minRating if >= 0, keep only products rated at or above this value
     * @param inStock   if non-null, keep only products matching inStock flag
     * @return filtered list, never null
     * @throws DomainException {@code FILTER_BAD_RANGE} when minPrice > maxPrice
     */
    public List<Product> filter(
            final String category,
            final String brand,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final double minRating,
            final Boolean inStock) {

        if (minPrice != null && maxPrice != null
                && minPrice.compareTo(maxPrice) > 0) {
            throw new DomainException("FILTER_BAD_RANGE",
                    "minPrice " + minPrice + " must be <= maxPrice " + maxPrice);
        }

        final List<Product> results = new ArrayList<>();
        for (final Product p : catalog.all()) {
            if (category != null
                    && !category.equalsIgnoreCase(p.getCategory())) {
                continue;
            }
            if (brand != null
                    && !brand.equalsIgnoreCase(p.getBrand())) {
                continue;
            }
            if (minPrice != null && p.getPrice().compareTo(minPrice) < 0) {
                continue;
            }
            if (maxPrice != null && p.getPrice().compareTo(maxPrice) > 0) {
                continue;
            }
            if (minRating >= 0 && p.getRating() < minRating) {
                continue;
            }
            if (inStock != null && p.isInStock() != inStock) {
                continue;
            }
            results.add(p);
        }
        return results;
    }
}
