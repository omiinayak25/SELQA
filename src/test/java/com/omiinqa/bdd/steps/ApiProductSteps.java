package com.omiinqa.bdd.steps;

import com.omiinqa.api.builder.RequestBuilder;
import com.omiinqa.api.client.ApiClient;
import com.omiinqa.api.constants.ApiEndpoints;
import com.omiinqa.api.models.dummyjson.DummyJsonLoginRequest;
import com.omiinqa.api.services.DummyJsonService;
import com.omiinqa.api.validator.ResponseValidator;
import com.omiinqa.bdd.context.ScenarioContext;
import com.omiinqa.config.FrameworkConfig;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Step definitions for DummyJSON product API scenarios.
 *
 * <p>Covers product listing, pagination, single-product fetch, search, category
 * listing, and authentication flows via {@link DummyJsonService}. All
 * {@link Response} objects are stored in {@link ScenarioContext} under
 * {@link ApiUserSteps#API_RESPONSE_KEY} so that the shared assertion steps in
 * {@link CommonApiAssertionSteps} (e.g. {@code "the API response status is {int}"})
 * are available without duplication.</p>
 */
public class ApiProductSteps {

    private static final Logger LOG = LoggerFactory.getLogger(ApiProductSteps.class);

    private final DummyJsonService dummyJsonService = new DummyJsonService();

    // -----------------------------------------------------------------------
    //  When — product list / search / categories
    // -----------------------------------------------------------------------

    @When("I request all DummyJSON products")
    public void requestAllDummyJsonProducts() {
        LOG.info("BDD: GET DummyJSON /products");
        final Response resp = ApiClient.get(
                new RequestBuilder()
                        .baseUri(FrameworkConfig.get().apiUrl("dummyjson"))
                        .basePath(ApiEndpoints.DUMMYJSON_PRODUCTS)
                        .build());
        ScenarioContext.put(ApiUserSteps.API_RESPONSE_KEY, resp);
    }

    @When("I request DummyJSON product with id {int}")
    public void requestDummyJsonProductById(final int productId) {
        LOG.info("BDD: GET DummyJSON /products/{}", productId);
        final Response resp = dummyJsonService.getProductRaw(productId);
        ScenarioContext.put(ApiUserSteps.API_RESPONSE_KEY, resp);
    }

    @When("I search DummyJSON products for {string}")
    public void searchDummyJsonProductsFor(final String term) {
        LOG.info("BDD: GET DummyJSON /products/search?q={}", term);
        final Response resp = dummyJsonService.searchProductsRaw(term);
        ScenarioContext.put(ApiUserSteps.API_RESPONSE_KEY, resp);
    }

    @When("I request DummyJSON product categories")
    public void requestDummyJsonCategories() {
        LOG.info("BDD: GET DummyJSON /products/categories");
        final Response resp = dummyJsonService.getCategories();
        ScenarioContext.put(ApiUserSteps.API_RESPONSE_KEY, resp);
    }

    @When("I request DummyJSON products with limit {int} and skip {int}")
    public void requestDummyJsonProductsPaginated(final int limit, final int skip) {
        LOG.info("BDD: GET DummyJSON /products?limit={}&skip={}", limit, skip);
        final Response resp = ApiClient.get(
                new RequestBuilder()
                        .baseUri(FrameworkConfig.get().apiUrl("dummyjson"))
                        .basePath(ApiEndpoints.DUMMYJSON_PRODUCTS)
                        .queryParam("limit", limit)
                        .queryParam("skip", skip)
                        .build());
        ScenarioContext.put(ApiUserSteps.API_RESPONSE_KEY, resp);
    }

    @When("I authenticate to DummyJSON with username {string} and password {string}")
    public void authenticateToDummyJson(final String username, final String password) {
        LOG.info("BDD: POST DummyJSON /auth/login username={}", username);
        // Use raw ApiClient so both success (200) and error (400/401) are captured as Response.
        final Response resp = ApiClient.post(
                new RequestBuilder()
                        .baseUri(FrameworkConfig.get().apiUrl("dummyjson"))
                        .basePath(ApiEndpoints.DUMMYJSON_AUTH_LOGIN)
                        .body(DummyJsonLoginRequest.builder()
                                .username(username)
                                .password(password)
                                .build())
                        .build());
        ScenarioContext.put(ApiUserSteps.API_RESPONSE_KEY, resp);
    }

    // -----------------------------------------------------------------------
    //  Then — DummyJSON-specific JSON path assertions
    // -----------------------------------------------------------------------

    @Then("the DummyJSON product list JSON path {string} is not null")
    public void dummyJsonProductListJsonPathNotNull(final String path) {
        final Response resp = ScenarioContext.get(ApiUserSteps.API_RESPONSE_KEY);
        ResponseValidator.of(resp).bodyJsonPathNotNull(path);
    }

    @Then("the DummyJSON product JSON path {string} is not null")
    public void dummyJsonProductJsonPathNotNull(final String path) {
        final Response resp = ScenarioContext.get(ApiUserSteps.API_RESPONSE_KEY);
        ResponseValidator.of(resp).bodyJsonPathNotNull(path);
    }

    @Then("the categories response body is not empty")
    public void categoriesResponseBodyNotEmpty() {
        final Response resp = ScenarioContext.get(ApiUserSteps.API_RESPONSE_KEY);
        ResponseValidator.of(resp).bodyNotEmpty();
    }

    @Then("the auth response contains an access token")
    public void authResponseContainsAccessToken() {
        final Response resp = ScenarioContext.get(ApiUserSteps.API_RESPONSE_KEY);
        ResponseValidator.of(resp).bodyJsonPathNotNull("accessToken");
    }
}
