package com.omiinqa.api.services;

import com.omiinqa.api.auth.BasicAuth;
import com.omiinqa.api.auth.BearerTokenAuth;
import com.omiinqa.api.builder.RequestBuilder;
import com.omiinqa.api.client.ApiClient;
import com.omiinqa.config.FrameworkConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Facade over the <a href="https://httpbin.org">httpbin.org</a> public HTTP-inspection API.
 *
 * <p><b>Pattern:</b> Facade (GoF) — encapsulates the URL construction and
 * {@link RequestBuilder} plumbing for every httpbin endpoint group so that test
 * classes can express intent ({@code service.basicAuth("user", "pass")}) rather
 * than HTTP mechanics ({@code new RequestBuilder().baseUri(...).basePath(...)}).
 * This mirrors the pattern established by {@link BookingService} and
 * {@link ReqResService} and keeps the test layer free of low-level REST Assured
 * calls.</p>
 *
 * <p><b>Scope of coverage:</b> HTTP methods (GET, POST, PUT, PATCH, DELETE),
 * request-header inspection, authentication (Basic + Bearer), cookie round-trips,
 * status-code generation, redirects, compressed responses (gzip/deflate),
 * response-header echoing, configurable server-side delay, and IP/user-agent
 * introspection.</p>
 *
 * <p><b>Base URL:</b> resolved once at construction time via
 * {@code FrameworkConfig.get().apiUrl("httpbin")} (maps to {@code https://httpbin.org}).
 * An overloaded constructor allows explicit base-URI injection for stub servers.</p>
 *
 * <p>Instances are stateless and safe to share across test methods.</p>
 */
public class HttpBinService {

    private static final Logger LOG = LoggerFactory.getLogger(HttpBinService.class);

    private final String baseUri;

    // -----------------------------------------------------------------------
    //  Constructors
    // -----------------------------------------------------------------------

    /**
     * Constructs a service wired to the {@code httpbin} API URL configured in
     * {@code config.properties} (key: {@code api.httpbin.url}).
     */
    public HttpBinService() {
        this.baseUri = FrameworkConfig.get().apiUrl("httpbin");
    }

    /**
     * Constructs a service with an explicit base URI — useful for pointing at a
     * local httpbin Docker container or a WireMock stub in CI.
     *
     * @param baseUri fully-qualified base URI (e.g. {@code "http://localhost:8080"}); must not be {@code null}
     */
    public HttpBinService(final String baseUri) {
        this.baseUri = baseUri;
    }

    // -----------------------------------------------------------------------
    //  HTTP method echo endpoints
    // -----------------------------------------------------------------------

    /**
     * Sends a GET request to {@code /get}.
     *
     * <p>httpbin echoes the request URL, query parameters, origin IP, and
     * headers back in the JSON body — useful for asserting that the framework
     * serialises query params correctly.</p>
     *
     * @return the raw response from {@code GET /get}
     */
    public Response get() {
        LOG.info("GET /get");
        return ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath("/get")
                        .build());
    }

    /**
     * Sends a GET request to {@code /get} with a map of query parameters.
     *
     * @param queryParams the query parameters to attach; must not be {@code null}
     * @return the raw response; the echoed {@code args} JSON object will contain
     *         every entry from {@code queryParams}
     */
    public Response get(final Map<String, Object> queryParams) {
        LOG.info("GET /get with queryParams={}", queryParams);
        return ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath("/get")
                        .queryParams(queryParams)
                        .build());
    }

    /**
     * Sends a POST request to {@code /post} with the given body.
     *
     * <p>httpbin echoes the request body under the {@code json} key when the
     * content type is {@code application/json}.</p>
     *
     * @param body the request payload (POJO, Map, or raw String); must not be {@code null}
     * @return the raw response from {@code POST /post}
     */
    public Response post(final Object body) {
        LOG.info("POST /post");
        return ApiClient.post(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath("/post")
                        .body(body)
                        .contentType(ContentType.JSON)
                        .build());
    }

    /**
     * Sends a PUT request to {@code /put} with the given body.
     *
     * @param body the request payload; must not be {@code null}
     * @return the raw response from {@code PUT /put}
     */
    public Response put(final Object body) {
        LOG.info("PUT /put");
        return ApiClient.put(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath("/put")
                        .body(body)
                        .contentType(ContentType.JSON)
                        .build());
    }

    /**
     * Sends a PATCH request to {@code /patch} with the given body.
     *
     * @param body the request payload; must not be {@code null}
     * @return the raw response from {@code PATCH /patch}
     */
    public Response patch(final Object body) {
        LOG.info("PATCH /patch");
        return ApiClient.patch(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath("/patch")
                        .body(body)
                        .contentType(ContentType.JSON)
                        .build());
    }

    /**
     * Sends a DELETE request to {@code /delete}.
     *
     * @return the raw response from {@code DELETE /delete}
     */
    public Response delete() {
        LOG.info("DELETE /delete");
        return ApiClient.delete(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath("/delete")
                        .build());
    }

    // -----------------------------------------------------------------------
    //  Header inspection
    // -----------------------------------------------------------------------

    /**
     * Sends a GET request to {@code /headers}.
     *
     * <p>httpbin returns every request header it received under the
     * {@code headers} JSON key — useful for asserting that custom headers survive
     * the framework's request pipeline unchanged.</p>
     *
     * @return the raw response containing echoed request headers
     */
    public Response headers() {
        LOG.info("GET /headers");
        return ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath("/headers")
                        .build());
    }

    /**
     * Sends a GET to {@code /headers} attaching the supplied custom headers so
     * that the test can verify they are echoed back correctly.
     *
     * @param customHeaders request headers to inject; must not be {@code null}
     * @return the raw response; echoed headers appear at {@code headers.<Name>}
     */
    public Response headers(final Map<String, String> customHeaders) {
        LOG.info("GET /headers with customHeaders={}", customHeaders.keySet());
        return ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath("/headers")
                        .headers(customHeaders)
                        .build());
    }

    // -----------------------------------------------------------------------
    //  Authentication
    // -----------------------------------------------------------------------

    /**
     * Sends a GET request to {@code /basic-auth/{user}/{passwd}} with the given
     * HTTP Basic credentials applied in preemptive mode.
     *
     * <p>httpbin returns {@code 200} when the credentials match the path parameters
     * and {@code 401} otherwise — the canonical way to exercise the framework's
     * {@link BasicAuth} strategy.</p>
     *
     * @param user   expected username (also used as the path segment)
     * @param passwd expected password (also used as the path segment)
     * @return the raw response; {@code 200} on match, {@code 401} on mismatch
     */
    public Response basicAuth(final String user, final String passwd) {
        LOG.info("GET /basic-auth/{}/{} with credentials", user, passwd);
        return ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath("/basic-auth/{user}/{passwd}")
                        .pathParam("user", user)
                        .pathParam("passwd", passwd)
                        .auth(new BasicAuth(user, passwd))
                        .build());
    }

    /**
     * Sends a GET request to {@code /basic-auth/{user}/{passwd}} with WRONG
     * credentials to provoke a {@code 401 Unauthorized} response.
     *
     * @param user   the username embedded in the URL path
     * @param passwd the password embedded in the URL path
     * @return the raw response (expected to be 401)
     */
    public Response basicAuthWrongCredentials(final String user, final String passwd) {
        LOG.info("GET /basic-auth/{}/{} with WRONG credentials (expecting 401)", user, passwd);
        return ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath("/basic-auth/{user}/{passwd}")
                        .pathParam("user", user)
                        .pathParam("passwd", passwd)
                        .auth(new BasicAuth("wrong", "credentials"))
                        .build());
    }

    /**
     * Sends a GET request to {@code /bearer} with a Bearer token.
     *
     * <p>httpbin echoes the token back in the response body under the {@code token}
     * key, making it straightforward to assert round-trip token fidelity.</p>
     *
     * @param token the Bearer token value (without the "Bearer " prefix)
     * @return the raw response from {@code GET /bearer}
     */
    public Response bearerAuth(final String token) {
        LOG.info("GET /bearer");
        return ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath("/bearer")
                        .auth(new BearerTokenAuth(token))
                        .build());
    }

    // -----------------------------------------------------------------------
    //  Cookies
    // -----------------------------------------------------------------------

    /**
     * Sends a GET to {@code /cookies/set} with the supplied cookies; httpbin
     * sets them via {@code Set-Cookie} headers and redirects to {@code /cookies}.
     *
     * <p>REST Assured follows the redirect automatically when
     * {@code urlEncodingEnabled = false}, so the final response body contains
     * the echoed cookie values.</p>
     *
     * @param cookieMap name → value pairs to set; must not be {@code null}
     * @return the final response after redirect; cookies appear at {@code cookies.<name>}
     */
    public Response cookiesSet(final Map<String, Object> cookieMap) {
        LOG.info("GET /cookies/set with cookies={}", cookieMap.keySet());
        return ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath("/cookies/set")
                        .queryParams(cookieMap)
                        .build());
    }

    /**
     * Sends a GET to {@code /cookies} with pre-set cookies in the request so
     * that the test can assert they are echoed back in the response body.
     *
     * @param cookies name → value pairs to send as request cookies
     * @return the raw response; cookie values appear at {@code cookies.<name>}
     */
    public Response cookiesRead(final Map<String, String> cookies) {
        LOG.info("GET /cookies with cookies={}", cookies.keySet());
        RequestBuilder builder = new RequestBuilder()
                .baseUri(baseUri)
                .basePath("/cookies");
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            builder = builder.cookie(entry.getKey(), entry.getValue());
        }
        return ApiClient.get(builder.build());
    }

    // -----------------------------------------------------------------------
    //  Status codes
    // -----------------------------------------------------------------------

    /**
     * Sends a GET request to {@code /status/{code}}.
     *
     * <p>httpbin returns an empty-body response with exactly the status code
     * requested — the definitive way to drive status-code assertions in a
     * data-driven test without mocking.</p>
     *
     * @param code the HTTP status code to request (e.g., 200, 404, 500)
     * @return the raw response whose status code equals {@code code}
     */
    public Response status(final int code) {
        LOG.info("GET /status/{}", code);
        return ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath("/status/{code}")
                        .pathParam("code", code)
                        .build());
    }

    // -----------------------------------------------------------------------
    //  Redirects
    // -----------------------------------------------------------------------

    /**
     * Sends a GET to {@code /redirect/{n}}, which performs {@code n} consecutive
     * {@code 302} redirects before landing on {@code /get}.
     *
     * <p>REST Assured follows all redirects automatically; the final status will
     * be {@code 200} and the response body will be the {@code /get} echo.</p>
     *
     * @param n the number of redirect hops (1–10 recommended)
     * @return the final response after all redirects have been followed
     */
    public Response redirect(final int n) {
        LOG.info("GET /redirect/{}", n);
        return ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath("/redirect/{n}")
                        .pathParam("n", n)
                        .build());
    }

    /**
     * Sends a GET to {@code /redirect-to} with a {@code url} query parameter;
     * httpbin issues a single {@code 302} redirect to that URL.
     *
     * @param targetUrl the absolute URL to redirect to
     * @return the raw response (REST Assured follows the redirect; final body
     *         reflects the target URL content or the httpbin {@code /get} echo)
     */
    public Response redirectTo(final String targetUrl) {
        LOG.info("GET /redirect-to?url={}", targetUrl);
        return ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath("/redirect-to")
                        .queryParam("url", targetUrl)
                        .build());
    }

    // -----------------------------------------------------------------------
    //  Response formats — compressed
    // -----------------------------------------------------------------------

    /**
     * Sends a GET to {@code /gzip}; httpbin returns a gzip-compressed response.
     *
     * <p>REST Assured decompresses the body automatically.  The JSON body
     * contains {@code "gzipped": true} to confirm the compression path was
     * exercised.</p>
     *
     * @return the decompressed response from {@code GET /gzip}
     */
    public Response gzip() {
        LOG.info("GET /gzip");
        return ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath("/gzip")
                        .build());
    }

    /**
     * Sends a GET to {@code /deflate}; httpbin returns a deflate-compressed response.
     *
     * <p>The JSON body contains {@code "deflated": true} to confirm the
     * compression path was exercised.</p>
     *
     * @return the decompressed response from {@code GET /deflate}
     */
    public Response deflate() {
        LOG.info("GET /deflate");
        return ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath("/deflate")
                        .build());
    }

    // -----------------------------------------------------------------------
    //  Response headers
    // -----------------------------------------------------------------------

    /**
     * Sends a GET to {@code /response-headers} with the supplied key-value pairs
     * as query parameters; httpbin echoes them as actual HTTP response headers
     * AND in the JSON body.
     *
     * <p>This is the primary mechanism for asserting that downstream code
     * correctly reads custom response headers.</p>
     *
     * @param headersMap the headers to echo; keys become response-header names
     * @return the raw response, which carries the requested headers
     */
    public Response responseHeaders(final Map<String, Object> headersMap) {
        LOG.info("GET /response-headers with headersMap={}", headersMap.keySet());
        return ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath("/response-headers")
                        .queryParams(headersMap)
                        .build());
    }

    // -----------------------------------------------------------------------
    //  Delay / timing
    // -----------------------------------------------------------------------

    /**
     * Sends a GET to {@code /delay/{seconds}}; httpbin sleeps for the requested
     * number of seconds before responding.
     *
     * <p>Used in conjunction with
     * {@link com.omiinqa.api.validator.ResponseValidator#responseTimeLessThan}
     * to verify that response-time assertions fire correctly when a threshold
     * is deliberately breached or just met.</p>
     *
     * @param seconds server-side sleep duration (0–10 are reliably supported)
     * @return the raw response; actual elapsed time will be ≥ {@code seconds} seconds
     */
    public Response delay(final int seconds) {
        LOG.info("GET /delay/{}", seconds);
        return ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath("/delay/{seconds}")
                        .pathParam("seconds", seconds)
                        .build());
    }

    // -----------------------------------------------------------------------
    //  Introspection helpers
    // -----------------------------------------------------------------------

    /**
     * Sends a GET to {@code /ip}; httpbin returns the caller's public IP address
     * in a JSON object {@code {"origin": "1.2.3.4"}}.
     *
     * @return the raw response containing the {@code origin} field
     */
    public Response ip() {
        LOG.info("GET /ip");
        return ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath("/ip")
                        .build());
    }

    /**
     * Sends a GET to {@code /user-agent}; httpbin echoes the {@code User-Agent}
     * request header back in the response body at the {@code user-agent} JSON key.
     *
     * @return the raw response containing the echoed {@code user-agent} field
     */
    public Response userAgent() {
        LOG.info("GET /user-agent");
        return ApiClient.get(
                new RequestBuilder()
                        .baseUri(baseUri)
                        .basePath("/user-agent")
                        .build());
    }
}
