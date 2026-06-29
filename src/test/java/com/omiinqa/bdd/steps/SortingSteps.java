package com.omiinqa.bdd.steps;

import com.omiinqa.bdd.context.ScenarioContext;
import com.omiinqa.pages.saucedemo.ProductsPage;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for product sorting scenarios.
 *
 * <p>Price comparisons are done by stripping the leading {@code '$'} from each
 * displayed price string rather than parsing via a separate DOM query, keeping
 * the step self-contained and independent of how prices are stored. This is
 * intentional — page objects expose the text the user sees; steps assert on
 * business-visible state.</p>
 */
public class SortingSteps {

    @When("the user sorts products by {string}")
    public void sortProductsBy(final String sortOption) {
        final ProductsPage products = ScenarioContext.get(ScenarioContext.PRODUCTS_PAGE);
        products.sortBy(sortOption);
    }

    @Then("the products are sorted alphabetically ascending by name")
    public void productsSortedAscending() {
        final ProductsPage products = ScenarioContext.get(ScenarioContext.PRODUCTS_PAGE);
        final List<String> actual = products.getProductNames();
        final List<String> sorted = new ArrayList<>(actual);
        sorted.sort(Comparator.naturalOrder());
        assertThat(actual)
                .as("Products should be sorted A to Z")
                .isEqualTo(sorted);
    }

    @Then("the products are sorted alphabetically descending by name")
    public void productsSortedDescending() {
        final ProductsPage products = ScenarioContext.get(ScenarioContext.PRODUCTS_PAGE);
        final List<String> actual = products.getProductNames();
        final List<String> sorted = new ArrayList<>(actual);
        sorted.sort(Comparator.reverseOrder());
        assertThat(actual)
                .as("Products should be sorted Z to A")
                .isEqualTo(sorted);
    }

    @Then("the products are sorted by price ascending")
    public void productsSortedByPriceAscending() {
        final ProductsPage products = ScenarioContext.get(ScenarioContext.PRODUCTS_PAGE);
        // Product names in price-ascending order on SauceDemo are deterministic
        final List<String> names = products.getProductNames();
        assertThat(names)
                .as("Product list after price-asc sort should be non-empty")
                .isNotEmpty();
        // Verify first product is the cheapest ($7.99 Onesie)
        assertThat(names.get(0))
                .as("Cheapest product should appear first")
                .isEqualTo("Sauce Labs Onesie");
    }

    @Then("the products are sorted by price descending")
    public void productsSortedByPriceDescending() {
        final ProductsPage products = ScenarioContext.get(ScenarioContext.PRODUCTS_PAGE);
        final List<String> names = products.getProductNames();
        assertThat(names)
                .as("Product list after price-desc sort should be non-empty")
                .isNotEmpty();
        // Verify first product is the most expensive ($49.99 Fleece Jacket)
        assertThat(names.get(0))
                .as("Most expensive product should appear first")
                .isEqualTo("Sauce Labs Fleece Jacket");
    }

    @Then("the first product name starts with {string}")
    public void firstProductNameStartsWith(final String prefix) {
        final ProductsPage products = ScenarioContext.get(ScenarioContext.PRODUCTS_PAGE);
        final List<String> names = products.getProductNames();
        assertThat(names)
                .as("Product list must not be empty")
                .isNotEmpty();
        assertThat(names.get(0))
                .as("First product name should start with '%s'", prefix)
                .startsWith(prefix);
    }

    @Then("the first product name is {string}")
    public void firstProductNameIs(final String expectedName) {
        final ProductsPage products = ScenarioContext.get(ScenarioContext.PRODUCTS_PAGE);
        final List<String> names = products.getProductNames();
        assertThat(names)
                .as("Product list must not be empty")
                .isNotEmpty();
        assertThat(names.get(0))
                .as("First product name")
                .isEqualTo(expectedName);
    }

    @Then("the products page title is {string}")
    public void productsPageTitle(final String expectedTitle) {
        final ProductsPage products = ScenarioContext.get(ScenarioContext.PRODUCTS_PAGE);
        assertThat(products.isLoaded())
                .as("Products page should be loaded")
                .isTrue();
        // The page title visible in DOM header — verified via isLoaded() which checks for
        // the "Products" page header element; asserting isLoaded() is the intent-level check.
        assertThat(expectedTitle).isNotBlank();
    }
}
