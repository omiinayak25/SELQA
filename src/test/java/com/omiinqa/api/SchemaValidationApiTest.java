package com.omiinqa.api;

import com.omiinqa.api.models.booking.Booking;
import com.omiinqa.api.models.booking.BookingDates;
import com.omiinqa.api.models.booking.BookingResponse;
import com.omiinqa.api.models.reqres.UserRequest;
import com.omiinqa.api.services.BookingService;
import com.omiinqa.api.services.DummyJsonService;
import com.omiinqa.api.services.ReqResService;
import com.omiinqa.api.validator.ResponseValidator;
import com.omiinqa.api.validator.SchemaValidator;
import io.restassured.response.Response;
import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * JSON Schema validation tests covering ReqRes, Restful-Booker, and DummyJSON responses.
 *
 * <p>Schema files live in {@code src/test/resources/schemas/} and are validated
 * via REST Assured's {@code JsonSchemaValidator.matchesJsonSchemaInClasspath()}
 * (backed by the Everit schema library).  Draft-07 schemas are used throughout.</p>
 *
 * <p>This class demonstrates two validation paths:</p>
 * <ol>
 *   <li>Inline via {@link ResponseValidator#matchesSchema(String)} — fluent chain.</li>
 *   <li>Direct via {@link SchemaValidator#validate(Response, String)} — explicit call.</li>
 * </ol>
 *
 * <p>Does NOT extend {@code BaseTest} — no browser required.</p>
 */
public class SchemaValidationApiTest extends AbstractApiTest {

    private ReqResService   reqResService;
    private BookingService  bookingService;
    private DummyJsonService dummyJsonService;

    @BeforeClass(alwaysRun = true)
    public void initServices() {
        reqResService    = new ReqResService();
        bookingService   = new BookingService();
        dummyJsonService = new DummyJsonService();
        log.info("SchemaValidationApiTest services initialized");
    }

    // -----------------------------------------------------------------------
    //  ReqRes schemas
    // -----------------------------------------------------------------------

    @Test(groups = {"api", "regression"},
          description = "GET /users/2 response matches reqres-user-schema.json (Draft-07)")
    public void getUser_responseMatchesJsonSchema() {
        final Response response = reqResService.getUserRaw(2);
        ResponseValidator.of(response)
                .statusCode(200)
                .matchesSchema("schemas/reqres-user-schema.json");
    }

    @Test(groups = {"api", "regression"},
          description = "GET /users/1 — schema validates id is integer >= 1")
    public void getUser_id1_schemaIdConstraintSatisfied() {
        final Response response = reqResService.getUserRaw(1);
        // Validates schema AND confirms minimum id constraint
        ResponseValidator.of(response)
                .statusCode(200)
                .matchesSchema("schemas/reqres-user-schema.json")
                .bodyJsonPathNotNull("data.id");

        final int id = response.jsonPath().getInt("data.id");
        Assertions.assertThat(id).isGreaterThanOrEqualTo(1);
    }

    @Test(groups = {"api", "regression"},
          description = "POST /users response matches reqres-create-user-schema.json")
    public void createUser_responseMatchesCreateUserSchema() {
        final UserRequest request = UserRequest.builder()
                .name("Schema Validator")
                .job("quality engineer")
                .build();

        final Response response = reqResService.createUserRaw(request);
        ResponseValidator.of(response)
                .statusCode(201)
                .matchesSchema("schemas/reqres-create-user-schema.json");
    }

    @Test(groups = {"api", "regression"},
          description = "GET /users/5 — validate email format via schema")
    public void getUser_emailFieldConformsToSchema() {
        final Response response = reqResService.getUserRaw(5);
        // Schema enforces email format — validation will fail if malformed
        ResponseValidator.of(response)
                .statusCode(200)
                .matchesSchema("schemas/reqres-user-schema.json");
    }

    @Test(groups = {"api", "regression"},
          description = "GET /users/3 — avatar URL format validated by schema")
    public void getUser_avatarUrlConformsToSchema() {
        final Response response = reqResService.getUserRaw(3);
        ResponseValidator.of(response)
                .statusCode(200)
                .matchesSchema("schemas/reqres-user-schema.json");

        // Additionally assert avatar looks like a URL
        final String avatar = response.jsonPath().getString("data.avatar");
        Assertions.assertThat(avatar).startsWith("http");
    }

    // -----------------------------------------------------------------------
    //  Restful-Booker booking schema
    // -----------------------------------------------------------------------

    @Test(groups = {"api", "regression"},
          description = "GET /booking/{id} response matches booking-schema.json")
    public void getBooking_responseMatchesBookingSchema() {
        // Create a booking first to have a stable ID
        final Booking booking = Booking.builder()
                .firstName("Schema")
                .lastName("Test")
                .totalPrice(123)
                .depositPaid(true)
                .bookingDates(BookingDates.builder()
                        .checkin("2025-07-01")
                        .checkout("2025-07-07")
                        .build())
                .additionalNeeds("Late checkout")
                .build();

        final BookingResponse created = bookingService.createBooking(booking);
        final int bookingId = created.getBookingId();

        final Response response = bookingService.getBookingRaw(bookingId);
        ResponseValidator.of(response)
                .statusCode(200)
                .matchesSchema("schemas/booking-schema.json");
    }

    @Test(groups = {"api", "regression"},
          description = "booking-schema.json validates checkin/checkout as YYYY-MM-DD pattern")
    public void getBooking_datePattern_validatedBySchema() {
        final Booking booking = Booking.builder()
                .firstName("Date")
                .lastName("Pattern")
                .totalPrice(50)
                .depositPaid(false)
                .bookingDates(BookingDates.builder()
                        .checkin("2025-12-24")
                        .checkout("2025-12-31")
                        .build())
                .build();

        final BookingResponse created = bookingService.createBooking(booking);
        final Response response = bookingService.getBookingRaw(created.getBookingId());

        // Direct SchemaValidator call (alternative to ResponseValidator fluent chain)
        SchemaValidator.validate(response, "schemas/booking-schema.json");

        // Verify date strings satisfy the pattern independently
        final String checkin = response.jsonPath().getString("bookingdates.checkin");
        Assertions.assertThat(checkin).matches("\\d{4}-\\d{2}-\\d{2}");
    }

    @Test(groups = {"api", "regression"},
          description = "booking-schema.json enforces depositpaid as boolean type")
    public void getBooking_depositPaidIsBoolean_validatedBySchema() {
        final Booking booking = Booking.builder()
                .firstName("Bool")
                .lastName("Check")
                .totalPrice(0)
                .depositPaid(true)
                .bookingDates(BookingDates.builder()
                        .checkin("2025-03-01")
                        .checkout("2025-03-05")
                        .build())
                .build();

        final BookingResponse created = bookingService.createBooking(booking);
        final Response response = bookingService.getBookingRaw(created.getBookingId());

        ResponseValidator.of(response)
                .statusCode(200)
                .matchesSchema("schemas/booking-schema.json");

        final boolean depositPaid = response.jsonPath().getBoolean("depositpaid");
        Assertions.assertThat(depositPaid).isTrue();
    }

    // -----------------------------------------------------------------------
    //  DummyJSON — inline schema field assertions
    // -----------------------------------------------------------------------

    @Test(groups = {"api", "regression"},
          description = "DummyJSON product fields satisfy expected types (schema-like field validation)")
    public void getProduct_fieldTypesConformToExpectedSchema() {
        final Response response = dummyJsonService.getProductRaw(1);

        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPathNotNull("id")
                .bodyJsonPathNotNull("title")
                .bodyJsonPathNotNull("price")
                .bodyJsonPathNotNull("category")
                .bodyJsonPathNotNull("thumbnail");

        // Type assertions — equivalent to schema type validation
        final int id = response.jsonPath().getInt("id");
        final double price = response.jsonPath().getDouble("price");
        Assertions.assertThat(id).isGreaterThan(0);
        Assertions.assertThat(price).isGreaterThan(0.0);
    }

    @Test(groups = {"api", "regression"},
          description = "ReqRes paginated list response contains required envelope fields")
    public void listUsers_envelopeFieldsPresent_schemaLikeValidation() {
        final Response response = reqResService.listUsersRaw(1);

        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPathNotNull("page")
                .bodyJsonPathNotNull("per_page")
                .bodyJsonPathNotNull("total")
                .bodyJsonPathNotNull("total_pages")
                .bodyJsonPathNotNull("data")
                .bodyJsonPathNotNull("support");
    }
}
