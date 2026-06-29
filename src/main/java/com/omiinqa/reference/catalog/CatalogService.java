package com.omiinqa.reference.catalog;

import com.omiinqa.reference.core.DomainException;
import com.omiinqa.reference.core.Validations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory product catalog with real, asserted business rules.
 * State is per-instance so each BDD scenario gets an isolated world.
 *
 * <p>Error codes (asserted by scenarios):
 * {@code CATALOG_NOT_FOUND} — no product with the given id;
 * {@code CATALOG_BLANK_NAME} — product name is blank;
 * {@code CATALOG_BAD_PRICE} — price is null or negative.</p>
 */
public class CatalogService {

    private final ConcurrentHashMap<Long, Product> store = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong(100);

    /**
     * Add a product to the catalog. Validates name (non-blank) and price
     * (non-null, non-negative).
     *
     * @return the stored product with its assigned id
     */
    public Product addProduct(final Product.ProductBuilder builder) {
        final Product prototype = builder.build();
        Validations.requireNotBlank(prototype.getName(), "name", "CATALOG_BLANK_NAME");
        if (prototype.getPrice() == null || prototype.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainException("CATALOG_BAD_PRICE",
                    "Product price must be non-null and non-negative");
        }
        final long id = ids.incrementAndGet();
        final Product product = Product.builder()
                .id(id)
                .name(prototype.getName())
                .category(prototype.getCategory())
                .brand(prototype.getBrand())
                .price(prototype.getPrice())
                .rating(prototype.getRating())
                .inStock(prototype.isInStock())
                .tags(prototype.getTags())
                .build();
        store.put(id, product);
        return product;
    }

    /**
     * Find a product by its id.
     *
     * @throws DomainException {@code CATALOG_NOT_FOUND} when no product exists
     */
    public Product findById(final long id) {
        final Product product = store.get(id);
        if (product == null) {
            throw new DomainException("CATALOG_NOT_FOUND",
                    "No product with id: " + id);
        }
        return product;
    }

    /** All products currently in the catalog. */
    public List<Product> all() {
        return Collections.unmodifiableList(new ArrayList<>(store.values()));
    }

    /** Number of products in the catalog. */
    public int size() {
        return store.size();
    }

    /**
     * Seed the catalog with a fixed set of products for deterministic BDD scenarios.
     * Idempotent: calling multiple times re-seeds (clears first).
     */
    public void seed(final List<Product.ProductBuilder> builders) {
        store.clear();
        builders.forEach(this::addProduct);
    }
}
