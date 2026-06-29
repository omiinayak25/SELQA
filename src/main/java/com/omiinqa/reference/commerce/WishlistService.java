package com.omiinqa.reference.commerce;

import com.omiinqa.reference.core.DomainException;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * In-memory wishlist service with real, asserted business rules.
 *
 * <p>Each BDD scenario receives a fresh {@code WishlistService} instance. It
 * shares the same {@link ProductRegistry} as {@code CartService} so the
 * {@link #moveToCart} operation is consistent: stock is checked against the
 * real in-memory availability before the item is transferred.</p>
 *
 * <p>Error codes (asserted by scenarios):</p>
 * <ul>
 *   <li>{@code WISH_UNKNOWN_PRODUCT} — productId not in the catalogue</li>
 *   <li>{@code WISH_DUPLICATE} — productId already in this wishlist</li>
 *   <li>{@code WISH_FULL} — adding would exceed {@link #MAX_SIZE}</li>
 *   <li>{@code CART_OUT_OF_STOCK} — moveToCart fails because stock is 0</li>
 * </ul>
 */
public class WishlistService {

    /** Maximum number of items allowed in a single wishlist. */
    public static final int MAX_SIZE = 100;

    private final ProductRegistry registry;

    /** Ordered set preserves insertion order for deterministic scenario assertions. */
    private final Set<String> items = new LinkedHashSet<>();

    /**
     * Create a wishlist backed by the given shared product registry.
     *
     * @param registry the product + stock registry for this scenario
     */
    public WishlistService(final ProductRegistry registry) {
        this.registry = registry;
    }

    // ------------------------------------------------------------------ //
    //  Mutation                                                            //
    // ------------------------------------------------------------------ //

    /**
     * Add a product to the wishlist.
     *
     * @throws DomainException {@code WISH_UNKNOWN_PRODUCT} if productId not in catalogue
     * @throws DomainException {@code WISH_DUPLICATE} if productId already in wishlist
     * @throws DomainException {@code WISH_FULL} if wishlist is at capacity
     */
    public void add(final String productId) {
        requireProductExists(productId);
        if (items.contains(productId)) {
            throw new DomainException("WISH_DUPLICATE",
                    "Product already in wishlist: " + productId);
        }
        if (items.size() >= MAX_SIZE) {
            throw new DomainException("WISH_FULL",
                    "Wishlist has reached the maximum of " + MAX_SIZE + " items");
        }
        items.add(productId);
    }

    /**
     * Remove a product from the wishlist.
     * No-op (no error) if the product is not present — idempotent removal.
     */
    public void remove(final String productId) {
        items.remove(productId);
    }

    /**
     * Move a wishlisted product into the supplied cart (qty = 1) and remove it
     * from the wishlist.
     *
     * <p>The stock check is delegated to {@link CartService#addItem} so the
     * same {@code CART_OUT_OF_STOCK} error code is raised — keeping the error
     * contract consistent regardless of the entry point.</p>
     *
     * @param productId product to move
     * @param cart      the destination cart (must share the same registry)
     * @throws DomainException {@code WISH_UNKNOWN_PRODUCT} if productId not in wishlist
     * @throws DomainException {@code CART_OUT_OF_STOCK} propagated from CartService
     */
    public void moveToCart(final String productId, final CartService cart) {
        if (!items.contains(productId)) {
            throw new DomainException("WISH_UNKNOWN_PRODUCT",
                    "Product not in wishlist, cannot move: " + productId);
        }
        // CartService raises CART_OUT_OF_STOCK if stock is 0 — propagates as-is.
        cart.addItem(productId, 1);
        items.remove(productId);
    }

    // ------------------------------------------------------------------ //
    //  Queries                                                             //
    // ------------------------------------------------------------------ //

    /** Returns {@code true} if the product is currently in the wishlist. */
    public boolean contains(final String productId) {
        return items.contains(productId);
    }

    /** Number of products in the wishlist. */
    public int size() {
        return items.size();
    }

    /** Unmodifiable view of the wishlisted product IDs in insertion order. */
    public Set<String> items() {
        return Collections.unmodifiableSet(items);
    }

    // ------------------------------------------------------------------ //
    //  Internal helpers                                                    //
    // ------------------------------------------------------------------ //

    private void requireProductExists(final String productId) {
        if (registry.findProduct(productId).isEmpty()) {
            throw new DomainException("WISH_UNKNOWN_PRODUCT",
                    "Unknown product: " + productId);
        }
    }
}
