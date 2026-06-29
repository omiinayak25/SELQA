package com.omiinqa.api.jsonplaceholder;

import com.omiinqa.api.AbstractApiTest;
import com.omiinqa.api.models.jsonplaceholder.JsonPlaceholderUser;
import com.omiinqa.api.services.JsonPlaceholderService;
import com.omiinqa.api.validator.ResponseValidator;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * API tests for the JSONPlaceholder {@code /users} resource.
 *
 * <p>Covers: GET all (10 users), GET by ID (boundary: 1 and 10, negative: 9999),
 * typed deep deserialization of nested {@link com.omiinqa.api.models.jsonplaceholder.Address}/
 * {@link com.omiinqa.api.models.jsonplaceholder.Geo}/
 * {@link com.omiinqa.api.models.jsonplaceholder.Company} sub-objects,
 * nested {@code /users/{id}/posts} and {@code /users/{id}/todos} sub-resources,
 * email-format assertions, response-time SLA, and data-driven GET by ID.</p>
 *
 * <p>Does NOT extend {@code BaseTest}; extends {@link AbstractApiTest}.</p>
 */
@Epic("JSONPlaceholder API")
@Feature("Users Resource")
public class UsersApiTest extends AbstractApiTest {

    private JsonPlaceholderService service;

    @BeforeClass(alwaysRun = true)
    public void initService() {
        service = new JsonPlaceholderService();
        log.info("UsersApiTest initialized");
    }

    // -----------------------------------------------------------------------
    //  GET /users — all
    // -----------------------------------------------------------------------

    @Story("GET all users")
    @Severity(SeverityLevel.BLOCKER)
    @Test(groups = {"api", "regression"},
          description = "GET /users returns 200 with exactly 10 pre-seeded users")
    public void getAllUsers_returns200And10Users() {
        final Response response = service.getAllUsers();
        ResponseValidator.of(response)
                .statusCode(200)
                .bodyNotEmpty()
                .contentType("application/json");

        final List<?> users = response.jsonPath().getList("$");
        Assertions.assertThat(users).hasSize(10);
    }

    @Story("GET all users — SLA")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          description = "GET /users responds within 5-second SLA")
    public void getAllUsers_respondsWithinSla() {
        final Response response = service.getAllUsers();
        ResponseValidator.of(response)
                .statusCode(200)
                .responseTimeLessThan(5, TimeUnit.SECONDS);
    }

    // -----------------------------------------------------------------------
    //  GET /users/{id} — boundary and data-driven
    // -----------------------------------------------------------------------

    @DataProvider(name = "userBoundaryIds")
    public Object[][] userBoundaryIds() {
        return new Object[][]{
            {1},   // first (lower boundary)
            {5},   // mid
            {10}   // last (upper boundary)
        };
    }

    @Story("GET user by ID")
    @Severity(SeverityLevel.CRITICAL)
    @Test(groups = {"api", "regression"},
          dataProvider = "userBoundaryIds",
          description = "GET /users/{id} returns 200 with matching id and required top-level fields")
    public void getUserById_validId_returns200AndMatchingId(final int id) {
        final Response response = service.getUserById(id);
        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPath("id", id)
                .bodyJsonPathNotNull("name")
                .bodyJsonPathNotNull("username")
                .bodyJsonPathNotNull("email")
                .bodyJsonPathNotNull("address")
                .bodyJsonPathNotNull("company");
    }

    // -----------------------------------------------------------------------
    //  GET /users/{id} — typed deserialization (deep nesting)
    // -----------------------------------------------------------------------

    @Story("GET user — typed deserialization")
    @Severity(SeverityLevel.CRITICAL)
    @Test(groups = {"api", "regression"},
          description = "GET /users/1 typed deserialization correctly populates nested Address, Geo, Company")
    public void getUser_id1_typedDeserializationCorrect() {
        final JsonPlaceholderUser user = service.getUser(1);

        Assertions.assertThat(user.getId()).isEqualTo(1);
        Assertions.assertThat(user.getName()).isNotBlank();
        Assertions.assertThat(user.getUsername()).isNotBlank();
        Assertions.assertThat(user.getEmail()).contains("@");
        Assertions.assertThat(user.getPhone()).isNotBlank();
        Assertions.assertThat(user.getWebsite()).isNotBlank();

        // Nested Address
        Assertions.assertThat(user.getAddress()).isNotNull();
        Assertions.assertThat(user.getAddress().getCity()).isNotBlank();
        Assertions.assertThat(user.getAddress().getZipcode()).isNotBlank();

        // Deeply nested Geo
        Assertions.assertThat(user.getAddress().getGeo()).isNotNull();
        Assertions.assertThat(user.getAddress().getGeo().getLat()).isNotBlank();
        Assertions.assertThat(user.getAddress().getGeo().getLng()).isNotBlank();

        // Nested Company
        Assertions.assertThat(user.getCompany()).isNotNull();
        Assertions.assertThat(user.getCompany().getName()).isNotBlank();
        Assertions.assertThat(user.getCompany().getCatchPhrase()).isNotBlank();
        Assertions.assertThat(user.getCompany().getBs()).isNotBlank();
    }

    // -----------------------------------------------------------------------
    //  GET /users/{id} — negative
    // -----------------------------------------------------------------------

    @Story("GET user by ID — negative")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          description = "GET /users/9999 returns 404 for non-existent user")
    public void getUserById_nonExistentId_returns404() {
        final Response response = service.getUserById(9999);
        ResponseValidator.of(response).statusCode(404);
    }

    // -----------------------------------------------------------------------
    //  Field-level assertions
    // -----------------------------------------------------------------------

    @Story("Email field format")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          description = "All 10 user email addresses contain '@' character")
    public void getAllUsers_emailFields_allContainAtSymbol() {
        final Response response = service.getAllUsers();
        final List<String> emails = response.jsonPath().getList("email");
        Assertions.assertThat(emails).isNotEmpty();
        emails.forEach(email ->
            Assertions.assertThat(email)
                .as("User email '%s' must contain '@'", email)
                .contains("@"));
    }

    @Story("Geo coordinates — numeric string")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          description = "GET /users/1 geo.lat and geo.lng are parseable as doubles")
    public void getUser_id1_geoCoordinatesAreParseable() {
        final Response response = service.getUserById(1);
        final String lat = response.jsonPath().getString("address.geo.lat");
        final String lng = response.jsonPath().getString("address.geo.lng");

        Assertions.assertThat(lat).isNotBlank();
        Assertions.assertThat(lng).isNotBlank();

        // Must be parseable as doubles to be valid coordinates
        Assertions.assertThatCode(() -> Double.parseDouble(lat))
                .as("address.geo.lat '%s' must be parseable as double", lat)
                .doesNotThrowAnyException();
        Assertions.assertThatCode(() -> Double.parseDouble(lng))
                .as("address.geo.lng '%s' must be parseable as double", lng)
                .doesNotThrowAnyException();
    }

    // -----------------------------------------------------------------------
    //  Nested sub-resources
    // -----------------------------------------------------------------------

    @Story("Nested posts under user")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          description = "GET /users/1/posts returns posts owned by user 1")
    public void getPostsByUserNested_user1_returnsMatchingPosts() {
        final Response response = service.getPostsByUserIdNested(1);
        ResponseValidator.of(response).statusCode(200);

        final List<Integer> userIds = response.jsonPath().getList("userId");
        Assertions.assertThat(userIds).isNotEmpty();
        userIds.forEach(uid ->
            Assertions.assertThat(uid).isEqualTo(1));
    }

    @Story("Nested todos under user")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          description = "GET /users/2/todos returns todos owned by user 2")
    public void getTodosByUserNested_user2_returnsMatchingTodos() {
        final Response response = service.getTodosByUserIdNested(2);
        ResponseValidator.of(response).statusCode(200);

        final List<Integer> userIds = response.jsonPath().getList("userId");
        Assertions.assertThat(userIds).isNotEmpty();
        userIds.forEach(uid ->
            Assertions.assertThat(uid).isEqualTo(2));
    }
}
