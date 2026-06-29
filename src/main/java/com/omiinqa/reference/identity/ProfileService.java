package com.omiinqa.reference.identity;

import com.omiinqa.reference.core.DomainException;
import com.omiinqa.reference.core.Validations;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * In-memory profile service for reading and updating user profile data.
 *
 * <p>Profiles are stored per-account (keyed by account-id). An account must be
 * registered through this service before its profile can be mutated. Phone
 * numbers are validated against a loose E.164-like rule; bio length is capped
 * at {@link #MAX_BIO_LENGTH} characters.</p>
 *
 * <p>Error codes (asserted by scenarios): {@code PROFILE_NOT_FOUND},
 * {@code PROFILE_BAD_PHONE}, {@code PROFILE_BIO_TOO_LONG},
 * {@code PROFILE_BAD_EMAIL}, {@code PROFILE_BLANK_NAME},
 * {@code PROFILE_DISPLAY_NAME_TOO_LONG}.</p>
 */
public class ProfileService {

    /** Maximum allowed bio length in characters. */
    public static final int MAX_BIO_LENGTH = 300;

    /** Maximum display-name length in characters. */
    public static final int MAX_DISPLAY_NAME_LENGTH = 50;

    /** E.164-ish: optional leading +, then 7-15 digits. */
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9]{7,15}$");

    private final Map<Long, Profile> profiles = new HashMap<>();

    /**
     * Register an account with the profile service so it can be updated later.
     * Called during on-boarding; safe to call multiple times for the same account.
     */
    public void registerAccount(final Account account) {
        profiles.putIfAbsent(account.getId(), new Profile(account));
    }

    /**
     * Update the first and last name of an account's profile.
     *
     * @return the updated profile
     */
    public Profile updateName(final long accountId,
                              final String firstName,
                              final String lastName) {
        final Profile p = require(accountId);
        Validations.requireNotBlank(firstName, "firstName", "PROFILE_BLANK_NAME");
        Validations.requireNotBlank(lastName, "lastName", "PROFILE_BLANK_NAME");
        p.firstName = firstName.strip();
        p.lastName = lastName.strip();
        return p;
    }

    /**
     * Update the display name.
     *
     * @return the updated profile
     */
    public Profile updateDisplayName(final long accountId, final String displayName) {
        final Profile p = require(accountId);
        Validations.requireNotBlank(displayName, "displayName", "PROFILE_BLANK_NAME");
        if (displayName.strip().length() > MAX_DISPLAY_NAME_LENGTH) {
            throw new DomainException("PROFILE_DISPLAY_NAME_TOO_LONG",
                    "Display name must not exceed " + MAX_DISPLAY_NAME_LENGTH + " characters");
        }
        p.displayName = displayName.strip();
        return p;
    }

    /**
     * Update the phone number; must pass E.164-ish validation.
     *
     * @return the updated profile
     */
    public Profile updatePhone(final long accountId, final String phone) {
        final Profile p = require(accountId);
        if (phone == null || !PHONE.matcher(phone.strip()).matches()) {
            throw new DomainException("PROFILE_BAD_PHONE",
                    "Phone number must be E.164-ish (7-15 digits, optional leading +): " + phone);
        }
        p.phone = phone.strip();
        return p;
    }

    /**
     * Update the bio; capped at {@link #MAX_BIO_LENGTH} characters.
     *
     * @return the updated profile
     */
    public Profile updateBio(final long accountId, final String bio) {
        final Profile p = require(accountId);
        if (bio != null && bio.length() > MAX_BIO_LENGTH) {
            throw new DomainException("PROFILE_BIO_TOO_LONG",
                    "Bio must not exceed " + MAX_BIO_LENGTH + " characters, got " + bio.length());
        }
        p.bio = bio;
        return p;
    }

    /**
     * Request an e-mail address change. The new address is stored as pending until
     * the caller invokes {@link #confirmEmailChange(long, String)}.
     *
     * @return the verification token
     */
    public String requestEmailChange(final long accountId, final String newEmail) {
        final Profile p = require(accountId);
        Validations.requireValidEmail(newEmail, "PROFILE_BAD_EMAIL");
        p.pendingEmail = newEmail.toLowerCase();
        return "ECHG-" + accountId + "-" + newEmail.hashCode();
    }

    /**
     * Confirm an e-mail change by consuming the token that was returned from
     * {@link #requestEmailChange(long, String)}.
     *
     * @return the updated profile
     */
    public Profile confirmEmailChange(final long accountId, final String token) {
        final Profile p = require(accountId);
        final String expected = "ECHG-" + accountId + "-" + p.pendingEmail.hashCode();
        if (!expected.equals(token)) {
            throw new DomainException("PROFILE_BAD_EMAIL",
                    "Email-change token is invalid or does not match pending email");
        }
        p.email = p.pendingEmail;
        p.pendingEmail = null;
        return p;
    }

    /** Retrieve a profile by account id. */
    public Profile get(final long accountId) {
        return require(accountId);
    }

    // ── private ───────────────────────────────────────────────────────────────

    private Profile require(final long accountId) {
        final Profile p = profiles.get(accountId);
        if (p == null) {
            throw new DomainException("PROFILE_NOT_FOUND",
                    "No profile found for account id: " + accountId);
        }
        return p;
    }

    // ── Profile value object ──────────────────────────────────────────────────

    /**
     * Mutable profile data for a single user account.
     */
    public static final class Profile {
        private final long accountId;
        public String firstName;
        public String lastName;
        public String displayName;
        public String phone;
        public String bio;
        public String email;
        public String pendingEmail;

        Profile(final Account account) {
            this.accountId = account.getId();
            this.email = account.getEmail();
            this.displayName = account.getUsername();
        }

        public long getAccountId() {
            return accountId;
        }
    }
}
