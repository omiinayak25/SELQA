package com.omiinqa.api.petstore;

import com.omiinqa.api.AbstractApiTest;
import com.omiinqa.api.models.petstore.Order;
import com.omiinqa.api.models.petstore.Pet;
import com.omiinqa.api.services.PetstoreService;
import com.omiinqa.api.validator.ResponseValidator;
import io.restassured.response.Response;
import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

/**
 * API tests for the Petstore {@code /store} resource group.
 *
 * <p><b>Coverage:</b></p>
 * <ul>
 *   <li>{@code GET /store/inventory} — returns a non-empty status→count map.</li>
 *   <li>{@code POST /store/order} → {@code GET /store/order/{id}} → {@code DELETE /store/order/{id}}
 *       chained scenario, verifying the full order lifecycle.</li>
 *   <li>{@code GET /store/order/{id}} with an invalid ID returns 404.</li>
 *   <li>Boundary: order with {@link Long#MAX_VALUE} as order ID on GET returns 404.</li>
 *   <li>Order payload round-trip: asserts that {@code petId} and {@code quantity}
 *       are echoed correctly in the created order response.</li>
 * </ul>
 *
 * <p>Does NOT extend {@code BaseTest} — no browser required.</p>
 */
public class StoreOrderApiTest extends AbstractApiTest {

    private PetstoreService petstoreService;

    /**
     * Initialises the service facade once per test class.
     */
    @BeforeClass(alwaysRun = true)
    public void setUpService() {
        petstoreService = new PetstoreService();
        log.info("StoreOrderApiTest initialised against: {}", config.apiUrl("petstore"));
    }

    // -----------------------------------------------------------------------
    //  GET /store/inventory
    // -----------------------------------------------------------------------

    /**
     * GET /store/inventory returns 200 with a non-empty map body.
     */
    @Test(groups = {"api", "regression"},
          description = "GET /store/inventory returns 200 and a non-empty inventory map")
    public void getInventory_returns200AndNonEmptyMap() {
        final Response raw = petstoreService.getInventory();

        ResponseValidator.of(raw)
                .statusCode(200)
                .bodyNotEmpty();

        // The response is a flat map of status-string → count; at least one entry required
        final Map<String, ?> inventory = raw.jsonPath().getMap("$");
        Assertions.assertThat(inventory)
                .as("Inventory map must not be empty on the public Petstore instance")
                .isNotEmpty();
    }

    /**
     * GET /store/inventory map contains at least one numeric (non-negative) count value.
     */
    @Test(groups = {"api", "regression"},
          description = "GET /store/inventory map values are non-negative integers")
    public void getInventory_mapValuesAreNonNegative() {
        final Response raw = petstoreService.getInventory();
        ResponseValidator.of(raw).statusCode(200);

        final Map<String, Integer> inventory = raw.jsonPath().getMap("$", String.class, Integer.class);
        inventory.values().forEach(count ->
                Assertions.assertThat(count)
                        .as("Inventory count for each status must be >= 0")
                        .isGreaterThanOrEqualTo(0));
    }

    // -----------------------------------------------------------------------
    //  POST /store/order
    // -----------------------------------------------------------------------

    /**
     * POST /store/order with a valid payload returns 200 and echoes {@code petId}.
     */
    @Test(groups = {"api", "regression"},
          description = "POST /store/order with valid payload returns 200 and echoes petId")
    public void placeOrder_validPayload_returns200AndEchosPetId() {
        final long petId = seedPetAndGetId();
        final Order order = buildOrder(petId, 1);

        final Response raw = petstoreService.placeOrderRaw(order);

        ResponseValidator.of(raw)
                .statusCode(200)
                .bodyJsonPathNotNull("id")
                .bodyJsonPath("petId", (int) petId)
                .bodyJsonPath("quantity", 1);
    }

    /**
     * POST /store/order — typed response has correct {@code petId} and {@code status}.
     */
    @Test(groups = {"api", "regression"},
          description = "POST /store/order typed response has petId and status=placed")
    public void placeOrder_typedResponse_petIdAndStatusCorrect() {
        final long petId = seedPetAndGetId();
        final Order placed = petstoreService.placeOrder(buildOrder(petId, 2));

        Assertions.assertThat(placed.getPetId()).isEqualTo(petId);
        Assertions.assertThat(placed.getQuantity()).isEqualTo(2);
    }

    // -----------------------------------------------------------------------
    //  GET /store/order/{orderId}
    // -----------------------------------------------------------------------

    /**
     * Chained: POST order → GET by ID → verify petId matches.
     */
    @Test(groups = {"api", "regression"},
          description = "POST /store/order then GET /store/order/{id} — chained petId matches")
    public void placeOrderThenGetOrder_chainedRequest_petIdMatches() {
        final long petId = seedPetAndGetId();
        final Order placed = petstoreService.placeOrder(buildOrder(petId, 1));
        final long orderId = placed.getId();

        final Order fetched = petstoreService.getOrder(orderId);

        Assertions.assertThat(fetched.getPetId()).isEqualTo(petId);
        Assertions.assertThat(fetched.getId()).isEqualTo(orderId);
    }

    /**
     * GET /store/order/{id} for a non-existent ID returns 404.
     */
    @Test(groups = {"api", "regression"},
          description = "GET /store/order/{id} for non-existent id returns 404")
    public void getOrder_nonExistentId_returns404() {
        final Response raw = petstoreService.getOrderRaw(Long.MAX_VALUE);

        ResponseValidator.of(raw).statusCode(404);
    }

    /**
     * GET /store/order with ID 0 (boundary lower bound) — Petstore returns 404.
     */
    @Test(groups = {"api", "regression"},
          description = "GET /store/order/{id} with id=0 (boundary) returns 404")
    public void getOrder_idZero_returns404() {
        final Response raw = petstoreService.getOrderRaw(0L);

        ResponseValidator.of(raw).statusCode(404);
    }

    // -----------------------------------------------------------------------
    //  DELETE /store/order/{orderId}
    // -----------------------------------------------------------------------

    /**
     * Full order lifecycle: POST order → GET order (verify) → DELETE → GET returns 404.
     */
    @Test(groups = {"api", "regression"},
          description = "Full order lifecycle: place → get → delete → GET returns 404 (chained)")
    public void placeGetDeleteOrder_chainedLifecycle_finalGetIs404() {
        final long petId = seedPetAndGetId();
        final Order placed = petstoreService.placeOrder(buildOrder(petId, 1));
        final long orderId = placed.getId();

        // Verify existence
        final Order fetched = petstoreService.getOrder(orderId);
        Assertions.assertThat(fetched.getId()).isEqualTo(orderId);

        // Delete
        final Response deleteResp = petstoreService.deleteOrder(orderId);
        ResponseValidator.of(deleteResp).statusCode(200);

        // Verify 404
        final Response getAfterDelete = petstoreService.getOrderRaw(orderId);
        ResponseValidator.of(getAfterDelete).statusCode(404);
    }

    /**
     * DELETE /store/order/{id} for a non-existent ID returns 404.
     */
    @Test(groups = {"api", "regression"},
          description = "DELETE /store/order/{id} for non-existent id returns 404")
    public void deleteOrder_nonExistentId_returns404() {
        final Response raw = petstoreService.deleteOrder(Long.MAX_VALUE - 1);

        ResponseValidator.of(raw).statusCode(404);
    }

    // -----------------------------------------------------------------------
    //  Private helpers
    // -----------------------------------------------------------------------

    /**
     * Seeds the server with a minimal available pet and returns its assigned ID.
     * This guarantees the order has a valid {@code petId} reference.
     *
     * @return the server-confirmed pet ID
     */
    private long seedPetAndGetId() {
        final Pet pet = Pet.builder()
                .id(System.currentTimeMillis())
                .name("OrderTestPet")
                .photoUrls(List.of("https://example.com/order-pet.jpg"))
                .status("available")
                .build();
        final Pet created = petstoreService.addPet(pet);
        return created.getId();
    }

    /**
     * Constructs a minimal valid {@link Order} payload.
     *
     * @param petId    the pet to order
     * @param quantity number of units
     * @return a ready-to-POST order
     */
    private Order buildOrder(final long petId, final int quantity) {
        return Order.builder()
                .petId(petId)
                .quantity(quantity)
                .shipDate("2026-07-01T10:00:00.000Z")
                .status("placed")
                .complete(false)
                .build();
    }
}
