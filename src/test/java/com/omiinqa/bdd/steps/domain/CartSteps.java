package com.omiinqa.bdd.steps.domain;

import com.omiinqa.bdd.support.DomainWorld;
import com.omiinqa.reference.commerce.CartService;
import com.omiinqa.reference.commerce.ProductRegistry;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the reference commerce / shopping-cart domain.
 *
 * <p>All mutating operations go through {@link DomainWorld#run} so that the
 * shared {@code CommonDomainSteps} assertions ("a domain error X is raised",
 * "the operation succeeds") work without any additional wiring. Services are
 * lazily created via {@link DomainWorld#service} so every BDD scenario starts
 * with a clean, isolated instance.</p>
 *
 * <p>Step text is prefixed with cart-domain nouns to avoid ambiguity with
 * steps from other domains.</p>
 */
public class CartSteps {

    private static final String REGISTRY_KEY = "commerce.registry";
    private static final String CART_KEY     = "commerce.cart";

    // ------------------------------------------------------------------ //
    //  Service accessors                                                   //
    // ------------------------------------------------------------------ //

    private ProductRegistry registry() {
        return DomainWorld.service(REGISTRY_KEY, ProductRegistry::new);
    }

    private CartService cart() {
        final ProductRegistry reg = registry();
        return DomainWorld.service(CART_KEY, () -> new CartService(reg));
    }

    // ------------------------------------------------------------------ //
    //  Given — state setup                                                 //
    // ------------------------------------------------------------------ //

    /** Reset the commerce product registry and create a fresh cart. */
    @Given("a clean cart and product registry")
    public void cleanCartAndRegistry() {
        DomainWorld.put(REGISTRY_KEY, new ProductRegistry());
        DomainWorld.put(CART_KEY, new CartService(registry()));
    }

    /** Seed an additional product with specific stock for boundary scenarios. */
    @Given("a product {string} named {string} priced {string} with {int} in stock")
    public void seedProduct(final String id, final String name,
                            final String price, final int stock) {
        registry().addProduct(id, name, price, stock);
    }

    /** Pre-populate the cart without going through the error-capture path. */
    @Given("I have already added {int} units of product {string} to the cart")
    public void preAddToCart(final int qty, final String productId) {
        cart().addItem(productId, qty);
    }

    /** Pre-apply a coupon so subsequent scenarios can assert totals. */
    @Given("coupon {string} is applied to the cart")
    public void preApplyCoupon(final String code) {
        cart().applyCoupon(code);
    }

    // ------------------------------------------------------------------ //
    //  When — actions                                                      //
    // ------------------------------------------------------------------ //

    @When("I add product {string} to the cart with quantity {int}")
    public void addProductToCart(final String productId, final int qty) {
        DomainWorld.run(() -> cart().addItem(productId, qty));
    }

    @When("I update product {string} quantity in the cart to {int}")
    public void updateProductQtyInCart(final String productId, final int qty) {
        DomainWorld.run(() -> cart().updateQty(productId, qty));
    }

    @When("I remove product {string} from the cart")
    public void removeProductFromCart(final String productId) {
        DomainWorld.run(() -> cart().removeItem(productId));
    }

    @When("I clear the cart")
    public void clearCart() {
        DomainWorld.run(() -> cart().clear());
    }

    @When("I apply coupon code {string} to the cart")
    public void applyCouponToCart(final String code) {
        DomainWorld.run(() -> cart().applyCoupon(code));
    }

    // ------------------------------------------------------------------ //
    //  Then — assertions                                                   //
    // ------------------------------------------------------------------ //

    @Then("the cart has {int} items")
    public void cartHasItems(final int expected) {
        assertThat(cart().itemCount())
                .as("total item count in cart")
                .isEqualTo(expected);
    }

    @Then("the cart has {int} distinct lines")
    public void cartHasDistinctLines(final int expected) {
        assertThat(cart().distinctLines())
                .as("distinct product lines in cart")
                .isEqualTo(expected);
    }

    @Then("the cart is empty")
    public void cartIsEmpty() {
        assertThat(cart().itemCount())
                .as("cart should be empty")
                .isZero();
    }

    @Then("the cart subtotal is {string}")
    public void cartSubtotalIs(final String expectedStr) {
        final BigDecimal expected = new BigDecimal(expectedStr);
        assertThat(cart().subtotal().compareTo(expected))
                .as("cart subtotal: expected %s but was %s", expected, cart().subtotal())
                .isZero();
    }

    @Then("the cart total is {string}")
    public void cartTotalIs(final String expectedStr) {
        final BigDecimal expected = new BigDecimal(expectedStr);
        assertThat(cart().total().compareTo(expected))
                .as("cart total: expected %s but was %s", expected, cart().total())
                .isZero();
    }

    @Then("the cart contains product {string}")
    public void cartContainsProduct(final String productId) {
        assertThat(cart().lines()).containsKey(productId);
    }

    @Then("the cart does not contain product {string}")
    public void cartDoesNotContainProduct(final String productId) {
        assertThat(cart().lines()).doesNotContainKey(productId);
    }

    @Then("the cart line for product {string} has quantity {int}")
    public void cartLineQtyIs(final String productId, final int expected) {
        assertThat(cart().lines()).containsKey(productId);
        assertThat(cart().lines().get(productId).getQuantity())
                .as("quantity for %s", productId)
                .isEqualTo(expected);
    }

    @Then("the active coupon is {string}")
    public void activeCouponIs(final String code) {
        assertThat(cart().activeCoupon())
                .as("active coupon code")
                .hasValue(code);
    }
}
