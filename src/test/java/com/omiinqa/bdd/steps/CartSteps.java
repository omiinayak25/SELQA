package com.omiinqa.bdd.steps;

import com.omiinqa.bdd.context.ScenarioContext;
import com.omiinqa.pages.saucedemo.CartPage;
import com.omiinqa.pages.saucedemo.CheckoutStepOnePage;
import com.omiinqa.pages.saucedemo.ProductsPage;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for shopping cart page interactions and assertions.
 *
 * <p>Coordinates with {@link InventorySteps} via {@link ScenarioContext}:
 * the cart page reference is stored under {@link ScenarioContext#CART_PAGE}
 * and retrieved here without requiring field injection or DI containers.</p>
 */
public class CartSteps {

    // -----------------------------------------------------------------------
    //  When — cart interactions
    // -----------------------------------------------------------------------

    @When("the user removes {string} from the cart")
    public void removeProductFromCart(final String productName) {
        final CartPage cart = ScenarioContext.get(ScenarioContext.CART_PAGE);
        cart.removeItem(productName);
    }

    @When("the user clicks continue shopping")
    public void clickContinueShopping() {
        final CartPage cart = ScenarioContext.get(ScenarioContext.CART_PAGE);
        final ProductsPage products = cart.continueShopping();
        ScenarioContext.put(ScenarioContext.PRODUCTS_PAGE, products);
    }

    @When("the user attempts to start checkout from an empty cart")
    public void startCheckoutFromEmptyCart() {
        final CartPage cart = ScenarioContext.get(ScenarioContext.CART_PAGE);
        final CheckoutStepOnePage step1 = cart.checkout();
        ScenarioContext.put(ScenarioContext.CURRENT_PAGE, step1);
    }

    // -----------------------------------------------------------------------
    //  Then — cart assertions
    // -----------------------------------------------------------------------

    @Then("the cart contains {int} items")
    public void cartContainsItems(final int expected) {
        final CartPage cart = ScenarioContext.get(ScenarioContext.CART_PAGE);
        assertThat(cart.getItemCount())
                .as("Cart item count")
                .isEqualTo(expected);
    }

    @Then("the cart contains {int} item")
    public void cartContainsItem(final int expected) {
        cartContainsItems(expected);
    }

    @Then("the cart contains the product {string}")
    public void cartContainsProduct(final String productName) {
        final CartPage cart = ScenarioContext.get(ScenarioContext.CART_PAGE);
        assertThat(cart.getCartItemNames())
                .as("Cart should contain '%s'", productName)
                .contains(productName);
    }

    @Then("the cart page is displayed with the item {string}")
    public void cartPageDisplayedWithItem(final String productName) {
        final CartPage cart = ScenarioContext.get(ScenarioContext.CART_PAGE);
        assertThat(cart.getItemCount())
                .as("Cart should have at least one item")
                .isGreaterThanOrEqualTo(1);
        assertThat(cart.getCartItemNames())
                .as("Cart should contain '%s'", productName)
                .contains(productName);
    }

    @Then("the cart page title is {string}")
    public void cartPageTitle(final String expectedTitle) {
        final CartPage cart = ScenarioContext.get(ScenarioContext.CART_PAGE);
        assertThat(cart.pageTitle())
                .as("Cart page title")
                .containsIgnoringCase(expectedTitle);
    }
}
