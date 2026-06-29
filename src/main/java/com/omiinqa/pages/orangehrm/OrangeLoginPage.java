package com.omiinqa.pages.orangehrm;

import com.omiinqa.config.FrameworkConfig;
import com.omiinqa.core.BasePage;
import org.openqa.selenium.By;

/**
 * Page Object for the OrangeHRM login screen.
 *
 * <p><b>Configuration-driven URL:</b> the application URL is read from
 * {@link FrameworkConfig#appUrl(String)} with key {@code "orangehrm"},
 * so environment-specific base URLs live in config files, not in page code.</p>
 *
 * <p><b>POM contract:</b> contains no assertions; exposes error state via
 * {@link #getErrorMessage()} and {@link #isErrorDisplayed()}, and returns the
 * next page object ({@link DashboardPage}) on successful login.</p>
 */
public class OrangeLoginPage extends BasePage {

    // ---------------------------------------------------------------- locators
    private static final By USERNAME_INPUT = By.cssSelector("input[name='username']");
    private static final By PASSWORD_INPUT = By.cssSelector("input[name='password']");
    private static final By LOGIN_BTN      = By.cssSelector("button[type='submit']");
    private static final By ERROR_ALERT    = By.cssSelector(".oxd-alert-content-text, p.oxd-text--p");

    // ----------------------------------------------------------------- actions

    /**
     * Opens the OrangeHRM login URL as configured.
     *
     * @return this page for method-chaining
     */
    public OrangeLoginPage open() {
        final String url = FrameworkConfig.get().appUrl("orangehrm");
        log.info("Opening OrangeHRM login page: {}", url);
        driver().get(url);
        waitForUrlContains("orangehrm");
        return this;
    }

    /**
     * Submits the login form with the supplied credentials.
     *
     * @param username OrangeHRM username
     * @param password OrangeHRM password
     * @return a {@link DashboardPage} (returned even on failure — tests guard
     *         against this by first checking {@link #isErrorDisplayed()})
     */
    public DashboardPage login(final String username, final String password) {
        log.info("OrangeHRM login as '{}'", username);
        type(USERNAME_INPUT, username);
        type(PASSWORD_INPUT, password);
        click(LOGIN_BTN);
        return new DashboardPage();
    }

    // ----------------------------------------------------------------- queries

    /**
     * @return the error alert text after a failed login; empty string if absent
     */
    public String getErrorMessage() {
        if (!isErrorDisplayed()) {
            return "";
        }
        return getText(ERROR_ALERT);
    }

    /**
     * @return {@code true} when a login error alert is visible
     */
    public boolean isErrorDisplayed() {
        return isDisplayed(ERROR_ALERT);
    }
}
