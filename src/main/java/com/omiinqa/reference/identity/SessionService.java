package com.omiinqa.reference.identity;

import com.omiinqa.reference.core.DomainException;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * In-memory session-management service.
 *
 * <p>Creates opaque session tokens, validates them, revokes individual sessions,
 * and enforces a per-user maximum of {@link #MAX_SESSIONS_PER_USER} concurrent
 * sessions. Session expiry is modelled by a call-counter ("tick") rather than
 * wall-clock time so BDD scenarios are fully deterministic.</p>
 *
 * <p>Error codes (asserted by scenarios): {@code SESSION_INVALID},
 * {@code SESSION_EXPIRED}, {@code SESSION_LIMIT}, {@code SESSION_NOT_FOUND}.</p>
 */
public class SessionService {

    /** Maximum simultaneous live sessions allowed per user. */
    public static final int MAX_SESSIONS_PER_USER = 3;

    /** Sessions created more than this many ticks ago are expired. */
    public static final int SESSION_TTL_TICKS = 10;

    private final Map<String, Session> sessions = new LinkedHashMap<>();
    private final Map<Long, Set<String>> userSessions = new ConcurrentHashMap<>();
    private final AtomicLong counter = new AtomicLong(9000);
    private int tick = 0;

    /** Advance the internal tick counter (used in tests to simulate time passing). */
    public void advanceTick(final int amount) {
        tick += amount;
    }

    /**
     * Create a new session for {@code accountId}.  Rejects when the user already
     * holds {@link #MAX_SESSIONS_PER_USER} live sessions.
     *
     * @return the opaque session token
     */
    public String createSession(final long accountId) {
        final Set<String> live = liveSessionsFor(accountId);
        if (live.size() >= MAX_SESSIONS_PER_USER) {
            throw new DomainException("SESSION_LIMIT",
                    "User " + accountId + " already has " + MAX_SESSIONS_PER_USER + " concurrent sessions");
        }
        final String token = "SES-" + accountId + "-" + counter.incrementAndGet();
        sessions.put(token, new Session(token, accountId, tick));
        userSessions.computeIfAbsent(accountId, k -> ConcurrentHashMap.newKeySet()).add(token);
        return token;
    }

    /**
     * Validate a session token and return the associated account id.
     *
     * @throws DomainException {@code SESSION_INVALID} for unknown tokens,
     *                         {@code SESSION_EXPIRED} for expired ones
     */
    public long validate(final String token) {
        final Session s = sessions.get(token);
        if (s == null || s.revoked) {
            throw new DomainException("SESSION_INVALID",
                    "Session token is invalid or revoked: " + token);
        }
        if ((tick - s.createdAtTick) > SESSION_TTL_TICKS) {
            throw new DomainException("SESSION_EXPIRED",
                    "Session token has expired: " + token);
        }
        return s.accountId;
    }

    /**
     * Revoke (invalidate) a specific session token.
     *
     * @throws DomainException {@code SESSION_NOT_FOUND} if the token is unknown
     */
    public void revoke(final String token) {
        final Session s = sessions.get(token);
        if (s == null) {
            throw new DomainException("SESSION_NOT_FOUND",
                    "Session token not found: " + token);
        }
        s.revoked = true;
    }

    /**
     * Revoke all sessions belonging to {@code accountId}.
     *
     * @return number of sessions revoked
     */
    public int revokeAll(final long accountId) {
        final Set<String> tokens = userSessions.getOrDefault(accountId, Set.of());
        int count = 0;
        for (final String t : tokens) {
            final Session s = sessions.get(t);
            if (s != null && !s.revoked) {
                s.revoked = true;
                count++;
            }
        }
        return count;
    }

    /** Return the count of non-expired, non-revoked sessions for {@code accountId}. */
    public int liveSessionCount(final long accountId) {
        return liveSessionsFor(accountId).size();
    }

    // ── private ───────────────────────────────────────────────────────────────

    private Set<String> liveSessionsFor(final long accountId) {
        return userSessions.getOrDefault(accountId, Set.of()).stream()
                .filter(t -> {
                    final Session s = sessions.get(t);
                    return s != null && !s.revoked && (tick - s.createdAtTick) <= SESSION_TTL_TICKS;
                })
                .collect(Collectors.toSet());
    }

    // ── inner class ───────────────────────────────────────────────────────────

    private static final class Session {
        final String token;
        final long accountId;
        final int createdAtTick;
        boolean revoked = false;

        Session(final String token, final long accountId, final int createdAtTick) {
            this.token = token;
            this.accountId = accountId;
            this.createdAtTick = createdAtTick;
        }
    }
}
