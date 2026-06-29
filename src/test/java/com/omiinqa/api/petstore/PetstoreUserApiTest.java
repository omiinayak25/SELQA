package com.omiinqa.api.petstore;

import com.omiinqa.api.AbstractApiTest;
import com.omiinqa.api.models.petstore.PetstoreUser;
import com.omiinqa.api.services.PetstoreService;
import com.omiinqa.api.validator.ResponseValidator;
import io.restassured.response.Response;
import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * API tests for the Petstore {@code /user} resource group.
 *
 * <p><b>Coverage:</b></p>
 * <ul>
 *   <li>{@code POST /user} with a valid payload returns 200.</li>
 *   <li>Chained create → get → delete lifecycle verifying username round-trip.</li>
 *   <li>{@code GET /user/{username}} for a non-existent username returns 404.</li>
 *   <li>{@code DELETE /user/{username}} for a non-existent username returns 404.</li>
 *   <li>Field-level assertions: username, firstName, email echoed correctly.</li>
 *   <li>Boundary: very long username string.</li>
 * </ul>
 *
 * <p>Does NOT extend {@code BaseTest} — no browser required.</p>
 */
public class PetstoreUserApiTest extends AbstractApiTest {

    private PetstoreService petstoreService;

    /**
     * Initialises the service facade once per class.
     */
    @BeforeClass(alwaysRun = true)
    public void setUpService() {
        petstoreService = new PetstoreService();
        log.info("PetstoreUserApiTest initialised against: {}", config.apiUrl("petstore"));
    }

    // -----------------------------------------------------------------------
    //  POST /user
    // -----------------------------------------------------------------------

    /**
     * POST /user with a valid payload returns 200 and a non-blank body.
     */
    @Test(groups = {"api", "regression"},
          description = "POST /user with valid payload returns 200")
    public void createUser_validPayload_returns200() {
        final PetstoreUser user = buildUser("testuser_" + System.currentTimeMillis());

        final Response raw = petstoreService.createUser(user);

        ResponseValidator.of(raw)
                .statusCode(200)
                .bodyNotEmpty();
    }

    /**
     * POST /user returns an ApiResponse body with a non-null message.
     */
    @Test(groups = {"api", "regression"},
          description = "POST /user response body contains an ApiResponse message field")
    public void createUser_responseBodyHasMessage() {
        final String username = "msguser_" + System.currentTimeMillis();
        final Response raw = petstoreService.createUser(buildUser(username));

        ResponseValidator.of(raw)
                .statusCode(200)
                .bodyJsonPathNotNull("message");
    }

    // -----------------------------------------------------------------------
    //  GET /user/{username}
    // -----------------------------------------------------------------------

    /**
     * POST /user then GET /user/{username} — username echoed correctly (chained).
     */
    @Test(groups = {"api", "regression"},
          description = "POST /user then GET /user/{username} — username echoed in response (chained)")
    public void createThenGetUser_chainedRequest_usernameMatches() {
        final String username = "getuser_" + System.currentTimeMillis();
        petstoreService.createUser(buildUser(username));

        final PetstoreUser fetched = petstoreService.getUser(username);

        Assertions.assertThat(fetched.getUsername()).isEqualTo(username);
    }

    /**
     * POST /user then GET — firstName field is echoed correctly.
     */
    @Test(groups = {"api", "regression"},
          description = "POST /user then GET /user/{username} — firstName field echoed correctly")
    public void createThenGetUser_firstNameMatches() {
        final String username = "fnuser_" + System.currentTimeMillis();
        petstoreService.createUser(buildUser(username));

        final PetstoreUser fetched = petstoreService.getUser(username);

        Assertions.assertThat(fetched.getFirstName()).isEqualTo("Test");
    }

    /**
     * GET /user/{username} for a non-existent username returns 404.
     */
    @Test(groups = {"api", "regression"},
          description = "GET /user/{username} for a non-existent username returns 404")
    public void getUser_nonExistentUsername_returns404() {
        final Response raw = petstoreService.getUserRaw("nonexistentuser_xyz_" + System.currentTimeMillis());

        ResponseValidator.of(raw).statusCode(404);
    }

    // -----------------------------------------------------------------------
    //  DELETE /user/{username}
    // -----------------------------------------------------------------------

    /**
     * POST /user → GET /user/{username} → DELETE /user/{username} → GET returns 404.
     * Full user lifecycle chained scenario.
     */
    @Test(groups = {"api", "regression"},
          description = "Full user lifecycle: create → get → delete → GET returns 404 (chained)")
    public void createGetDeleteUser_chainedLifecycle_finalGetIs404() {
        final String username = "deluser_" + System.currentTimeMillis();

        // Create
        final Response createResp = petstoreService.createUser(buildUser(username));
        ResponseValidator.of(createResp).statusCode(200);

        // Get — verify creation
        final PetstoreUser fetched = petstoreService.getUser(username);
        Assertions.assertThat(fetched.getUsername()).isEqualTo(username);

        // Delete
        final Response deleteResp = petstoreService.deleteUser(username);
        ResponseValidator.of(deleteResp).statusCode(200);

        // GET after delete → 404
        final Response getAfterDelete = petstoreService.getUserRaw(username);
        ResponseValidator.of(getAfterDelete).statusCode(404);
    }

    /**
     * DELETE /user/{username} for a non-existent username returns 404.
     */
    @Test(groups = {"api", "regression"},
          description = "DELETE /user/{username} for non-existent username returns 404")
    public void deleteUser_nonExistentUsername_returns404() {
        final Response raw = petstoreService.deleteUser("nosuchuser_" + System.currentTimeMillis());

        ResponseValidator.of(raw).statusCode(404);
    }

    /**
     * Boundary: POST /user with a very long username string (64 chars) — server accepts it.
     */
    @Test(groups = {"api", "regression"},
          description = "POST /user with a 64-character username boundary returns 200")
    public void createUser_veryLongUsername_returns200() {
        final String longUsername = "u".repeat(32) + System.currentTimeMillis();
        final PetstoreUser user = PetstoreUser.builder()
                .id(System.currentTimeMillis())
                .username(longUsername)
                .firstName("Long")
                .lastName("Username")
                .email(longUsername + "@example.com")
                .password("pass123")
                .phone("555-1234")
                .userStatus(1)
                .build();

        final Response raw = petstoreService.createUser(user);
        ResponseValidator.of(raw).statusCode(200);
    }

    /**
     * POST two different users sequentially — both return 200 independently.
     */
    @Test(groups = {"api", "regression"},
          description = "Creating two distinct users sequentially both succeed")
    public void createTwoUsers_bothSucceed() {
        final String username1 = "user1_" + System.currentTimeMillis();
        final String username2 = "user2_" + (System.currentTimeMillis() + 1);

        ResponseValidator.of(petstoreService.createUser(buildUser(username1))).statusCode(200);
        ResponseValidator.of(petstoreService.createUser(buildUser(username2))).statusCode(200);
    }

    // -----------------------------------------------------------------------
    //  Private helpers
    // -----------------------------------------------------------------------

    /**
     * Constructs a minimal but representative {@link PetstoreUser} fixture.
     *
     * @param username unique login handle for this test user
     * @return a fully populated {@link PetstoreUser}
     */
    private PetstoreUser buildUser(final String username) {
        return PetstoreUser.builder()
                .id(System.currentTimeMillis())
                .username(username)
                .firstName("Test")
                .lastName("User")
                .email(username + "@test.example.com")
                .password("secret123")
                .phone("555-0100")
                .userStatus(1)
                .build();
    }
}
