package com.omiinqa.api;

import com.omiinqa.api.models.reqres.ReqResListResponse;
import com.omiinqa.api.models.reqres.ReqResSingleUserResponse;
import com.omiinqa.api.models.reqres.UserRequest;
import com.omiinqa.api.models.reqres.UserResponse;
import com.omiinqa.api.services.ReqResService;
import com.omiinqa.api.validator.ResponseValidator;
import io.restassured.response.Response;
import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

/**
 * End-to-end API tests for the ReqRes user resource.
 *
 * <p>Covers positive reads, create (POST), update (PUT/PATCH), delete, pagination,
 * boundary user IDs, and 404 negative cases.  Groups are set to {@code api}
 * and {@code regression} so CI can target them independently of UI or DB suites.</p>
 *
 * <p>Does NOT extend {@code BaseTest} — no WebDriver is needed.  Extends
 * {@link AbstractApiTest} which configures REST Assured once per suite.</p>
 */
public class ReqResUserApiTest extends AbstractApiTest {

    private ReqResService reqResService;

    @BeforeClass(alwaysRun = true)
    public void initService() {
        reqResService = new ReqResService();
        log.info("ReqResUserApiTest initialized against: {}", config.apiUrl("reqres"));
    }

    // -----------------------------------------------------------------------
    //  GET /users/{id}  — positive
    // -----------------------------------------------------------------------

    @Test(groups = {"api", "smoke"}, description = "GET /users/2 returns 200 with correct user data")
    public void getUser_validId_returns200AndCorrectData() {
        final ReqResSingleUserResponse userResponse = reqResService.getUser(2);

        Assertions.assertThat(userResponse).isNotNull();
        Assertions.assertThat(userResponse.getData()).isNotNull();
        Assertions.assertThat(userResponse.getData().getId()).isEqualTo(2);
        Assertions.assertThat(userResponse.getData().getEmail()).isNotBlank();
        Assertions.assertThat(userResponse.getSupport()).isNotNull();
        log.info("User 2: {} {}", userResponse.getData().getFirstName(), userResponse.getData().getLastName());
    }

    @Test(groups = {"api", "regression"}, description = "GET /users/{id} responds within 5s for valid ID")
    public void getUser_validId_respondsWithinSla() {
        final Response raw = reqResService.getUserRaw(2);
        ResponseValidator.of(raw)
                .statusCode(200)
                .responseTimeLessThan(5, TimeUnit.SECONDS);
    }

    @Test(groups = {"api", "regression"}, description = "GET /users/1 — first valid boundary user")
    public void getUser_boundaryId1_returns200() {
        final Response raw = reqResService.getUserRaw(1);
        ResponseValidator.of(raw)
                .statusCode(200)
                .bodyJsonPathNotNull("data.id");
    }

    @Test(groups = {"api", "regression"}, description = "GET /users/12 — last known valid boundary user")
    public void getUser_boundaryId12_returns200() {
        final Response raw = reqResService.getUserRaw(12);
        ResponseValidator.of(raw)
                .statusCode(200)
                .bodyJsonPath("data.id", 12);
    }

    // -----------------------------------------------------------------------
    //  GET /users/{id}  — negative
    // -----------------------------------------------------------------------

    @Test(groups = {"api", "regression"}, description = "GET /users/9999 returns 404 for non-existent user")
    public void getUser_nonExistentId_returns404() {
        final Response raw = reqResService.getUserRaw(9999);
        ResponseValidator.of(raw).statusCode(404);
    }

    @Test(groups = {"api", "regression"}, description = "GET /users/0 returns 404 for ID zero")
    public void getUser_idZero_returns404() {
        final Response raw = reqResService.getUserRaw(0);
        ResponseValidator.of(raw).statusCode(404);
    }

    // -----------------------------------------------------------------------
    //  GET /users?page=N  — pagination
    // -----------------------------------------------------------------------

    @Test(groups = {"api", "regression"}, description = "GET /users?page=1 returns page metadata and data array")
    public void listUsers_page1_returnsUserListWithMetadata() {
        final ReqResListResponse list = reqResService.listUsers(1);

        Assertions.assertThat(list.getPage()).isEqualTo(1);
        Assertions.assertThat(list.getData()).isNotEmpty();
        Assertions.assertThat(list.getTotal()).isGreaterThan(0);
        Assertions.assertThat(list.getTotalPages()).isGreaterThanOrEqualTo(1);
    }

    @Test(groups = {"api", "regression"}, description = "GET /users?page=2 returns second page")
    public void listUsers_page2_returnsDifferentRecords() {
        final ReqResListResponse page1 = reqResService.listUsers(1);
        final ReqResListResponse page2 = reqResService.listUsers(2);

        Assertions.assertThat(page2.getPage()).isEqualTo(2);
        // The first IDs on each page must be different
        Assertions.assertThat(page1.getData().get(0).getId())
                .isNotEqualTo(page2.getData().get(0).getId());
    }

    @Test(groups = {"api", "regression"}, description = "List users response contains Content-Type: application/json")
    public void listUsers_responseHasJsonContentType() {
        final Response raw = reqResService.listUsersRaw(1);
        ResponseValidator.of(raw)
                .statusCode(200)
                .hasHeader("Content-Type", "application/json");
    }

    // -----------------------------------------------------------------------
    //  POST /users
    // -----------------------------------------------------------------------

    @Test(groups = {"api", "regression"}, description = "POST /users creates user and returns 201 with generated ID")
    public void createUser_validPayload_returns201WithId() {
        final UserRequest request = UserRequest.builder()
                .name("Morpheus")
                .job("leader")
                .build();

        final Response raw = reqResService.createUserRaw(request);
        ResponseValidator.of(raw)
                .statusCode(201)
                .bodyJsonPathNotNull("id")
                .bodyJsonPathNotNull("createdAt")
                .bodyJsonPath("name", "Morpheus")
                .bodyJsonPath("job", "leader")
                .responseTimeLessThan(5, TimeUnit.SECONDS);
    }

    @Test(groups = {"api", "regression"}, description = "POST /users — typed response contains createdAt timestamp")
    public void createUser_typedResponse_containsCreatedAtTimestamp() {
        final UserRequest request = UserRequest.builder()
                .name("Trinity")
                .job("hacker")
                .build();

        final UserResponse userResponse = reqResService.createUser(request);

        Assertions.assertThat(userResponse.getId()).isNotBlank();
        Assertions.assertThat(userResponse.getCreatedAt()).isNotBlank();
        Assertions.assertThat(userResponse.getName()).isEqualTo("Trinity");
        Assertions.assertThat(userResponse.getJob()).isEqualTo("hacker");
    }

    @DataProvider(name = "userCreationData")
    public Object[][] userCreationData() {
        return new Object[][]{
            {"Neo",      "the chosen one"},
            {"Oracle",   "prophet"},
            {"Agent Smith", "program"}
        };
    }

    @Test(groups = {"api", "regression"},
          dataProvider = "userCreationData",
          description = "POST /users with multiple payloads all return 201")
    public void createUser_dataProvider_allReturn201(final String name, final String job) {
        final UserRequest request = UserRequest.builder().name(name).job(job).build();
        final Response raw = reqResService.createUserRaw(request);
        ResponseValidator.of(raw)
                .statusCode(201)
                .bodyJsonPath("name", name)
                .bodyJsonPath("job", job);
    }

    // -----------------------------------------------------------------------
    //  PUT /users/{id}
    // -----------------------------------------------------------------------

    @Test(groups = {"api", "regression"}, description = "PUT /users/{id} returns 200 with updatedAt")
    public void updateUser_validPayload_returns200WithUpdatedAt() {
        final UserRequest request = UserRequest.builder()
                .name("Morpheus")
                .job("zion resident")
                .build();

        // ReqRes PUT echoes back the request — assert via a PUT call
        final com.omiinqa.api.builder.RequestBuilder builder =
                new com.omiinqa.api.builder.RequestBuilder()
                        .baseUri(config.apiUrl("reqres"))
                        .basePath(com.omiinqa.api.constants.ApiEndpoints.REQRES_USER_BY_ID)
                        .pathParam("id", 2)
                        .body(request);
        final Response putRaw = com.omiinqa.api.client.ApiClient.put(builder.build());
        ResponseValidator.of(putRaw)
                .statusCode(200)
                .bodyJsonPathNotNull("updatedAt")
                .bodyJsonPath("name", "Morpheus")
                .bodyJsonPath("job", "zion resident");
    }

    @Test(groups = {"api", "regression"}, description = "PUT /users — typed response has updatedAt timestamp")
    public void updateUser_typedResponse_hasUpdatedAtTimestamp() {
        final UserRequest request = UserRequest.builder()
                .name("Neo Updated")
                .job("the one")
                .build();

        final UserResponse updated = reqResService.updateUser(2, request);
        Assertions.assertThat(updated.getUpdatedAt()).isNotBlank();
    }

    // -----------------------------------------------------------------------
    //  PATCH /users/{id}
    // -----------------------------------------------------------------------

    @Test(groups = {"api", "regression"}, description = "PATCH /users/{id} returns 200 with updatedAt")
    public void patchUser_partialPayload_returns200WithUpdatedAt() {
        final UserRequest partial = UserRequest.builder()
                .job("senior zion resident")
                .build();

        final UserResponse patched = reqResService.patchUser(2, partial);
        Assertions.assertThat(patched.getUpdatedAt()).isNotBlank();
        Assertions.assertThat(patched.getJob()).isEqualTo("senior zion resident");
    }

    // -----------------------------------------------------------------------
    //  DELETE /users/{id}
    // -----------------------------------------------------------------------

    @Test(groups = {"api", "regression"}, description = "DELETE /users/{id} returns 204 No Content")
    public void deleteUser_validId_returns204() {
        final Response raw = reqResService.deleteUser(2);
        ResponseValidator.of(raw).statusCode(204);
    }
}
