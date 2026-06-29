package com.omiinqa.api;

import com.omiinqa.api.models.dummyjson.DummyJsonLoginResponse;
import com.omiinqa.api.models.dummyjson.DummyJsonProduct;
import com.omiinqa.api.models.dummyjson.DummyJsonProductList;
import com.omiinqa.api.services.DummyJsonService;
import com.omiinqa.api.validator.ResponseValidator;
import io.restassured.response.Response;
import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

/**
 * API tests for the DummyJSON product and authentication endpoints.
 *
 * <p>Covers: product list (positive, pagination, boundary limit/skip), single
 * product retrieval, product search, category list, 404 negative case,
 * JWT-authenticated login and profile retrieval (request chaining), and
 * data-driven product ID tests.</p>
 *
 * <p>Does NOT extend {@code BaseTest} — no browser required.</p>
 */
public class DummyJsonApiTest extends AbstractApiTest {

    private DummyJsonService dummyJsonService;

    @BeforeClass(alwaysRun = true)
    public void initService() {
        dummyJsonService = new DummyJsonService();
        log.info("DummyJsonApiTest initialized against: {}", config.apiUrl("dummyjson"));
    }

    // -----------------------------------------------------------------------
    //  Products — list
    // -----------------------------------------------------------------------

    @Test(groups = {"api", "smoke"}, description = "GET /products returns 200 with non-empty product list")
    public void getProducts_defaultPage_returns200WithProducts() {
        final DummyJsonProductList list = dummyJsonService.getProducts();

        Assertions.assertThat(list).isNotNull();
        Assertions.assertThat(list.getProducts()).isNotEmpty();
        Assertions.assertThat(list.getTotal()).isGreaterThan(0);
        log.info("Total products: {}", list.getTotal());
    }

    @Test(groups = {"api", "regression"}, description = "GET /products?limit=5&skip=0 returns exactly 5 products")
    public void getProducts_limit5_returnsExactly5Products() {
        final DummyJsonProductList list = dummyJsonService.getProducts(5, 0);

        Assertions.assertThat(list.getProducts()).hasSize(5);
        Assertions.assertThat(list.getLimit()).isEqualTo(5);
    }

    @Test(groups = {"api", "regression"}, description = "GET /products?limit=1&skip=0 — boundary: single product")
    public void getProducts_limit1_returnsSingleProduct() {
        final DummyJsonProductList list = dummyJsonService.getProducts(1, 0);
        Assertions.assertThat(list.getProducts()).hasSize(1);
    }

    @Test(groups = {"api", "regression"}, description = "Product list response includes pagination metadata")
    public void getProducts_responseIncludesPaginationMetadata() {
        final DummyJsonProductList list = dummyJsonService.getProducts(10, 0);

        Assertions.assertThat(list.getSkip()).isEqualTo(0);
        Assertions.assertThat(list.getLimit()).isEqualTo(10);
    }

    @Test(groups = {"api", "regression"}, description = "GET /products skip=10 returns different products than skip=0")
    public void getProducts_differentSkip_returnsDifferentProducts() {
        final DummyJsonProductList page1 = dummyJsonService.getProducts(5, 0);
        final DummyJsonProductList page2 = dummyJsonService.getProducts(5, 5);

        Assertions.assertThat(page1.getProducts().get(0).getId())
                .isNotEqualTo(page2.getProducts().get(0).getId());
    }

    // -----------------------------------------------------------------------
    //  Products — single
    // -----------------------------------------------------------------------

    @Test(groups = {"api", "smoke"}, description = "GET /products/1 returns correct product with all required fields")
    public void getProduct_id1_returnsCompleteProductRecord() {
        final DummyJsonProduct product = dummyJsonService.getProduct(1);

        Assertions.assertThat(product.getId()).isEqualTo(1);
        Assertions.assertThat(product.getTitle()).isNotBlank();
        Assertions.assertThat(product.getPrice()).isGreaterThan(0.0);
        Assertions.assertThat(product.getCategory()).isNotBlank();
        Assertions.assertThat(product.getThumbnail()).isNotBlank();
    }

    @DataProvider(name = "validProductIds")
    public Object[][] validProductIds() {
        return new Object[][]{{1}, {5}, {10}, {25}, {100}};
    }

    @Test(groups = {"api", "regression"},
          dataProvider = "validProductIds",
          description = "GET /products/{id} returns 200 for multiple valid IDs")
    public void getProduct_dataProvider_validIds_return200(final int productId) {
        final Response raw = dummyJsonService.getProductRaw(productId);
        ResponseValidator.of(raw)
                .statusCode(200)
                .bodyJsonPath("id", productId)
                .responseTimeLessThan(5, TimeUnit.SECONDS);
    }

    @Test(groups = {"api", "regression"}, description = "GET /products/{id} — product price is positive")
    public void getProduct_validId_priceIsPositive() {
        final DummyJsonProduct product = dummyJsonService.getProduct(1);
        Assertions.assertThat(product.getPrice()).isGreaterThan(0.0);
    }

    @Test(groups = {"api", "regression"}, description = "GET /products/{id} — rating is between 0 and 5")
    public void getProduct_validId_ratingInValidRange() {
        final DummyJsonProduct product = dummyJsonService.getProduct(1);
        Assertions.assertThat(product.getRating())
                .isBetween(0.0, 5.0);
    }

    // -----------------------------------------------------------------------
    //  Products — negative
    // -----------------------------------------------------------------------

    @Test(groups = {"api", "regression"}, description = "GET /products/99999 returns 404 for non-existent product")
    public void getProduct_nonExistentId_returns404() {
        final Response raw = dummyJsonService.getProductNotFound(99999);
        ResponseValidator.of(raw).statusCode(404);
    }

    // -----------------------------------------------------------------------
    //  Products — search
    // -----------------------------------------------------------------------

    @Test(groups = {"api", "regression"}, description = "GET /products/search?q=phone returns relevant results")
    public void searchProducts_validQuery_returnsMatchingProducts() {
        final DummyJsonProductList results = dummyJsonService.searchProducts("phone");

        Assertions.assertThat(results.getProducts()).isNotEmpty();
        // Each result title or description should contain "phone" (case-insensitive)
        results.getProducts().forEach(p ->
            Assertions.assertThat(
                    p.getTitle().toLowerCase() + " " + p.getDescription().toLowerCase())
                .containsIgnoringCase("phone")
        );
    }

    @Test(groups = {"api", "regression"},
          description = "GET /products/search?q=<empty> returns all products (or zero — both valid)")
    public void searchProducts_emptyQuery_returnsResponse200() {
        final Response raw = dummyJsonService.searchProductsRaw("");
        ResponseValidator.of(raw).statusCode(200);
    }

    @Test(groups = {"api", "regression"},
          description = "GET /products/search?q=xyznotexist returns empty list for unknown term")
    public void searchProducts_unknownTerm_returnsEmptyList() {
        final DummyJsonProductList results = dummyJsonService.searchProducts("xyznotexistproduct12345");
        Assertions.assertThat(results.getProducts()).isEmpty();
        Assertions.assertThat(results.getTotal()).isEqualTo(0);
    }

    // -----------------------------------------------------------------------
    //  Categories
    // -----------------------------------------------------------------------

    @Test(groups = {"api", "regression"}, description = "GET /products/categories returns 200 with category list")
    public void getCategories_returns200WithNonEmptyList() {
        final Response raw = dummyJsonService.getCategories();
        ResponseValidator.of(raw)
                .statusCode(200)
                .bodyNotEmpty();
    }

    // -----------------------------------------------------------------------
    //  Authentication + profile (request chaining)
    // -----------------------------------------------------------------------

    @Test(groups = {"api", "smoke"}, description = "POST /auth/login returns 200 with JWT access token")
    public void login_validCredentials_returnsAccessToken() {
        final DummyJsonLoginResponse loginResponse = dummyJsonService.login("emilys", "emilyspass");

        Assertions.assertThat(loginResponse.getAccessToken()).isNotBlank();
        Assertions.assertThat(loginResponse.getUsername()).isEqualTo("emilys");
        Assertions.assertThat(loginResponse.getEmail()).isNotBlank();
    }

    @Test(groups = {"api", "regression"},
          description = "Login then GET /auth/me returns the authenticated user profile (chaining)")
    public void loginThenGetProfile_chainedRequest_profileMatchesLoginUser() {
        // Step 1 — login
        final DummyJsonLoginResponse loginResponse = dummyJsonService.login("emilys", "emilyspass");
        final String accessToken = loginResponse.getAccessToken();
        Assertions.assertThat(accessToken).isNotBlank();

        // Step 2 — use token to fetch profile
        final Response profileResponse = dummyJsonService.getAuthenticatedUser(accessToken);
        ResponseValidator.of(profileResponse)
                .statusCode(200)
                .bodyJsonPath("username", "emilys");
    }

    @Test(groups = {"api", "regression"},
          description = "GET /auth/me without token returns 401 Unauthorized")
    public void getProfile_withoutToken_returns401() {
        // Pass an empty/invalid token — DummyJSON should reject it
        final Response raw = dummyJsonService.getAuthenticatedUser("invalid-token");
        ResponseValidator.of(raw).statusCode(401);
    }
}
