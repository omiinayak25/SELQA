package com.omiinqa.reference.commerce;

import lombok.Data;

import java.math.BigDecimal;

/**
 * A single line in the shopping cart — one distinct product with its current
 * quantity and the unit price locked at the time of addition.
 *
 * <p>Locking the price prevents unexpected subtotal drift when the catalogue
 * is updated mid-session, which is the standard retail behaviour asserted by
 * money-math scenarios.</p>
 */
@Data
public class CartLine {

    private final CatalogItem product;
    private int quantity;

    /**
     * Line total: {@code unitPrice × quantity} with exact
     * {@link BigDecimal} arithmetic.
     */
    public BigDecimal lineTotal() {
        return product.getPrice().multiply(BigDecimal.valueOf(quantity));
    }
}
