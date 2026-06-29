package com.omiinqa.bdd.steps.domain;

import com.omiinqa.bdd.support.DomainWorld;
import com.omiinqa.reference.orders.Order;
import com.omiinqa.reference.orders.OrderService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the reference <b>orders</b> domain.
 *
 * <p>Drives {@link OrderService} for status transitions, retrieval and revenue
 * queries. Mutating calls are wrapped in {@link DomainWorld#run} /
 * {@link DomainWorld#capture} so {@code CommonDomainSteps} assertions work
 * without any step being redefined here.</p>
 *
 * <p>All step text is prefixed with "order" or "the order" to avoid collisions
 * with UI checkout steps in the SauceDemo suite.</p>
 */
public class OrderSteps {

    private static final String ORDER_SVC  = "orderService";
    private static final String LAST_ORDER = "order.lastOrder";

    private OrderService orderService() {
        return DomainWorld.service(ORDER_SVC, OrderService::new);
    }

    // -----------------------------------------------------------------------
    // Given — service setup
    // -----------------------------------------------------------------------

    @Given("a clean order service")
    public void cleanOrderService() {
        DomainWorld.put(ORDER_SVC, new OrderService());
    }

    // -----------------------------------------------------------------------
    // Given — seed orders (placed via CheckoutService in CheckoutSteps / here)
    // -----------------------------------------------------------------------

    /**
     * Directly store a minimal order in the service at the given status so
     * transition scenarios start from a known, controllable state without
     * needing to replay the full checkout flow.
     */
    @Given("an existing order {string} with status {string} and total {string}")
    public void existingOrderWithStatusAndTotal(
            final String orderRef, final String statusStr, final String totalStr) {

        final Order.Status status = Order.Status.valueOf(statusStr);
        final BigDecimal total = new BigDecimal(totalStr);

        // Build a minimal but valid order and register it in the shared world
        final Order order = buildSeedOrder(Long.parseLong(orderRef), total, status);
        orderService().store(order);
        DomainWorld.put(LAST_ORDER, order);
        DomainWorld.put("order.ref." + orderRef, order);
    }

    // -----------------------------------------------------------------------
    // When — transitions
    // -----------------------------------------------------------------------

    @When("I transition order {string} to status {string}")
    public void transitionOrder(final String orderRef, final String newStatusStr) {
        final long id = resolveOrderId(orderRef);
        final Order.Status newStatus = Order.Status.valueOf(newStatusStr);
        DomainWorld.run(() -> {
            final Order updated = orderService().transition(id, newStatus);
            DomainWorld.put(LAST_ORDER, updated);
        });
    }

    @When("I retrieve order {string}")
    public void retrieveOrder(final String orderRef) {
        final long id = Long.parseLong(orderRef);
        DomainWorld.run(() -> {
            final Order found = orderService().get(id);
            DomainWorld.put(LAST_ORDER, found);
        });
    }

    @When("I retrieve a non-existent order {string}")
    public void retrieveNonExistentOrder(final String orderRef) {
        final long id = Long.parseLong(orderRef);
        DomainWorld.run(() -> orderService().get(id));
    }

    // -----------------------------------------------------------------------
    // Then — assertions
    // -----------------------------------------------------------------------

    @Then("the order status is {string}")
    public void theOrderStatusIs(final String expectedStatus) {
        final Order order = DomainWorld.get(LAST_ORDER);
        assertThat(order).as("last order must be set").isNotNull();
        assertThat(order.getStatus().name())
                .as("order status")
                .isEqualTo(expectedStatus);
    }

    @Then("the order total is {string}")
    public void theOrderTotalIs(final String expectedTotal) {
        final Order order = DomainWorld.get(LAST_ORDER);
        assertThat(order).as("last order must be set").isNotNull();
        assertThat(order.getTotal())
                .as("order total")
                .isEqualByComparingTo(new BigDecimal(expectedTotal));
    }

    @Then("the order tax is {string}")
    public void theOrderTaxIs(final String expectedTax) {
        final Order order = DomainWorld.get(LAST_ORDER);
        assertThat(order).as("last order must be set").isNotNull();
        assertThat(order.getTax())
                .as("order tax")
                .isEqualByComparingTo(new BigDecimal(expectedTax));
    }

    @Then("the order shipping is {string}")
    public void theOrderShippingIs(final String expectedShipping) {
        final Order order = DomainWorld.get(LAST_ORDER);
        assertThat(order).as("last order must be set").isNotNull();
        assertThat(order.getShipping())
                .as("order shipping")
                .isEqualByComparingTo(new BigDecimal(expectedShipping));
    }

    @Then("the order discount is {string}")
    public void theOrderDiscountIs(final String expectedDiscount) {
        final Order order = DomainWorld.get(LAST_ORDER);
        assertThat(order).as("last order must be set").isNotNull();
        assertThat(order.getDiscount())
                .as("order discount")
                .isEqualByComparingTo(new BigDecimal(expectedDiscount));
    }

    @Then("the order subtotal is {string}")
    public void theOrderSubtotalIs(final String expectedSubtotal) {
        final Order order = DomainWorld.get(LAST_ORDER);
        assertThat(order).as("last order must be set").isNotNull();
        assertThat(order.getSubtotal())
                .as("order subtotal")
                .isEqualByComparingTo(new BigDecimal(expectedSubtotal));
    }

    @Then("the order count for status {string} is {int}")
    public void theOrderCountForStatusIs(final String statusStr, final int expectedCount) {
        final Order.Status status = Order.Status.valueOf(statusStr);
        final List<Order> orders = orderService().listByStatus(status);
        assertThat(orders).as("orders with status " + statusStr).hasSize(expectedCount);
    }

    @Then("the total revenue is {string}")
    public void theTotalRevenueIs(final String expectedRevenue) {
        assertThat(orderService().totalRevenue())
                .as("total revenue")
                .isEqualByComparingTo(new BigDecimal(expectedRevenue));
    }

    @Then("the order service contains {int} order(s)")
    public void theOrderServiceContainsOrders(final int expectedCount) {
        assertThat(orderService().orderCount())
                .as("total order count")
                .isEqualTo(expectedCount);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private long resolveOrderId(final String orderRef) {
        // If there is a stored order under this ref key, use its id; else parse directly
        final Order stored = DomainWorld.get("order.ref." + orderRef);
        return stored != null ? stored.getId() : Long.parseLong(orderRef);
    }

    /** Build a minimal but structurally complete {@link Order} for seeding. */
    private Order buildSeedOrder(final long id, final BigDecimal total, final Order.Status status) {
        final BigDecimal subtotal = total; // simplified seed — no tax/shipping split needed
        return Order.builder()
                .id(id)
                .lines(List.of())
                .subtotal(subtotal)
                .tax(BigDecimal.ZERO.setScale(2))
                .shipping(BigDecimal.ZERO.setScale(2))
                .discount(BigDecimal.ZERO.setScale(2))
                .total(total.setScale(2))
                .status(status)
                .build();
    }
}
