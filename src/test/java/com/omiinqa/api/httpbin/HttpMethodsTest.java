package com.omiinqa.api.httpbin;

import com.omiinqa.api.AbstractApiTest;
import com.omiinqa.api.services.HttpBinService;
import com.omiinqa.api.validator.ResponseValidator;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Validates all five core HTTP verb echo endpoints exposed by httpbin:
 * {@code /get}, {@code /post}, {@code /put}, {@code /patch}, and {@code /delete}.
 *
 * <p><b>Design rationale:</b> httpbin mirrors every request attribute back in the
 * response body, making it the ideal target for asserting that the OmiinQA
 * {@link com.omiinqa.api.builder.RequestBuilder} and {@link com.omiinqa.api.client.ApiClient}
 * pipeline correctly serialises method, URL, query parameters, and JSON bodies
 * without network mocking.  Each test class in the {@code httpbin} package covers
 * one orthogonal concern; this class owns HTTP-method semantics.</p>
 *
 * <p>Tests do NOT extend {@code BaseTest} — no browser or WebDriver is needed.</p>
 */
public class HttpMethodsTest extends AbstractApiTest {

    private HttpBinService httpBin;

    /**
     * Initialises the {@link HttpBinService} facade once for the entire class.
     * Using {@code @BeforeClass} amortises the one-time config lookup across
     * all test methods.
     */
    @BeforeClass(alwaysRun = true)
    public void setUp() {
        httpBin = new HttpBinService();
        log.info("HttpMethodsTest initialised against: {}", config.apiUrl("httpbin"));
    }

    // -----------------------------------------------------------------------
    //  GET
    // -----------------------------------------------------------------------

    /**
     * Verifies that {@code GET /get} returns {@code 200} with a non-empty body
     * containing the echoed request URL.
     */
    @Test(groups = {"api", "regression"},
          description = "GET /get returns 200 and echoes request url")
    public void get_noParams_returns200WithUrl() {
        final Response response = httpBin.get();
        ResponseValidator.of(response)
                .statusCode(200)
                .contentType("application/json")
                .bodyJsonPathNotNull("url")
                .responseTimeLessThan(10, TimeUnit.SECONDS);
    }

    /**
     * Verifies that query parameters sent to {@code /get} are echoed back under
     * the {@code args} JSON key so callers know params survived serialisation.
     */
    @Test(groups = {"api", "regression"},
          description = "GET /get with query params echoes them in args")
    public void get_withQueryParams_echoesArgsInBody() {
        final Map<String, Object> params = Map.of("foo", "bar", "count", "42");
        final Response response = httpBin.get(params);
        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPath("args.foo", "bar")
                .bodyJsonPath("args.count", "42");
    }

    // -----------------------------------------------------------------------
    //  POST
    // -----------------------------------------------------------------------

    /**
     * Verifies that a JSON body posted to {@code /post} is echoed back verbatim
     * under the {@code json} key — confirming Jackson serialisation and
     * {@code Content-Type: application/json} header are applied correctly.
     */
    @Test(groups = {"api", "regression"},
          description = "POST /post echoes the JSON body under the 'json' key")
    public void post_jsonBody_echoedUnderJsonKey() {
        final Map<String, Object> body = Map.of("name", "omiinqa", "version", 1);
        final Response response = httpBin.post(body);
        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPath("json.name", "omiinqa")
                .bodyJsonPath("json.version", 1);
    }

    /**
     * Verifies that the request URL echoed at {@code url} contains the expected
     * httpbin host when a POST is made.
     */
    @Test(groups = {"api", "regression"},
          description = "POST /post body echoes the request url field")
    public void post_jsonBody_echoesRequestUrl() {
        final Map<String, Object> body = Map.of("key", "value");
        final Response response = httpBin.post(body);
        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPathContains("url", "httpbin");
    }

    // -----------------------------------------------------------------------
    //  PUT
    // -----------------------------------------------------------------------

    /**
     * Verifies that {@code PUT /put} returns {@code 200} and the JSON body is
     * echoed back under the {@code json} key.
     */
    @Test(groups = {"api", "regression"},
          description = "PUT /put echoes the JSON body and returns 200")
    public void put_jsonBody_echoedInResponse() {
        final Map<String, Object> body = Map.of("action", "replace", "id", 7);
        final Response response = httpBin.put(body);
        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPath("json.action", "replace")
                .bodyJsonPath("json.id", 7);
    }

    // -----------------------------------------------------------------------
    //  PATCH
    // -----------------------------------------------------------------------

    /**
     * Verifies that {@code PATCH /patch} returns {@code 200} and the submitted
     * partial payload is echoed back correctly.
     */
    @Test(groups = {"api", "regression"},
          description = "PATCH /patch echoes the JSON body and returns 200")
    public void patch_jsonBody_echoedInResponse() {
        final Map<String, Object> body = Map.of("field", "updated-value");
        final Response response = httpBin.patch(body);
        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPath("json.field", "updated-value");
    }

    // -----------------------------------------------------------------------
    //  DELETE
    // -----------------------------------------------------------------------

    /**
     * Verifies that {@code DELETE /delete} returns {@code 200} with a non-empty
     * body — httpbin echoes the request metadata even for DELETE requests.
     */
    @Test(groups = {"api", "regression"},
          description = "DELETE /delete returns 200 with non-empty body")
    public void delete_noBody_returns200() {
        final Response response = httpBin.delete();
        ResponseValidator.of(response)
                .statusCode(200)
                .bodyNotEmpty()
                .bodyJsonPathNotNull("url");
    }

    // -----------------------------------------------------------------------
    //  Data-driven: method echo round-trip
    // -----------------------------------------------------------------------

    /**
     * Provides {method-label, expected-status} pairs for the
     * {@link #httpMethod_echoesUrl_returnsOk} data-driven test.
     *
     * @return two-dimensional array of test inputs
     */
    @DataProvider(name = "httpMethods")
    public Object[][] httpMethods() {
        return new Object[][]{
            {"GET",    200},
            {"POST",   200},
            {"PUT",    200},
            {"PATCH",  200},
            {"DELETE", 200},
        };
    }

    /**
     * Data-driven test that invokes the appropriate service method for each HTTP
     * verb and asserts the status code is 200 for all of them.
     *
     * @param method         the HTTP verb label (for logging)
     * @param expectedStatus expected HTTP status code
     */
    @Test(groups = {"api", "regression"},
          dataProvider = "httpMethods",
          description = "Each HTTP method endpoint returns 200 with non-empty body")
    public void httpMethod_echoesUrl_returnsOk(final String method, final int expectedStatus) {
        final Map<String, Object> body = Map.of("method", method);
        final Response response;
        switch (method) {
            case "GET"    -> response = httpBin.get();
            case "POST"   -> response = httpBin.post(body);
            case "PUT"    -> response = httpBin.put(body);
            case "PATCH"  -> response = httpBin.patch(body);
            case "DELETE" -> response = httpBin.delete();
            default -> throw new IllegalArgumentException("Unknown method: " + method);
        }
        ResponseValidator.of(response)
                .statusCode(expectedStatus)
                .bodyNotEmpty();
    }
}
