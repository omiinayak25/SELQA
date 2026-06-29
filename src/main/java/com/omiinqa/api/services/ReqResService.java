package com.omiinqa.api.services;

import com.omiinqa.api.builder.RequestBuilder;
import com.omiinqa.api.client.ApiClient;
import com.omiinqa.api.constants.ApiEndpoints;
import com.omiinqa.api.models.reqres.ReqResListResponse;
import com.omiinqa.api.models.reqres.ReqResSingleUserResponse;
import com.omiinqa.api.models.reqres.UserRequest;
import com.omiinqa.api.models.reqres.UserResponse;
import com.omiinqa.config.FrameworkConfig;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Facade over the ReqRes API (https://reqres.in/api).
 *
 * <p><b>Pattern:</b> Facade (GoF) — hides the mechanical details of building
 * REST Assured request specifications and calling {@link ApiClient} behind
 * intention-revealing method names.  Test classes depend only on this service
 * rather than on the HTTP layer directly, so URL changes or auth-scheme
 * changes require edits in one place only.</p>
 *
 * <p>Methods returning typed POJOs bind the response via Jackson.  Methods
 * returning raw {@link Response} are provided for scenarios where the test
 * needs access to the full HTTP response (status, headers, negative-case
 * assertions).</p>
 *
 * <p>All methods are stateless; the class can be instantiated once per test
 * class or shared across a test session.</p>
 */
public class ReqResService {

    private static final Logger LOG = LoggerFactory.getLogger(ReqResService.class);

    private final String baseUri;

    /**
     * Constructs a service wired to the {@code reqres} API URL defined in
     * {@code config.properties} (key: {@code api.reqres.url}).
     */
    public ReqResService() {
        this.baseUri = FrameworkConfig.get().apiUrl("reqres");
    }

    /**
     * Constructs a service with an explicit base URI, useful for environment
     * override in integration or contract tests.
     *
     * @param baseUri the fully-qualified base URI including scheme and host
     */
    public ReqResService(final String baseUri) {
        this.baseUri = baseUri;
    }

    // -----------------------------------------------------------------------
    //  Read operations
    // -----------------------------------------------------------------------

    /**
     * Retrieves a paginated list of users from ReqRes.
     *
     * @param page 1-based page number
     * @return the typed list envelope; never {@code null}
     */
    public ReqResListResponse listUsers(final int page) {
        LOG.info("Listing ReqRes users page={}", page);
        final Response response = ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(ApiEndpoints.REQRES_USERS)
                        .queryParam("page", page)
                        .build());
        return response.as(ReqResListResponse.class);
    }

    /**
     * Retrieves a paginated list of users and returns the raw response for
     * low-level assertions (status, headers).
     *
     * @param page 1-based page number
     * @return the raw REST Assured {@link Response}
     */
    public Response listUsersRaw(final int page) {
        LOG.info("Listing ReqRes users (raw) page={}", page);
        return ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(ApiEndpoints.REQRES_USERS)
                        .queryParam("page", page)
                        .build());
    }

    /**
     * Retrieves a single user by ID.
     *
     * @param userId the ReqRes user ID (must be 1–12 for the built-in data set)
     * @return the typed single-user envelope
     */
    public ReqResSingleUserResponse getUser(final int userId) {
        LOG.info("Getting ReqRes user id={}", userId);
        final Response response = ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(ApiEndpoints.REQRES_USER_BY_ID)
                        .pathParam("id", userId)
                        .build());
        return response.as(ReqResSingleUserResponse.class);
    }

    /**
     * Retrieves a single user by ID and returns the raw response.
     *
     * @param userId the ReqRes user ID
     * @return the raw REST Assured {@link Response}
     */
    public Response getUserRaw(final int userId) {
        LOG.info("Getting ReqRes user (raw) id={}", userId);
        return ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(ApiEndpoints.REQRES_USER_BY_ID)
                        .pathParam("id", userId)
                        .build());
    }

    // -----------------------------------------------------------------------
    //  Write operations
    // -----------------------------------------------------------------------

    /**
     * Creates a new user on ReqRes (POST /users).
     *
     * <p>ReqRes does not persist the user across requests; it echoes back
     * the submitted payload with a generated {@code id} and {@code createdAt}
     * timestamp.</p>
     *
     * @param request the user payload to create; must not be {@code null}
     * @return the typed creation response with server-assigned ID and timestamp
     */
    public UserResponse createUser(final UserRequest request) {
        LOG.info("Creating ReqRes user name={}", request.getName());
        final Response response = ApiClient.post(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(ApiEndpoints.REQRES_USERS)
                        .body(request)
                        .build());
        return response.as(UserResponse.class);
    }

    /**
     * Creates a user and returns the raw response for status/header assertions.
     *
     * @param request the user payload; must not be {@code null}
     * @return the raw REST Assured {@link Response}
     */
    public Response createUserRaw(final UserRequest request) {
        LOG.info("Creating ReqRes user (raw) name={}", request.getName());
        return ApiClient.post(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(ApiEndpoints.REQRES_USERS)
                        .body(request)
                        .build());
    }

    /**
     * Fully replaces a user with PUT /users/{id}.
     *
     * @param userId  the user ID to update
     * @param request the full replacement payload; must not be {@code null}
     * @return the typed update response with {@code updatedAt} timestamp
     */
    public UserResponse updateUser(final int userId, final UserRequest request) {
        LOG.info("Updating (PUT) ReqRes user id={}", userId);
        final Response response = ApiClient.put(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(ApiEndpoints.REQRES_USER_BY_ID)
                        .pathParam("id", userId)
                        .body(request)
                        .build());
        return response.as(UserResponse.class);
    }

    /**
     * Partially updates a user with PATCH /users/{id}.
     *
     * @param userId  the user ID to patch
     * @param request the partial update payload; must not be {@code null}
     * @return the typed update response with {@code updatedAt} timestamp
     */
    public UserResponse patchUser(final int userId, final UserRequest request) {
        LOG.info("Patching ReqRes user id={}", userId);
        final Response response = ApiClient.patch(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(ApiEndpoints.REQRES_USER_BY_ID)
                        .pathParam("id", userId)
                        .body(request)
                        .build());
        return response.as(UserResponse.class);
    }

    /**
     * Deletes a user with DELETE /users/{id}.
     *
     * @param userId the user ID to delete
     * @return the raw REST Assured {@link Response} (ReqRes returns 204 No Content)
     */
    public Response deleteUser(final int userId) {
        LOG.info("Deleting ReqRes user id={}", userId);
        return ApiClient.delete(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(ApiEndpoints.REQRES_USER_BY_ID)
                        .pathParam("id", userId)
                        .build());
    }
}
