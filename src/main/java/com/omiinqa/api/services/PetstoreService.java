package com.omiinqa.api.services;

import com.omiinqa.api.builder.RequestBuilder;
import com.omiinqa.api.client.ApiClient;
import com.omiinqa.api.models.petstore.Order;
import com.omiinqa.api.models.petstore.Pet;
import com.omiinqa.api.models.petstore.PetstoreUser;
import com.omiinqa.config.FrameworkConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Facade over the Swagger Petstore v2 API
 * (<a href="https://petstore.swagger.io/v2">https://petstore.swagger.io/v2</a>).
 *
 * <p><b>Pattern:</b> Facade (GoF) — encapsulates the full HTTP plumbing for all
 * three Petstore resource groups (pet, store, user) behind a single, intention-
 * revealing API.  Test classes depend on this facade exclusively; they never
 * touch {@link RequestBuilder} or {@link ApiClient} directly, which keeps tests
 * readable and decoupled from transport concerns.</p>
 *
 * <p><b>Design decisions:</b></p>
 * <ul>
 *   <li>Every method comes in two forms where relevant:
 *     <ol>
 *       <li>A <em>typed</em> variant that deserialises the response body into a
 *           domain POJO (e.g., {@link #getPet(long)}) — used when the caller
 *           needs field-level assertions.</li>
 *       <li>A <em>raw</em> variant suffixed with {@code Raw} that returns the
 *           unmodified {@link Response} — used for status-code assertions,
 *           schema validation, and negative-case tests where a 4xx/5xx is
 *           expected.</li>
 *     </ol>
 *   </li>
 *   <li>Path constants are inlined here (rather than added to {@code ApiEndpoints})
 *       because Petstore is a self-contained domain and adding petstore constants
 *       to the shared endpoint file would require modifying existing infrastructure,
 *       which is prohibited by the Foundation Contract.</li>
 *   <li>The base URI is resolved once in the constructor from
 *       {@code FrameworkConfig.get().apiUrl("petstore")} (maps to
 *       {@code https://petstore.swagger.io/v2}).</li>
 * </ul>
 *
 * <p><b>Thread safety:</b> The service holds only immutable state ({@code baseUri});
 * safe to share across parallel test methods within the same class.</p>
 */
public class PetstoreService {

    private static final Logger LOG = LoggerFactory.getLogger(PetstoreService.class);

    // -----------------------------------------------------------------------
    //  Petstore path templates
    // -----------------------------------------------------------------------

    /** Add a new pet or update an existing one via PUT. */
    private static final String PATH_PET            = "/pet";

    /** Single pet by ID — GET, POST (update), DELETE. */
    private static final String PATH_PET_BY_ID      = "/pet/{petId}";

    /** Find pets by status — GET with query param {@code status}. */
    private static final String PATH_PET_BY_STATUS  = "/pet/findByStatus";

    /** Store inventory map — GET returns a status→count map. */
    private static final String PATH_INVENTORY      = "/store/inventory";

    /** Place an order (POST) on the store. */
    private static final String PATH_STORE_ORDER    = "/store/order";

    /** Single store order by ID — GET or DELETE. */
    private static final String PATH_ORDER_BY_ID    = "/store/order/{orderId}";

    /** Create a new user account. */
    private static final String PATH_USER           = "/user";

    /** Single user by username — GET, PUT, DELETE. */
    private static final String PATH_USER_BY_NAME   = "/user/{username}";

    // -----------------------------------------------------------------------
    //  State
    // -----------------------------------------------------------------------

    private final String baseUri;

    // -----------------------------------------------------------------------
    //  Construction
    // -----------------------------------------------------------------------

    /**
     * Constructs a service wired to the {@code petstore} URL from framework config.
     *
     * <p>Resolves to {@code https://petstore.swagger.io/v2} by default.</p>
     */
    public PetstoreService() {
        this.baseUri = FrameworkConfig.get().apiUrl("petstore");
        LOG.debug("PetstoreService initialised with baseUri={}", this.baseUri);
    }

    /**
     * Constructs a service with an explicit base URI — useful for pointing at
     * a local mock or a staging Petstore instance.
     *
     * @param baseUri the fully-qualified base URI, e.g.
     *                {@code "https://petstore.swagger.io/v2"}; must not be blank
     */
    public PetstoreService(final String baseUri) {
        this.baseUri = baseUri;
        LOG.debug("PetstoreService initialised with explicit baseUri={}", this.baseUri);
    }

    // -----------------------------------------------------------------------
    //  Pet — Add / Update
    // -----------------------------------------------------------------------

    /**
     * Adds a new pet via {@code POST /pet}.
     *
     * <p>The Petstore server returns the created pet with its server-assigned
     * (or caller-supplied) {@code id} in the response body.</p>
     *
     * @param pet the pet payload to create; must not be {@code null}
     * @return the typed {@link Pet} as returned by the server (may differ from input)
     */
    public Pet addPet(final Pet pet) {
        LOG.info("POST /pet name={}", pet.getName());
        final Response response = ApiClient.post(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(PATH_PET)
                        .contentType(ContentType.JSON)
                        .body(pet)
                        .build());
        return response.as(Pet.class);
    }

    /**
     * Adds a new pet and returns the raw {@link Response}.
     *
     * <p>Use this variant for status-code assertions (e.g., 200) and schema
     * validation without requiring a successful deserialisation.</p>
     *
     * @param pet the pet payload to create; must not be {@code null}
     * @return the unmodified REST Assured {@link Response}
     */
    public Response addPetRaw(final Pet pet) {
        LOG.info("POST /pet (raw) name={}", pet.getName());
        return ApiClient.post(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(PATH_PET)
                        .contentType(ContentType.JSON)
                        .body(pet)
                        .build());
    }

    /**
     * Updates an existing pet via {@code PUT /pet}.
     *
     * <p>The request body must include the pet's {@code id} to identify the
     * record to update.  The entire pet document is replaced.</p>
     *
     * @param pet the updated pet payload; must include a valid {@code id}
     * @return the typed {@link Pet} as confirmed by the server
     */
    public Pet updatePet(final Pet pet) {
        LOG.info("PUT /pet id={} name={}", pet.getId(), pet.getName());
        final Response response = ApiClient.put(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(PATH_PET)
                        .contentType(ContentType.JSON)
                        .body(pet)
                        .build());
        return response.as(Pet.class);
    }

    /**
     * Updates an existing pet and returns the raw {@link Response}.
     *
     * @param pet the updated pet payload; must include a valid {@code id}
     * @return the unmodified REST Assured {@link Response}
     */
    public Response updatePetRaw(final Pet pet) {
        LOG.info("PUT /pet (raw) id={} name={}", pet.getId(), pet.getName());
        return ApiClient.put(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(PATH_PET)
                        .contentType(ContentType.JSON)
                        .body(pet)
                        .build());
    }

    // -----------------------------------------------------------------------
    //  Pet — Read
    // -----------------------------------------------------------------------

    /**
     * Retrieves a single pet by its numeric ID via {@code GET /pet/{petId}}.
     *
     * @param petId the pet's numeric identifier
     * @return the typed {@link Pet} record
     */
    public Pet getPet(final long petId) {
        LOG.info("GET /pet/{}", petId);
        final Response response = ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(PATH_PET_BY_ID)
                        .pathParam("petId", petId)
                        .build());
        return response.as(Pet.class);
    }

    /**
     * Retrieves a single pet and returns the raw {@link Response}.
     *
     * <p>Use this variant for status-code and schema assertions, including
     * 404-not-found negative tests.</p>
     *
     * @param petId the pet's numeric identifier
     * @return the unmodified REST Assured {@link Response}
     */
    public Response getPetRaw(final long petId) {
        LOG.info("GET /pet/{} (raw)", petId);
        return ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(PATH_PET_BY_ID)
                        .pathParam("petId", petId)
                        .build());
    }

    /**
     * Finds pets by availability status via {@code GET /pet/findByStatus?status=<value>}.
     *
     * <p>Valid status values per the Petstore spec: {@code available},
     * {@code pending}, {@code sold}.  The server returns a JSON array of
     * matching {@link Pet} objects.</p>
     *
     * @param status the status filter value; must not be {@code null}
     * @return the raw {@link Response} containing a JSON array of pets
     */
    public Response findByStatus(final String status) {
        LOG.info("GET /pet/findByStatus?status={}", status);
        return ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(PATH_PET_BY_STATUS)
                        .queryParam("status", status)
                        .build());
    }

    // -----------------------------------------------------------------------
    //  Pet — Delete
    // -----------------------------------------------------------------------

    /**
     * Deletes a pet via {@code DELETE /pet/{petId}}.
     *
     * <p>The Petstore API returns a 200 with an {@code ApiResponse} body on
     * success, and 404 when the pet does not exist.</p>
     *
     * @param petId the pet's numeric identifier to delete
     * @return the raw {@link Response} (200 on success, 404 if absent)
     */
    public Response deletePet(final long petId) {
        LOG.info("DELETE /pet/{}", petId);
        return ApiClient.delete(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(PATH_PET_BY_ID)
                        .pathParam("petId", petId)
                        .build());
    }

    // -----------------------------------------------------------------------
    //  Store — Inventory
    // -----------------------------------------------------------------------

    /**
     * Retrieves the store inventory map via {@code GET /store/inventory}.
     *
     * <p>Returns a JSON object whose keys are status strings (e.g.,
     * {@code "available"}, {@code "pending"}, {@code "sold"}) and whose values
     * are integer counts of pets in each status.  The map is expected to be
     * non-empty on the public Petstore instance.</p>
     *
     * @return the raw {@link Response} containing the inventory map
     */
    public Response getInventory() {
        LOG.info("GET /store/inventory");
        return ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(PATH_INVENTORY)
                        .build());
    }

    // -----------------------------------------------------------------------
    //  Store — Order
    // -----------------------------------------------------------------------

    /**
     * Places a new store order via {@code POST /store/order}.
     *
     * @param order the order payload; {@code petId} and {@code quantity} are required
     * @return the typed {@link Order} as confirmed by the server
     */
    public Order placeOrder(final Order order) {
        LOG.info("POST /store/order petId={}", order.getPetId());
        final Response response = ApiClient.post(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(PATH_STORE_ORDER)
                        .contentType(ContentType.JSON)
                        .body(order)
                        .build());
        return response.as(Order.class);
    }

    /**
     * Places a new store order and returns the raw {@link Response}.
     *
     * @param order the order payload; must not be {@code null}
     * @return the unmodified REST Assured {@link Response}
     */
    public Response placeOrderRaw(final Order order) {
        LOG.info("POST /store/order (raw) petId={}", order.getPetId());
        return ApiClient.post(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(PATH_STORE_ORDER)
                        .contentType(ContentType.JSON)
                        .body(order)
                        .build());
    }

    /**
     * Retrieves a store order by ID via {@code GET /store/order/{orderId}}.
     *
     * @param orderId the order identifier (1–10 are pre-loaded on the public instance)
     * @return the typed {@link Order}
     */
    public Order getOrder(final long orderId) {
        LOG.info("GET /store/order/{}", orderId);
        final Response response = ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(PATH_ORDER_BY_ID)
                        .pathParam("orderId", orderId)
                        .build());
        return response.as(Order.class);
    }

    /**
     * Retrieves a store order by ID and returns the raw {@link Response}.
     *
     * <p>Use this variant for 4xx negative-case assertions.</p>
     *
     * @param orderId the order identifier
     * @return the unmodified REST Assured {@link Response}
     */
    public Response getOrderRaw(final long orderId) {
        LOG.info("GET /store/order/{} (raw)", orderId);
        return ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(PATH_ORDER_BY_ID)
                        .pathParam("orderId", orderId)
                        .build());
    }

    /**
     * Deletes a store order via {@code DELETE /store/order/{orderId}}.
     *
     * @param orderId the identifier of the order to delete
     * @return the raw {@link Response} (200 on success, 404 if absent)
     */
    public Response deleteOrder(final long orderId) {
        LOG.info("DELETE /store/order/{}", orderId);
        return ApiClient.delete(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(PATH_ORDER_BY_ID)
                        .pathParam("orderId", orderId)
                        .build());
    }

    // -----------------------------------------------------------------------
    //  User
    // -----------------------------------------------------------------------

    /**
     * Creates a new user account via {@code POST /user}.
     *
     * <p>The Petstore server returns an {@code ApiResponse} envelope on success
     * rather than the user object; callers should follow up with
     * {@link #getUser(String)} to verify the created account.</p>
     *
     * @param user the user payload; {@code username} is required for subsequent lookups
     * @return the raw {@link Response} (200 + ApiResponse body on success)
     */
    public Response createUser(final PetstoreUser user) {
        LOG.info("POST /user username={}", user.getUsername());
        return ApiClient.post(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(PATH_USER)
                        .contentType(ContentType.JSON)
                        .body(user)
                        .build());
    }

    /**
     * Retrieves a user by username via {@code GET /user/{username}}.
     *
     * @param username the user's login handle
     * @return the typed {@link PetstoreUser} record
     */
    public PetstoreUser getUser(final String username) {
        LOG.info("GET /user/{}", username);
        final Response response = ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(PATH_USER_BY_NAME)
                        .pathParam("username", username)
                        .build());
        return response.as(PetstoreUser.class);
    }

    /**
     * Retrieves a user by username and returns the raw {@link Response}.
     *
     * <p>Use this variant for status-code assertions and 404 negative-case tests.</p>
     *
     * @param username the user's login handle
     * @return the unmodified REST Assured {@link Response}
     */
    public Response getUserRaw(final String username) {
        LOG.info("GET /user/{} (raw)", username);
        return ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(PATH_USER_BY_NAME)
                        .pathParam("username", username)
                        .build());
    }

    /**
     * Deletes a user account via {@code DELETE /user/{username}}.
     *
     * @param username the user's login handle to delete
     * @return the raw {@link Response} (200 on success, 404 if absent)
     */
    public Response deleteUser(final String username) {
        LOG.info("DELETE /user/{}", username);
        return ApiClient.delete(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(PATH_USER_BY_NAME)
                        .pathParam("username", username)
                        .build());
    }
}
