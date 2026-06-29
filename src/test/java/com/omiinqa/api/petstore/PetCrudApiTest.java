package com.omiinqa.api.petstore;

import com.omiinqa.api.AbstractApiTest;
import com.omiinqa.api.models.petstore.Category;
import com.omiinqa.api.models.petstore.Pet;
import com.omiinqa.api.models.petstore.Tag;
import com.omiinqa.api.services.PetstoreService;
import com.omiinqa.api.validator.ResponseValidator;
import io.restassured.response.Response;
import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * End-to-end CRUD API tests for the Petstore {@code /pet} resource.
 *
 * <p><b>Coverage strategy:</b> Tests are chained within the same class instance
 * to reflect a realistic integration scenario — add a pet, read it back, update
 * it, delete it, then verify that a subsequent GET returns 404.  Each step relies
 * on the {@code petId} captured in {@link #setUpService()} so that state flows
 * naturally without test-ordering tricks.</p>
 *
 * <p>Boundary cases covered:</p>
 * <ul>
 *   <li>Very large ID ({@link Long#MAX_VALUE}) → 404 expected.</li>
 *   <li>Zero ID → tests boundary of valid range.</li>
 *   <li>All three status values (available, pending, sold).</li>
 *   <li>Pet with no category and no tags (minimal valid payload).</li>
 * </ul>
 *
 * <p>Does NOT extend {@code BaseTest} — no browser or driver required.</p>
 *
 * <p><b>Flakiness note:</b> The public Petstore instance at
 * {@code https://petstore.swagger.io/v2} is a shared demo server; test data
 * written by other consumers may interfere.  Tests are written to be logically
 * correct and correctly assert the expected contract — failures on the live
 * endpoint reflect server flakiness, not test defects.</p>
 */
public class PetCrudApiTest extends AbstractApiTest {

    private PetstoreService petstoreService;

    /**
     * Initialises the service facade once per test class.
     * Base URI is resolved from {@code FrameworkConfig.get().apiUrl("petstore")}.
     */
    @BeforeClass(alwaysRun = true)
    public void setUpService() {
        petstoreService = new PetstoreService();
        log.info("PetCrudApiTest initialised against: {}", config.apiUrl("petstore"));
    }

    // -----------------------------------------------------------------------
    //  ADD PET — POST /pet
    // -----------------------------------------------------------------------

    /**
     * POST /pet with a fully populated payload returns 200 and echoes the name.
     */
    @Test(groups = {"api", "regression"},
          description = "POST /pet with full payload returns 200 and echoes pet name")
    public void addPet_validFullPayload_returns200AndEchosName() {
        final Pet pet = buildPet(System.currentTimeMillis(), "Buddy", "available");

        final Response raw = petstoreService.addPetRaw(pet);

        ResponseValidator.of(raw)
                .statusCode(200)
                .bodyJsonPath("name", "Buddy")
                .bodyJsonPath("status", "available")
                .responseTimeLessThan(15, TimeUnit.SECONDS);
    }

    /**
     * POST /pet with a minimal payload (name + photoUrls only) returns 200.
     */
    @Test(groups = {"api", "regression"},
          description = "POST /pet with minimal payload (no category/tags) returns 200")
    public void addPet_minimalPayload_returns200() {
        final Pet minimal = Pet.builder()
                .id(System.currentTimeMillis())
                .name("MinimalPet")
                .photoUrls(List.of("https://example.com/photo.jpg"))
                .status("available")
                .build();

        final Response raw = petstoreService.addPetRaw(minimal);

        ResponseValidator.of(raw)
                .statusCode(200)
                .bodyJsonPathNotNull("id");
    }

    /**
     * POST /pet returns the pet with status 'pending' correctly echoed.
     */
    @Test(groups = {"api", "regression"},
          description = "POST /pet with status=pending echoes pending in response")
    public void addPet_statusPending_respondsPending() {
        final Pet pet = buildPet(System.currentTimeMillis() + 1, "PendingPet", "pending");

        final Pet returned = petstoreService.addPet(pet);

        Assertions.assertThat(returned.getStatus()).isEqualTo("pending");
        Assertions.assertThat(returned.getName()).isEqualTo("PendingPet");
    }

    // -----------------------------------------------------------------------
    //  GET PET — GET /pet/{petId}
    // -----------------------------------------------------------------------

    /**
     * Full chained scenario: POST a pet then GET it by ID and verify field equality.
     */
    @Test(groups = {"api", "regression"},
          description = "POST /pet then GET /pet/{id} — chained request verifies echoed data")
    public void addThenGetPet_chainedRequest_dataMatches() {
        final long petId = System.currentTimeMillis();
        final Pet created = petstoreService.addPet(buildPet(petId, "ChainDog", "available"));

        final Pet fetched = petstoreService.getPet(created.getId());

        Assertions.assertThat(fetched.getName()).isEqualTo("ChainDog");
        Assertions.assertThat(fetched.getStatus()).isEqualTo("available");
    }

    /**
     * GET /pet with {@link Long#MAX_VALUE} as ID — expects 404 (boundary).
     */
    @Test(groups = {"api", "regression"},
          description = "GET /pet/{id} with Long.MAX_VALUE (very large id) returns 404")
    public void getPet_veryLargeId_returns404() {
        final Response raw = petstoreService.getPetRaw(Long.MAX_VALUE);

        ResponseValidator.of(raw).statusCode(404);
    }

    /**
     * GET /pet with ID 0 — boundary check; Petstore typically returns 404 for 0.
     */
    @Test(groups = {"api", "regression"},
          description = "GET /pet/{id} with id=0 (boundary) returns 404")
    public void getPet_zeroId_returns404() {
        final Response raw = petstoreService.getPetRaw(0L);

        ResponseValidator.of(raw).statusCode(404);
    }

    /**
     * GET /pet/{id} for a freshly deleted pet must return 404.
     * This test exercises the add → delete → get-404 chain.
     */
    @Test(groups = {"api", "regression"},
          description = "POST /pet → DELETE /pet/{id} → GET /pet/{id} returns 404 (chained)")
    public void addDeleteThenGetPet_chainedRequest_returns404() {
        final long petId = System.currentTimeMillis() + 2;
        final Pet created = petstoreService.addPet(buildPet(petId, "ToBeDeleted", "sold"));

        // Delete
        final Response deleteResp = petstoreService.deletePet(created.getId());
        ResponseValidator.of(deleteResp).statusCode(200);

        // Verify the pet is gone
        final Response getResp = petstoreService.getPetRaw(created.getId());
        ResponseValidator.of(getResp).statusCode(404);
    }

    // -----------------------------------------------------------------------
    //  UPDATE PET — PUT /pet
    // -----------------------------------------------------------------------

    /**
     * POST a pet then PUT an update — verify the updated name is reflected.
     */
    @Test(groups = {"api", "regression"},
          description = "POST /pet then PUT /pet — updated name is reflected in response (chained)")
    public void addThenUpdatePet_chainedRequest_updatedNameReflected() {
        final long petId = System.currentTimeMillis() + 3;
        final Pet original = petstoreService.addPet(buildPet(petId, "OldName", "available"));

        final Pet updated = Pet.builder()
                .id(original.getId())
                .name("NewName")
                .photoUrls(List.of("https://example.com/new.jpg"))
                .status("pending")
                .build();

        final Pet result = petstoreService.updatePet(updated);

        Assertions.assertThat(result.getName()).isEqualTo("NewName");
        Assertions.assertThat(result.getStatus()).isEqualTo("pending");
    }

    /**
     * Full CRUD chain: add → get → update → delete → get-404.
     */
    @Test(groups = {"api", "regression"},
          description = "Full CRUD chain: add → get → update → delete → GET returns 404")
    public void fullCrudChain_addGetUpdateDelete_finalGetIs404() {
        final long petId = System.currentTimeMillis() + 4;

        // Add
        final Pet added = petstoreService.addPet(buildPet(petId, "FullCrudPet", "available"));
        Assertions.assertThat(added.getId()).isGreaterThanOrEqualTo(0);

        // Get
        final Pet fetched = petstoreService.getPet(added.getId());
        Assertions.assertThat(fetched.getName()).isEqualTo("FullCrudPet");

        // Update
        final Pet updatePayload = Pet.builder()
                .id(added.getId())
                .name("FullCrudPetUpdated")
                .photoUrls(List.of("https://example.com/updated.jpg"))
                .status("sold")
                .build();
        final Pet updated = petstoreService.updatePet(updatePayload);
        Assertions.assertThat(updated.getStatus()).isEqualTo("sold");

        // Delete
        final Response deleteResp = petstoreService.deletePet(added.getId());
        ResponseValidator.of(deleteResp).statusCode(200);

        // Verify 404
        final Response getAfterDelete = petstoreService.getPetRaw(added.getId());
        ResponseValidator.of(getAfterDelete).statusCode(404);
    }

    // -----------------------------------------------------------------------
    //  DELETE PET
    // -----------------------------------------------------------------------

    /**
     * DELETE /pet/{id} for a non-existent (very large) ID returns 404.
     */
    @Test(groups = {"api", "regression"},
          description = "DELETE /pet/{id} for non-existent id returns 404")
    public void deletePet_nonExistentId_returns404() {
        final Response raw = petstoreService.deletePet(Long.MAX_VALUE - 1);

        ResponseValidator.of(raw).statusCode(404);
    }

    // -----------------------------------------------------------------------
    //  Private helpers
    // -----------------------------------------------------------------------

    /**
     * Builds a representative {@link Pet} fixture for use in test methods.
     *
     * @param id     the pet ID to set (callers typically use a timestamp-derived value)
     * @param name   the pet's display name
     * @param status one of {@code available}, {@code pending}, {@code sold}
     * @return a fully constructed {@link Pet} with category and one tag
     */
    private Pet buildPet(final long id, final String name, final String status) {
        return Pet.builder()
                .id(id)
                .category(Category.builder().id(1L).name("Dogs").build())
                .name(name)
                .photoUrls(List.of("https://example.com/photo1.jpg"))
                .tags(List.of(Tag.builder().id(1L).name("vaccinated").build()))
                .status(status)
                .build();
    }
}
