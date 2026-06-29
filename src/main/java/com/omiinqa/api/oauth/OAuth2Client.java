package com.omiinqa.api.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omiinqa.exceptions.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OAuth 2.0 client that supports the <em>Client Credentials</em> and
 * <em>Resource Owner Password</em> grant flows.
 *
 * <p><b>Design rationale:</b> Using the JDK {@link HttpClient} instead of
 * REST Assured for token requests isolates OAuth from REST Assured's
 * {@code RequestSpecification} lifecycle, which is designed for JSON APIs.
 * Token endpoints use {@code application/x-www-form-urlencoded} bodies;
 * constructing these manually avoids leaking REST Assured's opinionated
 * serialization path.  Jackson handles the JSON response.</p>
 *
 * <p><b>Caching:</b> Tokens are stored in a {@link TokenCache} keyed by
 * {@code tokenUrl + clientId + scope}.  Subsequent calls return the cached
 * token until it expires (with a 30-second safety buffer).  Call
 * {@link TokenCache#get()}.{@link TokenCache#evictAll()} to force re-fetch
 * in test tear-down scenarios.</p>
 *
 * <h2>Client Credentials flow</h2>
 * <pre>{@code
 * OAuth2Client oauth = new OAuth2Client();
 * OAuth2Token token = oauth.requestToken(
 *     "https://auth.example.com/oauth/token",
 *     "my-client-id",
 *     "my-client-secret",
 *     "read:orders");
 * // Use token.toBearerHeader() in subsequent API calls
 * }</pre>
 *
 * <h2>Password grant flow</h2>
 * <pre>{@code
 * OAuth2Token token = oauth.passwordToken(
 *     "https://auth.example.com/token",
 *     "client-id", "client-secret",
 *     "alice@example.com", "s3cr3t", "openid profile");
 * }</pre>
 */
public final class OAuth2Client {

    private static final Logger LOG = LoggerFactory.getLogger(OAuth2Client.class);

    private static final String GRANT_CLIENT_CREDENTIALS = "client_credentials";
    private static final String GRANT_PASSWORD           = "password";

    private final HttpClient   httpClient;
    private final ObjectMapper objectMapper;
    private final TokenCache   tokenCache;

    /**
     * Creates an {@code OAuth2Client} with default settings (30-second timeout,
     * shared {@link TokenCache} singleton).
     */
    public OAuth2Client() {
        this(Duration.ofSeconds(30));
    }

    /**
     * Creates an {@code OAuth2Client} with the specified request timeout.
     *
     * @param timeout per-request timeout for token requests; must not be {@code null}
     */
    public OAuth2Client(final Duration timeout) {
        this.httpClient   = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(timeout)
                .build();
        this.objectMapper = new ObjectMapper();
        this.tokenCache   = TokenCache.get();
    }

    // -----------------------------------------------------------------------
    //  Public API
    // -----------------------------------------------------------------------

    /**
     * Requests an access token using the OAuth 2.0 <em>Client Credentials</em>
     * grant (RFC 6749 §4.4).
     *
     * <p>The client ID and secret are sent as
     * {@code application/x-www-form-urlencoded} form parameters in the request
     * body (not in an HTTP Basic header) for maximum compatibility with
     * authorization servers.  Cached tokens are returned without a network
     * round-trip if they are still valid.</p>
     *
     * @param tokenUrl     the token endpoint URL (e.g.,
     *                     {@code "https://auth.example.com/oauth/token"})
     * @param clientId     the OAuth 2.0 client ID
     * @param clientSecret the OAuth 2.0 client secret
     * @param scope        space-separated scope string; may be {@code null} or empty
     * @return a valid, non-expired {@link OAuth2Token}
     * @throws ApiException if the token request fails or returns a non-2xx status
     */
    public OAuth2Token requestToken(final String tokenUrl,
                                    final String clientId,
                                    final String clientSecret,
                                    final String scope) {
        final String cacheKey = buildCacheKey(tokenUrl, clientId, scope);
        final OAuth2Token cached = tokenCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        final Map<String, String> params = new LinkedHashMap<>();
        params.put("grant_type",    GRANT_CLIENT_CREDENTIALS);
        params.put("client_id",     clientId);
        params.put("client_secret", clientSecret);
        if (scope != null && !scope.isBlank()) {
            params.put("scope", scope);
        }

        return executeTokenRequest(tokenUrl, params, cacheKey);
    }

    /**
     * Requests an access token using the OAuth 2.0 <em>Resource Owner
     * Password Credentials</em> grant (RFC 6749 §4.3).
     *
     * <p>This grant type is deprecated in OAuth 2.1 but is still widely used
     * in enterprise systems and API testing against internal authorization
     * servers.  Use only against trusted authorization servers.</p>
     *
     * @param tokenUrl     the token endpoint URL
     * @param clientId     the OAuth 2.0 client ID
     * @param clientSecret the OAuth 2.0 client secret
     * @param username     the resource owner's username
     * @param password     the resource owner's password
     * @param scope        space-separated scope string; may be {@code null}
     * @return a valid, non-expired {@link OAuth2Token}
     * @throws ApiException if the token request fails or returns a non-2xx status
     */
    public OAuth2Token passwordToken(final String tokenUrl,
                                     final String clientId,
                                     final String clientSecret,
                                     final String username,
                                     final String password,
                                     final String scope) {
        final String cacheKey = buildCacheKey(tokenUrl, clientId + ":" + username, scope);
        final OAuth2Token cached = tokenCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        final Map<String, String> params = new LinkedHashMap<>();
        params.put("grant_type",    GRANT_PASSWORD);
        params.put("client_id",     clientId);
        params.put("client_secret", clientSecret);
        params.put("username",      username);
        params.put("password",      password);
        if (scope != null && !scope.isBlank()) {
            params.put("scope", scope);
        }

        return executeTokenRequest(tokenUrl, params, cacheKey);
    }

    // -----------------------------------------------------------------------
    //  Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Encodes form parameters and sends the token POST request.
     *
     * @param tokenUrl the token endpoint
     * @param params   the form parameters (grant_type, credentials, scope, etc.)
     * @param cacheKey the key under which to cache the result
     * @return the parsed and cached {@link OAuth2Token}
     */
    private OAuth2Token executeTokenRequest(final String tokenUrl,
                                            final Map<String, String> params,
                                            final String cacheKey) {
        final String formBody = encodeForm(params);
        LOG.debug("OAuth2 token request → {} | grant={}",
                tokenUrl, params.get("grant_type"));

        try {
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUrl))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Accept",       "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody, StandardCharsets.UTF_8))
                    .build();

            final HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApiException(
                        "OAuth2 token request failed: HTTP " + response.statusCode()
                        + " from [" + tokenUrl + "]. Body: " + response.body());
            }

            final OAuth2Token token = objectMapper.readValue(response.body(), OAuth2Token.class);

            // Reconstruct with issuedAt set (Jackson cannot set this directly from JSON)
            final OAuth2Token withIssuedAt = OAuth2Token.builder()
                    .accessToken(token.getAccessToken())
                    .tokenType(token.getTokenType())
                    .expiresIn(token.getExpiresIn())
                    .refreshToken(token.getRefreshToken())
                    .scope(token.getScope())
                    .issuedAt(Instant.now())
                    .build();

            tokenCache.put(cacheKey, withIssuedAt);
            LOG.debug("OAuth2 token obtained; type={} expiresIn={}s",
                    withIssuedAt.getTokenType(), withIssuedAt.getExpiresIn());
            return withIssuedAt;

        } catch (final ApiException ae) {
            throw ae;
        } catch (final InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new ApiException("OAuth2 token request interrupted", ie);
        } catch (final Exception ex) {
            throw new ApiException("OAuth2 token request failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Encodes a map of string pairs as {@code application/x-www-form-urlencoded}.
     *
     * @param params key-value form parameters
     * @return URL-encoded string suitable for an HTTP body
     */
    private static String encodeForm(final Map<String, String> params) {
        return params.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(),   StandardCharsets.UTF_8)
                        + "=" +
                          URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    /**
     * Builds a deterministic cache key from the token URL, client/user
     * identifier, and scope.
     *
     * @param tokenUrl   the token endpoint URL
     * @param clientPart the client ID (or "clientId:username" for password grant)
     * @param scope      the requested scope (may be {@code null})
     * @return non-null cache key string
     */
    private static String buildCacheKey(final String tokenUrl,
                                        final String clientPart,
                                        final String scope) {
        return tokenUrl + "|" + clientPart + "|" + (scope == null ? "" : scope);
    }
}
