package com.omiinqa.reference.identity;

import com.omiinqa.reference.core.DomainException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory Multi-Factor Authentication service.
 *
 * <p>Supports TOTP-like code verification (deterministic — no real TOTP library),
 * one-time backup-code generation and single-use consumption, and MFA
 * enable/disable per account.</p>
 *
 * <p>TOTP verification is intentionally deterministic: the "valid" code for an
 * account is derived from the enrolled secret and the current tick value so that
 * BDD scenarios can predict and assert it without randomness.</p>
 *
 * <p>Error codes (asserted by scenarios): {@code MFA_NOT_ENROLLED},
 * {@code MFA_ALREADY_ENROLLED}, {@code MFA_BAD_CODE},
 * {@code MFA_BACKUP_USED}, {@code MFA_BACKUP_INVALID},
 * {@code MFA_NOT_ENABLED}, {@code MFA_NOT_FOUND}.</p>
 */
public class MfaService {

    /** Number of one-time backup codes generated on enrolment. */
    public static final int BACKUP_CODE_COUNT = 8;

    private final Map<Long, MfaRecord> records = new HashMap<>();
    private int tick = 0;

    /** Advance the internal tick counter (used in tests to simulate tick-based windows). */
    public void advanceTick(final int amount) {
        tick += amount;
    }

    /**
     * Enrol an account in MFA.  Returns a new {@link EnrolmentResult} containing
     * the TOTP secret (opaque string) and the backup codes.
     *
     * @throws DomainException {@code MFA_ALREADY_ENROLLED} if the account already has MFA
     */
    public EnrolmentResult enrol(final long accountId) {
        if (records.containsKey(accountId)) {
            throw new DomainException("MFA_ALREADY_ENROLLED",
                    "Account " + accountId + " is already enrolled in MFA");
        }
        final String secret = "TOTP-SECRET-" + accountId;
        final List<String> backupCodes = generateBackupCodes(accountId);
        final MfaRecord rec = new MfaRecord(secret, backupCodes);
        records.put(accountId, rec);
        return new EnrolmentResult(secret, new ArrayList<>(backupCodes));
    }

    /**
     * Verify a TOTP-like code for {@code accountId}.  The expected code for a
     * given tick is derived deterministically from the secret — one tick window
     * either side of the current tick is accepted to simulate clock skew.
     *
     * @throws DomainException {@code MFA_NOT_ENROLLED} or {@code MFA_BAD_CODE}
     */
    public void verifyCode(final long accountId, final String code) {
        final MfaRecord rec = require(accountId);
        for (int delta = -1; delta <= 1; delta++) {
            if (expectedCode(rec.secret, tick + delta).equals(code)) {
                return;
            }
        }
        throw new DomainException("MFA_BAD_CODE",
                "Invalid TOTP code for account: " + accountId);
    }

    /**
     * Return the expected TOTP code for {@code accountId} at the current tick
     * (convenience method used by step definitions to generate valid codes).
     */
    public String currentExpectedCode(final long accountId) {
        return expectedCode(require(accountId).secret, tick);
    }

    /**
     * Use a backup code.  Each backup code is single-use; previously consumed
     * codes raise {@code MFA_BACKUP_USED}; unrecognised codes raise
     * {@code MFA_BACKUP_INVALID}.
     */
    public void useBackupCode(final long accountId, final String code) {
        final MfaRecord rec = require(accountId);
        if (rec.usedBackupCodes.contains(code)) {
            throw new DomainException("MFA_BACKUP_USED",
                    "Backup code has already been used: " + code);
        }
        if (!rec.backupCodes.contains(code)) {
            throw new DomainException("MFA_BACKUP_INVALID",
                    "Backup code not recognised: " + code);
        }
        rec.usedBackupCodes.add(code);
    }

    /**
     * Disable MFA for an account after verifying a valid TOTP code.
     *
     * @throws DomainException {@code MFA_NOT_ENROLLED} or {@code MFA_BAD_CODE}
     */
    public void disable(final long accountId, final String code) {
        verifyCode(accountId, code);
        records.remove(accountId);
    }

    /** Return {@code true} if the account has an active MFA enrolment. */
    public boolean isEnrolled(final long accountId) {
        return records.containsKey(accountId);
    }

    /** Return the backup codes for an account (for test inspection). */
    public List<String> backupCodes(final long accountId) {
        return new ArrayList<>(require(accountId).backupCodes);
    }

    // ── private ───────────────────────────────────────────────────────────────

    private MfaRecord require(final long accountId) {
        final MfaRecord rec = records.get(accountId);
        if (rec == null) {
            throw new DomainException("MFA_NOT_ENROLLED",
                    "Account " + accountId + " is not enrolled in MFA");
        }
        return rec;
    }

    /** Deterministic 6-digit TOTP-like code from secret + tick. */
    private static String expectedCode(final String secret, final int tickValue) {
        final long hash = (secret.hashCode() * 1_000_003L + tickValue * 999_983L) & 0x7FFF_FFFFL;
        return String.format("%06d", hash % 1_000_000L);
    }

    /** Generate deterministic backup codes (seeded from account-id + index). */
    private static List<String> generateBackupCodes(final long accountId) {
        final List<String> codes = new ArrayList<>(BACKUP_CODE_COUNT);
        for (int i = 0; i < BACKUP_CODE_COUNT; i++) {
            final long seed = accountId * 1_000_003L + i * 999_983L + 777_777L;
            codes.add(String.format("BKP-%06d", Math.abs(seed) % 1_000_000L));
        }
        return codes;
    }

    // ── inner classes ─────────────────────────────────────────────────────────

    /** Result of a successful MFA enrolment. */
    public static final class EnrolmentResult {
        public final String secret;
        public final List<String> backupCodes;

        EnrolmentResult(final String secret, final List<String> backupCodes) {
            this.secret = secret;
            this.backupCodes = backupCodes;
        }
    }

    private static final class MfaRecord {
        final String secret;
        final List<String> backupCodes;
        final List<String> usedBackupCodes = new ArrayList<>();

        MfaRecord(final String secret, final List<String> backupCodes) {
            this.secret = secret;
            this.backupCodes = backupCodes;
        }
    }
}
