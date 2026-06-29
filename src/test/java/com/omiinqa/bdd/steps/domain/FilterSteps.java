package com.omiinqa.bdd.steps.domain;

import com.omiinqa.bdd.support.DomainWorld;
import com.omiinqa.reference.catalog.CatalogService;
import com.omiinqa.reference.catalog.FilterService;
import com.omiinqa.reference.catalog.Product;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the reference filter domain (catalog filtering).
 *
 * <p>All step text is prefixed with "filter" to guarantee global uniqueness.
 * The filter steps store their results in the scenario world so assertion steps
 * can read them. Outcomes are routed via {@link DomainWorld}.</p>
 */
public class FilterSteps {

    private static final String FILTER_SVC = "filterService";
    private static final String FILTER_RESULTS = "filter.results";

    private FilterService filterService() {
        final CatalogService catalog = DomainWorld.service("catalogService", CatalogService::new);
        return DomainWorld.service(FILTER_SVC, () -> new FilterService(catalog));
    }

    // ------------------------------------------------------------------ When

    @When("I filter the catalog by category {string}")
    public void filterByCategory(final String category) {
        final List<Product> results = DomainWorld.capture(
                () -> filterService().filter(category, null, null, null, -1, null));
        if (results != null) {
            DomainWorld.put(FILTER_RESULTS, results);
        }
    }

    @When("I filter the catalog by brand {string}")
    public void filterByBrand(final String brand) {
        final List<Product> results = DomainWorld.capture(
                () -> filterService().filter(null, brand, null, null, -1, null));
        if (results != null) {
            DomainWorld.put(FILTER_RESULTS, results);
        }
    }

    @When("I filter the catalog by price between {string} and {string}")
    public void filterByPriceRange(final String min, final String max) {
        final List<Product> results = DomainWorld.capture(
                () -> filterService().filter(null, null,
                        new BigDecimal(min), new BigDecimal(max), -1, null));
        if (results != null) {
            DomainWorld.put(FILTER_RESULTS, results);
        }
    }

    @When("I filter the catalog by price between {string} and {string} inverted")
    public void filterByPriceRangeInverted(final String min, final String max) {
        DomainWorld.run(() -> filterService().filter(null, null,
                new BigDecimal(min), new BigDecimal(max), -1, null));
    }

    @When("I filter the catalog by minimum rating {double}")
    public void filterByMinRating(final double minRating) {
        final List<Product> results = DomainWorld.capture(
                () -> filterService().filter(null, null, null, null, minRating, null));
        if (results != null) {
            DomainWorld.put(FILTER_RESULTS, results);
        }
    }

    @When("I filter the catalog to in-stock products only")
    public void filterToInStockOnly() {
        final List<Product> results = DomainWorld.capture(
                () -> filterService().filter(null, null, null, null, -1, true));
        if (results != null) {
            DomainWorld.put(FILTER_RESULTS, results);
        }
    }

    @When("I filter the catalog to out-of-stock products only")
    public void filterToOutOfStockOnly() {
        final List<Product> results = DomainWorld.capture(
                () -> filterService().filter(null, null, null, null, -1, false));
        if (results != null) {
            DomainWorld.put(FILTER_RESULTS, results);
        }
    }

    @When("I filter the catalog by category {string} and brand {string}")
    public void filterByCategoryAndBrand(final String category, final String brand) {
        final List<Product> results = DomainWorld.capture(
                () -> filterService().filter(category, brand, null, null, -1, null));
        if (results != null) {
            DomainWorld.put(FILTER_RESULTS, results);
        }
    }

    @When("I filter the catalog by category {string} in-stock and price between {string} and {string}")
    public void filterByCategoryInStockAndPrice(final String category, final String min, final String max) {
        final List<Product> results = DomainWorld.capture(
                () -> filterService().filter(category, null,
                        new BigDecimal(min), new BigDecimal(max), -1, true));
        if (results != null) {
            DomainWorld.put(FILTER_RESULTS, results);
        }
    }

    // ------------------------------------------------------------------ Then

    @Then("the filter returns {int} products")
    public void filterReturnsProducts(final int expected) {
        final List<Product> results = DomainWorld.get(FILTER_RESULTS);
        assertThat(results).as("filter results").isNotNull();
        assertThat(results).as("filter result count").hasSize(expected);
    }

    @Then("the filter results contain product {string}")
    public void filterResultsContainProduct(final String name) {
        final List<Product> results = DomainWorld.get(FILTER_RESULTS);
        assertThat(results).as("filter results").isNotNull();
        assertThat(results)
                .extracting(Product::getName)
                .as("filter results should contain '%s'", name)
                .contains(name);
    }

    @Then("the filter results do not contain product {string}")
    public void filterResultsDoNotContainProduct(final String name) {
        final List<Product> results = DomainWorld.get(FILTER_RESULTS);
        assertThat(results).as("filter results").isNotNull();
        assertThat(results)
                .extracting(Product::getName)
                .as("filter results should NOT contain '%s'", name)
                .doesNotContain(name);
    }

    @Then("all filter results are in category {string}")
    public void allFilterResultsInCategory(final String category) {
        final List<Product> results = DomainWorld.get(FILTER_RESULTS);
        assertThat(results).as("filter results").isNotNull();
        assertThat(results)
                .as("all results should be in category '%s'", category)
                .allMatch(p -> category.equalsIgnoreCase(p.getCategory()),
                        "category = " + category);
    }

    @Then("all filter results are from brand {string}")
    public void allFilterResultsFromBrand(final String brand) {
        final List<Product> results = DomainWorld.get(FILTER_RESULTS);
        assertThat(results).as("filter results").isNotNull();
        assertThat(results)
                .as("all results should be from brand '%s'", brand)
                .allMatch(p -> brand.equalsIgnoreCase(p.getBrand()),
                        "brand = " + brand);
    }

    @Then("all filter results are in stock")
    public void allFilterResultsAreInStock() {
        final List<Product> results = DomainWorld.get(FILTER_RESULTS);
        assertThat(results).as("filter results").isNotNull();
        assertThat(results)
                .as("all results should be in stock")
                .allMatch(Product::isInStock, "inStock = true");
    }

    @Then("all filter results are out of stock")
    public void allFilterResultsAreOutOfStock() {
        final List<Product> results = DomainWorld.get(FILTER_RESULTS);
        assertThat(results).as("filter results").isNotNull();
        assertThat(results)
                .as("all results should be out of stock")
                .allMatch(p -> !p.isInStock(), "inStock = false");
    }

    @Then("all filter results have price between {string} and {string}")
    public void allFilterResultsHavePriceBetween(final String min, final String max) {
        final List<Product> results = DomainWorld.get(FILTER_RESULTS);
        assertThat(results).as("filter results").isNotNull();
        final BigDecimal minPrice = new BigDecimal(min);
        final BigDecimal maxPrice = new BigDecimal(max);
        assertThat(results)
                .as("all results price in [%s, %s]", min, max)
                .allMatch(p -> p.getPrice().compareTo(minPrice) >= 0
                                && p.getPrice().compareTo(maxPrice) <= 0,
                        "price in range");
    }

    @Then("all filter results have rating at least {double}")
    public void allFilterResultsHaveRatingAtLeast(final double minRating) {
        final List<Product> results = DomainWorld.get(FILTER_RESULTS);
        assertThat(results).as("filter results").isNotNull();
        assertThat(results)
                .as("all results rating >= %s", minRating)
                .allMatch(p -> p.getRating() >= minRating, "rating >= " + minRating);
    }

    @Then("the filter results are empty")
    public void filterResultsAreEmpty() {
        final List<Product> results = DomainWorld.get(FILTER_RESULTS);
        assertThat(results).as("filter results should be empty").isEmpty();
    }
}
