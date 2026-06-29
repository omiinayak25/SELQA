package com.omiinqa.reference.orders;

import com.omiinqa.reference.core.DomainException;
import com.omiinqa.reference.core.Validations;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Reference checkout service: validates a shopping cart, applies shipping and
 * tax rules, resolves optional coupon discounts, and produces a persisted
 * {@link Order} ready for fulfilment.
 *
 * <h3>Business rules (asserted by BDD scenarios):</h3>
 * <ul>
 *   <li>Cart must contain at least one line → {@code CHK_EMPTY_CART}</li>
 *   <li>All four address fields (recipient, street, city, postal code) must be
 *       non-blank → {@code CHK_BAD_ADDRESS}</li>
 *   <li>Payment method must be non-null → {@code CHK_BAD_PAYMENT}</li>
 *   <li>Tax rate: 8 % of subtotal (rounded half-up to 2 d.p.)</li>
 *   <li>Shipping: free when subtotal &gt;= {@link #FREE_SHIPPING_THRESHOLD},
 *       otherwise {@link #FLAT_SHIPPING_RATE}</li>
 *   <li>Coupons: {@code SAVE10} → 10 % off subtotal; {@code FLAT5} → $5 off;
 *       unknown coupon → zero discount (no error — silent ignore)</li>
 *   <li>Discount never exceeds subtotal (floor = 0)</li>
 * </ul>
 *
 * <p>State is per-instance so each BDD scenario gets an isolated world with no
 * cross-contamination between runs.</p>
 */
public class CheckoutService {

    /** Subtotal at or above this threshold qualifies for free shipping. */
    public static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("50.00");

    /** Flat shipping charge applied when subtotal is below the free threshold. */
    public static final BigDecimal FLAT_SHIPPING_RATE = new BigDecimal("5.99");

    /** Tax rate applied to the cart subtotal (8 %). */
    public static final BigDecimal TAX_RATE = new BigDecimal("0.08");

    private final AtomicLong ids;
    private final OrderService orderService;

    /**
     * Create a checkout service that persists completed orders into
     * {@code orderService}. Both services share the same ID sequence so order
     * IDs are globally unique within a scenario.
     */
    public CheckoutService(final OrderService orderService) {
        this.orderService = orderService;
        this.ids = new AtomicLong(1000);
    }

    /**
     * Validate the cart and address, compute pricing, and place the order.
     *
     * @param lines          one or more order lines (must not be empty)
     * @param address        shipping address with all required fields
     * @param paymentMethod  non-null payment method
     * @param couponCode     optional coupon code; {@code null} or unknown → no discount
     * @return the newly placed {@link Order} in {@code CREATED} status
     * @throws DomainException with {@code CHK_EMPTY_CART}, {@code CHK_BAD_ADDRESS},
     *                         or {@code CHK_BAD_PAYMENT} on validation failure
     */
    public Order checkout(final List<OrderLine> lines,
                          final ShippingAddress address,
                          final PaymentMethod paymentMethod,
                          final String couponCode) {

        validateCart(lines);
        validateAddress(address);
        validatePayment(paymentMethod);

        final BigDecimal subtotal = computeSubtotal(lines);
        final BigDecimal tax      = computeTax(subtotal);
        final BigDecimal shipping = computeShipping(subtotal);
        final BigDecimal discount = computeDiscount(subtotal, couponCode);
        final BigDecimal total    = subtotal.add(tax).add(shipping).subtract(discount)
                                            .max(BigDecimal.ZERO)
                                            .setScale(2, RoundingMode.HALF_UP);

        final Order order = Order.builder()
                .id(ids.incrementAndGet())
                .lines(List.copyOf(lines))
                .subtotal(subtotal.setScale(2, RoundingMode.HALF_UP))
                .tax(tax)
                .shipping(shipping)
                .discount(discount)
                .total(total)
                .status(Order.Status.CREATED)
                .couponCode(couponCode)
                .shippingAddress(address)
                .paymentMethod(paymentMethod)
                .build();

        orderService.store(order);
        return order;
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private void validateCart(final List<OrderLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new DomainException("CHK_EMPTY_CART", "Cart must contain at least one item");
        }
    }

    private void validateAddress(final ShippingAddress address) {
        if (address == null) {
            throw new DomainException("CHK_BAD_ADDRESS", "Shipping address is required");
        }
        Validations.requireNotBlank(address.getRecipientName(), "recipientName", "CHK_BAD_ADDRESS");
        Validations.requireNotBlank(address.getStreet(),        "street",         "CHK_BAD_ADDRESS");
        Validations.requireNotBlank(address.getCity(),          "city",            "CHK_BAD_ADDRESS");
        Validations.requireNotBlank(address.getPostalCode(),    "postalCode",      "CHK_BAD_ADDRESS");
    }

    private void validatePayment(final PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            throw new DomainException("CHK_BAD_PAYMENT", "A valid payment method is required");
        }
    }

    private BigDecimal computeSubtotal(final List<OrderLine> lines) {
        return lines.stream()
                    .map(OrderLine::lineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal computeTax(final BigDecimal subtotal) {
        return subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal computeShipping(final BigDecimal subtotal) {
        return subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0
               ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
               : FLAT_SHIPPING_RATE.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Resolve coupon discount against the subtotal.
     *
     * <ul>
     *   <li>{@code SAVE10} — 10 % of subtotal (rounded half-up to 2 d.p.)</li>
     *   <li>{@code FLAT5}  — flat $5.00 (capped at subtotal)</li>
     *   <li>anything else / null — $0.00 discount</li>
     * </ul>
     */
    BigDecimal computeDiscount(final BigDecimal subtotal, final String couponCode) {
        if (couponCode == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return switch (couponCode.toUpperCase()) {
            case "SAVE10" -> subtotal.multiply(new BigDecimal("0.10"))
                                     .setScale(2, RoundingMode.HALF_UP);
            case "FLAT5"  -> new BigDecimal("5.00").min(subtotal)
                                                   .setScale(2, RoundingMode.HALF_UP);
            default       -> BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        };
    }
}
