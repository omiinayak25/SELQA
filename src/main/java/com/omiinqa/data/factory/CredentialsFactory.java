package com.omiinqa.data.factory;

import com.omiinqa.data.model.Credentials;

/**
 * Factory for {@link Credentials} test fixtures encoding all known SauceDemo
 * and framework-standard user accounts.
 *
 * <p><strong>Pattern:</strong> Static Factory — encodes the SauceDemo user
 * matrix in one authoritative place. When SauceDemo credentials change,
 * only this class needs updating; all tests that call these methods benefit
 * automatically.</p>
 *
 * <p><strong>SauceDemo users (password {@code secret_sauce} for all):</strong>
 * <ul>
 *   <li>{@code standard_user} — normal user; full application access</li>
 *   <li>{@code locked_out_user} — account locked; login should fail</li>
 *   <li>{@code problem_user} — logs in but exhibits UI bugs</li>
 *   <li>{@code performance_glitch_user} — logs in with artificial delay</li>
 * </ul>
 * </p>
 *
 * <p><strong>Usage:</strong>
 * <pre>{@code
 *   Credentials std     = CredentialsFactory.standardUser();
 *   Credentials locked  = CredentialsFactory.lockedOutUser();
 *   Credentials invalid = CredentialsFactory.invalidUser();
 * }</pre>
 * </p>
 */
public final class CredentialsFactory {

    /** The shared password for all SauceDemo accounts. */
    private static final String SAUCE_PASSWORD = "secret_sauce";

    private CredentialsFactory() {
        // static factory — not instantiable
    }

    // -------------------------------------------------------------------------
    // SauceDemo accounts
    // -------------------------------------------------------------------------

    /**
     * Returns credentials for the standard SauceDemo user.
     *
     * <p>This account has full, unrestricted access to the application and is
     * used for happy-path login, cart, and checkout tests.</p>
     *
     * @return {@code standard_user} / {@code secret_sauce}
     */
    public static Credentials standardUser() {
        return Credentials.builder()
                .username("standard_user")
                .password(SAUCE_PASSWORD)
                .build();
    }

    /**
     * Returns credentials for the locked-out SauceDemo user.
     *
     * <p>Login with this account is expected to display an error message and
     * deny access. Use in negative login test scenarios.</p>
     *
     * @return {@code locked_out_user} / {@code secret_sauce}
     */
    public static Credentials lockedOutUser() {
        return Credentials.builder()
                .username("locked_out_user")
                .password(SAUCE_PASSWORD)
                .build();
    }

    /**
     * Returns credentials for the problem SauceDemo user.
     *
     * <p>This account logs in successfully but exhibits broken UI behaviours
     * (wrong images, broken add-to-cart). Use to validate error-state detection
     * and visual regression tests.</p>
     *
     * @return {@code problem_user} / {@code secret_sauce}
     */
    public static Credentials problemUser() {
        return Credentials.builder()
                .username("problem_user")
                .password(SAUCE_PASSWORD)
                .build();
    }

    /**
     * Returns credentials for the performance-glitch SauceDemo user.
     *
     * <p>Login and page loads for this account are artificially delayed.
     * Use to validate timeout handling and performance thresholds.</p>
     *
     * @return {@code performance_glitch_user} / {@code secret_sauce}
     */
    public static Credentials performanceGlitchUser() {
        return Credentials.builder()
                .username("performance_glitch_user")
                .password(SAUCE_PASSWORD)
                .build();
    }

    // -------------------------------------------------------------------------
    // Framework / generic accounts
    // -------------------------------------------------------------------------

    /**
     * Returns credentials that do not correspond to any registered account,
     * for testing authentication rejection (wrong username + password).
     *
     * @return intentionally invalid credentials
     */
    public static Credentials invalidUser() {
        return Credentials.builder()
                .username("no_such_user_xyz")
                .password("wrong_password_123")
                .build();
    }

    /**
     * Returns credentials with a valid username but wrong password, for
     * testing partial-match rejection scenarios.
     *
     * @return valid username with incorrect password
     */
    public static Credentials validUsernameWrongPassword() {
        return Credentials.builder()
                .username("standard_user")
                .password("wrong_password")
                .build();
    }

    /**
     * Returns credentials with an empty username for blank-field validation.
     *
     * @return blank username with valid password
     */
    public static Credentials blankUsername() {
        return Credentials.builder()
                .username("")
                .password(SAUCE_PASSWORD)
                .build();
    }

    /**
     * Returns credentials with an empty password for blank-field validation.
     *
     * @return valid username with blank password
     */
    public static Credentials blankPassword() {
        return Credentials.builder()
                .username("standard_user")
                .password("")
                .build();
    }
}
