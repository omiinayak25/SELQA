package com.omiinqa.bdd.steps;

import com.omiinqa.api.models.reqres.UserRequest;
import com.omiinqa.api.services.ReqResService;
import com.omiinqa.api.validator.ResponseValidator;
import com.omiinqa.bdd.context.ScenarioContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for ReqRes API user-management scenarios (BDD layer over
 * {@link ReqResService}).
 *
 * <p>The last HTTP {@link Response} is stored in {@link ScenarioContext} under
 * the key {@link #API_RESPONSE_KEY} so that generic assertion steps defined in
 * {@link CommonApiAssertionSteps} can consume it regardless of which
 * {@code When} step produced it. This avoids duplicate {@code @Then} definitions
 * across step classes — a common Cucumber pitfall in multi-class glue
 * configurations.</p>
 *
 * <p>Cucumber creates a fresh instance of this class per scenario, so there is
 * no cross-scenario state leakage.</p>
 */
public class ApiUserSteps {

    private static final Logger LOG = LoggerFactory.getLogger(ApiUserSteps.class);

    /** ScenarioContext key for the most recent raw API response. */
    static final String API_RESPONSE_KEY = "lastApiResponse";

    private final ReqResService reqResService = new ReqResService();

    // -----------------------------------------------------------------------
    //  When — user list
    // -----------------------------------------------------------------------

    @When("I request the ReqRes user list on page {int}")
    public void requestReqResUserListOnPage(final int page) {
        LOG.info("BDD: request ReqRes user list page={}", page);
        final Response resp = reqResService.listUsersRaw(page);
        ScenarioContext.put(API_RESPONSE_KEY, resp);
    }

    // -----------------------------------------------------------------------
    //  When — single user
    // -----------------------------------------------------------------------

    @When("I request ReqRes user with id {int}")
    public void requestReqResUserById(final int userId) {
        LOG.info("BDD: request ReqRes user id={}", userId);
        final Response resp = reqResService.getUserRaw(userId);
        ScenarioContext.put(API_RESPONSE_KEY, resp);
    }

    // -----------------------------------------------------------------------
    //  When — create
    // -----------------------------------------------------------------------

    @When("I create a ReqRes user with name {string} and job {string}")
    public void createReqResUser(final String name, final String job) {
        LOG.info("BDD: create ReqRes user name='{}' job='{}'", name, job);
        final UserRequest req = UserRequest.builder().name(name).job(job).build();
        final Response resp = reqResService.createUserRaw(req);
        ScenarioContext.put(API_RESPONSE_KEY, resp);
    }

    // -----------------------------------------------------------------------
    //  When — update / patch / delete
    // -----------------------------------------------------------------------

    @When("I update ReqRes user {int} with name {string} and job {string}")
    public void updateReqResUser(final int userId, final String name, final String job) {
        LOG.info("BDD: PUT ReqRes user id={}", userId);
        final UserRequest req = UserRequest.builder().name(name).job(job).build();
        final Response resp = com.omiinqa.api.client.ApiClient.put(
                new com.omiinqa.api.builder.RequestBuilder()
                        .baseUri(com.omiinqa.config.FrameworkConfig.get().apiUrl("reqres"))
                        .basePath(com.omiinqa.api.constants.ApiEndpoints.REQRES_USER_BY_ID)
                        .pathParam("id", userId)
                        .body(req)
                        .build());
        ScenarioContext.put(API_RESPONSE_KEY, resp);
    }

    @When("I patch ReqRes user {int} with name {string} and job {string}")
    public void patchReqResUser(final int userId, final String name, final String job) {
        LOG.info("BDD: PATCH ReqRes user id={}", userId);
        final UserRequest req = UserRequest.builder().name(name).job(job).build();
        final Response resp = com.omiinqa.api.client.ApiClient.patch(
                new com.omiinqa.api.builder.RequestBuilder()
                        .baseUri(com.omiinqa.config.FrameworkConfig.get().apiUrl("reqres"))
                        .basePath(com.omiinqa.api.constants.ApiEndpoints.REQRES_USER_BY_ID)
                        .pathParam("id", userId)
                        .body(req)
                        .build());
        ScenarioContext.put(API_RESPONSE_KEY, resp);
    }

    @When("I delete ReqRes user with id {int}")
    public void deleteReqResUser(final int userId) {
        LOG.info("BDD: DELETE ReqRes user id={}", userId);
        final Response resp = reqResService.deleteUser(userId);
        ScenarioContext.put(API_RESPONSE_KEY, resp);
    }

    // -----------------------------------------------------------------------
    //  Then — user list assertions
    // -----------------------------------------------------------------------

    @Then("the user list contains {int} users")
    public void userListContainsUsers(final int count) {
        final Response resp = ScenarioContext.get(API_RESPONSE_KEY);
        assertThat(resp.jsonPath().getList("data"))
                .as("User list should contain %d users", count)
                .hasSize(count);
    }

    @Then("the user list JSON path {string} equals {int}")
    public void userListJsonPathEqualsInt(final String path, final int expected) {
        final Response resp = ScenarioContext.get(API_RESPONSE_KEY);
        ResponseValidator.of(resp).bodyJsonPath(path, expected);
    }

    @Then("the user list JSON path {string} is not null")
    public void userListJsonPathNotNull(final String path) {
        final Response resp = ScenarioContext.get(API_RESPONSE_KEY);
        ResponseValidator.of(resp).bodyJsonPathNotNull(path);
    }

    // -----------------------------------------------------------------------
    //  Then — single user assertions
    // -----------------------------------------------------------------------

    @Then("the single user JSON path {string} is not null")
    public void singleUserJsonPathNotNull(final String path) {
        final Response resp = ScenarioContext.get(API_RESPONSE_KEY);
        ResponseValidator.of(resp).bodyJsonPathNotNull(path);
    }

    // -----------------------------------------------------------------------
    //  Then — create user assertions
    // -----------------------------------------------------------------------

    @Then("the create user response contains name {string}")
    public void createUserResponseContainsName(final String name) {
        final Response resp = ScenarioContext.get(API_RESPONSE_KEY);
        ResponseValidator.of(resp).bodyJsonPathContains("name", name);
    }

    @Then("the create user response contains a non-null id")
    public void createUserResponseContainsNonNullId() {
        final Response resp = ScenarioContext.get(API_RESPONSE_KEY);
        ResponseValidator.of(resp).bodyJsonPathNotNull("id");
    }
}
