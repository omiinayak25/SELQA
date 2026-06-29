package com.omiinqa.reference.identity;

import com.omiinqa.reference.core.DomainException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory OTP (one-time password) service.
 *
 * <p>OTPs are generated deterministically (seeded from the account-id + a
 * generation counter) as 6-digit numeric strings, so scenarios can assert an
 * exact code without randomness. Each OTP is single-use, has a configurable
 * expiry-by-tick, and limits verification attempts to
 * {@link #MAX_VERIFY_ATTEMPTS} before the OTP is invalidated.</p>
 *
 * <p>Error codes (asserted by scenarios): {@code OTP_INVALID},
 * {@code OTP_EXPIRED}, {@code OTP_ATTEMPTS_EXCEEDED}, {@code OTP_USED},
 * {@code OTP_NOT_FOUND}.</p>
 */
public class OtpService {

    /** Maximum wrong-code guesses before the OTP is permanently invalidated. */
    public static final int MAX_VERIFY_ATTEMPTS = 3;

    /** OTPs issued more than this many ticks ago are expired. */
    public static final int OTP_TTL_TICKS = 5;

    private final Map<Long, OtpRecord> otpByAccount = new HashMap<>();
    private final AtomicLong generation = new AtomicLong(0);
    private int tick = 0;

    /** Advance the internal tick counter (used in tests to simulate time passing). */
    public void advanceTick(final int amount) {
        tick += amount;
    }

    /**
     * Generate and store a deterministic 6-digit OTP for {@code accountId}.
     * Any previously-issued OTP for that account is overwritten.
     *
     * @return the 6-digit numeric OTP string
     */
    public String generate(final long accountId) {
        final long gen = generation.incrementAndGet();
        // Deterministic seed: mix account-id and generation count — no randomness.
        final long seed = accountId * 1_000_003L + gen * 999_983L;
        final String code = String.format("%06d", Math.abs(seed) % 1_000_000L);
        otpByAccount.put(accountId, new OtpRecord(code, tick));
        return code;
    }

    /**
     * Verify the supplied code for {@code accountId}.
     *
     * @throws DomainException {@code OTP_NOT_FOUND} — no active OTP for account,
     *                         {@code OTP_USED} — already consumed,
     *                         {@code OTP_EXPIRED} — past its TTL,
     *                         {@code OTP_ATTEMPTS_EXCEEDED} — too many wrong guesses,
     *                         {@code OTP_INVALID} — code does not match
     */
    public void verify(final long accountId, final String code) {
        final OtpRecord rec = otpByAccount.get(accountId);
        if (rec == null) {
            throw new DomainException("OTP_NOT_FOUND",
                    "No OTP active for account: " + accountId);
        }
        if (rec.used) {
            throw new DomainException("OTP_USED",
                    "OTP has already been used for account: " + accountId);
        }
        if ((tick - rec.issuedAtTick) > OTP_TTL_TICKS) {
            throw new DomainException("OTP_EXPIRED",
                    "OTP has expired for account: " + accountId);
        }
        if (rec.attempts >= MAX_VERIFY_ATTEMPTS) {
            throw new DomainException("OTP_ATTEMPTS_EXCEEDED",
                    "Maximum verification attempts exceeded for account: " + accountId);
        }
        if (!rec.code.equals(code)) {
            rec.attempts++;
            if (rec.attempts >= MAX_VERIFY_ATTEMPTS) {
                throw new DomainException("OTP_ATTEMPTS_EXCEEDED",
                        "Maximum verification attempts exceeded for account: " + accountId);
            }
            throw new DomainException("OTP_INVALID",
                    "Incorrect OTP code for account: " + accountId);
        }
        rec.used = true;
    }

    /** Return the raw OTP code currently stored for {@code accountId} (test helper). */
    public String currentCode(final long accountId) {
        final OtpRecord rec = otpByAccount.get(accountId);
        if (rec == null) {
            throw new DomainException("OTP_NOT_FOUND",
                    "No OTP active for account: " + accountId);
        }
        return rec.code;
    }

    // ── inner class ───────────────────────────────────────────────────────────

    private static final class OtpRecord {
        final String code;
        final int issuedAtTick;
        int attempts = 0;
        boolean used = false;

        OtpRecord(final String code, final int issuedAtTick) {
            this.code = code;
            this.issuedAtTick = issuedAtTick;
        }
    }
}
