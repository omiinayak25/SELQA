package com.omiinqa.api.httpbin;

import com.omiinqa.api.AbstractApiTest;
import com.omiinqa.api.services.HttpBinService;
import com.omiinqa.api.validator.ResponseValidator;
import io.restassured.response.Response;
import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * Validates cookie round-trip behaviour using httpbin's {@code /cookies} and
 * {@code /cookies/set} endpoints.
 *
 * <p><b>Endpoints under test:</b></p>
 * <ul>
 *   <li>{@code GET /cookies/set?name=value} — httpbin issues {@code Set-Cookie}
 *       headers and redirects to {@code /cookies}, which echoes them in the body.</li>
 *   <li>{@code GET /cookies} — httpbin mirrors every cookie in the request back
 *       in the JSON response body at {@code cookies.<name>}.</li>
 * </ul>
 *
 * <p><b>Why these tests matter:</b> cookie handling is a common source of
 * session-management bugs (wrong path, missing {@code HttpOnly}, wrong domain).
 * Running the OmiinQA {@link com.omiinqa.api.builder.RequestBuilder} cookie API
 * against a real mirror confirms that cookies survive the request pipeline
 * and that the framework can both set and read cookies correctly.</p>
 *
 * <p>Tests do NOT extend {@code BaseTest} — no browser or WebDriver is needed.</p>
 */
public class HttpCookiesTest extends AbstractApiTest {

    private HttpBinService httpBin;

    /**
     * Initialises the {@link HttpBinService} facade once for the class lifecycle.
     */
    @BeforeClass(alwaysRun = true)
    public void setUp() {
        httpBin = new HttpBinService();
        log.info("HttpCookiesTest initialised against: {}", config.apiUrl("httpbin"));
    }

    // -----------------------------------------------------------------------
    //  /cookies/set — set via redirect
    // -----------------------------------------------------------------------

    /**
     * Verifies that {@code GET /cookies/set?session=abc123} causes httpbin to
     * echo the cookie value after the redirect, confirming that cookies survive
     * the redirect chain in REST Assured.
     */
    @Test(groups = {"api", "regression"},
          description = "GET /cookies/set?session=abc123 echoes cookie after redirect")
    public void cookiesSet_singleCookie_isEchoedAfterRedirect() {
        final Map<String, Object> cookies = Map.of("session", "abc123");
        final Response response = httpBin.cookiesSet(cookies);
        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPathNotNull("cookies.session");
    }

    /**
     * Verifies that the cookie value set via {@code /cookies/set} is echoed
     * with the exact value submitted — no encoding artifacts.
     */
    @Test(groups = {"api", "regression"},
          description = "Cookie value set via /cookies/set is echoed exactly")
    public void cookiesSet_singleCookie_valueMatchesExactly() {
        final Map<String, Object> cookies = Map.of("token", "omiinqa-session-xyz");
        final Response response = httpBin.cookiesSet(cookies);
        ResponseValidator.of(response).statusCode(200);
        final String echoed = response.jsonPath().getString("cookies.token");
        Assertions.assertThat(echoed).isEqualTo("omiinqa-session-xyz");
    }

    /**
     * Verifies that multiple cookies set via {@code /cookies/set} are all echoed
     * back after the redirect — testing that the query-param batch approach works.
     */
    @Test(groups = {"api", "regression"},
          description = "Multiple cookies set via /cookies/set are all echoed after redirect")
    public void cookiesSet_multipleCookies_allEchoed() {
        final Map<String, Object> cookies = Map.of(
                "user_id", "42",
                "locale", "en-US");
        final Response response = httpBin.cookiesSet(cookies);
        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPathNotNull("cookies.user_id")
                .bodyJsonPathNotNull("cookies.locale");
    }

    // -----------------------------------------------------------------------
    //  /cookies — read cookies sent in request
    // -----------------------------------------------------------------------

    /**
     * Verifies that a cookie sent in the request via
     * {@link com.omiinqa.api.builder.RequestBuilder#cookie(String, String)}
     * is reflected back in the {@code /cookies} response body.
     */
    @Test(groups = {"api", "regression"},
          description = "Cookie sent in request is echoed by /cookies endpoint")
    public void cookiesRead_requestCookie_isEchoed() {
        final Map<String, String> cookies = Map.of("auth_token", "read-test-token");
        final Response response = httpBin.cookiesRead(cookies);
        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPath("cookies.auth_token", "read-test-token");
    }

    /**
     * Verifies that two cookies sent in a single request are both echoed by
     * {@code /cookies}, confirming multiple cookie injection via the builder.
     */
    @Test(groups = {"api", "regression"},
          description = "Two request cookies are both echoed by /cookies endpoint")
    public void cookiesRead_twoCookies_bothEchoed() {
        final Map<String, String> cookies = Map.of(
                "cookie_a", "value_a",
                "cookie_b", "value_b");
        final Response response = httpBin.cookiesRead(cookies);
        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPath("cookies.cookie_a", "value_a")
                .bodyJsonPath("cookies.cookie_b", "value_b");
    }
}
