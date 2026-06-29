package com.omiinqa.bdd.steps.domain;

import com.omiinqa.bdd.support.DomainWorld;
import com.omiinqa.reference.catalog.CatalogService;
import com.omiinqa.reference.catalog.Product;
import com.omiinqa.reference.catalog.SearchService;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the reference search domain (catalog full-text search).
 *
 * <p>All step text is prefixed with "search" to guarantee global uniqueness.
 * Outcomes are routed via {@link DomainWorld}.</p>
 */
public class SearchSteps {

    private static final String SEARCH_SVC = "searchService";
    private static final String SEARCH_RESULTS = "search.results";

    private SearchService searchService() {
        final CatalogService catalog = DomainWorld.service("catalogService", CatalogService::new);
        return DomainWorld.service(SEARCH_SVC, () -> new SearchService(catalog));
    }

    // ------------------------------------------------------------------ When

    @When("I search the catalog for {string}")
    public void searchCatalog(final String query) {
        final List<Product> results = DomainWorld.capture(() -> searchService().search(query));
        if (results != null) {
            DomainWorld.put(SEARCH_RESULTS, results);
        }
    }

    @When("I search the catalog with an empty query")
    public void searchCatalogEmptyQuery() {
        final List<Product> results = DomainWorld.capture(() -> searchService().search(""));
        if (results != null) {
            DomainWorld.put(SEARCH_RESULTS, results);
        }
    }

    // ------------------------------------------------------------------ Then

    @Then("the search returns {int} products")
    public void searchReturnsProducts(final int expected) {
        final List<Product> results = DomainWorld.get(SEARCH_RESULTS);
        assertThat(results).as("search results").isNotNull();
        assertThat(results).as("search result count").hasSize(expected);
    }

    @Then("the search results contain product {string}")
    public void searchResultsContainProduct(final String name) {
        final List<Product> results = DomainWorld.get(SEARCH_RESULTS);
        assertThat(results).as("search results").isNotNull();
        assertThat(results)
                .as("product '%s' should be in results", name)
                .extracting(Product::getName)
                .contains(name);
    }

    @Then("the search results do not contain product {string}")
    public void searchResultsDoNotContainProduct(final String name) {
        final List<Product> results = DomainWorld.get(SEARCH_RESULTS);
        assertThat(results).as("search results").isNotNull();
        assertThat(results)
                .as("product '%s' should NOT be in results", name)
                .extracting(Product::getName)
                .doesNotContain(name);
    }

    @Then("the first search result is {string}")
    public void firstSearchResultIs(final String name) {
        final List<Product> results = DomainWorld.get(SEARCH_RESULTS);
        assertThat(results).as("search results").isNotNull().isNotEmpty();
        assertThat(results.get(0).getName())
                .as("first search result")
                .isEqualTo(name);
    }

    @Then("the search results are empty")
    public void searchResultsAreEmpty() {
        final List<Product> results = DomainWorld.get(SEARCH_RESULTS);
        assertThat(results).as("search results should be empty").isEmpty();
    }
}
