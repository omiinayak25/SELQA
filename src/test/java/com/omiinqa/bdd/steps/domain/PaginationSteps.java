package com.omiinqa.bdd.steps.domain;

import com.omiinqa.bdd.support.DomainWorld;
import com.omiinqa.reference.catalog.CatalogService;
import com.omiinqa.reference.catalog.Page;
import com.omiinqa.reference.catalog.PaginationService;
import com.omiinqa.reference.catalog.Product;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the reference pagination domain.
 *
 * <p>All step text is prefixed with "page" or "pagination" to guarantee global
 * uniqueness. Outcomes are routed via {@link DomainWorld}.</p>
 */
public class PaginationSteps {

    private static final String PAGE_SVC = "paginationService";
    private static final String PAGE_RESULT = "pagination.page";

    private PaginationService pageService() {
        return DomainWorld.service(PAGE_SVC, PaginationService::new);
    }

    // ------------------------------------------------------------------ When

    @When("I paginate the full catalog with page {int} and size {int}")
    public void paginateFullCatalog(final int page, final int size) {
        final CatalogService catalog = DomainWorld.service("catalogService", CatalogService::new);
        final List<Product> all = catalog.all();
        final Page<Product> result = DomainWorld.capture(() -> pageService().paginate(all, page, size));
        if (result != null) {
            DomainWorld.put(PAGE_RESULT, result);
        }
    }

    @When("I paginate an empty list with page {int} and size {int}")
    public void paginateEmptyList(final int page, final int size) {
        final Page<Product> result = DomainWorld.capture(
                () -> pageService().paginate(List.of(), page, size));
        if (result != null) {
            DomainWorld.put(PAGE_RESULT, result);
        }
    }

    @When("I paginate the full catalog with invalid page {int} and size {int}")
    public void paginateWithInvalidParams(final int page, final int size) {
        final CatalogService catalog = DomainWorld.service("catalogService", CatalogService::new);
        DomainWorld.run(() -> pageService().paginate(catalog.all(), page, size));
    }

    // ------------------------------------------------------------------ Then

    @Then("page {int} has {int} items")
    public void pageHasItems(final int pageNum, final int expectedItems) {
        final Page<?> page = DomainWorld.get(PAGE_RESULT);
        assertThat(page).as("page result").isNotNull();
        assertThat(page.page()).as("page number").isEqualTo(pageNum);
        assertThat(page.items()).as("items on page " + pageNum).hasSize(expectedItems);
    }

    @Then("the page total is {int} products")
    public void pageTotalIs(final int expectedTotal) {
        final Page<?> page = DomainWorld.get(PAGE_RESULT);
        assertThat(page).as("page result").isNotNull();
        assertThat(page.total()).as("total products").isEqualTo(expectedTotal);
    }

    @Then("the total pages is {int}")
    public void totalPagesIs(final int expectedTotalPages) {
        final Page<?> page = DomainWorld.get(PAGE_RESULT);
        assertThat(page).as("page result").isNotNull();
        assertThat(page.totalPages()).as("totalPages").isEqualTo(expectedTotalPages);
    }

    @Then("the result has next page")
    public void resultHasNextPage() {
        final Page<?> page = DomainWorld.get(PAGE_RESULT);
        assertThat(page).as("page result").isNotNull();
        assertThat(page.hasNext()).as("hasNext").isTrue();
    }

    @Then("the result has no next page")
    public void resultHasNoNextPage() {
        final Page<?> page = DomainWorld.get(PAGE_RESULT);
        assertThat(page).as("page result").isNotNull();
        assertThat(page.hasNext()).as("hasNext should be false").isFalse();
    }

    @Then("the result has previous page")
    public void resultHasPreviousPage() {
        final Page<?> page = DomainWorld.get(PAGE_RESULT);
        assertThat(page).as("page result").isNotNull();
        assertThat(page.hasPrev()).as("hasPrev").isTrue();
    }

    @Then("the result has no previous page")
    public void resultHasNoPreviousPage() {
        final Page<?> page = DomainWorld.get(PAGE_RESULT);
        assertThat(page).as("page result").isNotNull();
        assertThat(page.hasPrev()).as("hasPrev should be false").isFalse();
    }

    @Then("the page items are empty")
    public void pageItemsAreEmpty() {
        final Page<?> page = DomainWorld.get(PAGE_RESULT);
        assertThat(page).as("page result").isNotNull();
        assertThat(page.items()).as("page items should be empty").isEmpty();
    }

    @Then("the page size is {int}")
    public void pageSizeIs(final int expectedSize) {
        final Page<?> page = DomainWorld.get(PAGE_RESULT);
        assertThat(page).as("page result").isNotNull();
        assertThat(page.size()).as("page size").isEqualTo(expectedSize);
    }
}
