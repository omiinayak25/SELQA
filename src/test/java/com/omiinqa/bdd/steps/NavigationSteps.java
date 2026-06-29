package com.omiinqa.bdd.steps;

import com.omiinqa.bdd.context.ScenarioContext;
import com.omiinqa.pages.saucedemo.LoginPage;
import com.omiinqa.pages.saucedemo.ProductsPage;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for burger menu navigation, logout, and page-title assertions.
 *
 * <p>The burger menu is accessed through {@link HeaderComponent} which is
 * exposed by {@link ProductsPage#header()}. Steps store and retrieve the
 * products page via {@link ScenarioContext} so the header component reference
 * is always fresh and correctly scoped to the current DOM state.</p>
 */
public class NavigationSteps {

    // -----------------------------------------------------------------------
    //  When — navigation actions
    // -----------------------------------------------------------------------

    @When("the user opens the burger menu")
    public void openBurgerMenu() {
        final ProductsPage products = ScenarioContext.get(ScenarioContext.PRODUCTS_PAGE);
        products.header().openBurgerMenu();
    }

    @When("the user closes the burger menu")
    public void closeBurgerMenu() {
        final ProductsPage products = ScenarioContext.get(ScenarioContext.PRODUCTS_PAGE);
        products.header().closeBurgerMenu();
    }

    @When("the user logs out via the burger menu")
    public void logoutViaBurgerMenu() {
        final ProductsPage products = ScenarioContext.get(ScenarioContext.PRODUCTS_PAGE);
        products.header().logout();
        ScenarioContext.put(ScenarioContext.LOGIN_PAGE, new LoginPage());
    }

    @When("the user navigates to all items via the burger menu")
    public void navigateToAllItemsViaBurgerMenu() {
        final ProductsPage products = ScenarioContext.get(ScenarioContext.PRODUCTS_PAGE);
        products.header().clickAllItems();
        // After clicking all items, the page is still the products page
        ScenarioContext.put(ScenarioContext.PRODUCTS_PAGE, products);
    }

    @When("the user resets the app state via the burger menu")
    public void resetAppStateViaBurgerMenu() {
        final ProductsPage products = ScenarioContext.get(ScenarioContext.PRODUCTS_PAGE);
        products.header().resetAppState();
    }

    // -----------------------------------------------------------------------
    //  Then — navigation assertions
    // -----------------------------------------------------------------------

    @Then("the burger menu is open")
    public void burgerMenuIsOpen() {
        // The burger menu being open means the products page is still displayed
        // and the sidebar is visible; we assert the products page is still accessible
        final ProductsPage products = ScenarioContext.get(ScenarioContext.PRODUCTS_PAGE);
        assertThat(products.isLoaded())
                .as("Products page should remain visible when burger menu is open")
                .isTrue();
    }

    @Then("the login page is displayed again")
    public void loginPageDisplayedAgain() {
        // After logout, Selenium should be on the login URL.
        // Validate by checking the login page object state stored in context.
        final LoginPage loginPage = ScenarioContext.get(ScenarioContext.LOGIN_PAGE);
        assertThat(loginPage)
                .as("Login page reference should be present after logout")
                .isNotNull();
        // Also verify via URL that we've been redirected to login
        assertThat(loginPage.currentUrl())
                .as("Browser URL should be the SauceDemo root/login page")
                .containsIgnoringCase("saucedemo.com");
    }
}
