package com.omiinqa.api.httpbin;

import com.omiinqa.api.AbstractApiTest;
import com.omiinqa.api.services.HttpBinService;
import com.omiinqa.api.validator.ResponseValidator;
import io.restassured.response.Response;
import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Validates httpbin's response-encoding and timing endpoints:
 * {@code /gzip}, {@code /deflate}, {@code /response-headers}, and {@code /delay/{n}}.
 *
 * <p>Also covers the utility introspection endpoints {@code /ip} and
 * {@code /user-agent} which return meta-information about the incoming request.</p>
 *
 * <p><b>Why these tests matter:</b></p>
 * <ul>
 *   <li><b>Compression:</b> REST Assured decompresses gzip/deflate automatically;
 *       these tests confirm the decompression path is exercised and the body is
 *       parseable as JSON after decompression.</li>
 *   <li><b>Redirect + response headers:</b> validates {@link com.omiinqa.api.validator.ResponseValidator#hasHeader}
 *       against headers that are set on demand.</li>
 *   <li><b>Delay:</b> verifies that the
 *       {@link com.omiinqa.api.validator.ResponseValidator#responseTimeLessThan}
 *       boundary assertion works — a 1-second delay must be less than 10 seconds
 *       (should pass), and a 0-second delay must also satisfy the same bound.</li>
 *   <li><b>IP / user-agent:</b> basic smoke for introspection endpoints.</li>
 * </ul>
 *
 * <p>Tests do NOT extend {@code BaseTest} — no browser or WebDriver is needed.</p>
 */
public class HttpResponseFormatsTest extends AbstractApiTest {

    private HttpBinService httpBin;

    /**
     * Initialises the {@link HttpBinService} facade once for the class lifecycle.
     */
    @BeforeClass(alwaysRun = true)
    public void setUp() {
        httpBin = new HttpBinService();
        log.info("HttpResponseFormatsTest initialised against: {}", config.apiUrl("httpbin"));
    }

    // -----------------------------------------------------------------------
    //  Gzip compression
    // -----------------------------------------------------------------------

    /**
     * Verifies that {@code GET /gzip} returns {@code 200} and that the
     * decompressed body contains {@code "gzipped": true}, confirming that REST
     * Assured's automatic decompression is functioning correctly.
     */
    @Test(groups = {"api", "regression"},
          description = "GET /gzip returns 200 and body contains gzipped=true")
    public void gzip_returns200WithGzippedFlagTrue() {
        final Response response = httpBin.gzip();
        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPath("gzipped", true);
    }

    /**
     * Verifies that the decompressed gzip response body is non-empty, confirming
     * that no content was lost during decompression.
     */
    @Test(groups = {"api", "regression"},
          description = "GET /gzip decompressed body is non-empty")
    public void gzip_decompressedBodyIsNotEmpty() {
        final Response response = httpBin.gzip();
        ResponseValidator.of(response)
                .statusCode(200)
                .bodyNotEmpty();
    }

    // -----------------------------------------------------------------------
    //  Deflate compression
    // -----------------------------------------------------------------------

    /**
     * Verifies that {@code GET /deflate} returns {@code 200} and that the
     * decompressed body contains {@code "deflated": true}.
     */
    @Test(groups = {"api", "regression"},
          description = "GET /deflate returns 200 and body contains deflated=true")
    public void deflate_returns200WithDeflatedFlagTrue() {
        final Response response = httpBin.deflate();
        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPath("deflated", true);
    }

    /**
     * Verifies that the deflate-decompressed body contains the method echo field,
     * confirming full JSON parseability after decompression.
     */
    @Test(groups = {"api", "regression"},
          description = "GET /deflate decompressed body contains 'method' field")
    public void deflate_decompressedBodyContainsMethod() {
        final Response response = httpBin.deflate();
        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPathNotNull("method");
    }

    // -----------------------------------------------------------------------
    //  Response headers
    // -----------------------------------------------------------------------

    /**
     * Verifies that custom headers requested via {@code /response-headers} are
     * present in the actual HTTP response headers of the reply.
     */
    @Test(groups = {"api", "regression"},
          description = "GET /response-headers echoes X-Test-Header as actual response header")
    public void responseHeaders_customHeader_isPresentInResponse() {
        final Map<String, Object> desired = Map.of("X-Test-Header", "passed");
        final Response response = httpBin.responseHeaders(desired);
        ResponseValidator.of(response)
                .statusCode(200)
                .hasHeader("X-Test-Header");
    }

    /**
     * Verifies that the value of the echoed response header matches exactly
     * the value that was requested.
     */
    @Test(groups = {"api", "regression"},
          description = "GET /response-headers echoed header value matches requested value")
    public void responseHeaders_customHeaderValue_matchesRequested() {
        final Map<String, Object> desired = Map.of("X-Framework", "omiinqa-v1");
        final Response response = httpBin.responseHeaders(desired);
        ResponseValidator.of(response).statusCode(200);
        final String actual = response.getHeader("X-Framework");
        Assertions.assertThat(actual).contains("omiinqa-v1");
    }

    // -----------------------------------------------------------------------
    //  Redirect
    // -----------------------------------------------------------------------

    /**
     * Verifies that {@code GET /redirect/1} follows one redirect and lands on
     * {@code 200 OK} — confirming that REST Assured's redirect-following is active.
     */
    @Test(groups = {"api", "regression"},
          description = "GET /redirect/1 follows redirect and lands on 200")
    public void redirect_oneHop_finalResponseIs200() {
        final Response response = httpBin.redirect(1);
        ResponseValidator.of(response).statusCode(200);
    }

    /**
     * Verifies that {@code GET /redirect/3} follows three consecutive redirects
     * and still lands on {@code 200 OK}.
     */
    @Test(groups = {"api", "regression"},
          description = "GET /redirect/3 follows three hops and lands on 200")
    public void redirect_threeHops_finalResponseIs200() {
        final Response response = httpBin.redirect(3);
        ResponseValidator.of(response).statusCode(200);
    }

    /**
     * Verifies that {@code GET /redirect-to?url=...} redirects to the httpbin
     * {@code /get} endpoint and the final response is {@code 200}.
     */
    @Test(groups = {"api", "regression"},
          description = "GET /redirect-to?url=httpbin.org/get lands on 200")
    public void redirectTo_httpbinGetUrl_finalResponseIs200() {
        final Response response = httpBin.redirectTo("https://httpbin.org/get");
        ResponseValidator.of(response).statusCode(200);
    }

    // -----------------------------------------------------------------------
    //  Delay / timing
    // -----------------------------------------------------------------------

    /**
     * Verifies that {@code GET /delay/1} responds within 10 seconds — a
     * generous upper bound that should always pass in non-degraded conditions.
     * The primary assertion is that the response-time check does not throw when
     * the threshold is not breached.
     */
    @Test(groups = {"api", "regression"},
          description = "GET /delay/1 responds within 10 seconds")
    public void delay_oneSecond_respondsWithin10Seconds() {
        final Response response = httpBin.delay(1);
        ResponseValidator.of(response)
                .statusCode(200)
                .responseTimeLessThan(10, TimeUnit.SECONDS);
    }

    /**
     * Verifies that {@code GET /delay/0} returns immediately — confirming the
     * timing assertion framework does not introduce its own latency overhead.
     */
    @Test(groups = {"api", "regression"},
          description = "GET /delay/0 returns 200 within 5 seconds")
    public void delay_zeroSeconds_returnsImmediately() {
        final Response response = httpBin.delay(0);
        ResponseValidator.of(response)
                .statusCode(200)
                .responseTimeLessThan(5, TimeUnit.SECONDS);
    }

    // -----------------------------------------------------------------------
    //  IP introspection
    // -----------------------------------------------------------------------

    /**
     * Verifies that {@code GET /ip} returns {@code 200} and the {@code origin}
     * field is non-null — confirming the endpoint is reachable and the field
     * is populated (exact IP value cannot be hard-coded in CI).
     */
    @Test(groups = {"api", "regression"},
          description = "GET /ip returns 200 and non-null origin field")
    public void ip_returns200WithOriginField() {
        final Response response = httpBin.ip();
        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPathNotNull("origin");
    }

    // -----------------------------------------------------------------------
    //  User-Agent introspection
    // -----------------------------------------------------------------------

    /**
     * Verifies that {@code GET /user-agent} returns {@code 200} and the
     * {@code user-agent} field is non-null — confirming REST Assured populates
     * the User-Agent header and httpbin echoes it correctly.
     */
    @Test(groups = {"api", "regression"},
          description = "GET /user-agent returns 200 and non-null user-agent field")
    public void userAgent_returns200WithUserAgentField() {
        final Response response = httpBin.userAgent();
        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPathNotNull("user-agent");
    }
}
