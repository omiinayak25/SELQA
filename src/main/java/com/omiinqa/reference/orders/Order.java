package com.omiinqa.reference.orders;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * An immutable order record produced by {@link CheckoutService} and managed by
 * {@link OrderService}.
 *
 * <p>All monetary fields are {@link BigDecimal} so arithmetic is lossless.
 * The lifecycle is modelled by {@link Status} and legal transitions are enforced
 * by {@code OrderService} — attempts to violate them throw a
 * {@code DomainException} with a stable, assertable code.</p>
 *
 * <h3>Error codes owned by this domain:</h3>
 * <ul>
 *   <li>{@code CHK_EMPTY_CART} — checkout attempted with no line items</li>
 *   <li>{@code CHK_BAD_ADDRESS} — required address field is blank</li>
 *   <li>{@code CHK_BAD_PAYMENT} — payment method token is invalid / null</li>
 *   <li>{@code ORDER_NOT_FOUND} — order ID does not exist</li>
 *   <li>{@code ORDER_BAD_TRANSITION} — status move violates the lifecycle</li>
 *   <li>{@code ORDER_CANNOT_CANCEL} — cancel requested after shipment</li>
 *   <li>{@code ORDER_CANNOT_REFUND} — refund requested in an ineligible state</li>
 * </ul>
 */
@Data
@Builder
public class Order {

    /**
     * Lifecycle states for an order.
     *
     * <p>Legal transitions:
     * {@code CREATED -> PAID -> SHIPPED -> DELIVERED};
     * any state before SHIPPED allows {@code -> CANCELLED};
     * {@code PAID} or {@code DELIVERED} allows {@code -> REFUNDED}.</p>
     */
    public enum Status {
        CREATED, PAID, SHIPPED, DELIVERED, CANCELLED, REFUNDED
    }

    /** Auto-assigned sequential order identifier. */
    private long id;

    /** Line items that make up this order (at least one). */
    private List<OrderLine> lines;

    /** Sum of all {@link OrderLine#lineTotal()} values, before adjustments. */
    private BigDecimal subtotal;

    /**
     * Tax amount computed as a fixed percentage of {@link #subtotal}
     * (8 % in the reference domain).
     */
    private BigDecimal tax;

    /** Shipping charge: flat rate when subtotal is below the free threshold. */
    private BigDecimal shipping;

    /**
     * Coupon-applied discount amount (zero when no coupon or invalid coupon
     * is supplied).
     */
    private BigDecimal discount;

    /**
     * Grand total = subtotal + tax + shipping - discount.
     * Always {@code >= 0}.
     */
    private BigDecimal total;

    /** Mutable lifecycle status — updated by {@link OrderService}. */
    private Status status;

    /** Applied coupon code, or {@code null} when none. */
    private String couponCode;

    /** Shipping address as captured at checkout time. */
    private ShippingAddress shippingAddress;

    /** Payment method used at checkout. */
    private PaymentMethod paymentMethod;
}
