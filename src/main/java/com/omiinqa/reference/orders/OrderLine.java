package com.omiinqa.reference.orders;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * A single line item within an {@link Order}, pairing a product with a quantity
 * and the unit price at time of purchase.
 *
 * <p>Using {@link BigDecimal} for money ensures lossless arithmetic across
 * subtotal, tax, discount and total calculations — never {@code float} or
 * {@code double}.</p>
 */
@Value
@Builder
public class OrderLine {

    /** Stable product identifier (e.g. {@code "SKU-001"}). */
    String productId;

    /** Human-readable product name for readability in BDD assertions. */
    String productName;

    /** Exact unit price at time of order creation. */
    BigDecimal unitPrice;

    /** Number of units ordered; must be &gt;= 1. */
    int quantity;

    /**
     * Line subtotal = {@code unitPrice * quantity}, computed without rounding
     * so intermediate values remain exact.
     */
    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
