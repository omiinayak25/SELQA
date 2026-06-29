package com.omiinqa.reference.orders;

import com.omiinqa.reference.core.DomainException;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Reference order management service: retrieves orders, enforces lifecycle
 * status transitions, and provides revenue analytics.
 *
 * <h3>Legal lifecycle transitions:</h3>
 * <pre>
 *   CREATED → PAID → SHIPPED → DELIVERED
 *   Any state before SHIPPED → CANCELLED   (after SHIPPED: ORDER_CANNOT_CANCEL)
 *   PAID | DELIVERED → REFUNDED            (other states: ORDER_CANNOT_REFUND)
 *   All other moves → ORDER_BAD_TRANSITION
 * </pre>
 *
 * <h3>Error codes owned by this service:</h3>
 * <ul>
 *   <li>{@code ORDER_NOT_FOUND}       — order ID does not exist in this world</li>
 *   <li>{@code ORDER_BAD_TRANSITION}  — status move not allowed by the lifecycle</li>
 *   <li>{@code ORDER_CANNOT_CANCEL}   — cancel attempted after shipment</li>
 *   <li>{@code ORDER_CANNOT_REFUND}   — refund attempted in a non-refundable state</li>
 * </ul>
 *
 * <p>State is per-instance so each BDD scenario gets a fresh, isolated service.</p>
 */
public class OrderService {

    /** States from which an order may be cancelled (not yet shipped). */
    private static final Set<Order.Status> CANCELLABLE =
            EnumSet.of(Order.Status.CREATED, Order.Status.PAID);

    /** States from which a refund may be initiated. */
    private static final Set<Order.Status> REFUNDABLE =
            EnumSet.of(Order.Status.PAID, Order.Status.DELIVERED);

    /** States counted towards total revenue. */
    private static final Set<Order.Status> REVENUE_STATES =
            EnumSet.of(Order.Status.PAID, Order.Status.SHIPPED,
                       Order.Status.DELIVERED);

    private final Map<Long, Order> store = new ConcurrentHashMap<>();

    // -----------------------------------------------------------------------
    // Package-level store hook — used by CheckoutService
    // -----------------------------------------------------------------------

    /**
     * Persist an order directly into the in-memory store.
     *
     * <p>Called by {@link CheckoutService} after a successful checkout and
     * also by BDD step definitions that need to seed the service with a
     * pre-configured order to test transitions without replaying the full
     * checkout flow.</p>
     */
    public void store(final Order order) {
        store.put(order.getId(), order);
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Retrieve an order by its ID.
     *
     * @throws DomainException {@code ORDER_NOT_FOUND} when the ID is unknown
     */
    public Order get(final long orderId) {
        final Order order = store.get(orderId);
        if (order == null) {
            throw new DomainException("ORDER_NOT_FOUND",
                    "No order found with id: " + orderId);
        }
        return order;
    }

    /**
     * Transition an order to a new status, enforcing legal lifecycle rules.
     *
     * @throws DomainException {@code ORDER_NOT_FOUND}, {@code ORDER_CANNOT_CANCEL},
     *                         {@code ORDER_CANNOT_REFUND}, or {@code ORDER_BAD_TRANSITION}
     */
    public Order transition(final long orderId, final Order.Status newStatus) {
        final Order order = get(orderId);
        final Order.Status current = order.getStatus();

        if (newStatus == Order.Status.CANCELLED) {
            enforceCancellable(current);
        } else if (newStatus == Order.Status.REFUNDED) {
            enforceRefundable(current);
        } else {
            enforceLegalForward(current, newStatus);
        }

        order.setStatus(newStatus);
        return order;
    }

    /**
     * Return all orders whose current status matches {@code status}.
     */
    public List<Order> listByStatus(final Order.Status status) {
        return store.values().stream()
                    .filter(o -> o.getStatus() == status)
                    .collect(Collectors.toList());
    }

    /**
     * Total revenue: sum of {@link Order#getTotal()} for every order in a
     * revenue-counting state ({@code PAID}, {@code SHIPPED}, {@code DELIVERED}).
     */
    public BigDecimal totalRevenue() {
        return store.values().stream()
                    .filter(o -> REVENUE_STATES.contains(o.getStatus()))
                    .map(Order::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Number of orders currently managed by this service instance. */
    public int orderCount() {
        return store.size();
    }

    // -----------------------------------------------------------------------
    // Transition guards
    // -----------------------------------------------------------------------

    private void enforceCancellable(final Order.Status current) {
        if (current == Order.Status.SHIPPED
                || current == Order.Status.DELIVERED
                || current == Order.Status.REFUNDED) {
            throw new DomainException("ORDER_CANNOT_CANCEL",
                    "Cannot cancel an order that has already been shipped (status=" + current + ")");
        }
        if (!CANCELLABLE.contains(current) && current != Order.Status.CANCELLED) {
            // Already cancelled or terminal
            throw new DomainException("ORDER_BAD_TRANSITION",
                    "Cannot cancel order in status: " + current);
        }
    }

    private void enforceRefundable(final Order.Status current) {
        if (!REFUNDABLE.contains(current)) {
            throw new DomainException("ORDER_CANNOT_REFUND",
                    "Cannot refund order in status: " + current
                    + ". Refunds are only allowed for PAID or DELIVERED orders.");
        }
    }

    private void enforceLegalForward(final Order.Status current, final Order.Status next) {
        final boolean legal = switch (current) {
            case CREATED    -> next == Order.Status.PAID;
            case PAID       -> next == Order.Status.SHIPPED;
            case SHIPPED    -> next == Order.Status.DELIVERED;
            default         -> false; // DELIVERED, CANCELLED, REFUNDED are terminal
        };
        if (!legal) {
            throw new DomainException("ORDER_BAD_TRANSITION",
                    "Illegal status transition: " + current + " -> " + next);
        }
    }
}
