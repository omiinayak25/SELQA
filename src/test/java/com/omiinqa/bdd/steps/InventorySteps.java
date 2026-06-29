package com.omiinqa.bdd.steps;

import com.omiinqa.bdd.context.ScenarioContext;
import com.omiinqa.businessflows.LoginFlow;
import com.omiinqa.pages.saucedemo.CartPage;
import com.omiinqa.pages.saucedemo.ProductDetailPage;
import com.omiinqa.pages.saucedemo.ProductsPage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for inventory, product listing, and product detail scenarios.
 *
 * <p>Delegates all UI interactions to {@link ProductsPage} and {@link ProductDetailPage};
 * state is shared across step classes via {@link ScenarioContext}. No assertions live
 * in page objects — only in this step class (Single Responsibility).</p>
 */
public class InventorySteps {

    // -----------------------------------------------------------------------
    //  Given / Background steps
    // -----------------------------------------------------------------------

    @Given("the standard user is logged in and on the products page")
    public void standardUserLoggedIn() {
        final ProductsPage products = LoginFlow.loginAsStandardUser();
        ScenarioContext.put(ScenarioContext.PRODUCTS_PAGE, products);
    }

    // -----------------------------------------------------------------------
    //  When — product list interactions
    // -----------------------------------------------------------------------

    @When("the user adds {string} to the cart from the inventory")
    public void addProductToCartFromInventory(final String productName) {
        final ProductsPage products = ScenarioContext.get(ScenarioContext.PRODUCTS_PAGE);
        products.addToCart(productName);
    }

    @When("the user adds the following products to the cart:")
    public void addMultipleProductsToCart(final List<String> productNames) {
        final ProductsPage products = ScenarioContext.get(ScenarioContext.PRODUCTS_PAGE);
        productNames.forEach(products::addToCart);
    }

    @When("the user removes {string} from the inventory")
    public void removeProductFromInventory(final String productName) {
        final ProductsPage products = ScenarioContext.get(ScenarioContext.PRODUCTS_PAGE);
        products.removeFromCart(productName);
    }

    @When("the user opens the product detail for {string}")
    public void openProductDetail(final String productName) {
        final ProductsPage products = ScenarioContext.get(ScenarioContext.PRODUCTS_PAGE);
        final ProductDetailPage detail = products.openProduct(productName);
        ScenarioContext.put(ScenarioContext.CURRENT_PAGE, detail);
    }

    @When("the user navigates back to the inventory from the detail page")
    public void navigateBackFromDetail() {
        final ProductDetailPage detail = ScenarioContext.get(ScenarioContext.CURRENT_PAGE);
        final ProductsPage products = detail.backToProducts();
        ScenarioContext.put(ScenarioContext.PRODUCTS_PAGE, products);
    }

    @When("the user adds the product to cart from the detail page")
    public void addToCartFromDetailPage() {
        final ProductDetailPage detail = ScenarioContext.get(ScenarioContext.CURRENT_PAGE);
        detail.addToCart();
    }

    @When("the user opens the cart")
    public void openCart() {
        final ProductsPage products = ScenarioContext.get(ScenarioContext.PRODUCTS_PAGE);
        final CartPage cart = products.openCart();
        ScenarioContext.put(ScenarioContext.CART_PAGE, cart);
    }

    // -----------------------------------------------------------------------
    //  Then — inventory assertions
    // -----------------------------------------------------------------------

    @Then("the following products are visible in the inventory:")
    public void followingProductsVisible(final List<String> expectedNames) {
        final ProductsPage products = ScenarioContext.get(ScenarioContext.PRODUCTS_PAGE);
        final List<String> actual = products.getProductNames();
        assertThat(actual)
                .as("All expected products should be visible in the inventory")
                .containsAll(expectedNames);
    }

    @Then("the cart badge shows {int}")
    public void cartBadgeShows(final int expected) {
        final ProductsPage products = ScenarioContext.get(ScenarioContext.PRODUCTS_PAGE);
        assertThat(products.getCartBadgeCount())
                .as("Cart badge count")
                .isEqualTo(expected);
    }

    @Then("the cart badge is not visible")
    public void cartBadgeNotVisible() {
        final ProductsPage products = ScenarioContext.get(ScenarioContext.PRODUCTS_PAGE);
        assertThat(products.getCartBadgeCount())
                .as("Cart badge should be absent (count == 0)")
                .isEqualTo(0);
    }

    @Then("the product detail page shows name {string}")
    public void productDetailShowsName(final String expectedName) {
        final ProductDetailPage detail = ScenarioContext.get(ScenarioContext.CURRENT_PAGE);
        assertThat(detail.getProductName())
                .as("Product detail page name")
                .isEqualTo(expectedName);
    }

    @Then("the product detail page shows a price")
    public void productDetailShowsPrice() {
        final ProductDetailPage detail = ScenarioContext.get(ScenarioContext.CURRENT_PAGE);
        assertThat(detail.getProductPrice())
                .as("Product detail page price")
                .isNotBlank()
                .contains("$");
    }
}
