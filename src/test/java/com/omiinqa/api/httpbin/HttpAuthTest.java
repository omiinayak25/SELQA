package com.omiinqa.api.httpbin;

import com.omiinqa.api.AbstractApiTest;
import com.omiinqa.api.services.HttpBinService;
import com.omiinqa.api.validator.ResponseValidator;
import io.restassured.response.Response;
import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Validates httpbin's authentication endpoints using the OmiinQA authentication
 * strategy abstraction.
 *
 * <p><b>Endpoints under test:</b></p>
 * <ul>
 *   <li>{@code GET /basic-auth/{user}/{passwd}} — returns {@code 200} when
 *       {@link com.omiinqa.api.auth.BasicAuth} credentials match the path, or
 *       {@code 401} when they differ.</li>
 *   <li>{@code GET /bearer} — returns {@code 200} and echoes the Bearer token
 *       in the body when an {@code Authorization: Bearer <token>} header is present.</li>
 * </ul>
 *
 * <p><b>Why these tests matter:</b> authentication strategies are cross-cutting
 * concerns; a regression in {@link com.omiinqa.api.auth.BasicAuth} or
 * {@link com.omiinqa.api.auth.BearerTokenAuth} would silently break all
 * test suites that rely on those strategies.  Running them against a real HTTP
 * mirror like httpbin catches incorrect header formatting that mocks cannot.</p>
 *
 * <p>Tests do NOT extend {@code BaseTest} — no browser or WebDriver is needed.</p>
 */
public class HttpAuthTest extends AbstractApiTest {

    private HttpBinService httpBin;

    /**
     * Initialises the {@link HttpBinService} facade once for the class lifecycle.
     */
    @BeforeClass(alwaysRun = true)
    public void setUp() {
        httpBin = new HttpBinService();
        log.info("HttpAuthTest initialised against: {}", config.apiUrl("httpbin"));
    }

    // -----------------------------------------------------------------------
    //  Basic auth — positive path
    // -----------------------------------------------------------------------

    /**
     * Verifies that correct Basic credentials return {@code 200 OK} and that
     * the response body confirms the authenticated user.
     */
    @Test(groups = {"api", "regression"},
          description = "Basic auth with correct credentials returns 200 and authenticated=true")
    public void basicAuth_correctCredentials_returns200Authenticated() {
        final Response response = httpBin.basicAuth("omiinqa", "secret");
        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPath("authenticated", true)
                .bodyJsonPath("user", "omiinqa");
    }

    /**
     * Verifies that the authenticated flag in the body is a boolean {@code true}
     * — not the string {@code "true"} — confirming Jackson deserialisation
     * behaves correctly.
     */
    @Test(groups = {"api", "regression"},
          description = "Basic auth response body 'authenticated' field is boolean true")
    public void basicAuth_correctCredentials_authenticatedIsBooleanTrue() {
        final Response response = httpBin.basicAuth("admin", "pass123");
        ResponseValidator.of(response).statusCode(200);
        final Boolean authenticated = response.jsonPath().getBoolean("authenticated");
        Assertions.assertThat(authenticated).isTrue();
    }

    // -----------------------------------------------------------------------
    //  Basic auth — negative path
    // -----------------------------------------------------------------------

    /**
     * Verifies that wrong Basic credentials return {@code 401 Unauthorized},
     * exercising the negative authentication path without network mocking.
     */
    @Test(groups = {"api", "regression"},
          description = "Basic auth with wrong credentials returns 401 Unauthorized")
    public void basicAuth_wrongCredentials_returns401() {
        final Response response = httpBin.basicAuthWrongCredentials("omiinqa", "secret");
        ResponseValidator.of(response).statusCode(401);
    }

    /**
     * Verifies that the {@code WWW-Authenticate} header is present on a 401
     * response — confirming the server correctly challenges unauthenticated callers.
     */
    @Test(groups = {"api", "regression"},
          description = "Basic auth 401 response includes WWW-Authenticate challenge header")
    public void basicAuth_wrongCredentials_hasWwwAuthenticateHeader() {
        final Response response = httpBin.basicAuthWrongCredentials("omiinqa", "secret");
        ResponseValidator.of(response)
                .statusCode(401)
                .hasHeader("WWW-Authenticate");
    }

    // -----------------------------------------------------------------------
    //  Bearer token auth
    // -----------------------------------------------------------------------

    /**
     * Verifies that a Bearer token is accepted by httpbin and the token value is
     * echoed back in the JSON body — confirming
     * {@link com.omiinqa.api.auth.BearerTokenAuth} formats the header correctly.
     */
    @Test(groups = {"api", "regression"},
          description = "Bearer token auth returns 200 and echoes the token in the body")
    public void bearerAuth_validToken_returns200WithTokenEchoed() {
        final String token = "test-bearer-token-12345";
        final Response response = httpBin.bearerAuth(token);
        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPath("authenticated", true)
                .bodyJsonPath("token", token);
    }

    /**
     * Verifies that the Bearer token echoed by httpbin matches the exact value
     * supplied — important when tokens contain special characters that may be
     * URL-encoded or truncated by a misconfigured pipeline.
     */
    @Test(groups = {"api", "regression"},
          description = "Bearer token value in response body matches the sent token exactly")
    public void bearerAuth_tokenWithSpecialChars_roundTripsCorrectly() {
        final String token = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.payload.sig";
        final Response response = httpBin.bearerAuth(token);
        ResponseValidator.of(response).statusCode(200);
        final String echoed = response.jsonPath().getString("token");
        Assertions.assertThat(echoed).isEqualTo(token);
    }
}
