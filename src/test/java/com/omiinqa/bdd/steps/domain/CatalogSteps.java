package com.omiinqa.bdd.steps.domain;

import com.omiinqa.bdd.support.DomainWorld;
import com.omiinqa.reference.catalog.CatalogService;
import com.omiinqa.reference.catalog.Product;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the reference catalog domain (product management).
 *
 * <p>All step text is prefixed with "catalog" to guarantee global uniqueness
 * across the multi-domain step registry. Outcomes are routed via
 * {@link DomainWorld} so shared {@code CommonDomainSteps} assertions work.</p>
 */
public class CatalogSteps {

    private static final String SVC = "catalogService";
    private static final String LAST_PRODUCT = "catalog.lastProduct";

    private CatalogService service() {
        return DomainWorld.service(SVC, CatalogService::new);
    }

    // ------------------------------------------------------------------ Given

    @Given("a clean catalog service")
    public void cleanCatalogService() {
        DomainWorld.put(SVC, new CatalogService());
    }

    @Given("the catalog is seeded with the standard product set")
    public void catalogSeededWithStandardSet() {
        service().seed(CatalogFixtures.standardProducts());
    }

    @Given("a catalog product {string} in category {string} brand {string} priced at {string} rated {double} and {string}")
    public void addCatalogProduct(final String name, final String category, final String brand,
                                   final String price, final double rating, final String stock) {
        final boolean inStock = "inStock".equalsIgnoreCase(stock) || "true".equalsIgnoreCase(stock);
        service().addProduct(Product.builder()
                .name(name)
                .category(category)
                .brand(brand)
                .price(new BigDecimal(price))
                .rating(rating)
                .inStock(inStock)
                .tags(List.of()));
    }

    // ------------------------------------------------------------------ When

    @When("I add a catalog product named {string} in category {string} brand {string} priced at {string}")
    public void addProduct(final String name, final String category, final String brand, final String price) {
        DomainWorld.run(() -> {
            final Product p = service().addProduct(Product.builder()
                    .name(name)
                    .category(category)
                    .brand(brand)
                    .price(new BigDecimal(price))
                    .rating(4.0)
                    .inStock(true)
                    .tags(List.of()));
            DomainWorld.put(LAST_PRODUCT, p);
        });
    }

    @When("I add a catalog product with blank name in category {string} priced at {string}")
    public void addProductBlankName(final String category, final String price) {
        DomainWorld.run(() -> service().addProduct(Product.builder()
                .name("")
                .category(category)
                .price(new BigDecimal(price))
                .rating(3.0)
                .inStock(true)
                .tags(List.of())));
    }

    @When("I add a catalog product named {string} with negative price {string}")
    public void addProductNegativePrice(final String name, final String price) {
        DomainWorld.run(() -> service().addProduct(Product.builder()
                .name(name)
                .category("General")
                .price(new BigDecimal(price))
                .rating(3.0)
                .inStock(true)
                .tags(List.of())));
    }

    @When("I look up catalog product by id {int}")
    public void lookUpCatalogById(final int id) {
        DomainWorld.run(() -> DomainWorld.put(LAST_PRODUCT, service().findById(id)));
    }

    @When("I look up the last added catalog product by id")
    public void lookUpLastAddedCatalogById() {
        final Product last = DomainWorld.get(LAST_PRODUCT);
        assertThat(last).as("a product must have been added first").isNotNull();
        DomainWorld.run(() -> DomainWorld.put(LAST_PRODUCT, service().findById(last.getId())));
    }

    // ------------------------------------------------------------------ Then

    @Then("the catalog contains {int} products")
    public void catalogContainsProducts(final int expected) {
        assertThat(service().size()).as("catalog size").isEqualTo(expected);
    }

    @Then("the catalog product name is {string}")
    public void catalogProductNameIs(final String expected) {
        final Product product = DomainWorld.get(LAST_PRODUCT);
        assertThat(product).as("last catalog product").isNotNull();
        assertThat(product.getName()).isEqualTo(expected);
    }

    @Then("the catalog product category is {string}")
    public void catalogProductCategoryIs(final String expected) {
        final Product product = DomainWorld.get(LAST_PRODUCT);
        assertThat(product).as("last catalog product").isNotNull();
        assertThat(product.getCategory()).isEqualTo(expected);
    }
}
