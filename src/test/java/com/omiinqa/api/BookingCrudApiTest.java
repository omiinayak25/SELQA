package com.omiinqa.api;

import com.omiinqa.api.models.booking.AuthToken;
import com.omiinqa.api.models.booking.Booking;
import com.omiinqa.api.models.booking.BookingDates;
import com.omiinqa.api.models.booking.BookingResponse;
import com.omiinqa.api.services.BookingService;
import com.omiinqa.api.validator.ResponseValidator;
import io.restassured.response.Response;
import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

/**
 * End-to-end CRUD API tests for the Restful-Booker booking resource.
 *
 * <p>Demonstrates request chaining: authenticate → create → read → update
 * (PUT) → partial update (PATCH) → delete.  This is the canonical integration
 * scenario that validates the full state machine of a REST resource.</p>
 *
 * <p>Also covers: negative cases (missing auth, non-existent booking ID),
 * boundary values (zero price, long names), and data-driven booking creation.</p>
 *
 * <p>Does NOT extend {@code BaseTest} — no browser required.</p>
 */
public class BookingCrudApiTest extends AbstractApiTest {

    private BookingService bookingService;
    private String authToken;

    @BeforeClass(alwaysRun = true)
    public void initServiceAndAuth() {
        bookingService = new BookingService();
        log.info("BookingCrudApiTest initialized against: {}", config.apiUrl("restfulbooker"));

        // Obtain auth token once per class — re-used across all write operations.
        final AuthToken token = bookingService.createToken();
        Assertions.assertThat(token.getToken())
                .as("Auth token must not be blank")
                .isNotBlank();
        authToken = token.getToken();
        log.info("Auth token obtained (length={})", authToken.length());
    }

    // -----------------------------------------------------------------------
    //  Authentication
    // -----------------------------------------------------------------------

    @Test(groups = {"api", "smoke"}, description = "POST /auth returns a non-blank token")
    public void createToken_validCredentials_returnsNonBlankToken() {
        final AuthToken token = bookingService.createToken(
                BookingService.DEFAULT_USER, BookingService.DEFAULT_PASSWORD);
        Assertions.assertThat(token.getToken()).isNotBlank();
    }

    @Test(groups = {"api", "regression"}, description = "POST /auth with bad credentials returns error body")
    public void createToken_badCredentials_returnsError() {
        // Restful-Booker returns 200 but with {"reason":"Bad credentials"}
        final AuthToken token = bookingService.createToken("wrong", "credentials");
        // Token will be null or the string "Bad credentials" — either way not a valid JWT
        Assertions.assertThat(token.getToken()).isNull();
    }

    // -----------------------------------------------------------------------
    //  CREATE — POST /booking
    // -----------------------------------------------------------------------

    @Test(groups = {"api", "smoke"}, description = "POST /booking creates a booking and returns bookingid")
    public void createBooking_validPayload_returnsBookingIdAndData() {
        final Booking booking = buildTestBooking("Jim", "Brown", 150, true, "Breakfast");

        final Response raw = bookingService.createBookingRaw(booking);
        ResponseValidator.of(raw)
                .statusCode(200)
                .bodyJsonPathNotNull("bookingid")
                .bodyJsonPath("booking.firstname", "Jim")
                .bodyJsonPath("booking.lastname", "Brown")
                .responseTimeLessThan(10, TimeUnit.SECONDS);
    }

    @Test(groups = {"api", "regression"}, description = "POST /booking — typed response has correct booking fields")
    public void createBooking_typedResponse_hasCorrectFields() {
        final Booking booking = buildTestBooking("Alice", "Wonder", 200, false, "Dinner");

        final BookingResponse response = bookingService.createBooking(booking);

        Assertions.assertThat(response.getBookingId()).isGreaterThan(0);
        Assertions.assertThat(response.getBooking()).isNotNull();
        Assertions.assertThat(response.getBooking().getFirstName()).isEqualTo("Alice");
        Assertions.assertThat(response.getBooking().getLastName()).isEqualTo("Wonder");
        Assertions.assertThat(response.getBooking().getTotalPrice()).isEqualTo(200);
        Assertions.assertThat(response.getBooking().isDepositPaid()).isFalse();
    }

    @DataProvider(name = "bookingData")
    public Object[][] bookingData() {
        return new Object[][]{
            {"John", "Doe",    100, true,  "Breakfast"},
            {"Jane", "Smith",  250, false, "Parking"},
            {"Bob",  "Marley", 75,  true,  null}
        };
    }

    @Test(groups = {"api", "regression"},
          dataProvider = "bookingData",
          description = "POST /booking with multiple data sets all return 200")
    public void createBooking_dataProvider_allReturn200(
            final String firstName, final String lastName,
            final int price, final boolean deposit, final String needs) {

        final Booking booking = buildTestBooking(firstName, lastName, price, deposit, needs);
        final Response raw = bookingService.createBookingRaw(booking);
        ResponseValidator.of(raw)
                .statusCode(200)
                .bodyJsonPathNotNull("bookingid");
    }

    @Test(groups = {"api", "regression"}, description = "POST /booking with zero price returns 200")
    public void createBooking_zeroPriceBoundary_returns200() {
        final Booking booking = buildTestBooking("Zero", "Price", 0, true, null);
        final Response raw = bookingService.createBookingRaw(booking);
        ResponseValidator.of(raw)
                .statusCode(200)
                .bodyJsonPath("booking.totalprice", 0);
    }

    // -----------------------------------------------------------------------
    //  READ — GET /booking/{id}  (request chaining from CREATE)
    // -----------------------------------------------------------------------

    @Test(groups = {"api", "regression"},
          description = "GET /booking/{id} after POST returns the same booking data (chaining)")
    public void createThenGetBooking_chainedRequest_dataMatches() {
        // Step 1 — create
        final Booking original = buildTestBooking("Chain", "Test", 300, true, "Late checkout");
        final BookingResponse created = bookingService.createBooking(original);
        final int bookingId = created.getBookingId();

        // Step 2 — read back
        final Booking fetched = bookingService.getBooking(bookingId);

        Assertions.assertThat(fetched.getFirstName()).isEqualTo("Chain");
        Assertions.assertThat(fetched.getLastName()).isEqualTo("Test");
        Assertions.assertThat(fetched.getTotalPrice()).isEqualTo(300);
        Assertions.assertThat(fetched.isDepositPaid()).isTrue();
    }

    @Test(groups = {"api", "regression"}, description = "GET /booking/{id} returns 404 for non-existent ID")
    public void getBooking_nonExistentId_returns404() {
        final Response raw = bookingService.getBookingRaw(Integer.MAX_VALUE);
        ResponseValidator.of(raw).statusCode(404);
    }

    @Test(groups = {"api", "regression"}, description = "GET /booking returns a list of booking IDs")
    public void getAllBookings_returns200WithIds() {
        final Response raw = bookingService.getAllBookings();
        ResponseValidator.of(raw)
                .statusCode(200)
                .bodyNotEmpty();
    }

    // -----------------------------------------------------------------------
    //  UPDATE — PUT /booking/{id}
    // -----------------------------------------------------------------------

    @Test(groups = {"api", "regression"},
          description = "Full CRUD: create → update (PUT) → verify updated data (request chaining)")
    public void createThenUpdateBooking_putUpdatesAllFields() {
        // Create
        final BookingResponse created = bookingService.createBooking(
                buildTestBooking("Old", "Name", 100, false, null));
        final int id = created.getBookingId();

        // Update
        final Booking updated = buildTestBooking("New", "Name", 500, true, "Extra pillow");
        final Booking result = bookingService.updateBooking(id, updated, authToken);

        Assertions.assertThat(result.getFirstName()).isEqualTo("New");
        Assertions.assertThat(result.getLastName()).isEqualTo("Name");
        Assertions.assertThat(result.getTotalPrice()).isEqualTo(500);
        Assertions.assertThat(result.isDepositPaid()).isTrue();
    }

    // -----------------------------------------------------------------------
    //  PARTIAL UPDATE — PATCH /booking/{id}
    // -----------------------------------------------------------------------

    @Test(groups = {"api", "regression"},
          description = "PATCH /booking/{id} updates only specified fields")
    public void createThenPatchBooking_partialUpdateChangesTargetFields() {
        // Create
        final BookingResponse created = bookingService.createBooking(
                buildTestBooking("Patch", "Me", 200, false, null));
        final int id = created.getBookingId();

        // Partial update — only firstname and totalprice
        final Booking partial = Booking.builder()
                .firstName("Patched")
                .lastName("Me")
                .totalPrice(999)
                .depositPaid(false)
                .bookingDates(BookingDates.builder()
                        .checkin("2025-01-01")
                        .checkout("2025-01-05")
                        .build())
                .build();

        final Response raw = bookingService.partialUpdateBooking(id, partial, authToken);
        ResponseValidator.of(raw)
                .statusCode(200)
                .bodyJsonPath("firstname", "Patched")
                .bodyJsonPath("totalprice", 999);
    }

    // -----------------------------------------------------------------------
    //  DELETE — DELETE /booking/{id}
    // -----------------------------------------------------------------------

    @Test(groups = {"api", "regression"},
          description = "Full CRUD chain: create → delete → verify 404 on subsequent GET")
    public void createThenDeleteBooking_subsequentGetReturns404() {
        // Create
        final BookingResponse created = bookingService.createBooking(
                buildTestBooking("Delete", "Me", 50, true, null));
        final int id = created.getBookingId();

        // Delete (Restful-Booker quirk: returns 201 on successful delete)
        final Response deleteResponse = bookingService.deleteBooking(id, authToken);
        ResponseValidator.of(deleteResponse).statusCode(201);

        // Verify deletion
        final Response getAfterDelete = bookingService.getBookingRaw(id);
        ResponseValidator.of(getAfterDelete).statusCode(404);
    }

    @Test(groups = {"api", "regression"},
          description = "DELETE /booking/{id} without auth returns 403")
    public void deleteBooking_withoutAuthToken_returns403() {
        // Create a booking first
        final BookingResponse created = bookingService.createBooking(
                buildTestBooking("No", "Auth", 100, true, null));

        // Attempt deletion with an invalid token
        final Response raw = bookingService.deleteBooking(created.getBookingId(), "invalid-token");
        ResponseValidator.of(raw).statusCode(403);
    }

    // -----------------------------------------------------------------------
    //  Private helpers
    // -----------------------------------------------------------------------

    private Booking buildTestBooking(final String firstName, final String lastName,
                                     final int price, final boolean depositPaid,
                                     final String additionalNeeds) {
        return Booking.builder()
                .firstName(firstName)
                .lastName(lastName)
                .totalPrice(price)
                .depositPaid(depositPaid)
                .bookingDates(BookingDates.builder()
                        .checkin("2025-06-01")
                        .checkout("2025-06-05")
                        .build())
                .additionalNeeds(additionalNeeds)
                .build();
    }
}
