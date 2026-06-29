package com.omiinqa.api.oauth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory cache for {@link OAuth2Token} instances keyed by a
 * stable cache key (typically a combination of token URL + client ID + scope).
 *
 * <p><b>Design rationale:</b> OAuth 2.0 token endpoints impose rate limits and
 * latency.  Re-using a valid (non-expired) token across multiple test threads
 * reduces both round-trip time and the chance of hitting rate limits in
 * parallel CI runs.  A {@link ConcurrentHashMap} provides lock-free reads for
 * the common case (cache hit) while remaining correct under concurrent writes.</p>
 *
 * <p>The cache performs no background eviction: entries are checked lazily via
 * {@link OAuth2Token#isExpired()} at the moment of retrieval.  An expired entry
 * is evicted and {@code null} is returned, prompting the caller ({@link OAuth2Client})
 * to fetch a fresh token.</p>
 *
 * <p>The singleton is process-scoped, so it is shared across all test classes
 * in the same JVM.  The cache key must include all parameters that affect the
 * token (URL, client ID, scope) to prevent cross-client token leakage.</p>
 */
public final class TokenCache {

    private static final Logger LOG = LoggerFactory.getLogger(TokenCache.class);

    private static final TokenCache INSTANCE = new TokenCache();

    private final ConcurrentHashMap<String, OAuth2Token> cache = new ConcurrentHashMap<>();

    private TokenCache() {}

    /**
     * Returns the singleton cache instance.
     *
     * @return the shared {@code TokenCache}
     */
    public static TokenCache get() {
        return INSTANCE;
    }

    /**
     * Returns a cached, non-expired token for the given key, or {@code null}
     * if no valid entry is present.
     *
     * <p>Expired entries are evicted during this call so they do not occupy
     * memory indefinitely.</p>
     *
     * @param cacheKey the lookup key; must not be {@code null}
     * @return a valid {@link OAuth2Token}, or {@code null}
     */
    public OAuth2Token get(final String cacheKey) {
        final OAuth2Token token = cache.get(cacheKey);
        if (token == null) {
            return null;
        }
        if (token.isExpired()) {
            LOG.debug("TokenCache: evicting expired token for key '{}'", cacheKey);
            cache.remove(cacheKey, token);
            return null;
        }
        LOG.debug("TokenCache: cache hit for key '{}' ({} seconds remaining)",
                cacheKey, token.remainingSeconds());
        return token;
    }

    /**
     * Stores a token under the given key, replacing any existing entry.
     *
     * @param cacheKey the storage key; must not be {@code null}
     * @param token    the token to cache; must not be {@code null}
     */
    public void put(final String cacheKey, final OAuth2Token token) {
        cache.put(cacheKey, token);
        LOG.debug("TokenCache: stored token for key '{}' (expires in {} s)",
                cacheKey, token.remainingSeconds());
    }

    /**
     * Removes all entries from the cache.  Useful in test tear-down when tests
     * need to guarantee a fresh token fetch.
     */
    public void evictAll() {
        cache.clear();
        LOG.debug("TokenCache: all entries evicted");
    }

    /**
     * Removes the entry for a specific key.
     *
     * @param cacheKey the key to evict; must not be {@code null}
     */
    public void evict(final String cacheKey) {
        cache.remove(cacheKey);
        LOG.debug("TokenCache: evicted key '{}'", cacheKey);
    }

    /**
     * Returns the number of entries currently in the cache (including potentially
     * expired entries that have not yet been lazily evicted).
     *
     * @return cache size
     */
    public int size() {
        return cache.size();
    }
}
