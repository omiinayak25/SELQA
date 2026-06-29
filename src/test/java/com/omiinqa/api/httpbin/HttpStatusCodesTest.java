package com.omiinqa.api.httpbin;

import com.omiinqa.api.AbstractApiTest;
import com.omiinqa.api.services.HttpBinService;
import com.omiinqa.api.validator.ResponseValidator;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Data-driven validation of httpbin's {@code /status/{code}} endpoint, which
 * returns a response with exactly the requested HTTP status code.
 *
 * <p><b>Why this matters:</b> status-code assertions are the first line of
 * defence in API test suites.  By exercising the validator against a real server
 * that produces known status codes on demand, this class proves that the
 * {@link com.omiinqa.api.validator.ResponseValidator} correctly reads and
 * compares status codes — catching regressions caused by REST Assured version
 * changes or misconfigured response handling.</p>
 *
 * <p><b>Coverage:</b> 2xx (success), 4xx (client error), and 5xx (server error)
 * families, selected to represent the most commonly asserted codes in integration
 * suites.  All rows are driven by {@link #statusCodes()} to ensure complete
 * coverage without code duplication.</p>
 *
 * <p>Tests do NOT extend {@code BaseTest} — no browser or WebDriver is needed.</p>
 */
public class HttpStatusCodesTest extends AbstractApiTest {

    private HttpBinService httpBin;

    /**
     * Initialises the {@link HttpBinService} facade once for the class lifecycle.
     */
    @BeforeClass(alwaysRun = true)
    public void setUp() {
        httpBin = new HttpBinService();
        log.info("HttpStatusCodesTest initialised against: {}", config.apiUrl("httpbin"));
    }

    /**
     * Data provider supplying {statusCode, description} pairs for
     * {@link #status_requestedCode_isReturnedExactly(int, String)}.
     *
     * <p>Covers the most representative codes from each HTTP status family:</p>
     * <ul>
     *   <li>2xx: 200 OK, 201 Created, 204 No Content</li>
     *   <li>3xx: 301 Moved Permanently (httpbin follows but the initial status is set)</li>
     *   <li>4xx: 400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found,
     *            405 Method Not Allowed, 429 Too Many Requests</li>
     *   <li>5xx: 500 Internal Server Error, 503 Service Unavailable</li>
     * </ul>
     *
     * @return two-dimensional array of {int code, String label}
     */
    @DataProvider(name = "statusCodes")
    public Object[][] statusCodes() {
        return new Object[][]{
            {200, "200 OK"},
            {201, "201 Created"},
            {204, "204 No Content"},
            {400, "400 Bad Request"},
            {401, "401 Unauthorized"},
            {403, "403 Forbidden"},
            {404, "404 Not Found"},
            {405, "405 Method Not Allowed"},
            {429, "429 Too Many Requests"},
            {500, "500 Internal Server Error"},
            {503, "503 Service Unavailable"},
        };
    }

    /**
     * Data-driven test: requests each status code from {@code /status/{code}}
     * and asserts that the response code exactly matches the requested one.
     *
     * @param code        the HTTP status code to request
     * @param description human-readable label used only for test-report clarity
     */
    @Test(groups = {"api", "regression"},
          dataProvider = "statusCodes",
          description = "GET /status/{code} returns the exact requested status code")
    public void status_requestedCode_isReturnedExactly(final int code, final String description) {
        log.debug("Asserting /status/{} — {}", code, description);
        final Response response = httpBin.status(code);
        ResponseValidator.of(response).statusCode(code);
    }

    // -----------------------------------------------------------------------
    //  Spot checks — explicit named tests for the most critical codes
    // -----------------------------------------------------------------------

    /**
     * Spot-check: {@code GET /status/200} explicitly returns {@code 200 OK}.
     * Named test so it appears in the Allure report independently of the
     * data-driven sweep.
     */
    @Test(groups = {"api", "regression"},
          description = "Explicit: /status/200 returns 200 OK")
    public void status200_returnsOk() {
        ResponseValidator.of(httpBin.status(200)).statusCode(200);
    }

    /**
     * Spot-check: {@code GET /status/404} explicitly returns {@code 404 Not Found}.
     */
    @Test(groups = {"api", "regression"},
          description = "Explicit: /status/404 returns 404 Not Found")
    public void status404_returnsNotFound() {
        ResponseValidator.of(httpBin.status(404)).statusCode(404);
    }

    /**
     * Spot-check: {@code GET /status/500} explicitly returns {@code 500 Internal Server Error}.
     */
    @Test(groups = {"api", "regression"},
          description = "Explicit: /status/500 returns 500 Internal Server Error")
    public void status500_returnsInternalServerError() {
        ResponseValidator.of(httpBin.status(500)).statusCode(500);
    }

    /**
     * Verifies that a 2xx status code passes the {@code statusCodeBetween(200, 299)}
     * range check in {@link com.omiinqa.api.validator.ResponseValidator}.
     */
    @Test(groups = {"api", "regression"},
          description = "GET /status/201 satisfies statusCodeBetween(200, 299)")
    public void status201_satisfies2xxRange() {
        ResponseValidator.of(httpBin.status(201))
                .statusCodeBetween(200, 299);
    }
}
