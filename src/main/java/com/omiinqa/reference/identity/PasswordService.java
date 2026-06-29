package com.omiinqa.reference.identity;

import com.omiinqa.reference.core.DomainException;
import com.omiinqa.reference.core.Validations;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * In-memory password-management service supporting:
 * <ul>
 *   <li>Authenticated password change (verify old password, strength check, reuse
 *       prevention for the last {@link #PASSWORD_HISTORY_SIZE} passwords).</li>
 *   <li>Stateless password-reset flow (issue token → verify → consume once).</li>
 * </ul>
 *
 * <p>Reset-token expiry is tracked via a call-counter ("tick") so BDD scenarios
 * remain deterministic without {@code Thread.sleep}.</p>
 *
 * <p>Error codes (asserted by scenarios): {@code PWD_NOT_FOUND},
 * {@code PWD_WRONG_CURRENT}, {@code PWD_WEAK}, {@code PWD_REUSED},
 * {@code PWD_RESET_TOKEN_INVALID}, {@code PWD_RESET_TOKEN_EXPIRED},
 * {@code PWD_RESET_TOKEN_USED}.</p>
 */
public class PasswordService {

    /** Number of previous passwords kept to prevent reuse. */
    public static final int PASSWORD_HISTORY_SIZE = 3;

    /** Reset tokens issued more than this many ticks ago are expired. */
    public static final int RESET_TOKEN_TTL_TICKS = 5;

    private final Map<Long, PasswordRecord> records = new HashMap<>();
    private final Map<String, ResetToken> resetTokens = new HashMap<>();
    private int tick = 0;

    /** Advance the internal tick counter (used in tests to simulate time passing). */
    public void advanceTick(final int amount) {
        tick += amount;
    }

    /**
     * Register an account with the password service.  Must be called once before
     * any password operation is invoked for that account.
     */
    public void registerAccount(final Account account) {
        records.putIfAbsent(account.getId(), new PasswordRecord(account.getPassword()));
    }

    /**
     * Change the password after verifying the current one.
     *
     * @throws DomainException {@code PWD_WRONG_CURRENT} if the old password is wrong,
     *                         {@code PWD_WEAK} if the new password fails strength rules,
     *                         {@code PWD_REUSED} if the new password appears in the history
     */
    public void changePassword(final long accountId,
                               final String currentPassword,
                               final String newPassword) {
        final PasswordRecord rec = require(accountId);
        if (!rec.current.equals(currentPassword)) {
            throw new DomainException("PWD_WRONG_CURRENT", "Current password is incorrect");
        }
        Validations.requireStrongPassword(newPassword, "PWD_WEAK");
        if (rec.isInHistory(newPassword)) {
            throw new DomainException("PWD_REUSED",
                    "New password must not match any of the last " + PASSWORD_HISTORY_SIZE + " passwords");
        }
        rec.setPassword(newPassword);
    }

    /**
     * Issue a password-reset token for the given account.
     *
     * @return the opaque reset token
     */
    public String issueResetToken(final long accountId) {
        require(accountId);
        final String token = "RST-" + accountId + "-" + tick + "-" + accountId * 31;
        resetTokens.put(token, new ResetToken(accountId, tick));
        return token;
    }

    /**
     * Reset the password using a previously-issued token. The token is consumed
     * on first use; subsequent calls with the same token raise
     * {@code PWD_RESET_TOKEN_USED}.
     */
    public void resetPassword(final String token, final String newPassword) {
        final ResetToken rt = resetTokens.get(token);
        if (rt == null) {
            throw new DomainException("PWD_RESET_TOKEN_INVALID",
                    "Reset token not recognised: " + token);
        }
        if (rt.used) {
            throw new DomainException("PWD_RESET_TOKEN_USED",
                    "Reset token has already been used");
        }
        if ((tick - rt.issuedAtTick) > RESET_TOKEN_TTL_TICKS) {
            throw new DomainException("PWD_RESET_TOKEN_EXPIRED",
                    "Reset token has expired");
        }
        Validations.requireStrongPassword(newPassword, "PWD_WEAK");
        rt.used = true;
        final PasswordRecord rec = require(rt.accountId);
        rec.setPassword(newPassword);
    }

    /** Return the current (hashed-equivalent) password stored for an account. */
    public String currentPassword(final long accountId) {
        return require(accountId).current;
    }

    // ── private ───────────────────────────────────────────────────────────────

    private PasswordRecord require(final long accountId) {
        final PasswordRecord rec = records.get(accountId);
        if (rec == null) {
            throw new DomainException("PWD_NOT_FOUND",
                    "No password record for account id: " + accountId);
        }
        return rec;
    }

    // ── inner helpers ─────────────────────────────────────────────────────────

    private static final class PasswordRecord {
        String current;
        final Deque<String> history = new ArrayDeque<>();

        PasswordRecord(final String initialPassword) {
            this.current = initialPassword;
        }

        boolean isInHistory(final String candidate) {
            return current.equals(candidate) || history.contains(candidate);
        }

        void setPassword(final String newPassword) {
            history.addFirst(current);
            while (history.size() > PASSWORD_HISTORY_SIZE) {
                history.removeLast();
            }
            current = newPassword;
        }
    }

    private static final class ResetToken {
        final long accountId;
        final int issuedAtTick;
        boolean used = false;

        ResetToken(final long accountId, final int issuedAtTick) {
            this.accountId = accountId;
            this.issuedAtTick = issuedAtTick;
        }
    }
}
