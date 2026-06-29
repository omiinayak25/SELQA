package com.omiinqa.api.services;

import com.omiinqa.api.builder.RequestBuilder;
import com.omiinqa.api.client.ApiClient;
import com.omiinqa.api.constants.ApiEndpoints;
import com.omiinqa.api.models.booking.AuthRequest;
import com.omiinqa.api.models.booking.AuthToken;
import com.omiinqa.api.models.booking.Booking;
import com.omiinqa.api.models.booking.BookingResponse;
import com.omiinqa.config.FrameworkConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Facade over the Restful-Booker API (https://restful-booker.herokuapp.com).
 *
 * <p><b>Pattern:</b> Facade (GoF) — abstracts the full CRUD lifecycle of a
 * hotel booking, including the token-based authentication handshake that
 * Restful-Booker uses instead of standard Bearer tokens.  This design keeps
 * test classes free of HTTP plumbing and makes request-chaining scenarios
 * (auth → create → read → update → delete) readable at the test level.</p>
 *
 * <p><b>Authentication note:</b> Restful-Booker requires write operations to
 * supply the auth token as a {@code Cookie: token=<value>} header, NOT as a
 * standard Bearer token.  This service applies the cookie via
 * {@link RequestBuilder#cookie(String, String)}.</p>
 *
 * <p>Stateless; safe to instantiate once per test class.</p>
 */
public class BookingService {

    private static final Logger LOG = LoggerFactory.getLogger(BookingService.class);

    /** Default Restful-Booker admin credentials (public API). */
    public static final String DEFAULT_USER     = "admin";

    /** Default Restful-Booker admin password (public API). */
    public static final String DEFAULT_PASSWORD = "password123";

    private final String baseUri;

    /**
     * Constructs a service wired to the {@code restfulbooker} URL in config.
     */
    public BookingService() {
        this.baseUri = FrameworkConfig.get().apiUrl("restfulbooker");
    }

    /**
     * Constructs a service with an explicit base URI.
     *
     * @param baseUri the fully-qualified base URI
     */
    public BookingService(final String baseUri) {
        this.baseUri = baseUri;
    }

    // -----------------------------------------------------------------------
    //  Authentication
    // -----------------------------------------------------------------------

    /**
     * Obtains an authentication token from Restful-Booker.
     *
     * <p>The token must be stored by the caller and passed to mutating
     * operations via the {@code token} cookie parameter in
     * {@link #createBookingWithToken}, {@link #updateBooking},
     * {@link #partialUpdateBooking}, and {@link #deleteBooking}.</p>
     *
     * @param username Restful-Booker username
     * @param password Restful-Booker password
     * @return the {@link AuthToken} containing the session token string
     */
    public AuthToken createToken(final String username, final String password) {
        LOG.info("Creating auth token for user={}", username);
        final AuthRequest request = AuthRequest.builder()
                .username(username)
                .password(password)
                .build();
        final Response response = ApiClient.post(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(ApiEndpoints.BOOKER_AUTH)
                        .body(request)
                        .build());
        return response.as(AuthToken.class);
    }

    /**
     * Obtains a token using the default public admin credentials.
     *
     * @return the session token
     */
    public AuthToken createToken() {
        return createToken(DEFAULT_USER, DEFAULT_PASSWORD);
    }

    // -----------------------------------------------------------------------
    //  Create
    // -----------------------------------------------------------------------

    /**
     * Creates a new booking (does NOT require auth token).
     *
     * @param booking the booking payload; must not be {@code null}
     * @return the creation response including the server-assigned booking ID
     */
    public BookingResponse createBooking(final Booking booking) {
        LOG.info("Creating booking for guest={} {}", booking.getFirstName(), booking.getLastName());
        final Response response = ApiClient.post(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(ApiEndpoints.BOOKER_BOOKING)
                        .header("Accept", "application/json")
                        .body(booking)
                        .build());
        return response.as(BookingResponse.class);
    }

    /**
     * Creates a new booking and returns the raw response.
     *
     * @param booking the booking payload; must not be {@code null}
     * @return the raw REST Assured {@link Response}
     */
    public Response createBookingRaw(final Booking booking) {
        LOG.info("Creating booking (raw) for guest={} {}", booking.getFirstName(), booking.getLastName());
        return ApiClient.post(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(ApiEndpoints.BOOKER_BOOKING)
                        .header("Accept", "application/json")
                        .body(booking)
                        .build());
    }

    /**
     * Creates a booking passing the auth token cookie — demonstrates the
     * full authenticated create flow.
     *
     * @param booking   the booking payload; must not be {@code null}
     * @param authToken the session token from {@link #createToken()}
     * @return the creation response
     */
    public BookingResponse createBookingWithToken(final Booking booking, final String authToken) {
        LOG.info("Creating booking with token for guest={} {}", booking.getFirstName(), booking.getLastName());
        final Response response = ApiClient.post(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(ApiEndpoints.BOOKER_BOOKING)
                        .header("Accept", "application/json")
                        .cookie("token", authToken)
                        .body(booking)
                        .build());
        return response.as(BookingResponse.class);
    }

    // -----------------------------------------------------------------------
    //  Read
    // -----------------------------------------------------------------------

    /**
     * Retrieves a single booking by ID.
     *
     * @param bookingId the booking ID returned at creation
     * @return the typed {@link Booking} record
     */
    public Booking getBooking(final int bookingId) {
        LOG.info("Getting booking id={}", bookingId);
        final Response response = ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(ApiEndpoints.BOOKER_BOOKING_BY_ID)
                        .pathParam("id", bookingId)
                        .header("Accept", "application/json")
                        .build());
        return response.as(Booking.class);
    }

    /**
     * Retrieves a booking and returns the raw response.
     *
     * @param bookingId the booking ID
     * @return the raw REST Assured {@link Response}
     */
    public Response getBookingRaw(final int bookingId) {
        LOG.info("Getting booking (raw) id={}", bookingId);
        return ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(ApiEndpoints.BOOKER_BOOKING_BY_ID)
                        .pathParam("id", bookingId)
                        .header("Accept", "application/json")
                        .build());
    }

    /**
     * Retrieves all booking IDs from Restful-Booker.
     *
     * @return the raw response containing an array of {@code {bookingid: N}} objects
     */
    public Response getAllBookings() {
        LOG.info("Getting all bookings");
        return ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(ApiEndpoints.BOOKER_BOOKING)
                        .header("Accept", "application/json")
                        .build());
    }

    // -----------------------------------------------------------------------
    //  Update
    // -----------------------------------------------------------------------

    /**
     * Fully replaces a booking with PUT /booking/{id} (requires auth token).
     *
     * @param bookingId the booking to replace
     * @param booking   the full replacement payload
     * @param authToken the session token from {@link #createToken()}
     * @return the typed updated {@link Booking}
     */
    public Booking updateBooking(final int bookingId,
                                 final Booking booking,
                                 final String authToken) {
        LOG.info("Updating (PUT) booking id={}", bookingId);
        final Response response = ApiClient.put(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(ApiEndpoints.BOOKER_BOOKING_BY_ID)
                        .pathParam("id", bookingId)
                        .header("Accept", "application/json")
                        .cookie("token", authToken)
                        .body(booking)
                        .build());
        return response.as(Booking.class);
    }

    /**
     * Partially updates a booking with PATCH /booking/{id} (requires auth token).
     *
     * @param bookingId the booking to patch
     * @param partial   the partial payload (only changed fields need be set)
     * @param authToken the session token from {@link #createToken()}
     * @return the raw {@link Response} (Restful-Booker returns the full record)
     */
    public Response partialUpdateBooking(final int bookingId,
                                         final Booking partial,
                                         final String authToken) {
        LOG.info("Partially updating (PATCH) booking id={}", bookingId);
        return ApiClient.patch(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(ApiEndpoints.BOOKER_BOOKING_BY_ID)
                        .pathParam("id", bookingId)
                        .header("Accept", "application/json")
                        .cookie("token", authToken)
                        .contentType(ContentType.JSON)
                        .body(partial)
                        .build());
    }

    // -----------------------------------------------------------------------
    //  Delete
    // -----------------------------------------------------------------------

    /**
     * Deletes a booking with DELETE /booking/{id} (requires auth token).
     *
     * <p>Restful-Booker returns {@code 201 Created} (not 204) on successful
     * deletion — a known API quirk.  Tests should assert 201 rather than 204.</p>
     *
     * @param bookingId the booking to delete
     * @param authToken the session token from {@link #createToken()}
     * @return the raw {@link Response} (typically 201 on success)
     */
    public Response deleteBooking(final int bookingId, final String authToken) {
        LOG.info("Deleting booking id={}", bookingId);
        return ApiClient.delete(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath(ApiEndpoints.BOOKER_BOOKING_BY_ID)
                        .pathParam("id", bookingId)
                        .cookie("token", authToken)
                        .build());
    }
}
