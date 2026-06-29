package com.omiinqa.reference.commerce;

import lombok.Builder;
import lombok.Data;

/**
 * Mutable stock record for a catalogue product.
 *
 * <p>Decoupled from {@link CatalogItem} so that inventory updates (restocks,
 * reservations) do not require touching the immutable product definition.
 * The {@code available} count is decremented by {@code CartService} when an
 * item is added to a cart and incremented when it is removed, giving a real
 * stock-reservation model that scenarios assert against
 * ({@code CART_OUT_OF_STOCK}).</p>
 */
@Data
@Builder
public class StockItem {

    /** Must match {@link CatalogItem#getId()} for the same product. */
    private final String productId;

    /** Current units on hand; mutable to support reservation logic. */
    private int available;
}
