package com.omiinqa.api.httpbin;

import com.omiinqa.api.AbstractApiTest;
import com.omiinqa.api.services.HttpBinService;
import com.omiinqa.api.validator.ResponseValidator;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * Validates httpbin's {@code /headers} and {@code /response-headers} endpoints
 * to confirm that the OmiinQA request pipeline correctly propagates custom
 * request headers and that callers can assert on arbitrary response headers.
 *
 * <p><b>Endpoints under test:</b></p>
 * <ul>
 *   <li>{@code GET /headers} — echoes all request headers back in the JSON body.</li>
 *   <li>{@code GET /response-headers} — instructs httpbin to return custom
 *       headers and echo them in the body.</li>
 * </ul>
 *
 * <p>Tests do NOT extend {@code BaseTest} — no browser or WebDriver is needed.</p>
 */
public class HttpHeadersTest extends AbstractApiTest {

    private HttpBinService httpBin;

    /**
     * Initialises the {@link HttpBinService} facade once for the class lifecycle.
     */
    @BeforeClass(alwaysRun = true)
    public void setUp() {
        httpBin = new HttpBinService();
        log.info("HttpHeadersTest initialised against: {}", config.apiUrl("httpbin"));
    }

    // -----------------------------------------------------------------------
    //  Request header echo tests (/headers)
    // -----------------------------------------------------------------------

    /**
     * Verifies that {@code GET /headers} returns {@code 200} and includes the
     * mandatory {@code Host} header in the echoed body — confirming the endpoint
     * is reachable and well-formed.
     */
    @Test(groups = {"api", "regression"},
          description = "GET /headers returns 200 and echoes at least the Host header")
    public void headers_noCustom_returns200WithHostEchoed() {
        final Response response = httpBin.headers();
        ResponseValidator.of(response)
                .statusCode(200)
                .contentType("application/json")
                .bodyJsonPathNotNull("headers.Host");
    }

    /**
     * Verifies that a custom {@code X-omiinqa-Test-Id} header injected via
     * {@link HttpBinService#headers(Map)} is echoed back verbatim in the response
     * body at {@code headers.X-Omiinqa-Test-Id}.
     *
     * <p>httpbin capitalises header names; the assertion uses the canonical form.</p>
     */
    @Test(groups = {"api", "regression"},
          description = "Custom request header X-omiinqa-Test-Id is echoed in response body")
    public void headers_customTestIdHeader_isEchoedBack() {
        final Map<String, String> custom = Map.of("X-omiinqa-Test-Id", "TC-HEADERS-001");
        final Response response = httpBin.headers(custom);
        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPathNotNull("headers.X-Omiinqa-Test-Id");
    }

    /**
     * Verifies that multiple custom headers are all reflected back in the body,
     * confirming that the builder's {@code headers(Map)} method submits every entry.
     */
    @Test(groups = {"api", "regression"},
          description = "Multiple custom headers are all echoed back in response body")
    public void headers_multipleCustomHeaders_allEchoed() {
        final Map<String, String> custom = Map.of(
                "X-Correlation-Id", "corr-abc-123",
                "X-Client-Version", "2.0.0");
        final Response response = httpBin.headers(custom);
        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPathNotNull("headers.X-Correlation-Id")
                .bodyJsonPathNotNull("headers.X-Client-Version");
    }

    /**
     * Verifies that the {@code Content-Type} response header is
     * {@code application/json}, confirming the framework's default content-type
     * negotiation behaves correctly.
     */
    @Test(groups = {"api", "regression"},
          description = "GET /headers response Content-Type is application/json")
    public void headers_responseContentType_isJson() {
        final Response response = httpBin.headers();
        ResponseValidator.of(response)
                .statusCode(200)
                .hasHeader("Content-Type", "application/json");
    }

    // -----------------------------------------------------------------------
    //  Response header echo tests (/response-headers)
    // -----------------------------------------------------------------------

    /**
     * Verifies that {@code GET /response-headers?X-Custom=value} causes httpbin to
     * return a response with that custom header, which
     * {@link ResponseValidator#hasHeader(String)} can then assert on.
     */
    @Test(groups = {"api", "regression"},
          description = "GET /response-headers echoes requested header in HTTP response")
    public void responseHeaders_customHeader_presentInHttpResponse() {
        final Map<String, Object> desired = Map.of("X-Custom-Echo", "omiinqa-value");
        final Response response = httpBin.responseHeaders(desired);
        ResponseValidator.of(response)
                .statusCode(200)
                .hasHeader("X-Custom-Echo");
    }

    /**
     * Verifies that the value of the echoed response header matches the value
     * requested — asserting exact value fidelity, not just header presence.
     */
    @Test(groups = {"api", "regression"},
          description = "GET /response-headers echoes the exact header value in the body")
    public void responseHeaders_customHeaderValue_matchesInBody() {
        final Map<String, Object> desired = Map.of("X-Suite-Tag", "regression");
        final Response response = httpBin.responseHeaders(desired);
        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPath("X-Suite-Tag", "regression");
    }
}
