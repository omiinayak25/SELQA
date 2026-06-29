package com.omiinqa.reference.identity;

import com.omiinqa.reference.core.DomainException;
import com.omiinqa.reference.core.Validations;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory authentication service with real, asserted business rules:
 * registration validation, credential checks, and account lockout after a
 * threshold of failed attempts. State is per-instance so each BDD scenario gets
 * an isolated world.
 *
 * <p>Error codes (asserted by scenarios): {@code AUTH_DUP_USERNAME},
 * {@code AUTH_DUP_EMAIL}, {@code AUTH_BAD_EMAIL}, {@code AUTH_WEAK_PASSWORD},
 * {@code AUTH_BLANK}, {@code AUTH_NOT_FOUND}, {@code AUTH_INVALID_CREDENTIALS},
 * {@code AUTH_LOCKED}, {@code AUTH_DISABLED}.</p>
 */
public class AuthService {

    /** Failed-login attempts before the account is locked. */
    public static final int MAX_FAILED_ATTEMPTS = 5;

    private final ConcurrentHashMap<String, Account> byUsername = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Account> byEmail = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong(1000);

    /** Register a new account, enforcing uniqueness and input validation. */
    public Account register(final String username, final String email, final String password) {
        Validations.requireNotBlank(username, "username", "AUTH_BLANK");
        Validations.requireValidEmail(email, "AUTH_BAD_EMAIL");
        Validations.requireStrongPassword(password, "AUTH_WEAK_PASSWORD");
        if (byUsername.containsKey(username.toLowerCase())) {
            throw new DomainException("AUTH_DUP_USERNAME", "Username already taken: " + username);
        }
        if (byEmail.containsKey(email.toLowerCase())) {
            throw new DomainException("AUTH_DUP_EMAIL", "Email already registered: " + email);
        }
        final Account account = Account.builder()
                .id(ids.incrementAndGet())
                .username(username)
                .email(email)
                .password(password)
                .status(Account.Status.ACTIVE)
                .build();
        byUsername.put(username.toLowerCase(), account);
        byEmail.put(email.toLowerCase(), account);
        return account;
    }

    /**
     * Authenticate by username or email. Increments the failure counter on a bad
     * password and locks the account when it reaches {@link #MAX_FAILED_ATTEMPTS}.
     *
     * @return the authenticated account
     * @throws DomainException with a specific code on any failure
     */
    public Account login(final String identifier, final String password) {
        final Account account = find(identifier)
                .orElseThrow(() -> new DomainException("AUTH_NOT_FOUND",
                        "No account for: " + identifier));
        if (account.getStatus() == Account.Status.DISABLED) {
            throw new DomainException("AUTH_DISABLED", "Account is disabled");
        }
        if (account.getStatus() == Account.Status.LOCKED) {
            throw new DomainException("AUTH_LOCKED", "Account is locked");
        }
        if (!account.getPassword().equals(password)) {
            account.setFailedLoginAttempts(account.getFailedLoginAttempts() + 1);
            if (account.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
                account.setStatus(Account.Status.LOCKED);
                throw new DomainException("AUTH_LOCKED",
                        "Account locked after " + MAX_FAILED_ATTEMPTS + " failed attempts");
            }
            throw new DomainException("AUTH_INVALID_CREDENTIALS", "Incorrect password");
        }
        account.setFailedLoginAttempts(0);
        return account;
    }

    public Optional<Account> find(final String identifier) {
        if (identifier == null) {
            return Optional.empty();
        }
        final String key = identifier.toLowerCase();
        return Optional.ofNullable(byUsername.getOrDefault(key, byEmail.get(key)));
    }

    public int accountCount() {
        return byUsername.size();
    }
}
