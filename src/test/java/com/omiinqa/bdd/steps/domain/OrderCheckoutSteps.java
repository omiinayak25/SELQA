package com.omiinqa.bdd.steps.domain;

import com.omiinqa.bdd.support.DomainWorld;
import com.omiinqa.reference.orders.CheckoutService;
import com.omiinqa.reference.orders.Order;
import com.omiinqa.reference.orders.OrderLine;
import com.omiinqa.reference.orders.OrderService;
import com.omiinqa.reference.orders.PaymentMethod;
import com.omiinqa.reference.orders.ShippingAddress;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Step definitions for the reference <b>checkout</b> domain.
 *
 * <p>Drives {@link CheckoutService} and asserts real business outcomes — tax,
 * shipping, discount and total are all computed by actual Java logic. There is
 * no browser, no fake, no no-op step.</p>
 *
 * <h3>Naming contract</h3>
 * <p>This class is intentionally named {@code OrderCheckoutSteps} (not
 * {@code CheckoutSteps}) to avoid a class-name clash with the SauceDemo UI
 * class at {@code com.omiinqa.bdd.steps.CheckoutSteps}. Every step phrase here
 * uses the prefix "a domain checkout" or "I initiate a domain checkout" or
 * similar so it does NOT match the UI phrases
 * "the customer purchases the following items:" or
 * "the order confirmation is shown".</p>
 */
public class OrderCheckoutSteps {

    private static final String ORDER_SVC    = "orderService";
    private static final String CHECKOUT_SVC = "checkoutService";
    private static final String LAST_ORDER   = "order.lastOrder";
    private static final String CART_LINES   = "checkout.cartLines";

    // -----------------------------------------------------------------------
    // Service accessors (lazy-init, shared via DomainWorld)
    // -----------------------------------------------------------------------

    private OrderService orderService() {
        return DomainWorld.service(ORDER_SVC, OrderService::new);
    }

    private CheckoutService checkoutService() {
        return DomainWorld.service(CHECKOUT_SVC,
                () -> new CheckoutService(orderService()));
    }

    // -----------------------------------------------------------------------
    // Background setup
    // -----------------------------------------------------------------------

    @Given("a clean checkout service")
    public void cleanCheckoutService() {
        final OrderService os = new OrderService();
        DomainWorld.put(ORDER_SVC, os);
        DomainWorld.put(CHECKOUT_SVC, new CheckoutService(os));
        DomainWorld.put(CART_LINES, new ArrayList<OrderLine>());
    }

    // -----------------------------------------------------------------------
    // Given — cart building
    // -----------------------------------------------------------------------

    /**
     * Add a single line to the domain checkout cart.
     * Step text deliberately differs from the SauceDemo UI step
     * "the customer purchases the following items:".
     */
    @Given("I add a domain cart item {string} priced {string} qty {int}")
    public void addDomainCartItem(final String name, final String price, final int qty) {
        final List<OrderLine> lines = DomainWorld.get(CART_LINES);
        lines.add(OrderLine.builder()
                .productId("SKU-" + name.hashCode())
                .productName(name)
                .unitPrice(new BigDecimal(price))
                .quantity(qty)
                .build());
    }

    /**
     * Populate the cart from a DataTable with columns: name, price, qty.
     * This step uses "I add domain cart items:" to distinguish it from
     * the SauceDemo UI step that uses "the customer purchases the following items:".
     */
    @Given("I add domain cart items:")
    public void addDomainCartItems(final DataTable table) {
        final List<OrderLine> lines = DomainWorld.get(CART_LINES);
        final List<Map<String, String>> rows = table.asMaps(String.class, String.class);
        for (final Map<String, String> row : rows) {
            lines.add(OrderLine.builder()
                    .productId("SKU-" + row.get("name").hashCode())
                    .productName(row.get("name"))
                    .unitPrice(new BigDecimal(row.get("price")))
                    .quantity(Integer.parseInt(row.get("qty")))
                    .build());
        }
    }

    // -----------------------------------------------------------------------
    // When — full checkout invocations
    // -----------------------------------------------------------------------

    /**
     * Perform a full domain checkout with all parameters supplied inline.
     * The coupon code is passed as the literal string "NONE" when there is
     * no coupon to apply.
     */
    @When("I perform a domain checkout with address {string} payment {string} coupon {string}")
    public void performDomainCheckout(
            final String addressToken, final String paymentToken, final String couponToken) {

        final ShippingAddress address = buildAddress(addressToken);
        final PaymentMethod payment   = parsePayment(paymentToken);
        final String coupon           = "NONE".equalsIgnoreCase(couponToken) ? null : couponToken;
        final List<OrderLine> lines   = DomainWorld.get(CART_LINES);

        DomainWorld.run(() -> {
            final Order order = checkoutService().checkout(lines, address, payment, coupon);
            DomainWorld.put(LAST_ORDER, order);
        });
    }

    /**
     * Checkout with a null address — triggers {@code CHK_BAD_ADDRESS}.
     */
    @When("I perform a domain checkout with no address and payment {string}")
    public void performDomainCheckoutNoAddress(final String paymentToken) {
        final PaymentMethod payment = parsePayment(paymentToken);
        final List<OrderLine> lines = DomainWorld.get(CART_LINES);
        DomainWorld.run(() -> checkoutService().checkout(lines, null, payment, null));
    }

    /**
     * Checkout with a null payment method — triggers {@code CHK_BAD_PAYMENT}.
     */
    @When("I perform a domain checkout with address {string} and no payment method")
    public void performDomainCheckoutNoPayment(final String addressToken) {
        final ShippingAddress address = buildAddress(addressToken);
        final List<OrderLine> lines   = DomainWorld.get(CART_LINES);
        DomainWorld.run(() -> checkoutService().checkout(lines, address, null, null));
    }

    /**
     * Checkout with an incomplete address — triggers {@code CHK_BAD_ADDRESS}.
     */
    @When("I perform a domain checkout with incomplete address missing {string} and payment {string}")
    public void performDomainCheckoutIncompleteAddress(
            final String missingField, final String paymentToken) {

        final PaymentMethod payment = parsePayment(paymentToken);
        final ShippingAddress address = buildIncompleteAddress(missingField);
        final List<OrderLine> lines   = DomainWorld.get(CART_LINES);
        DomainWorld.run(() -> checkoutService().checkout(lines, address, payment, null));
    }

    /**
     * Checkout an explicitly empty cart — triggers {@code CHK_EMPTY_CART}.
     */
    @When("I perform a domain checkout with an empty cart and address {string} payment {string}")
    public void performDomainCheckoutEmptyCart(
            final String addressToken, final String paymentToken) {

        final ShippingAddress address = buildAddress(addressToken);
        final PaymentMethod payment   = parsePayment(paymentToken);
        DomainWorld.run(() -> checkoutService().checkout(List.of(), address, payment, null));
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Build a fully populated address from the shorthand token {@code "VALID"}
     * or a pipe-delimited {@code "name|street|city|zip"} string used in outlines.
     */
    private ShippingAddress buildAddress(final String token) {
        if ("VALID".equalsIgnoreCase(token)) {
            return ShippingAddress.builder()
                    .recipientName("Jane Doe")
                    .street("10 Main St")
                    .city("Testville")
                    .postalCode("12345")
                    .build();
        }
        // Pipe-delimited: "name|street|city|zip"
        final String[] parts = token.split("\\|", -1);
        return ShippingAddress.builder()
                .recipientName(parts.length > 0 ? parts[0] : "")
                .street(parts.length > 1 ? parts[1] : "")
                .city(parts.length > 2 ? parts[2] : "")
                .postalCode(parts.length > 3 ? parts[3] : "")
                .build();
    }

    /**
     * Build an address with one field intentionally blank to test
     * {@code CHK_BAD_ADDRESS} per-field validation.
     */
    private ShippingAddress buildIncompleteAddress(final String missingField) {
        return switch (missingField.toLowerCase()) {
            case "recipientname" -> ShippingAddress.builder()
                    .recipientName("").street("10 Main").city("Town").postalCode("99999").build();
            case "street" -> ShippingAddress.builder()
                    .recipientName("Jane").street("").city("Town").postalCode("99999").build();
            case "city" -> ShippingAddress.builder()
                    .recipientName("Jane").street("10 Main").city("").postalCode("99999").build();
            case "postalcode" -> ShippingAddress.builder()
                    .recipientName("Jane").street("10 Main").city("Town").postalCode("").build();
            default -> ShippingAddress.builder()
                    .recipientName("Jane").street("10 Main").city("Town").postalCode("").build();
        };
    }

    /** Parse a payment method token; returns {@code null} for "NONE". */
    private PaymentMethod parsePayment(final String token) {
        if ("NONE".equalsIgnoreCase(token)) {
            return null;
        }
        return PaymentMethod.valueOf(token.toUpperCase());
    }
}
