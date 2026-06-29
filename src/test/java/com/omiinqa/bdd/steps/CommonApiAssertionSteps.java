package com.omiinqa.bdd.steps;

import com.omiinqa.api.validator.ResponseValidator;
import com.omiinqa.bdd.context.ScenarioContext;
import io.cucumber.java.en.Then;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared assertion step definitions for all API feature files.
 *
 * <p>Cucumber requires that each step phrase is defined in exactly one step
 * class across the entire glue path. This class owns the generic API assertions
 * that are shared by both {@link ApiUserSteps} (ReqRes) and
 * {@link ApiProductSteps} (DummyJSON) feature files:</p>
 * <ul>
 *   <li>{@code Then the API response status is {int}}</li>
 *   <li>{@code Then the response content type contains {string}}</li>
 * </ul>
 *
 * <p>Both {@link ApiUserSteps} and {@link ApiProductSteps} store their
 * {@link Response} in {@link ScenarioContext} under
 * {@link ApiUserSteps#API_RESPONSE_KEY} so these shared steps can retrieve
 * and assert on the response without knowing which service produced it.</p>
 */
public class CommonApiAssertionSteps {

    private static final Logger LOG = LoggerFactory.getLogger(CommonApiAssertionSteps.class);

    @Then("the API response status is {int}")
    public void apiResponseStatusIs(final int expectedStatus) {
        final Response resp = ScenarioContext.get(ApiUserSteps.API_RESPONSE_KEY);
        LOG.debug("Asserting API response status == {}", expectedStatus);
        ResponseValidator.of(resp).statusCode(expectedStatus);
    }

    @Then("the response content type contains {string}")
    public void responseContentTypeContains(final String mediaType) {
        final Response resp = ScenarioContext.get(ApiUserSteps.API_RESPONSE_KEY);
        LOG.debug("Asserting response Content-Type contains '{}'", mediaType);
        ResponseValidator.of(resp).contentType(mediaType);
    }
}
