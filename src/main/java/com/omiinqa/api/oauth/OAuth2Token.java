package com.omiinqa.api.oauth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

/**
 * Immutable value object representing an OAuth 2.0 access token response.
 *
 * <p>The fields map directly to the token response JSON defined by
 * <a href="https://www.rfc-editor.org/rfc/rfc6749#section-5.1">RFC 6749 §5.1</a>:
 * {@code access_token}, {@code token_type}, {@code expires_in},
 * {@code refresh_token}, and {@code scope}.</p>
 *
 * <p>An {@link #isExpired()} helper compares the current wall-clock time
 * against the {@link #issuedAt} instant plus {@link #expiresIn} seconds,
 * applying a configurable 30-second clock-skew buffer to avoid using a
 * token that expires in flight.</p>
 *
 * <p>Jackson deserialization is handled via {@code @Jacksonized} + {@code @Builder},
 * which instructs Jackson to use the generated {@code OAuth2Token.OAuth2TokenBuilder}
 * so no manual {@code @JsonCreator} is needed.
 * {@code @JsonIgnoreProperties(ignoreUnknown = true)} ensures forward-compatibility
 * when token responses include extra vendor fields.</p>
 */
@Getter
@Builder
@Jacksonized
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public final class OAuth2Token {

    /** Clock-skew buffer: treat the token as expired this many seconds early. */
    private static final long SKEW_BUFFER_SECONDS = 30L;

    /** The bearer token string to be sent in {@code Authorization: Bearer <accessToken>} headers. */
    @JsonProperty("access_token")
    private final String accessToken;

    /** Token type, usually {@code "Bearer"} (case-insensitive per RFC 6749). */
    @JsonProperty("token_type")
    private final String tokenType;

    /**
     * Token lifetime in seconds from the moment of issuance.
     * May be {@code null} if the server omits the field (some providers do).
     */
    @JsonProperty("expires_in")
    private final Long expiresIn;

    /** Opaque refresh token for silent token renewal; may be {@code null}. */
    @JsonProperty("refresh_token")
    private final String refreshToken;

    /** Space-separated list of granted scopes; may be {@code null} or empty. */
    @JsonProperty("scope")
    private final String scope;

    /**
     * Wall-clock instant when this token was received.  Set by {@link OAuth2Client}
     * immediately after a successful token request.  Used to compute expiry.
     * Not present in the token JSON — set programmatically.
     */
    private final Instant issuedAt;

    // -----------------------------------------------------------------------
    //  Expiry helpers
    // -----------------------------------------------------------------------

    /**
     * Returns {@code true} when this token should no longer be used.
     *
     * <p>A token is considered expired if:</p>
     * <ul>
     *   <li>{@link #expiresIn} is {@code null} (unknown lifetime — callers
     *       must treat as expired to force re-fetch), or</li>
     *   <li>the current time is at or past
     *       {@code issuedAt + expiresIn - SKEW_BUFFER_SECONDS}.</li>
     * </ul>
     *
     * @return {@code true} when the token is expired or has no known expiry
     */
    public boolean isExpired() {
        if (expiresIn == null || issuedAt == null) {
            return true;
        }
        final Instant expiryWithBuffer = issuedAt.plusSeconds(expiresIn - SKEW_BUFFER_SECONDS);
        return Instant.now().isAfter(expiryWithBuffer);
    }

    /**
     * Returns the remaining lifetime in seconds, or {@code 0} if already
     * expired or the lifetime is unknown.
     *
     * @return non-negative remaining seconds
     */
    public long remainingSeconds() {
        if (expiresIn == null || issuedAt == null) {
            return 0L;
        }
        final long remaining = issuedAt.plusSeconds(expiresIn)
                .getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0L, remaining);
    }

    /**
     * Convenience method: formats the token as an HTTP {@code Authorization}
     * header value.
     *
     * @return {@code "Bearer <accessToken>"}
     */
    public String toBearerHeader() {
        return "Bearer " + accessToken;
    }
}
