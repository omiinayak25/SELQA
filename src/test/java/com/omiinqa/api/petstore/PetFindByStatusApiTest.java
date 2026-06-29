package com.omiinqa.api.petstore;

import com.omiinqa.api.AbstractApiTest;
import com.omiinqa.api.models.petstore.Category;
import com.omiinqa.api.models.petstore.Pet;
import com.omiinqa.api.services.PetstoreService;
import com.omiinqa.api.validator.ResponseValidator;
import io.restassured.response.Response;
import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Data-driven API tests for the Petstore {@code GET /pet/findByStatus} endpoint.
 *
 * <p><b>Coverage:</b></p>
 * <ul>
 *   <li>All three valid status enum values ({@code available}, {@code pending},
 *       {@code sold}) exercised via a {@link DataProvider}.</li>
 *   <li>For each status, asserts that every pet in the returned array has the
 *       requested status — this is the core invariant of the endpoint.</li>
 *   <li>Verifies the response is a non-empty JSON array (the public instance
 *       always has pets in every status category).</li>
 *   <li>Boundary: invalid / unknown status value returns a 2xx (Petstore returns
 *       200 with an empty array for unknown status values).</li>
 * </ul>
 *
 * <p>Does NOT extend {@code BaseTest} — no browser required.</p>
 */
public class PetFindByStatusApiTest extends AbstractApiTest {

    private PetstoreService petstoreService;

    /**
     * Initialises the service facade once per class.
     */
    @BeforeClass(alwaysRun = true)
    public void setUpService() {
        petstoreService = new PetstoreService();
        log.info("PetFindByStatusApiTest initialised against: {}", config.apiUrl("petstore"));
    }

    // -----------------------------------------------------------------------
    //  DataProviders
    // -----------------------------------------------------------------------

    /**
     * Provides the three valid Petstore status enum values.
     *
     * @return two-dimensional array: [[status]] for each valid status
     */
    @DataProvider(name = "validStatuses")
    public Object[][] validStatuses() {
        return new Object[][]{
            {"available"},
            {"pending"},
            {"sold"}
        };
    }

    // -----------------------------------------------------------------------
    //  Data-driven status tests
    // -----------------------------------------------------------------------

    /**
     * GET /pet/findByStatus?status={status} returns 200 for every valid status value.
     *
     * @param status one of {@code available}, {@code pending}, {@code sold}
     */
    @Test(groups = {"api", "regression"},
          dataProvider = "validStatuses",
          description = "GET /pet/findByStatus returns 200 for each valid status enum value")
    public void findByStatus_validStatus_returns200(final String status) {
        final Response raw = petstoreService.findByStatus(status);

        ResponseValidator.of(raw)
                .statusCode(200)
                .bodyNotEmpty();
    }

    /**
     * Verifies that every pet returned for the given status actually has that status.
     *
     * <p>This is the primary invariant of the endpoint: the server must not return
     * pets with a different status than was requested.</p>
     *
     * @param status one of {@code available}, {@code pending}, {@code sold}
     */
    @Test(groups = {"api", "regression"},
          dataProvider = "validStatuses",
          description = "All pets returned by findByStatus match the requested status")
    public void findByStatus_validStatus_allReturnedPetsHaveRequestedStatus(final String status) {
        final Response raw = petstoreService.findByStatus(status);
        ResponseValidator.of(raw).statusCode(200);

        final List<String> statuses = raw.jsonPath().getList("status");

        // Every element in the array must carry the requested status
        Assertions.assertThat(statuses)
                .as("All returned pets must have status='%s'", status)
                .isNotEmpty()
                .allMatch(s -> status.equals(s),
                          "every pet status equals '" + status + "'");
    }

    // -----------------------------------------------------------------------
    //  Additional coverage
    // -----------------------------------------------------------------------

    /**
     * GET /pet/findByStatus?status=available — response time is acceptable.
     */
    @Test(groups = {"api", "regression"},
          description = "findByStatus=available responds within 15 seconds")
    public void findByStatus_available_respondsWithinThreshold() {
        final Response raw = petstoreService.findByStatus("available");

        ResponseValidator.of(raw)
                .statusCode(200)
                .responseTimeLessThan(15, java.util.concurrent.TimeUnit.SECONDS);
    }

    /**
     * GET /pet/findByStatus?status=available — returned array contains at least one pet.
     */
    @Test(groups = {"api", "regression"},
          description = "findByStatus=available returns at least one pet on the public instance")
    public void findByStatus_available_returnsAtLeastOnePet() {
        final Response raw = petstoreService.findByStatus("available");
        ResponseValidator.of(raw).statusCode(200);

        final List<?> pets = raw.jsonPath().getList("$");
        Assertions.assertThat(pets)
                .as("Expected at least one available pet on the public instance")
                .isNotEmpty();
    }

    /**
     * Seed the server with a pet having status 'sold', then verify findByStatus=sold
     * returns that pet (presence check, not exact count).
     */
    @Test(groups = {"api", "regression"},
          description = "After seeding a sold pet, findByStatus=sold contains a pet with status=sold")
    public void findByStatus_sold_afterSeeding_containsSoldPet() {
        // Seed a sold pet to guarantee at least one exists
        final Pet soldPet = Pet.builder()
                .id(System.currentTimeMillis())
                .name("SoldPetSeed")
                .photoUrls(List.of("https://example.com/sold.jpg"))
                .status("sold")
                .category(Category.builder().id(2L).name("Cats").build())
                .build();
        petstoreService.addPet(soldPet);

        // Now query
        final Response raw = petstoreService.findByStatus("sold");
        ResponseValidator.of(raw).statusCode(200);

        final List<String> statuses = raw.jsonPath().getList("status");
        Assertions.assertThat(statuses)
                .as("At least one sold pet must be present after seeding")
                .isNotEmpty()
                .allMatch("sold"::equals, "all statuses == sold");
    }

    /**
     * GET /pet/findByStatus?status=pending returns a JSON array (may be empty on a fresh server).
     */
    @Test(groups = {"api", "regression"},
          description = "findByStatus=pending returns 200 with a JSON array body")
    public void findByStatus_pending_returns200AndArray() {
        final Response raw = petstoreService.findByStatus("pending");
        ResponseValidator.of(raw).statusCode(200);

        // Response must be a JSON array — getList("$") must not throw
        final List<?> pets = raw.jsonPath().getList("$");
        Assertions.assertThat(pets).isNotNull();
    }

    /**
     * GET /pet/findByStatus with an unknown status value — Petstore returns 200 + empty array.
     * Boundary: confirms the endpoint does not error on unexpected enum values.
     */
    @Test(groups = {"api", "regression"},
          description = "findByStatus with unknown status value returns 200 (Petstore ignores unknown enum)")
    public void findByStatus_unknownStatus_returns200() {
        final Response raw = petstoreService.findByStatus("unknown_status_xyz");

        // Petstore v2 returns 200 with an empty or ignored-value array
        ResponseValidator.of(raw).statusCode(200);
    }
}
