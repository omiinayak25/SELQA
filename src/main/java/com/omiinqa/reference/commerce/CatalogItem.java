package com.omiinqa.reference.commerce;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * An immutable product in the reference commerce catalogue.
 *
 * <p>Holds the stable product identity and unit price. Stock availability is
 * tracked separately in {@link StockItem} so that inventory updates do not
 * require replacing the catalogue entry. BDD scenarios assert on the
 * {@code id} and {@code name} for readability, and on {@code price} for
 * money-math assertions (subtotal, discount, total).</p>
 */
@Value
@Builder
public class CatalogItem {

    /** Stable, unique product identifier (e.g. {@code "SKU-001"}). */
    String id;

    /** Human-readable product name asserted in scenarios. */
    String name;

    /**
     * Unit price as an exact {@link BigDecimal} — never {@code float} or
     * {@code double} — so coupon / discount arithmetic is lossless.
     */
    BigDecimal price;
}
