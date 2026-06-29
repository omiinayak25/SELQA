package com.omiinqa.pages.saucedemo;

import com.omiinqa.core.BasePage;
import org.openqa.selenium.By;

/**
 * Page Object for SauceDemo Checkout Step 1 — customer information form
 * ({@code /checkout-step-one.html}).
 *
 * <p><b>POM contract:</b> this class only fills the form and navigates; the
 * test decides whether to assert on the error message or the returned page.
 * Keeping validation-state exposure ({@link #getErrorMessage()}) separate from
 * form-submission ({@link #continueToOverview()}) allows a single page class
 * to support both happy-path and error-path scenarios without duplication.</p>
 */
public class CheckoutStepOnePage extends BasePage {

    // ---------------------------------------------------------------- locators
    private static final By FIRST_NAME_INPUT = By.id("first-name");
    private static final By LAST_NAME_INPUT  = By.id("last-name");
    private static final By ZIP_CODE_INPUT   = By.id("postal-code");
    private static final By CONTINUE_BTN     = By.id("continue");
    private static final By CANCEL_BTN       = By.id("cancel");
    private static final By ERROR_MESSAGE    = By.cssSelector("[data-test='error']");

    // ----------------------------------------------------------------- actions

    /**
     * Fills in the customer information fields.
     *
     * @param firstName customer first name
     * @param lastName  customer last name
     * @param zipCode   postal/zip code
     * @return this page for a fluent call to {@link #continueToOverview()}
     */
    public CheckoutStepOnePage enterCustomerInfo(final String firstName,
                                                  final String lastName,
                                                  final String zipCode) {
        log.info("Entering customer info: {} {}, zip={}", firstName, lastName, zipCode);
        type(FIRST_NAME_INPUT, firstName);
        type(LAST_NAME_INPUT, lastName);
        type(ZIP_CODE_INPUT, zipCode);
        return this;
    }

    /**
     * Clicks «Continue» to proceed to the order overview step.
     *
     * <p>On validation failure the browser stays on this page; the test should
     * call {@link #getErrorMessage()} to inspect the error.</p>
     *
     * @return a {@link CheckoutStepTwoPage} (order overview)
     */
    public CheckoutStepTwoPage continueToOverview() {
        log.info("Continuing to checkout overview");
        click(CONTINUE_BTN);
        return new CheckoutStepTwoPage();
    }

    /**
     * Clicks «Cancel» to return to the cart without completing checkout.
     *
     * @return the {@link CartPage}
     */
    public CartPage cancel() {
        log.info("Cancelling checkout step one");
        click(CANCEL_BTN);
        return new CartPage();
    }

    // ----------------------------------------------------------------- queries

    /**
     * Returns the validation error text shown when required fields are missing
     * or invalid. Returns an empty string when no error is visible.
     *
     * @return error message text, trimmed; empty string if absent
     */
    public String getErrorMessage() {
        if (!isDisplayed(ERROR_MESSAGE)) {
            return "";
        }
        return getText(ERROR_MESSAGE);
    }

    /**
     * @return {@code true} when the error banner is currently displayed
     */
    public boolean isErrorDisplayed() {
        return isDisplayed(ERROR_MESSAGE);
    }
}
