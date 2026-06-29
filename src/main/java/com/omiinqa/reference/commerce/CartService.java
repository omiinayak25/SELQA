package com.omiinqa.reference.commerce;

import com.omiinqa.reference.core.DomainException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory shopping-cart service with real, asserted business rules.
 *
 * <p>Each BDD scenario receives a fresh {@code CartService} instance (via
 * {@code DomainWorld.service(key, CartService::new)}) so state never leaks
 * between scenarios. The service owns an embedded product + stock registry
 * seeded at construction — no external catalogue dependency.</p>
 *
 * <p>Error codes (asserted by scenarios):</p>
 * <ul>
 *   <li>{@code CART_BAD_QTY} — quantity is &lt;= 0</li>
 *   <li>{@code CART_UNKNOWN_PRODUCT} — productId not in registry</li>
 *   <li>{@code CART_OUT_OF_STOCK} — requested qty exceeds available stock</li>
 *   <li>{@code CART_FULL} — adding a new distinct product line would exceed
 *       {@link #MAX_DISTINCT_LINES}</li>
 *   <li>{@code CART_BAD_COUPON} — coupon code is unknown</li>
 * </ul>
 */
public class CartService {

    /** Maximum number of distinct product lines allowed in a cart. */
    public static final int MAX_DISTINCT_LINES = 50;

    /** Backing registry shared by all CartService instances in a scenario. */
    private final ProductRegistry registry;

    /** Ordered map of productId -> CartLine. Insertion order preserved for deterministic assertions. */
    private final Map<String, CartLine> lines = new LinkedHashMap<>();

    /** Active coupon code; {@code null} when none applied. */
    private String couponCode;

    /** Discount percentage in [0, 100]; 0 when no coupon is applied. */
    private BigDecimal discountPct = BigDecimal.ZERO;

    /**
     * Create a cart that draws from the supplied shared product registry.
     * Pass the same {@code ProductRegistry} instance to both {@code CartService}
     * and {@code WishlistService} in one scenario so stock changes are visible
     * across both services.
     *
     * @param registry the product + stock registry for this scenario
     */
    public CartService(final ProductRegistry registry) {
        this.registry = registry;
    }

    // ------------------------------------------------------------------ //
    //  Mutation                                                            //
    // ------------------------------------------------------------------ //

    /**
     * Add {@code qty} units of {@code productId} to the cart.
     *
     * <ul>
     *   <li>If the product is already in the cart the quantities are merged.</li>
     *   <li>Stock is checked against the <em>additional</em> requested qty; the
     *       check uses total-in-cart after merge so double-adding respects the
     *       real stock level.</li>
     * </ul>
     *
     * @throws DomainException {@code CART_BAD_QTY} if qty &lt;= 0
     * @throws DomainException {@code CART_UNKNOWN_PRODUCT} if productId not found
     * @throws DomainException {@code CART_OUT_OF_STOCK} if qty exceeds available stock
     * @throws DomainException {@code CART_FULL} if max distinct lines would be exceeded
     */
    public void addItem(final String productId, final int qty) {
        if (qty <= 0) {
            throw new DomainException("CART_BAD_QTY",
                    "Quantity must be > 0 but was: " + qty);
        }
        final CatalogItem product = requireProduct(productId);
        final StockItem stock = registry.stock(productId);

        if (!lines.containsKey(productId) && lines.size() >= MAX_DISTINCT_LINES) {
            throw new DomainException("CART_FULL",
                    "Cart already contains " + MAX_DISTINCT_LINES + " distinct product lines");
        }

        final int alreadyInCart = lines.containsKey(productId)
                ? lines.get(productId).getQuantity() : 0;
        final int totalAfterAdd = alreadyInCart + qty;

        if (totalAfterAdd > stock.getAvailable()) {
            throw new DomainException("CART_OUT_OF_STOCK",
                    "Only " + stock.getAvailable() + " units available for " + productId
                            + "; cart already holds " + alreadyInCart
                            + ", requested additional " + qty);
        }

        if (lines.containsKey(productId)) {
            lines.get(productId).setQuantity(totalAfterAdd);
        } else {
            final CartLine line = new CartLine(product);
            line.setQuantity(qty);
            lines.put(productId, line);
        }
    }

    /**
     * Update the quantity of an existing cart line to {@code newQty}.
     *
     * @throws DomainException {@code CART_BAD_QTY} if newQty &lt;= 0
     * @throws DomainException {@code CART_UNKNOWN_PRODUCT} if productId not in cart
     * @throws DomainException {@code CART_OUT_OF_STOCK} if newQty exceeds stock
     */
    public void updateQty(final String productId, final int newQty) {
        if (newQty <= 0) {
            throw new DomainException("CART_BAD_QTY",
                    "Quantity must be > 0 but was: " + newQty);
        }
        if (!lines.containsKey(productId)) {
            throw new DomainException("CART_UNKNOWN_PRODUCT",
                    "Product not in cart: " + productId);
        }
        final StockItem stock = registry.stock(productId);
        if (newQty > stock.getAvailable()) {
            throw new DomainException("CART_OUT_OF_STOCK",
                    "Only " + stock.getAvailable() + " units available for " + productId);
        }
        lines.get(productId).setQuantity(newQty);
    }

    /**
     * Remove a product line from the cart entirely.
     * No-op (no error) if the product is not in the cart — idempotent removal.
     */
    public void removeItem(final String productId) {
        lines.remove(productId);
    }

    /** Remove all lines and reset any applied coupon. */
    public void clear() {
        lines.clear();
        couponCode = null;
        discountPct = BigDecimal.ZERO;
    }

    /**
     * Apply a percentage-off coupon code.
     *
     * <p>Built-in coupons (case-insensitive):</p>
     * <ul>
     *   <li>{@code SAVE10} — 10 % off</li>
     *   <li>{@code SAVE20} — 20 % off</li>
     *   <li>{@code HALFOFF} — 50 % off</li>
     *   <li>{@code FREESHIP} — 0 % (shipping discount, not cart discount)</li>
     * </ul>
     *
     * @throws DomainException {@code CART_BAD_COUPON} if the code is unknown
     */
    public void applyCoupon(final String code) {
        if (code == null || code.isBlank()) {
            throw new DomainException("CART_BAD_COUPON", "Coupon code must not be blank");
        }
        final BigDecimal pct = resolveCoupon(code.toUpperCase());
        this.couponCode = code.toUpperCase();
        this.discountPct = pct;
    }

    // ------------------------------------------------------------------ //
    //  Queries                                                             //
    // ------------------------------------------------------------------ //

    /** Total units across all lines. */
    public int itemCount() {
        return lines.values().stream().mapToInt(CartLine::getQuantity).sum();
    }

    /** Number of distinct product lines in the cart. */
    public int distinctLines() {
        return lines.size();
    }

    /**
     * Sum of {@code unitPrice × qty} for every line, before discount.
     * Returned as a {@link BigDecimal} with 2 decimal places (HALF_UP).
     */
    public BigDecimal subtotal() {
        return lines.values().stream()
                .map(CartLine::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Subtotal after applying the active coupon discount.
     * If no coupon is applied this equals {@link #subtotal()}.
     * Result has 2 decimal places (HALF_UP).
     */
    public BigDecimal total() {
        final BigDecimal sub = subtotal();
        if (discountPct.compareTo(BigDecimal.ZERO) == 0) {
            return sub;
        }
        final BigDecimal discount = sub
                .multiply(discountPct)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        return sub.subtract(discount).setScale(2, RoundingMode.HALF_UP);
    }

    /** Returns the active coupon code, or an empty Optional if none. */
    public Optional<String> activeCoupon() {
        return Optional.ofNullable(couponCode);
    }

    /** Unmodifiable view of the current cart lines keyed by productId. */
    public Map<String, CartLine> lines() {
        return Collections.unmodifiableMap(lines);
    }

    // ------------------------------------------------------------------ //
    //  Internal helpers                                                    //
    // ------------------------------------------------------------------ //

    private CatalogItem requireProduct(final String productId) {
        return registry.findProduct(productId)
                .orElseThrow(() -> new DomainException("CART_UNKNOWN_PRODUCT",
                        "Unknown product: " + productId));
    }

    private static BigDecimal resolveCoupon(final String code) {
        return switch (code) {
            case "SAVE10"   -> BigDecimal.valueOf(10);
            case "SAVE20"   -> BigDecimal.valueOf(20);
            case "HALFOFF"  -> BigDecimal.valueOf(50);
            case "FREESHIP" -> BigDecimal.ZERO;
            default -> throw new DomainException("CART_BAD_COUPON",
                    "Unknown coupon code: " + code);
        };
    }
}
