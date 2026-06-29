package com.omiinqa.data.factory;

import com.omiinqa.data.builder.UserTestDataBuilder;
import com.omiinqa.data.model.User;

/**
 * Factory for semantically-named {@link User} test fixtures.
 *
 * <p><strong>Pattern:</strong> Static Factory — each method encodes a
 * meaningful test scenario rather than requiring callers to construct and
 * configure objects inline. This keeps tests readable ({@code
 * UserFactory.validUser()}) and concentrates the definition of "what is a
 * valid user" in one place, so changes propagate automatically.</p>
 *
 * <p>All methods return new instances on every call (no caching), so tests
 * never share mutable state.</p>
 *
 * <p><strong>Usage:</strong>
 * <pre>{@code
 *   User valid   = UserFactory.validUser();
 *   User admin   = UserFactory.adminUser();
 *   User noEmail = UserFactory.userWithMissingEmail();
 * }</pre>
 * </p>
 */
public final class UserFactory {

    private UserFactory() {
        // static factory — not instantiable
    }

    // -------------------------------------------------------------------------
    // Factory methods
    // -------------------------------------------------------------------------

    /**
     * Returns a fully-populated, valid user suitable for happy-path registration
     * or profile tests.
     *
     * @return valid user with all fields populated
     */
    public static User validUser() {
        return UserTestDataBuilder.aUser().random().build();
    }

    /**
     * Returns a user with intentionally invalid data (e.g., malformed email,
     * blank password) to exercise validation-error paths.
     *
     * <p>The invalid data is consistent: the email lacks an {@code @} symbol
     * and the password is empty, matching typical constraint violations.</p>
     *
     * @return invalid user for negative test scenarios
     */
    public static User invalidUser() {
        return UserTestDataBuilder.aUser()
                .random()
                .withEmail("not-a-valid-email")
                .withPassword("")
                .build();
    }

    /**
     * Returns a user that represents an administrative account, with a
     * deterministic username for role-based access control (RBAC) tests.
     *
     * @return admin user with {@code admin_} prefixed username
     */
    public static User adminUser() {
        return UserTestDataBuilder.aUser()
                .random()
                .withUsername("admin_" + System.currentTimeMillis())
                .withEmail("admin@omiinqa.test")
                .build();
    }

    /**
     * Returns a user whose email field is {@code null}, triggering
     * required-field validation in registration / update flows.
     *
     * @return user with null email for missing-field validation tests
     */
    public static User userWithMissingEmail() {
        return UserTestDataBuilder.aUser()
                .random()
                .withEmail(null)
                .build();
    }

    /**
     * Returns a user with a missing (null) password for password-required
     * validation tests.
     *
     * @return user with null password
     */
    public static User userWithMissingPassword() {
        return UserTestDataBuilder.aUser()
                .random()
                .withPassword(null)
                .build();
    }

    /**
     * Returns a user with a duplicate / pre-existing email to exercise
     * conflict-detection (HTTP 409 / "email already taken") scenarios.
     *
     * @return user with a fixed, known-conflicting email address
     */
    public static User duplicateEmailUser() {
        return UserTestDataBuilder.aUser()
                .random()
                .withEmail("existing.user@omiinqa.test")
                .build();
    }
}
