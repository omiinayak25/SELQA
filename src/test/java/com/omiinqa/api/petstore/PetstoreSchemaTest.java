package com.omiinqa.api.petstore;

import com.omiinqa.api.AbstractApiTest;
import com.omiinqa.api.models.petstore.Category;
import com.omiinqa.api.models.petstore.Pet;
import com.omiinqa.api.models.petstore.Tag;
import com.omiinqa.api.services.PetstoreService;
import com.omiinqa.api.validator.ResponseValidator;
import com.omiinqa.api.validator.SchemaValidator;
import io.restassured.response.Response;
import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

/**
 * JSON Schema validation tests for the Petstore {@code /pet} resource.
 *
 * <p>Schema file: {@code src/test/resources/schemas/petstore-pet-schema.json} (Draft-07).
 * Tests validate that {@code GET /pet/{petId}} responses conform to the schema's
 * type, format, and required-field constraints.</p>
 *
 * <p><b>Two validation paths exercised:</b></p>
 * <ol>
 *   <li>Fluent chain via {@link ResponseValidator#matchesSchema(String)}.</li>
 *   <li>Direct call via {@link SchemaValidator#validate(Response, String)}.</li>
 * </ol>
 *
 * <p><b>Additional coverage:</b></p>
 * <ul>
 *   <li>Schema validates the {@code status} field is one of the three enum values.</li>
 *   <li>Schema validates {@code id} is a non-negative integer.</li>
 *   <li>Schema validates {@code name} is a non-empty string.</li>
 *   <li>Data-driven: all three valid statuses exercised through a DataProvider,
 *       each seed pet is retrieved and schema-validated.</li>
 * </ul>
 *
 * <p>Does NOT extend {@code BaseTest} — no browser required.</p>
 */
public class PetstoreSchemaTest extends AbstractApiTest {

    /** Classpath path to the Petstore pet JSON Schema (Draft-07). */
    private static final String PET_SCHEMA = "schemas/petstore-pet-schema.json";

    private PetstoreService petstoreService;

    /**
     * Initialises the service facade once per test class.
     */
    @BeforeClass(alwaysRun = true)
    public void setUpService() {
        petstoreService = new PetstoreService();
        log.info("PetstoreSchemaTest initialised against: {}", config.apiUrl("petstore"));
    }

    // -----------------------------------------------------------------------
    //  DataProvider
    // -----------------------------------------------------------------------

    /**
     * Supplies the three valid Petstore pet status values for data-driven schema tests.
     *
     * @return [[status]] for each valid status
     */
    @DataProvider(name = "petStatuses")
    public Object[][] petStatuses() {
        return new Object[][]{
            {"available"},
            {"pending"},
            {"sold"}
        };
    }

    // -----------------------------------------------------------------------
    //  Schema validation tests
    // -----------------------------------------------------------------------

    /**
     * GET /pet/{petId} response conforms to petstore-pet-schema.json (fluent path).
     * Seeds a pet first to guarantee a stable ID.
     */
    @Test(groups = {"api", "regression"},
          description = "GET /pet/{id} response matches petstore-pet-schema.json (fluent ResponseValidator)")
    public void getPet_responseMatchesPetSchema_fluentPath() {
        final long petId = System.currentTimeMillis();
        petstoreService.addPet(buildPet(petId, "SchemaFluentPet", "available"));

        final Response raw = petstoreService.getPetRaw(petId);

        ResponseValidator.of(raw)
                .statusCode(200)
                .matchesSchema(PET_SCHEMA);
    }

    /**
     * GET /pet/{petId} response validated via direct {@link SchemaValidator#validate} call.
     */
    @Test(groups = {"api", "regression"},
          description = "GET /pet/{id} response matches petstore-pet-schema.json (direct SchemaValidator)")
    public void getPet_responseMatchesPetSchema_directPath() {
        final long petId = System.currentTimeMillis() + 1;
        petstoreService.addPet(buildPet(petId, "SchemaDirectPet", "available"));

        final Response raw = petstoreService.getPetRaw(petId);
        ResponseValidator.of(raw).statusCode(200);

        // Direct call — alternative to fluent chain
        SchemaValidator.validate(raw, PET_SCHEMA);
    }

    /**
     * Schema validates that {@code status} is one of the enum values.
     * Data-driven across all three valid statuses.
     *
     * @param status one of {@code available}, {@code pending}, {@code sold}
     */
    @Test(groups = {"api", "regression"},
          dataProvider = "petStatuses",
          description = "Schema validates status enum constraint for each valid status value")
    public void getPet_allStatuses_schemaEnumConstraintSatisfied(final String status) {
        final long petId = System.currentTimeMillis() + status.hashCode();
        petstoreService.addPet(buildPet(petId, "EnumPet_" + status, status));

        final Response raw = petstoreService.getPetRaw(petId);
        ResponseValidator.of(raw).statusCode(200);

        // Schema enforces "enum": ["available","pending","sold"] on status field
        SchemaValidator.validate(raw, PET_SCHEMA);

        // Also assert the field value independently
        final String returnedStatus = raw.jsonPath().getString("status");
        Assertions.assertThat(returnedStatus)
                .as("status field must be one of the valid enum values")
                .isIn("available", "pending", "sold");
    }

    /**
     * Schema validates that {@code id} is a non-negative integer (minimum: 0).
     */
    @Test(groups = {"api", "regression"},
          description = "Schema validates id is a non-negative integer (minimum: 0)")
    public void getPet_idIsNonNegativeInteger_validatedBySchema() {
        final long petId = System.currentTimeMillis() + 2;
        petstoreService.addPet(buildPet(petId, "IdSchemaPet", "available"));

        final Response raw = petstoreService.getPetRaw(petId);
        ResponseValidator.of(raw)
                .statusCode(200)
                .matchesSchema(PET_SCHEMA);

        final long returnedId = raw.jsonPath().getLong("id");
        Assertions.assertThat(returnedId)
                .as("Pet id must be >= 0 per schema minimum constraint")
                .isGreaterThanOrEqualTo(0L);
    }

    /**
     * Schema validates that {@code name} is a non-empty string (minLength: 1).
     */
    @Test(groups = {"api", "regression"},
          description = "Schema validates name is a non-empty string (minLength: 1)")
    public void getPet_nameIsNonEmptyString_validatedBySchema() {
        final long petId = System.currentTimeMillis() + 3;
        final String expectedName = "NameConstraintPet";
        petstoreService.addPet(buildPet(petId, expectedName, "available"));

        final Response raw = petstoreService.getPetRaw(petId);
        ResponseValidator.of(raw)
                .statusCode(200)
                .matchesSchema(PET_SCHEMA);

        final String returnedName = raw.jsonPath().getString("name");
        Assertions.assertThat(returnedName)
                .as("Pet name must not be blank per schema minLength: 1")
                .isNotBlank();
    }

    /**
     * Schema validates that {@code photoUrls} is a JSON array.
     */
    @Test(groups = {"api", "regression"},
          description = "Schema validates photoUrls is an array type")
    public void getPet_photoUrlsIsArray_validatedBySchema() {
        final long petId = System.currentTimeMillis() + 4;
        petstoreService.addPet(buildPet(petId, "PhotoUrlPet", "available"));

        final Response raw = petstoreService.getPetRaw(petId);
        ResponseValidator.of(raw)
                .statusCode(200)
                .matchesSchema(PET_SCHEMA)
                .bodyJsonPathNotNull("photoUrls");

        final List<?> photoUrls = raw.jsonPath().getList("photoUrls");
        Assertions.assertThat(photoUrls)
                .as("photoUrls must be a JSON array")
                .isNotNull();
    }

    /**
     * Pet created with a category sub-object — schema validates category structure.
     */
    @Test(groups = {"api", "regression"},
          description = "Schema validates optional category object structure when present")
    public void getPet_withCategory_categorySchemaConstraintSatisfied() {
        final long petId = System.currentTimeMillis() + 5;
        petstoreService.addPet(buildPet(petId, "CatPet", "sold"));

        final Response raw = petstoreService.getPetRaw(petId);
        ResponseValidator.of(raw)
                .statusCode(200)
                .matchesSchema(PET_SCHEMA);

        // category.name must be a string when present
        final String categoryName = raw.jsonPath().getString("category.name");
        if (categoryName != null) {
            Assertions.assertThat(categoryName).isNotBlank();
        }
    }

    // -----------------------------------------------------------------------
    //  Private helpers
    // -----------------------------------------------------------------------

    /**
     * Constructs a representative {@link Pet} fixture including category and tag.
     *
     * @param id     the pet ID
     * @param name   the pet name
     * @param status the pet status
     * @return a fully constructed {@link Pet}
     */
    private Pet buildPet(final long id, final String name, final String status) {
        return Pet.builder()
                .id(id)
                .category(Category.builder().id(10L).name("TestCategory").build())
                .name(name)
                .photoUrls(List.of("https://example.com/schema-test.jpg"))
                .tags(List.of(Tag.builder().id(10L).name("schema-test").build()))
                .status(status)
                .build();
    }
}
