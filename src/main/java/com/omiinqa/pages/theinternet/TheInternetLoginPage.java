package com.omiinqa.pages.theinternet;

import com.omiinqa.config.FrameworkConfig;
import com.omiinqa.core.BasePage;
import org.openqa.selenium.By;

/**
 * Page Object for the The-Internet login screen
 * ({@code https://the-internet.herokuapp.com/login}).
 *
 * <p><b>Page Object Model contract:</b></p>
 * <ul>
 *   <li>Contains <em>no assertions</em> — state is surfaced via getters so this
 *       class is reusable across positive, negative, and boundary test cases.</li>
 *   <li>Fluent API: {@link #login(String, String)} returns {@code this} so callers
 *       may chain directly into a state-query method without an intermediate
 *       variable.</li>
 *   <li>Locators are {@code private static final} {@link By} fields — declared
 *       once, named for intent, impossible to scatter or duplicate.</li>
 * </ul>
 *
 * <p>Always call {@link #open()} at the start of a test to land on a known URL
 * rather than relying on ambient browser state.</p>
 */
public class TheInternetLoginPage extends BasePage {

    // ---------------------------------------------------------------- locators

    /** Username text-input field. */
    private static final By USERNAME = By.id("username");

    /** Password text-input field. */
    private static final By PASSWORD = By.id("password");

    /** Submit / «Login» button. */
    private static final By LOGIN_BTN = By.cssSelector("button[type='submit']");

    /**
     * Flash-message {@code <div>} shown after both successful and failed attempts.
     * The element is present in the DOM only after a form submission.
     */
    private static final By FLASH_MESSAGE = By.id("flash");

    // ----------------------------------------------------------------- actions

    /**
     * Navigates the browser to the login page URL as configured in
     * {@code FrameworkConfig.get().appUrl("theinternet")} with the {@code /login}
     * path appended.
     *
     * @return this {@link TheInternetLoginPage} for method chaining
     */
    public TheInternetLoginPage open() {
        final String url = FrameworkConfig.get().appUrl("theinternet") + "/login";
        log.info("Opening The-Internet login page: {}", url);
        driver().get(url);
        waitForUrlContains("/login");
        return this;
    }

    /**
     * Fills the username and password fields and clicks the login button.
     *
     * <p>No navigation assertion is performed here — callers inspect
     * {@link #getFlashMessage()}, {@link #isFlashSuccess()}, or
     * {@link #isFlashError()} to verify the outcome.</p>
     *
     * @param username the value to type into the username field
     * @param password the value to type into the password field
     * @return this {@link TheInternetLoginPage} for method chaining
     */
    public TheInternetLoginPage login(final String username, final String password) {
        log.info("Attempting login as '{}'", username);
        type(USERNAME, username);
        type(PASSWORD, password);
        click(LOGIN_BTN);
        return this;
    }

    // ----------------------------------------------------------------- queries

    /**
     * Returns the trimmed text content of the flash-message {@code <div>}.
     *
     * <p>The flash element is rendered only after a form submission; call this
     * method after {@link #login(String, String)} to read the result.</p>
     *
     * @return trimmed flash-message text; never {@code null}
     */
    public String getFlashMessage() {
        return getText(FLASH_MESSAGE);
    }

    /**
     * Returns {@code true} when the flash-message element carries the CSS class
     * {@code success}, indicating a successful authentication.
     *
     * @return {@code true} on success, {@code false} otherwise
     */
    public boolean isFlashSuccess() {
        final String classes = getAttribute(FLASH_MESSAGE, "class");
        return classes != null && classes.contains("success");
    }

    /**
     * Returns {@code true} when the flash-message element carries the CSS class
     * {@code error}, indicating a failed authentication attempt.
     *
     * @return {@code true} on failure, {@code false} otherwise
     */
    public boolean isFlashError() {
        final String classes = getAttribute(FLASH_MESSAGE, "class");
        return classes != null && classes.contains("error");
    }
}
