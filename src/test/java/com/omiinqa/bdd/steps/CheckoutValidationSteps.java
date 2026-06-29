package com.omiinqa.bdd.steps;

import com.omiinqa.bdd.context.ScenarioContext;
import com.omiinqa.businessflows.AddToCartFlow;
import com.omiinqa.pages.saucedemo.CartPage;
import com.omiinqa.pages.saucedemo.CheckoutCompletePage;
import com.omiinqa.pages.saucedemo.CheckoutStepOnePage;
import com.omiinqa.pages.saucedemo.CheckoutStepTwoPage;
import com.omiinqa.pages.saucedemo.ProductsPage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for checkout form validation, checkout overview, and
 * order confirmation scenarios.
 *
 * <p>Covers both error-path (missing fields) and happy-path (valid data)
 * flows through CheckoutStepOnePage and CheckoutStepTwoPage. State is wired
 * through {@link ScenarioContext} under typed keys to avoid cross-step coupling.</p>
 */
public class CheckoutValidationSteps {

    // Reuse ScenarioContext key for step one page
    private static final String STEP_ONE_KEY = "checkoutStepOnePage";
    private static final String STEP_TWO_KEY = "checkoutStepTwoPage";

    // -----------------------------------------------------------------------
    //  Given — background setup
    // -----------------------------------------------------------------------

    @Given("the standard user has added {string} to the cart and reached checkout step one")
    public void standardUserHasAddedProductAndReachedCheckoutStepOne(final String productName) {
        final CartPage cart = AddToCartFlow.loginAndAddProducts(List.of(productName));
        ScenarioContext.put(ScenarioContext.CART_PAGE, cart);
        final CheckoutStepOnePage step1 = cart.checkout();
        ScenarioContext.put(STEP_ONE_KEY, step1);
    }

    // -----------------------------------------------------------------------
    //  When — checkout step one interactions
    // -----------------------------------------------------------------------

    @When("the user submits the checkout form without filling any field")
    public void submitCheckoutFormBlank() {
        final CheckoutStepOnePage step1 = ScenarioContext.get(STEP_ONE_KEY);
        step1.continueToOverview();
    }

    @When("the user submits checkout step one with first name {string} last name {string} zip {string}")
    public void submitCheckoutStepOne(final String first, final String last, final String zip) {
        final CheckoutStepOnePage step1 = ScenarioContext.get(STEP_ONE_KEY);
        step1.enterCustomerInfo(first, last, zip);
        final CheckoutStepTwoPage step2 = step1.continueToOverview();
        ScenarioContext.put(STEP_TWO_KEY, step2);
    }

    @When("the user cancels checkout step one")
    public void cancelCheckoutStepOne() {
        final CheckoutStepOnePage step1 = ScenarioContext.get(STEP_ONE_KEY);
        final CartPage cart = step1.cancel();
        ScenarioContext.put(ScenarioContext.CART_PAGE, cart);
    }

    @When("the user cancels from the checkout overview")
    public void cancelFromCheckoutOverview() {
        final CheckoutStepTwoPage step2 = ScenarioContext.get(STEP_TWO_KEY);
        final ProductsPage products = step2.cancel();
        ScenarioContext.put(ScenarioContext.PRODUCTS_PAGE, products);
    }

    @When("the user proceeds to checkout overview with customer info {string} {string} {string}")
    public void proceedToCheckoutOverview(final String first, final String last, final String zip) {
        // Open the cart if it hasn't been opened yet (e.g. step came directly from inventory).
        CartPage cart = ScenarioContext.get(ScenarioContext.CART_PAGE);
        if (cart == null) {
            final ProductsPage products = ScenarioContext.get(ScenarioContext.PRODUCTS_PAGE);
            cart = products.openCart();
            ScenarioContext.put(ScenarioContext.CART_PAGE, cart);
        }
        final CheckoutStepOnePage step1 = cart.checkout();
        step1.enterCustomerInfo(first, last, zip);
        final CheckoutStepTwoPage step2 = step1.continueToOverview();
        ScenarioContext.put(STEP_TWO_KEY, step2);
    }

    @When("the user checks out with customer info {string} {string} {string}")
    public void checkoutWithCustomerInfo(final String first, final String last, final String zip) {
        // Open the cart if not already opened (step may follow directly after add-to-cart).
        CartPage cart = ScenarioContext.get(ScenarioContext.CART_PAGE);
        if (cart == null) {
            final ProductsPage products = ScenarioContext.get(ScenarioContext.PRODUCTS_PAGE);
            cart = products.openCart();
            ScenarioContext.put(ScenarioContext.CART_PAGE, cart);
        }
        final CheckoutStepOnePage step1 = cart.checkout();
        step1.enterCustomerInfo(first, last, zip);
        final CheckoutStepTwoPage step2 = step1.continueToOverview();
        final CheckoutCompletePage complete = step2.finish();
        ScenarioContext.put(ScenarioContext.CHECKOUT_COMPLETE, complete);
    }

    // -----------------------------------------------------------------------
    //  Then — checkout step one assertions
    // -----------------------------------------------------------------------

    @Then("the checkout step one error contains {string}")
    public void checkoutStepOneErrorContains(final String fragment) {
        final CheckoutStepOnePage step1 = ScenarioContext.get(STEP_ONE_KEY);
        assertThat(step1.isErrorDisplayed())
                .as("Checkout step-one error banner should be visible")
                .isTrue();
        assertThat(step1.getErrorMessage())
                .as("Checkout step-one error message")
                .containsIgnoringCase(fragment);
    }

    @Then("the checkout step one error is displayed")
    public void checkoutStepOneErrorDisplayed() {
        final CheckoutStepOnePage step1 = ScenarioContext.get(STEP_ONE_KEY);
        assertThat(step1.isErrorDisplayed())
                .as("Checkout step-one error banner should be visible")
                .isTrue();
    }

    @Then("the checkout step one page is displayed")
    public void checkoutStepOnePageDisplayed() {
        final CheckoutStepOnePage step1 = ScenarioContext.get(ScenarioContext.CURRENT_PAGE);
        assertThat(step1)
                .as("Checkout step one page should have been navigated to")
                .isNotNull();
    }

    @Then("the checkout overview page is displayed")
    public void checkoutOverviewPageDisplayed() {
        final CheckoutStepTwoPage step2 = ScenarioContext.get(STEP_TWO_KEY);
        assertThat(step2)
                .as("Checkout overview (step two) page should be present")
                .isNotNull();
        assertThat(step2.getItemNames())
                .as("Overview should list at least one item")
                .isNotEmpty();
    }

    @Then("the checkout overview contains the item {string}")
    public void checkoutOverviewContainsItem(final String productName) {
        final CheckoutStepTwoPage step2 = ScenarioContext.get(STEP_TWO_KEY);
        assertThat(step2.getItemNames())
                .as("Checkout overview should contain '%s'", productName)
                .contains(productName);
    }

    // -----------------------------------------------------------------------
    //  Then — checkout totals assertions
    // -----------------------------------------------------------------------

    @Then("the checkout overview subtotal is not empty")
    public void checkoutOverviewSubtotalNotEmpty() {
        final CheckoutStepTwoPage step2 = ScenarioContext.get(STEP_TWO_KEY);
        assertThat(step2.getSummarySubtotal())
                .as("Checkout overview subtotal")
                .isNotBlank();
    }

    @Then("the checkout overview tax is not empty")
    public void checkoutOverviewTaxNotEmpty() {
        final CheckoutStepTwoPage step2 = ScenarioContext.get(STEP_TWO_KEY);
        assertThat(step2.getTax())
                .as("Checkout overview tax")
                .isNotBlank();
    }

    @Then("the checkout overview total is not empty")
    public void checkoutOverviewTotalNotEmpty() {
        final CheckoutStepTwoPage step2 = ScenarioContext.get(STEP_TWO_KEY);
        assertThat(step2.getTotal())
                .as("Checkout overview total")
                .isNotBlank();
    }

    @Then("the checkout overview subtotal contains {string}")
    public void checkoutOverviewSubtotalContains(final String substring) {
        final CheckoutStepTwoPage step2 = ScenarioContext.get(STEP_TWO_KEY);
        assertThat(step2.getSummarySubtotal())
                .as("Checkout overview subtotal should contain '%s'", substring)
                .contains(substring);
    }

    @Then("the checkout overview total contains {string}")
    public void checkoutOverviewTotalContains(final String substring) {
        final CheckoutStepTwoPage step2 = ScenarioContext.get(STEP_TWO_KEY);
        assertThat(step2.getTotal())
                .as("Checkout overview total should contain '%s'", substring)
                .contains(substring);
    }

    // -----------------------------------------------------------------------
    //  Then — order confirmation assertions
    // -----------------------------------------------------------------------

    @Then("the order confirmation body text contains {string}")
    public void orderConfirmationBodyContains(final String fragment) {
        final CheckoutCompletePage complete = ScenarioContext.get(ScenarioContext.CHECKOUT_COMPLETE);
        assertThat(complete.getCompleteText())
                .as("Order confirmation body text")
                .containsIgnoringCase(fragment);
    }

    @Then("the user clicks Back Home and lands on the products page")
    public void clickBackHomeAndLandOnProductsPage() {
        final CheckoutCompletePage complete = ScenarioContext.get(ScenarioContext.CHECKOUT_COMPLETE);
        final ProductsPage products = complete.backToProducts();
        ScenarioContext.put(ScenarioContext.PRODUCTS_PAGE, products);
        assertThat(products.isLoaded())
                .as("Products page should be loaded after clicking Back Home")
                .isTrue();
    }
}
