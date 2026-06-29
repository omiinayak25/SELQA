package com.omiinqa.reference.identity;

import com.omiinqa.reference.core.DomainException;
import com.omiinqa.reference.core.Validations;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory registration service implementing the full sign-up lifecycle:
 * terms acceptance enforcement, email-verification token issue and verification,
 * and duplicate detection.
 *
 * <p>Token expiry is modelled by an injected call-counter ("tick") rather than
 * wall-clock time so BDD scenarios remain deterministic and instant.</p>
 *
 * <p>Error codes (asserted by scenarios): {@code REG_TERMS_REQUIRED},
 * {@code REG_BLANK}, {@code REG_BAD_EMAIL}, {@code REG_WEAK_PASSWORD},
 * {@code REG_DUP_EMAIL}, {@code REG_DUP_USERNAME},
 * {@code REG_TOKEN_INVALID}, {@code REG_TOKEN_EXPIRED},
 * {@code REG_NOT_FOUND}, {@code REG_ALREADY_VERIFIED}.</p>
 */
public class RegistrationService {

    /** Tokens issued more than this many "ticks" ago are expired. */
    public static final int TOKEN_TTL_TICKS = 5;

    private final Map<String, PendingRegistration> pendingByEmail = new HashMap<>();
    private final Map<String, Account> verifiedByEmail = new HashMap<>();
    private final Map<String, Account> verifiedByUsername = new HashMap<>();
    private final Map<String, IssuedToken> tokenStore = new HashMap<>();
    private final AtomicLong ids = new AtomicLong(2000);
    private int tick = 0;

    /** Advance the internal tick counter (used in tests to simulate time passing). */
    public void advanceTick(final int amount) {
        tick += amount;
    }

    /**
     * Begin registration: validates inputs, enforces terms acceptance and uniqueness,
     * stores a pending registration, and issues an e-mail verification token.
     *
     * @return the opaque verification token to be sent by e-mail
     */
    public String beginRegistration(final String username,
                                    final String email,
                                    final String password,
                                    final boolean termsAccepted) {
        if (!termsAccepted) {
            throw new DomainException("REG_TERMS_REQUIRED",
                    "User must accept terms and conditions before registering");
        }
        Validations.requireNotBlank(username, "username", "REG_BLANK");
        Validations.requireValidEmail(email, "REG_BAD_EMAIL");
        Validations.requireStrongPassword(password, "REG_WEAK_PASSWORD");

        final String emailKey = email.toLowerCase();
        final String usernameKey = username.toLowerCase();

        if (verifiedByEmail.containsKey(emailKey) || pendingByEmail.containsKey(emailKey)) {
            throw new DomainException("REG_DUP_EMAIL", "Email already registered: " + email);
        }
        if (verifiedByUsername.containsKey(usernameKey)) {
            throw new DomainException("REG_DUP_USERNAME", "Username already taken: " + username);
        }

        final PendingRegistration pending = new PendingRegistration(username, email, password);
        pendingByEmail.put(emailKey, pending);

        final String token = "VRFY-" + usernameKey + "-" + ids.incrementAndGet();
        tokenStore.put(token, new IssuedToken(emailKey, tick));
        return token;
    }

    /**
     * Complete registration by consuming the verification token.
     *
     * @return the newly activated {@link Account}
     */
    public Account verifyEmail(final String token) {
        final IssuedToken issued = tokenStore.get(token);
        if (issued == null) {
            throw new DomainException("REG_TOKEN_INVALID", "Verification token not recognised: " + token);
        }
        if ((tick - issued.issuedAtTick) > TOKEN_TTL_TICKS) {
            throw new DomainException("REG_TOKEN_EXPIRED", "Verification token has expired");
        }
        tokenStore.remove(token);

        final PendingRegistration pending = pendingByEmail.get(issued.emailKey);
        if (pending == null) {
            throw new DomainException("REG_NOT_FOUND", "No pending registration for token");
        }
        pendingByEmail.remove(issued.emailKey);

        final Account account = Account.builder()
                .id(ids.incrementAndGet())
                .username(pending.username)
                .email(pending.email)
                .password(pending.password)
                .emailVerified(true)
                .status(Account.Status.ACTIVE)
                .build();
        verifiedByEmail.put(issued.emailKey, account);
        verifiedByUsername.put(pending.username.toLowerCase(), account);
        return account;
    }

    /**
     * Look up a verified account by e-mail.
     *
     * @return the account or {@code null} if not found / not yet verified
     */
    public Account findVerified(final String email) {
        return verifiedByEmail.get(email.toLowerCase());
    }

    public int pendingCount() {
        return pendingByEmail.size();
    }

    public int verifiedCount() {
        return verifiedByEmail.size();
    }

    // ── inner helpers ─────────────────────────────────────────────────────────

    private static final class PendingRegistration {
        final String username;
        final String email;
        final String password;

        PendingRegistration(final String username, final String email, final String password) {
            this.username = username;
            this.email = email;
            this.password = password;
        }
    }

    private static final class IssuedToken {
        final String emailKey;
        final int issuedAtTick;

        IssuedToken(final String emailKey, final int issuedAtTick) {
            this.emailKey = emailKey;
            this.issuedAtTick = issuedAtTick;
        }
    }
}
