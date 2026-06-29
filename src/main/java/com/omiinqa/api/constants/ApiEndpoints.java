package com.omiinqa.api.constants;

/**
 * Central registry of API endpoint path constants for all third-party services
 * used in the automation suite.
 *
 * <p><b>Design rationale:</b> Collects every URL path template into a single
 * place so that endpoint changes require a one-line edit rather than a grep
 * across dozens of test files. Path parameters are expressed as REST Assured
 * path-param tokens (e.g., {@code {id}}) and should be resolved via
 * {@code RequestSpecification.pathParam()} rather than string concatenation.</p>
 *
 * <p>Base URLs are <em>not</em> stored here; they are environment-specific and
 * are retrieved at runtime via
 * {@code FrameworkConfig.get().apiUrl(String key)}.</p>
 */
public final class ApiEndpoints {

    private ApiEndpoints() {
        // Utility class — no instantiation.
    }

    // -----------------------------------------------------------------------
    //  ReqRes (https://reqres.in/api)
    // -----------------------------------------------------------------------

    /** List users — supports query param {@code page}. */
    public static final String REQRES_USERS         = "/users";

    /** Single user by numeric ID. */
    public static final String REQRES_USER_BY_ID    = "/users/{id}";

    /** Register / login on ReqRes. */
    public static final String REQRES_REGISTER      = "/register";

    /** ReqRes login endpoint. */
    public static final String REQRES_LOGIN         = "/login";

    /** ReqRes unknown resource list. */
    public static final String REQRES_UNKNOWN       = "/unknown";

    /** ReqRes single unknown resource. */
    public static final String REQRES_UNKNOWN_BY_ID = "/unknown/{id}";

    // -----------------------------------------------------------------------
    //  Restful-Booker (https://restful-booker.herokuapp.com)
    // -----------------------------------------------------------------------

    /** Generate an auth token. */
    public static final String BOOKER_AUTH          = "/auth";

    /** Collection: create (POST) or list all IDs (GET). */
    public static final String BOOKER_BOOKING       = "/booking";

    /** Single booking by ID — GET, PUT, PATCH, DELETE. */
    public static final String BOOKER_BOOKING_BY_ID = "/booking/{id}";

    /** Ping health-check. */
    public static final String BOOKER_PING          = "/ping";

    // -----------------------------------------------------------------------
    //  DummyJSON (https://dummyjson.com)
    // -----------------------------------------------------------------------

    /** Product collection. */
    public static final String DUMMYJSON_PRODUCTS        = "/products";

    /** Single product. */
    public static final String DUMMYJSON_PRODUCT_BY_ID   = "/products/{id}";

    /** Product search — supports query param {@code q}. */
    public static final String DUMMYJSON_PRODUCTS_SEARCH = "/products/search";

    /** All product categories. */
    public static final String DUMMYJSON_CATEGORIES      = "/products/categories";

    /** DummyJSON authentication. */
    public static final String DUMMYJSON_AUTH_LOGIN      = "/auth/login";

    /** Authenticated user profile (requires Bearer token). */
    public static final String DUMMYJSON_AUTH_ME         = "/auth/me";

    /** User collection. */
    public static final String DUMMYJSON_USERS           = "/users";

    /** Single DummyJSON user. */
    public static final String DUMMYJSON_USER_BY_ID      = "/users/{id}";
}
