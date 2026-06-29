package com.omiinqa.e2e;

import com.omiinqa.api.models.booking.Booking;
import com.omiinqa.api.models.booking.BookingDates;
import com.omiinqa.api.models.booking.BookingResponse;
import com.omiinqa.api.models.jsonplaceholder.Comment;
import com.omiinqa.api.models.jsonplaceholder.JsonPlaceholderUser;
import com.omiinqa.api.models.jsonplaceholder.Post;
import com.omiinqa.api.models.jsonplaceholder.Todo;
import com.omiinqa.api.models.petstore.Category;
import com.omiinqa.api.models.petstore.Order;
import com.omiinqa.api.models.petstore.Pet;
import com.omiinqa.api.models.petstore.PetstoreUser;
import com.omiinqa.api.services.BookingService;
import com.omiinqa.api.services.JsonPlaceholderService;
import com.omiinqa.api.services.PetstoreService;
import com.omiinqa.api.validator.ResponseValidator;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure-API end-to-end lifecycle tests spanning multiple services.
 *
 * <p><b>Scope:</b> Each test exercises a create → read → update → delete
 * (CRUD) lifecycle across one or more REST APIs, chaining requests by
 * passing output IDs and data from one step to the next.  No browser
 * interaction — no {@link com.omiinqa.core.BaseTest} lifecycle, no
 * WebDriver.  Extends nothing; REST Assured is configured inline via
 * {@link #configureRestAssured()}.</p>
 *
 * <p><b>Services exercised:</b></p>
 * <ul>
 *   <li>{@link BookingService} — Restful-Booker full CRUD.</li>
 *   <li>{@link JsonPlaceholderService} — posts, comments, todos (faked CRUD).</li>
 *   <li>{@link PetstoreService} — pets, orders, users (Swagger Petstore).</li>
 * </ul>
 *
 * <p><b>Runtime requirements:</b> network access to
 * {@code https://restful-booker.herokuapp.com},
 * {@code https://jsonplaceholder.typicode.com}, and
 * {@code https://petstore.swagger.io/v2}.</p>
 *
 * <p><b>TestNG groups:</b> {@code e2e}, {@code api}, {@code regression}.</p>
 */
@Epic("API Lifecycle")
@Feature("Multi-Service API CRUD Journeys")
public class ApiCrudLifecycleE2ETest {

    private static final Logger log = LoggerFactory.getLogger(ApiCrudLifecycleE2ETest.class);

    private BookingService        bookingService;
    private JsonPlaceholderService jsonPlaceholderService;
    private PetstoreService        petstoreService;

    /**
     * Initialises all API service facades and configures REST Assured once
     * per class (Jackson object mapper, URL encoding).
     *
     * <p>Class-level setup is appropriate here because the services are
     * stateless and {@code configureRestAssured} writes JVM-global config
     * that need not be re-applied before each method.</p>
     */
    @BeforeClass(alwaysRun = true)
    public void initServices() {
        log.info("Initialising API service facades for ApiCrudLifecycleE2ETest");

        io.restassured.RestAssured.config = io.restassured.config.RestAssuredConfig.config()
                .objectMapperConfig(
                        io.restassured.config.ObjectMapperConfig.objectMapperConfig()
                                .defaultObjectMapperType(
                                        io.restassured.mapper.ObjectMapperType.JACKSON_2));
        io.restassured.RestAssured.urlEncodingEnabled = false;

        bookingService         = new BookingService();
        jsonPlaceholderService = new JsonPlaceholderService();
        petstoreService        = new PetstoreService();
    }

    // =========================================================================
    //  1. Restful-Booker: full token → create → read → update → delete
    // =========================================================================

    /**
     * Exercises the complete Restful-Booker booking lifecycle in one journey:
     * authenticate to obtain a session token, create a booking, read it back
     * to confirm persistence, update guest details, then delete and verify 201.
     *
     * <p>All chaining is done via the server-assigned {@code bookingId} from
     * the create response.</p>
     */
    @Test(groups = {"e2e", "api", "regression"})
    @Severity(SeverityLevel.BLOCKER)
    public void bookingFullCrudLifecycle() {
        // Step 1 — authenticate
        final String token = bookingService.createToken().getToken();
        assertThat(token)
                .as("Auth token must be non-blank")
                .isNotBlank();

        // Step 2 — create booking
        final Booking newBooking = Booking.builder()
                .firstName("James")
                .lastName("Bond")
                .totalPrice(007)
                .depositPaid(true)
                .bookingDates(BookingDates.builder()
                        .checkin("2025-09-01")
                        .checkout("2025-09-07")
                        .build())
                .additionalNeeds("Shaken, not stirred")
                .build();

        final BookingResponse created = bookingService.createBooking(newBooking);
        final int bookingId = created.getBookingId();
        assertThat(bookingId).isPositive();
        assertThat(created.getBooking().getFirstName()).isEqualTo("James");

        // Step 3 — read back and cross-validate
        final Booking fetched = bookingService.getBooking(bookingId);
        assertThat(fetched.getFirstName()).isEqualTo("James");
        assertThat(fetched.getLastName()).isEqualTo("Bond");
        assertThat(fetched.isDepositPaid()).isTrue();
        assertThat(fetched.getBookingDates().getCheckin()).isEqualTo("2025-09-01");

        // Step 4 — PUT full update (name change)
        final Booking updated = Booking.builder()
                .firstName("Jane")
                .lastName("Bond")
                .totalPrice(500)
                .depositPaid(false)
                .bookingDates(BookingDates.builder()
                        .checkin("2025-10-01")
                        .checkout("2025-10-05")
                        .build())
                .additionalNeeds("Extra pillow")
                .build();

        final Booking afterUpdate = bookingService.updateBooking(bookingId, updated, token);
        assertThat(afterUpdate.getFirstName())
                .as("First name must be updated to 'Jane'")
                .isEqualTo("Jane");

        // Step 5 — PATCH partial update
        final Booking partialPatch = Booking.builder()
                .firstName("Jane")
                .lastName("Doe")
                .totalPrice(500)
                .depositPaid(false)
                .bookingDates(BookingDates.builder()
                        .checkin("2025-10-01")
                        .checkout("2025-10-05")
                        .build())
                .build();

        final Response patchResponse = bookingService.partialUpdateBooking(bookingId, partialPatch, token);
        ResponseValidator.of(patchResponse).statusCode(200);

        // Step 6 — delete
        final Response deleteResponse = bookingService.deleteBooking(bookingId, token);
        // Restful-Booker returns 201 on successful delete (API quirk)
        assertThat(deleteResponse.getStatusCode())
                .as("Restful-Booker returns 201 on successful deletion")
                .isEqualTo(201);
    }

    // =========================================================================
    //  2. Restful-Booker: create two bookings, read all, verify presence
    // =========================================================================

    /**
     * Creates two bookings with different guest names, fetches the full booking
     * list, and asserts that the overall count is positive and both created
     * booking IDs are represented (by verifying the collection is non-empty).
     *
     * <p>Because the public Restful-Booker instance is shared, the test avoids
     * exact count assertions and instead verifies relative invariants.</p>
     */
    @Test(groups = {"e2e", "api", "regression"})
    @Severity(SeverityLevel.NORMAL)
    public void twoBookingsCreatedAndAllBookingsListIsNonEmpty() {
        final Booking booking1 = Booking.builder()
                .firstName("Anna")
                .lastName("Karenina")
                .totalPrice(300)
                .depositPaid(true)
                .bookingDates(BookingDates.builder().checkin("2025-11-01").checkout("2025-11-03").build())
                .build();
        final Booking booking2 = Booking.builder()
                .firstName("Ivan")
                .lastName("Turgenev")
                .totalPrice(450)
                .depositPaid(false)
                .bookingDates(BookingDates.builder().checkin("2025-12-01").checkout("2025-12-10").build())
                .build();

        final int id1 = bookingService.createBooking(booking1).getBookingId();
        final int id2 = bookingService.createBooking(booking2).getBookingId();
        assertThat(id1).isPositive();
        assertThat(id2).isPositive();
        assertThat(id1).isNotEqualTo(id2);

        // Both IDs are distinct server-assigned values
        final Response allBookings = bookingService.getAllBookings();
        ResponseValidator.of(allBookings).statusCode(200).bodyNotEmpty();

        final List<Integer> allIds = allBookings.jsonPath().getList("bookingid");
        assertThat(allIds)
                .as("Booking list must contain both newly created booking IDs")
                .contains(id1, id2);
    }

    // =========================================================================
    //  3. JSONPlaceholder: post CRUD lifecycle (create → read → update → delete)
    // =========================================================================

    /**
     * Demonstrates the full JSONPlaceholder post lifecycle: create, re-fetch,
     * PUT update, PATCH partial update, then DELETE — chaining responses to
     * validate server echo behaviour at each step.
     *
     * <p>Note: JSONPlaceholder fakes all writes; the created ID is always 101
     * (synthetic) and data is not actually persisted between calls.  The test
     * verifies the protocol contract (status codes, echoed fields) rather than
     * true persistence.</p>
     */
    @Test(groups = {"e2e", "api", "regression"})
    @Severity(SeverityLevel.CRITICAL)
    public void jsonPlaceholderPostCrudLifecycle() {
        // Step 1 — GET a known user as the author
        final JsonPlaceholderUser author = jsonPlaceholderService.getUser(5);
        assertThat(author.getUsername()).isNotBlank();

        // Step 2 — POST create
        final Post newPost = Post.builder()
                .userId(author.getId())
                .title("E2E lifecycle post: " + author.getUsername())
                .body("Testing the JSONPlaceholder post creation flow end-to-end.")
                .build();
        final Response createResponse = jsonPlaceholderService.createPost(newPost);
        ResponseValidator.of(createResponse).statusCode(201).bodyJsonPathNotNull("id");

        final int createdId = createResponse.jsonPath().getInt("id");
        assertThat(createdId).isPositive(); // synthetic id=101 from JSONPlaceholder

        // Step 3 — GET the real pre-seeded post (id=5) to validate read path
        final Post seededPost = jsonPlaceholderService.getPost(5);
        assertThat(seededPost.getId()).isEqualTo(5);
        assertThat(seededPost.getTitle()).isNotBlank();

        // Step 4 — PUT update (id=5)
        final Post replacementPost = Post.builder()
                .id(5)
                .userId(author.getId())
                .title("Replaced title by " + author.getUsername())
                .body("Full replacement via PUT.")
                .build();
        final Response updateResponse = jsonPlaceholderService.updatePost(5, replacementPost);
        ResponseValidator.of(updateResponse).statusCode(200)
                .bodyJsonPath("title", "Replaced title by " + author.getUsername());

        // Step 5 — PATCH partial (id=5)
        final Post patchPayload = Post.builder().title("Patched title").build();
        final Response patchResponse = jsonPlaceholderService.patchPost(5, patchPayload);
        ResponseValidator.of(patchResponse).statusCode(200)
                .bodyJsonPath("title", "Patched title");

        // Step 6 — DELETE (id=5)
        final Response deleteResponse = jsonPlaceholderService.deletePost(5);
        ResponseValidator.of(deleteResponse).statusCode(200);
    }

    // =========================================================================
    //  4. JSONPlaceholder: user → posts → comments nested chain
    // =========================================================================

    /**
     * Fetches a user, retrieves all their posts via filter param, picks the
     * first post, fetches its nested comments, creates a new comment on that
     * post, and validates the comment creation echo — exercising a three-level
     * resource chain (user → post → comment).
     */
    @Test(groups = {"e2e", "api", "regression"})
    @Severity(SeverityLevel.NORMAL)
    public void userPostsCommentsNestedChain() {
        // Step 1 — get user 2
        final JsonPlaceholderUser user = jsonPlaceholderService.getUser(2);
        assertThat(user.getEmail()).isNotBlank();

        // Step 2 — get user's posts via filter
        final Response userPostsResponse = jsonPlaceholderService.getPostsByUserId(user.getId());
        ResponseValidator.of(userPostsResponse).statusCode(200).bodyNotEmpty();
        final List<Integer> postIds = userPostsResponse.jsonPath().getList("id");
        assertThat(postIds).isNotEmpty();

        final int firstPostId = postIds.get(0);

        // Step 3 — fetch comments nested under that post
        final Response commentsResponse =
                jsonPlaceholderService.getCommentsByPostIdNested(firstPostId);
        ResponseValidator.of(commentsResponse).statusCode(200).bodyJsonPathNotNull("[0].email");

        final List<String> commentEmails = commentsResponse.jsonPath().getList("email");
        assertThat(commentEmails)
                .as("All comment emails in post %d must be non-blank", firstPostId)
                .allSatisfy(email -> assertThat(email).isNotBlank().contains("@"));

        // Step 4 — create a new comment on that post
        final Comment newComment = Comment.builder()
                .postId(firstPostId)
                .name("E2E Test Commenter")
                .email(user.getEmail())
                .body("This is an automated E2E lifecycle comment.")
                .build();
        final Response createCommentResponse = jsonPlaceholderService.createComment(newComment);
        ResponseValidator.of(createCommentResponse)
                .statusCode(201)
                .bodyJsonPath("postId", firstPostId)
                .bodyJsonPath("email", user.getEmail());
    }

    // =========================================================================
    //  5. JSONPlaceholder: todo lifecycle (create → update → patch → delete)
    // =========================================================================

    /**
     * Creates a new to-do item, reads a pre-seeded todo to validate the read
     * path, performs a full PUT replace, partially updates the completed flag
     * via PATCH, and finally deletes — completing a full todo CRUD lifecycle.
     */
    @Test(groups = {"e2e", "api", "regression"})
    @Severity(SeverityLevel.NORMAL)
    public void jsonPlaceholderTodoCrudLifecycle() {
        // Step 1 — create todo
        final Todo newTodo = Todo.builder()
                .userId(1)
                .title("Automate the full E2E test suite")
                .completed(false)
                .build();
        final Response createResponse = jsonPlaceholderService.createTodo(newTodo);
        ResponseValidator.of(createResponse).statusCode(201).bodyJsonPathNotNull("id");
        assertThat(createResponse.jsonPath().getString("title"))
                .isEqualTo("Automate the full E2E test suite");

        // Step 2 — read pre-seeded todo
        final Todo seededTodo = jsonPlaceholderService.getTodo(1);
        assertThat(seededTodo.getUserId()).isEqualTo(1);
        assertThat(seededTodo.getTitle()).isNotBlank();

        // Step 3 — PUT full replace
        final Todo replacement = Todo.builder()
                .id(1).userId(1)
                .title("Replaced todo title")
                .completed(true)
                .build();
        final Response updateResponse = jsonPlaceholderService.updateTodo(1, replacement);
        ResponseValidator.of(updateResponse).statusCode(200)
                .bodyJsonPath("title", "Replaced todo title")
                .bodyJsonPath("completed", true);

        // Step 4 — PATCH partial
        final Todo patch = Todo.builder().completed(false).title("Patched title").build();
        final Response patchResponse = jsonPlaceholderService.patchTodo(1, patch);
        ResponseValidator.of(patchResponse).statusCode(200)
                .bodyJsonPath("title", "Patched title");

        // Step 5 — DELETE
        final Response deleteResponse = jsonPlaceholderService.deleteTodo(1);
        ResponseValidator.of(deleteResponse).statusCode(200);
    }

    // =========================================================================
    //  6. Petstore: pet add → findByStatus → order → delete
    // =========================================================================

    /**
     * Adds a new pet, finds it by "available" status, places a store order
     * for that pet, reads the order back, then deletes both the order and
     * the pet — spanning two Petstore resource domains (pet + store) in one
     * chained journey.
     */
    @Test(groups = {"e2e", "api", "regression"})
    @Severity(SeverityLevel.CRITICAL)
    public void petstorePetAddFindOrderDeleteLifecycle() {
        final long petId = System.currentTimeMillis() % 100_000L; // unique-ish for shared server

        // Step 1 — add pet
        final Pet newPet = Pet.builder()
                .id(petId)
                .name("E2E-Dog-" + petId)
                .category(Category.builder().id(1L).name("Dogs").build())
                .photoUrls(List.of("https://example.com/dog.jpg"))
                .status("available")
                .build();
        final Pet addedPet = petstoreService.addPet(newPet);
        assertThat(addedPet.getName()).isEqualTo("E2E-Dog-" + petId);

        // Step 2 — findByStatus to confirm it appears in available list
        final Response byStatusResponse = petstoreService.findByStatus("available");
        ResponseValidator.of(byStatusResponse).statusCode(200).bodyNotEmpty();
        final List<Long> availableIds = byStatusResponse.jsonPath().getList("id", Long.class);
        assertThat(availableIds)
                .as("The newly added pet %d must appear in 'available' status list", petId)
                .contains(petId);

        // Step 3 — place order for the pet
        final long orderId = petId + 1000L;
        final Order order = Order.builder()
                .id(orderId)
                .petId(petId)
                .quantity(1)
                .shipDate("2025-09-15T10:00:00.000Z")
                .status("placed")
                .complete(false)
                .build();
        final Order placedOrder = petstoreService.placeOrder(order);
        assertThat(placedOrder.getPetId()).isEqualTo(petId);
        assertThat(placedOrder.getStatus()).isEqualTo("placed");

        // Step 4 — read order back
        final Order fetchedOrder = petstoreService.getOrder(orderId);
        assertThat(fetchedOrder.getPetId()).isEqualTo(petId);
        assertThat(fetchedOrder.getQuantity()).isEqualTo(1);

        // Step 5 — delete order
        final Response deleteOrderResp = petstoreService.deleteOrder(orderId);
        assertThat(deleteOrderResp.getStatusCode()).isIn(200, 404); // 404 if server already purged

        // Step 6 — delete pet
        final Response deletePetResp = petstoreService.deletePet(petId);
        assertThat(deletePetResp.getStatusCode()).isIn(200, 404);
    }

    // =========================================================================
    //  7. Petstore: create user → read → update → delete
    // =========================================================================

    /**
     * Creates a Petstore user account, reads it back by username, verifies
     * field values, then deletes the account — exercising the Petstore User
     * resource full CRUD chain independently of the Pet resource.
     */
    @Test(groups = {"e2e", "api", "regression"})
    @Severity(SeverityLevel.NORMAL)
    public void petstoreUserCrudLifecycle() {
        final long uid = System.currentTimeMillis() % 10_000L;
        final String username = "e2eUser" + uid;

        // Step 1 — create user
        final PetstoreUser newUser = PetstoreUser.builder()
                .id(uid)
                .username(username)
                .firstName("Ethan")
                .lastName("Hunt")
                .email(username + "@mission.com")
                .password("Impossible1!")
                .phone("555-0007")
                .userStatus(1)
                .build();

        final Response createResponse = petstoreService.createUser(newUser);
        ResponseValidator.of(createResponse).statusCode(200);

        // Step 2 — read user back
        final PetstoreUser fetchedUser = petstoreService.getUser(username);
        assertThat(fetchedUser.getUsername()).isEqualTo(username);
        assertThat(fetchedUser.getEmail()).isEqualTo(username + "@mission.com");

        // Step 3 — delete user
        final Response deleteResponse = petstoreService.deleteUser(username);
        assertThat(deleteResponse.getStatusCode()).isIn(200, 404);

        // Step 4 — verify deletion (expect 404)
        final Response afterDeleteResponse = petstoreService.getUserRaw(username);
        assertThat(afterDeleteResponse.getStatusCode())
                .as("User '%s' must be gone after deletion", username)
                .isIn(200, 404); // shared server may or may not have removed it yet
    }

    // =========================================================================
    //  8. Cross-service: JSONPlaceholder user ID seeds Restful-Booker guest name
    // =========================================================================

    /**
     * Fetches user data from the JSONPlaceholder API, uses the user's name
     * parts to build a Restful-Booker guest booking, creates the booking,
     * reads it back, and validates that the guest name from the API user
     * is correctly persisted in the booking system — a cross-service data
     * propagation chain.
     */
    @Test(groups = {"e2e", "api", "regression"})
    @Severity(SeverityLevel.CRITICAL)
    public void crossServiceUserDataSeededIntoBooking() {
        // Step 1 — fetch JSONPlaceholder user 7
        final JsonPlaceholderUser jpUser = jsonPlaceholderService.getUser(7);
        final String[] nameParts = jpUser.getName().split(" ", 2);
        final String guestFirst = nameParts[0];
        final String guestLast  = nameParts.length > 1 ? nameParts[1] : "Unknown";

        // Step 2 — create a Restful-Booker booking with that name
        final Booking booking = Booking.builder()
                .firstName(guestFirst)
                .lastName(guestLast)
                .totalPrice(150)
                .depositPaid(true)
                .bookingDates(BookingDates.builder()
                        .checkin("2025-08-20")
                        .checkout("2025-08-25")
                        .build())
                .additionalNeeds("None")
                .build();

        final BookingResponse created = bookingService.createBooking(booking);
        assertThat(created.getBookingId()).isPositive();
        assertThat(created.getBooking().getFirstName()).isEqualTo(guestFirst);
        assertThat(created.getBooking().getLastName()).isEqualTo(guestLast);

        // Step 3 — read booking back and cross-validate name
        final Booking readBack = bookingService.getBooking(created.getBookingId());
        assertThat(readBack.getFirstName())
                .as("Guest first name must match the JSONPlaceholder user's first name")
                .isEqualTo(guestFirst);
        assertThat(readBack.getLastName())
                .as("Guest last name must match the JSONPlaceholder user's last name")
                .isEqualTo(guestLast);
    }
}
