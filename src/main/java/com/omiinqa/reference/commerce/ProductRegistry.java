package com.omiinqa.reference.commerce;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Shared in-memory product catalogue and stock registry for the reference
 * commerce domain.
 *
 * <p>Seeded at construction with a fixed set of products so that BDD scenarios
 * use stable, predictable product IDs and prices without any external catalogue
 * service. Both {@link CartService} and {@link WishlistService} accept a
 * {@code ProductRegistry} at construction time; passing the <em>same</em>
 * instance in a scenario ensures stock changes made by one service are visible
 * to the other.</p>
 *
 * <p>The product registry is designed for BDD scenario use only — it is not
 * thread-safe, but each scenario runs in a single thread and gets a fresh
 * instance via {@code DomainWorld.service(...)}.</p>
 */
public class ProductRegistry {

    private final Map<String, CatalogItem> catalogue = new HashMap<>();
    private final Map<String, StockItem> stock = new HashMap<>();

    /**
     * Create a registry pre-seeded with the standard reference-domain product
     * set used by commerce BDD scenarios.
     *
     * <pre>
     * ID          Name                  Price     Stock
     * ---------   -------------------   -------   -----
     * SKU-BOOK    Clean Code Book       19.99     10
     * SKU-MUG     Developer Mug          8.50     25
     * SKU-LAPTOP  Laptop Stand          45.00      5
     * SKU-HEADSET Noise-Cancel Headset 149.99      3
     * SKU-CHAIR   Ergonomic Chair      299.00      2
     * SKU-RARE    Rare Collectible       9.99      1
     * SKU-ZERO    Out-of-Stock Widget   14.99      0
     * </pre>
     */
    public ProductRegistry() {
        seed("SKU-BOOK",    "Clean Code Book",          "19.99",  10);
        seed("SKU-MUG",     "Developer Mug",             "8.50",  25);
        seed("SKU-LAPTOP",  "Laptop Stand",             "45.00",   5);
        seed("SKU-HEADSET", "Noise-Cancel Headset",    "149.99",   3);
        seed("SKU-CHAIR",   "Ergonomic Chair",         "299.00",   2);
        seed("SKU-RARE",    "Rare Collectible",          "9.99",   1);
        seed("SKU-ZERO",    "Out-of-Stock Widget",      "14.99",   0);
    }

    // ------------------------------------------------------------------ //
    //  Queries                                                             //
    // ------------------------------------------------------------------ //

    /**
     * Look up a product by ID.
     *
     * @param productId the product SKU
     * @return the catalogue entry, or empty if not found
     */
    public Optional<CatalogItem> findProduct(final String productId) {
        return Optional.ofNullable(catalogue.get(productId));
    }

    /**
     * Look up the stock record for a product.
     *
     * @param productId the product SKU
     * @return the stock item (never null for a registered product)
     * @throws IllegalArgumentException if the product is not registered
     */
    public StockItem stock(final String productId) {
        final StockItem s = stock.get(productId);
        if (s == null) {
            throw new IllegalArgumentException("No stock record for: " + productId);
        }
        return s;
    }

    /**
     * Add a custom product to the registry (useful for boundary-testing scenarios
     * that need very precise stock levels without modifying the default seed).
     *
     * @param id       unique product SKU
     * @param name     display name
     * @param price    unit price string (parsed as exact BigDecimal)
     * @param available initial stock level
     */
    public void addProduct(final String id, final String name,
                           final String price, final int available) {
        seed(id, name, price, available);
    }

    // ------------------------------------------------------------------ //
    //  Internal                                                            //
    // ------------------------------------------------------------------ //

    private void seed(final String id, final String name,
                      final String priceStr, final int available) {
        final CatalogItem item = CatalogItem.builder()
                .id(id)
                .name(name)
                .price(new BigDecimal(priceStr))
                .build();
        catalogue.put(id, item);
        stock.put(id, StockItem.builder()
                .productId(id)
                .available(available)
                .build());
    }
}
