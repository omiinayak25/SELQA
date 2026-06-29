package com.omiinqa.businessflows;

import com.omiinqa.pages.saucedemo.LoginPage;
import com.omiinqa.pages.saucedemo.ProductsPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Facade for SauceDemo authentication flows.
 *
 * <p><b>Facade pattern rationale:</b> tests frequently need to reach an
 * authenticated state before executing their real scenario. Writing
 * {@code new LoginPage().open().login(user, pass)} in every test method creates
 * fragile duplication — if the login URL or credentials change, every test
 * breaks. {@link LoginFlow} centralises that orchestration: each credential
 * variant is one named method, tests express <em>intent</em> rather than
 * mechanics, and maintenance is a one-line change here.</p>
 *
 * <p><b>No assertions inside flows:</b> flows compose page objects into journeys
 * and return the final page object so tests can assert on it. Flows are
 * reusable data-setup infrastructure, not test logic.</p>
 */
public final class LoginFlow {

    private static final Logger log = LoggerFactory.getLogger(LoginFlow.class);

    // SauceDemo shared credentials
    private static final String PASSWORD = "secret_sauce";

    private LoginFlow() {
        // Utility class — instantiation prevented
    }

    // ----------------------------------------------------------------- flows

    /**
     * Opens SauceDemo and logs in as the standard (fully functional) user.
     *
     * @return the {@link ProductsPage} reached after successful login
     */
    public static ProductsPage loginAsStandardUser() {
        log.info("LoginFlow: logging in as standard_user");
        return loginAs("standard_user", PASSWORD);
    }

    /**
     * Opens SauceDemo and logs in as the locked-out user (expects login to fail;
     * the returned page object is on the login page — callers should check
     * {@link com.omiinqa.pages.saucedemo.LoginPage#isErrorDisplayed()} via the
     * login page reference, not this return value).
     *
     * <p>Note: because the locked-out user never reaches the inventory,
     * {@link ProductsPage#isLoaded()} will return {@code false}. Tests that
     * exercise locked-out behaviour should call
     * {@link #loginAsLockedOutUser()} and then verify the error via a
     * {@link com.omiinqa.pages.saucedemo.LoginPage} instance.</p>
     *
     * @return a {@link ProductsPage} handle (not loaded — for locked-out flow);
     *         use a separate {@link com.omiinqa.pages.saucedemo.LoginPage}
     *         reference to inspect the error
     */
    public static ProductsPage loginAsLockedOutUser() {
        log.info("LoginFlow: logging in as locked_out_user");
        return loginAs("locked_out_user", PASSWORD);
    }

    /**
     * Opens SauceDemo and logs in as the problem user (images are broken,
     * useful for visual-regression or robustness scenarios).
     *
     * @return the {@link ProductsPage}
     */
    public static ProductsPage loginAsProblemUser() {
        log.info("LoginFlow: logging in as problem_user");
        return loginAs("problem_user", PASSWORD);
    }

    /**
     * Opens SauceDemo and logs in as the performance-glitch user (page loads
     * are artificially delayed — useful for timeout/slow-network scenarios).
     *
     * @return the {@link ProductsPage}
     */
    public static ProductsPage loginAsPerformanceGlitchUser() {
        log.info("LoginFlow: logging in as performance_glitch_user");
        return loginAs("performance_glitch_user", PASSWORD);
    }

    /**
     * Opens SauceDemo and logs in with the supplied credentials.
     *
     * <p>Use this overload when a test needs a custom username/password pair
     * (e.g. an error-case with a completely wrong password).</p>
     *
     * @param username SauceDemo username
     * @param password SauceDemo password
     * @return the {@link ProductsPage} reached after clicking login
     */
    public static ProductsPage loginAs(final String username, final String password) {
        log.info("LoginFlow: loginAs({}, ***)", username);
        return new LoginPage()
                .open()
                .login(username, password);
    }
}
