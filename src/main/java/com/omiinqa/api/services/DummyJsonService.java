package com.omiinqa.api.services;

import com.omiinqa.api.auth.BearerTokenAuth;
import com.omiinqa.api.builder.RequestBuilder;
import com.omiinqa.api.client.ApiClient;
import com.omiinqa.api.constants.ApiEndpoints;
import com.omiinqa.api.models.dummyjson.DummyJsonLoginRequest;
import com.omiinqa.api.models.dummyjson.DummyJsonLoginResponse;
import com.omiinqa.api.models.dummyjson.DummyJsonProduct;
import com.omiinqa.api.models.dummyjson.DummyJsonProductList;
import com.omiinqa.config.FrameworkConfig;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Facade over the DummyJSON API (https://dummyjson.com).
 *
 * <p><b>Pattern:</b> Facade (GoF) — encapsulates product browsing, product
 * search, and JWT-authenticated user-profile flows behind intention-revealing
 * methods.  Demonstrates how {@link BearerTokenAuth} plugs into
 * {@link RequestBuilder} for authenticated endpoints without coupling tests
 * to HTTP header mechanics.</p>
 *
 * <p>Stateless; safe to instantiate once per test class.</p>
 */
public class DummyJsonService {

    private static final Logger LOG = LoggerFactory.getLogger(DummyJsonService.class);

    private final String baseUri;

    /**
     * Constructs a service wired to the {@code dummyjson} URL in config.
     */
    public DummyJsonService() {
        this.baseUri = FrameworkConfig.get().apiUrl("dummyjson");
    }

    /**
     * Constructs a service with an explicit base URI.
     *
     * @param baseUri the fully-qualified base URI
     */
    public DummyJsonService(final String baseUri) {
        this.baseUri = baseUri;
    }

    // -----------------------------------------------------------------------
    //  Authentication
    // -----------------------------------------------------------------------

    /**
     * Authenticates against DummyJSON and returns a typed login response
     * containing the JWT access token.
     *
     * @param username DummyJSON username (e.g., {@code "emilys"})
     * @param password plaintext password (e.g., {@code "emilyspass"})
     * @return the typed login response with {@code accessToken}
     */
    public DummyJsonLoginResponse login(final String username, final String password) {
        LOG.info("Logging in to DummyJSON as user={}", username);
        final DummyJsonLoginRequest request = DummyJsonLoginRequest.builder()
                .username(username)
                .password(password)
                .build();
        final Response response = ApiClient.post(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(ApiEndpoints.DUMMYJSON_AUTH_LOGIN)
                        .body(request)
                        .build());
        return response.as(DummyJsonLoginResponse.class);
    }

    /**
     * Retrieves the authenticated user's profile.  Requires a valid JWT token
     * obtained from {@link #login(String, String)}.
     *
     * @param accessToken the JWT access token from {@link DummyJsonLoginResponse#getAccessToken()}
     * @return the raw response (profile object mirrors the login response structure)
     */
    public Response getAuthenticatedUser(final String accessToken) {
        LOG.info("Getting authenticated DummyJSON user profile");
        return ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(ApiEndpoints.DUMMYJSON_AUTH_ME)
                        .auth(new BearerTokenAuth(accessToken))
                        .build());
    }

    // -----------------------------------------------------------------------
    //  Products
    // -----------------------------------------------------------------------

    /**
     * Retrieves the full product list (first page, default limit).
     *
     * @return the typed paginated product list
     */
    public DummyJsonProductList getProducts() {
        LOG.info("Fetching DummyJSON products (first page)");
        final Response response = ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(ApiEndpoints.DUMMYJSON_PRODUCTS)
                        .build());
        return response.as(DummyJsonProductList.class);
    }

    /**
     * Retrieves a paginated product list.
     *
     * @param limit  max records per page
     * @param skip   records to skip (offset)
     * @return the typed paginated product list
     */
    public DummyJsonProductList getProducts(final int limit, final int skip) {
        LOG.info("Fetching DummyJSON products limit={} skip={}", limit, skip);
        final Response response = ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(ApiEndpoints.DUMMYJSON_PRODUCTS)
                        .queryParam("limit", limit)
                        .queryParam("skip", skip)
                        .build());
        return response.as(DummyJsonProductList.class);
    }

    /**
     * Retrieves a single product by ID.
     *
     * @param productId the DummyJSON product ID (1-based)
     * @return the typed product record
     */
    public DummyJsonProduct getProduct(final int productId) {
        LOG.info("Fetching DummyJSON product id={}", productId);
        final Response response = ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(ApiEndpoints.DUMMYJSON_PRODUCT_BY_ID)
                        .pathParam("id", productId)
                        .build());
        return response.as(DummyJsonProduct.class);
    }

    /**
     * Retrieves a single product and returns the raw response.
     *
     * @param productId the DummyJSON product ID
     * @return the raw REST Assured {@link Response}
     */
    public Response getProductRaw(final int productId) {
        LOG.info("Fetching DummyJSON product (raw) id={}", productId);
        return ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(ApiEndpoints.DUMMYJSON_PRODUCT_BY_ID)
                        .pathParam("id", productId)
                        .build());
    }

    /**
     * Searches for products by keyword.
     *
     * @param query the search term
     * @return the typed paginated search result list
     */
    public DummyJsonProductList searchProducts(final String query) {
        LOG.info("Searching DummyJSON products query='{}'", query);
        final Response response = ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(ApiEndpoints.DUMMYJSON_PRODUCTS_SEARCH)
                        .queryParam("q", query)
                        .build());
        return response.as(DummyJsonProductList.class);
    }

    /**
     * Searches for products and returns the raw response.
     *
     * @param query the search term
     * @return the raw REST Assured {@link Response}
     */
    public Response searchProductsRaw(final String query) {
        LOG.info("Searching DummyJSON products (raw) query='{}'", query);
        return ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(ApiEndpoints.DUMMYJSON_PRODUCTS_SEARCH)
                        .queryParam("q", query)
                        .build());
    }

    /**
     * Retrieves all product categories.
     *
     * @return the raw response containing a JSON array of category objects
     */
    public Response getCategories() {
        LOG.info("Fetching DummyJSON product categories");
        return ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(ApiEndpoints.DUMMYJSON_CATEGORIES)
                        .build());
    }

    /**
     * Attempts to fetch a product with an invalid ID — intended for negative
     * testing that the API returns 404.
     *
     * @param invalidId a non-existent product ID
     * @return the raw response (expected to be 404)
     */
    public Response getProductNotFound(final int invalidId) {
        LOG.info("Fetching DummyJSON product (expected 404) id={}", invalidId);
        return ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(ApiEndpoints.DUMMYJSON_PRODUCT_BY_ID)
                        .pathParam("id", invalidId)
                        .build());
    }
}
