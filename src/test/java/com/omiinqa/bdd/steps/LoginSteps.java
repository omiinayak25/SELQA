package com.omiinqa.bdd.steps;

import com.omiinqa.bdd.context.ScenarioContext;
import com.omiinqa.pages.saucedemo.LoginPage;
import com.omiinqa.pages.saucedemo.ProductsPage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for SauceDemo authentication scenarios. Steps delegate to
 * page objects and stash the resulting page in {@link ScenarioContext} so later
 * steps (and other step classes) can continue the journey.
 */
public class LoginSteps {

    @Given("the user is on the SauceDemo login page")
    public void userOnLoginPage() {
        ScenarioContext.put(ScenarioContext.LOGIN_PAGE, new LoginPage().open());
    }

    @When("the user logs in as {string} with password {string}")
    public void userLogsIn(final String username, final String password) {
        final LoginPage login = ScenarioContext.get(ScenarioContext.LOGIN_PAGE);
        final ProductsPage products = login.login(username, password);
        ScenarioContext.put(ScenarioContext.PRODUCTS_PAGE, products);
    }

    @Then("the products page is displayed")
    public void productsPageDisplayed() {
        final ProductsPage products = ScenarioContext.get(ScenarioContext.PRODUCTS_PAGE);
        assertThat(products.isLoaded()).as("inventory loaded").isTrue();
    }

    @Then("the inventory contains {int} products")
    public void inventoryContains(final int count) {
        final ProductsPage products = ScenarioContext.get(ScenarioContext.PRODUCTS_PAGE);
        assertThat(products.getProductCount()).isEqualTo(count);
    }

    @Then("a login error containing {string} is shown")
    public void loginErrorShown(final String fragment) {
        final LoginPage login = ScenarioContext.get(ScenarioContext.LOGIN_PAGE);
        assertThat(login.isErrorDisplayed()).isTrue();
        assertThat(login.getErrorMessage()).containsIgnoringCase(fragment);
    }
}
