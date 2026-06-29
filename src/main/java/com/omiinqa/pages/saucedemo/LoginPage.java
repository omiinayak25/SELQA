package com.omiinqa.pages.saucedemo;

import com.omiinqa.config.FrameworkConfig;
import com.omiinqa.core.BasePage;
import org.openqa.selenium.By;

/**
 * Page Object for the SauceDemo login screen
 * ({@code https://www.saucedemo.com}).
 *
 * <p><b>Page Object Model contract:</b></p>
 * <ul>
 *   <li>Contains <em>no assertions</em> — state is surfaced via getters so this
 *       class is reusable across positive, negative, and boundary test cases.</li>
 *   <li>Fluent navigation: {@link #login(String, String)} returns
 *       {@link ProductsPage} on success, allowing test code to chain page calls
 *       naturally.</li>
 *   <li>Locators are {@code private static final} {@link By} fields — declared
 *       once, named for intent, impossible to scatter or duplicate.</li>
 * </ul>
 *
 * <p>Callers should always start a test with {@link #open()} to land on a
 * known URL, rather than relying on browser state.</p>
 */
public class LoginPage extends BasePage {

    // ---------------------------------------------------------------- locators
    private static final By USERNAME_INPUT = By.id("user-name");
    private static final By PASSWORD_INPUT = By.id("password");
    private static final By LOGIN_BUTTON   = By.id("login-button");
    private static final By ERROR_MESSAGE  = By.cssSelector("[data-test='error']");

    // ----------------------------------------------------------------- actions

    /**
     * Navigates the browser to the SauceDemo login URL as configured in
     * {@code FrameworkConfig.get().appUrl("saucedemo")}.
     *
     * @return this {@link LoginPage} for method chaining (self-return on open)
     */
    public LoginPage open() {
        final String url = FrameworkConfig.get().appUrl("saucedemo");
        log.info("Opening SauceDemo login page: {}", url);
        driver().get(url);
        waitForUrlContains("saucedemo.com");
        return this;
    }

    /**
     * Fills in the credentials and submits the login form.
     *
     * <p>On success the application redirects to the inventory/products page,
     * so this method returns a {@link ProductsPage}. On failure the page stays
     * on login — the test should check {@link #getErrorMessage()} to verify the
     * expected error rather than calling this method's return value.</p>
     *
     * @param username SauceDemo username (e.g. {@code standard_user})
     * @param password SauceDemo password (e.g. {@code secret_sauce})
     * @return a {@link ProductsPage} representing the page reached after login
     */
    public ProductsPage login(final String username, final String password) {
        log.info("Logging in as '{}'", username);
        type(USERNAME_INPUT, username);
        type(PASSWORD_INPUT, password);
        click(LOGIN_BUTTON);
        return new ProductsPage();
    }

    // ----------------------------------------------------------------- queries

    /**
     * Returns the text of the error banner shown after a failed login attempt.
     * Returns an empty string when no error is present (use
     * {@link #isErrorDisplayed()} first to guard).
     *
     * @return the error message text, trimmed; empty string if absent
     */
    public String getErrorMessage() {
        if (!isErrorDisplayed()) {
            return "";
        }
        return getText(ERROR_MESSAGE);
    }

    /**
     * @return {@code true} when the {@code [data-test="error"]} banner is
     *         visible — indicates a failed login attempt
     */
    public boolean isErrorDisplayed() {
        return isDisplayed(ERROR_MESSAGE);
    }
}
