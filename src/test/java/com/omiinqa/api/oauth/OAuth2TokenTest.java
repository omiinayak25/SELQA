package com.omiinqa.api.oauth;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Offline unit tests for {@link OAuth2Token} expiry logic and
 * {@link TokenCache} behaviour.
 *
 * <p>Tests are grouped as {@code "api"} and {@code "unit"} — no network
 * connections are required; all assertions operate on in-memory state.</p>
 */
public class OAuth2TokenTest {

    @AfterMethod(alwaysRun = true)
    public void clearCache() {
        TokenCache.get().evictAll();
    }

    // -----------------------------------------------------------------------
    //  OAuth2Token expiry tests
    // -----------------------------------------------------------------------

    @Test(groups = {"api", "unit"},
          description = "Token with a long TTL is not expired")
    public void tokenWithLongTtlIsNotExpired() {
        final OAuth2Token token = OAuth2Token.builder()
                .accessToken("abc123")
                .tokenType("Bearer")
                .expiresIn(3600L)           // 1 hour
                .issuedAt(Instant.now())
                .build();

        assertThat(token.isExpired()).isFalse();
    }

    @Test(groups = {"api", "unit"},
          description = "Token issued in the past with 60 s TTL is expired")
    public void tokenIssuedOneHourAgoIsExpired() {
        // Issued 2 hours ago with 1-hour TTL → expired
        final OAuth2Token token = OAuth2Token.builder()
                .accessToken("staleToken")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .issuedAt(Instant.now().minusSeconds(7200))
                .build();

        assertThat(token.isExpired()).isTrue();
    }

    @Test(groups = {"api", "unit"},
          description = "Token with null expiresIn is treated as expired (unknown TTL)")
    public void tokenWithNullExpiresInIsExpired() {
        final OAuth2Token token = OAuth2Token.builder()
                .accessToken("unknownTtl")
                .tokenType("Bearer")
                .expiresIn(null)
                .issuedAt(Instant.now())
                .build();

        assertThat(token.isExpired()).isTrue();
    }

    @Test(groups = {"api", "unit"},
          description = "Token within the 30-second skew buffer is treated as expired")
    public void tokenExpiringInLessThan30SecondsIsExpired() {
        // Expires in 10 seconds — within the 30-second buffer, so treated as expired
        final OAuth2Token token = OAuth2Token.builder()
                .accessToken("almostGone")
                .tokenType("Bearer")
                .expiresIn(10L)
                .issuedAt(Instant.now())
                .build();

        assertThat(token.isExpired()).isTrue();
    }

    @Test(groups = {"api", "unit"},
          description = "remainingSeconds returns zero for an expired token")
    public void remainingSecondsIsZeroForExpiredToken() {
        final OAuth2Token token = OAuth2Token.builder()
                .accessToken("gone")
                .expiresIn(60L)
                .issuedAt(Instant.now().minusSeconds(120))
                .build();

        assertThat(token.remainingSeconds()).isEqualTo(0L);
    }

    @Test(groups = {"api", "unit"},
          description = "remainingSeconds is positive for a fresh token")
    public void remainingSecondsIsPositiveForFreshToken() {
        final OAuth2Token token = OAuth2Token.builder()
                .accessToken("fresh")
                .expiresIn(3600L)
                .issuedAt(Instant.now())
                .build();

        assertThat(token.remainingSeconds()).isGreaterThan(0L).isLessThanOrEqualTo(3600L);
    }

    @Test(groups = {"api", "unit"},
          description = "toBearerHeader formats the header value correctly")
    public void toBearerHeaderFormatsCorrectly() {
        final OAuth2Token token = OAuth2Token.builder()
                .accessToken("myToken42")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .issuedAt(Instant.now())
                .build();

        assertThat(token.toBearerHeader()).isEqualTo("Bearer myToken42");
    }

    // -----------------------------------------------------------------------
    //  TokenCache tests
    // -----------------------------------------------------------------------

    @Test(groups = {"api", "unit"},
          description = "TokenCache returns null for unknown key")
    public void cacheReturnsNullForUnknownKey() {
        assertThat(TokenCache.get().get("not-in-cache")).isNull();
    }

    @Test(groups = {"api", "unit"},
          description = "TokenCache returns a stored, valid token")
    public void cacheReturnsStoredValidToken() {
        final OAuth2Token token = OAuth2Token.builder()
                .accessToken("cached-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .issuedAt(Instant.now())
                .build();

        TokenCache.get().put("key1", token);

        final OAuth2Token retrieved = TokenCache.get().get("key1");
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getAccessToken()).isEqualTo("cached-token");
    }

    @Test(groups = {"api", "unit"},
          description = "TokenCache evicts expired token and returns null")
    public void cacheEvictsExpiredToken() {
        final OAuth2Token expiredToken = OAuth2Token.builder()
                .accessToken("expired")
                .tokenType("Bearer")
                .expiresIn(1L)              // 1-second TTL, already inside 30-s buffer
                .issuedAt(Instant.now().minusSeconds(120))
                .build();

        TokenCache.get().put("expiredKey", expiredToken);

        // Should be evicted on retrieval
        assertThat(TokenCache.get().get("expiredKey")).isNull();
    }

    @Test(groups = {"api", "unit"},
          description = "TokenCache.evictAll clears all entries")
    public void evictAllClearsCache() {
        final OAuth2Token token = OAuth2Token.builder()
                .accessToken("t")
                .expiresIn(3600L)
                .issuedAt(Instant.now())
                .build();

        TokenCache.get().put("k1", token);
        TokenCache.get().put("k2", token);
        TokenCache.get().evictAll();

        assertThat(TokenCache.get().size()).isEqualTo(0);
    }
}
