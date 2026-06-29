package com.omiinqa.pages.orangehrm;

import com.omiinqa.core.BasePage;
import org.openqa.selenium.By;

/**
 * Page Object for the OrangeHRM "Forgot Your Password?" assistance page
 * ({@code /auth/requestPasswordResetCode}).
 *
 * <p><b>Purpose:</b> the login screen exposes a "Forgot your password?" link
 * that navigates to this password-reset request page. Authentication tests need
 * to verify the link is reachable and the reset form renders correctly.</p>
 *
 * <p><b>Design note:</b> This page is intentionally small — it is navigated to
 * primarily to verify reachability. Extracting it as a dedicated page object
 * rather than hard-coding the URL in tests allows future tests to interact with
 * the reset form (e.g. submit username) without modifying test code (Open/Closed
 * Principle).</p>
 *
 * <p><b>POM contract:</b> no assertions.</p>
 */
public class LoginAssistPage extends BasePage {

    // ---------------------------------------------------------------- Locators
    /** The "Forgot your password?" link on the login form. */
    private static final By FORGOT_PWD_LINK = By.cssSelector(".orangehrm-login-forgot-header");
    /** Header/title on the reset-request page. */
    private static final By RESET_HEADER    = By.cssSelector(
            ".orangehrm-forgot-password-title, h6.oxd-text--h6");
    /** The username input on the reset request form. */
    private static final By RESET_INPUT     = By.cssSelector("input.oxd-input");
    /** "Reset Password" submit button on the forgot-password form. */
    private static final By RESET_BTN       = By.xpath("//button[@type='submit']");
    /** "Cancel" button that returns the user to the login page. */
    private static final By CANCEL_BTN      = By.xpath(
            "//button[@type='button' and normalize-space()='Cancel']");

    // ================================================================= Actions

    /**
     * Clicks the "Forgot your password?" link on the OrangeHRM login page.
     * Precondition: driver must already be on the login page.
     *
     * @return {@code this} after click (URL changes to forgot-password path)
     */
    public LoginAssistPage clickForgotPasswordLink() {
        log.info("Clicking 'Forgot your password?' link");
        click(FORGOT_PWD_LINK);
        return this;
    }

    /**
     * Types the username or email into the reset-request input field.
     *
     * @param usernameOrEmail value to submit for the reset request
     * @return {@code this}
     */
    public LoginAssistPage enterResetUsername(final String usernameOrEmail) {
        log.info("Entering reset username: {}", usernameOrEmail);
        type(RESET_INPUT, usernameOrEmail);
        return this;
    }

    /**
     * Submits the password-reset request form.
     *
     * @return {@code this}
     */
    public LoginAssistPage clickResetPassword() {
        log.info("Submitting password reset request");
        click(RESET_BTN);
        return this;
    }

    /**
     * Clicks the Cancel button to return to the login page.
     *
     * @return a new {@link OrangeLoginPage} instance
     */
    public OrangeLoginPage clickCancel() {
        log.info("Cancelling password reset, returning to login");
        click(CANCEL_BTN);
        return new OrangeLoginPage();
    }

    // ================================================================= Queries

    /**
     * @return {@code true} when the "Forgot your password?" link is visible on
     *         the current login page
     */
    public boolean isForgotPasswordLinkVisible() {
        return isDisplayed(FORGOT_PWD_LINK);
    }

    /**
     * @return {@code true} when the reset-request page header is rendered,
     *         confirming successful navigation to the forgot-password page
     */
    public boolean isResetPageLoaded() {
        return isDisplayed(RESET_HEADER);
    }

    /**
     * @return the text of the reset page header/title element
     */
    public String getResetPageHeader() {
        return getText(RESET_HEADER);
    }

    /**
     * @return the current browser URL; useful for asserting the URL contains
     *         {@code "requestPasswordResetCode"}
     */
    public String getUrl() {
        return currentUrl();
    }
}
