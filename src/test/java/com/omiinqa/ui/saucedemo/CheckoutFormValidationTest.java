package com.omiinqa.ui.saucedemo;

import com.omiinqa.businessflows.AddToCartFlow;
import com.omiinqa.core.BaseTest;
import com.omiinqa.pages.saucedemo.CartPage;
import com.omiinqa.pages.saucedemo.CheckoutStepOnePage;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checkout step-one form validation tests covering missing-required-field errors
 * and boundary inputs (very long strings, special characters, numeric-only zip).
 *
 * <p><b>Strategy:</b> each test arrives at {@link CheckoutStepOnePage} via a
 * pre-populated cart (one product is sufficient — the form validation is
 * independent of cart contents). The {@code missingFields} data provider drives
 * the three missing-field scenarios; boundary tests are individual methods because
 * their assertions differ qualitatively from simple presence/absence of an error.</p>
 *
 * <p>No assertions exist on the page objects themselves — the test layer owns all
 * assertion logic, consistent with the OmiinQA framework contract.</p>
 */
@Epic("SauceDemo")
@Feature("Checkout Form Validation")
public class CheckoutFormValidationTest extends BaseTest {

    private static final String FIRST = "John";
    private static final String LAST  = "Doe";
    private static final String ZIP   = "12345";

    // --------------------------------------------------------- helper

    /**
     * Helper that logs in, adds one product to the cart, and navigates to
     * checkout step one — the starting state for every test in this class.
     *
     * @return the loaded {@link CheckoutStepOnePage}
     */
    private CheckoutStepOnePage reachCheckoutStepOne() {
        final CartPage cart = AddToCartFlow.loginAndAddProducts(
                List.of("Sauce Labs Backpack"));
        return cart.checkout();
    }

    // --------------------------------------------------------- data providers

    /**
     * Provides the three required-field missing-field scenarios with expected
     * error message fragments.
     *
     * <p>Rows: [firstName, lastName, zipCode, expectedErrorFragment]</p>
     *
     * @return matrix of [firstName, lastName, zipCode, expectedErrorFragment]
     */
    @DataProvider(name = "missingFields")
    public Object[][] missingFields() {
        return new Object[][] {
                {"",      LAST,  ZIP,   "First Name is required"},
                {FIRST,   "",    ZIP,   "Last Name is required"},
                {FIRST,   LAST,  "",    "Postal Code is required"},
        };
    }

    // --------------------------------------------------------- test methods

    /**
     * Asserts that submitting the checkout form with a missing required field
     * shows the relevant required-field error for each of the three fields.
     *
     * @param firstName             first name value (may be blank)
     * @param lastName              last name value (may be blank)
     * @param zipCode               zip/postal code value (may be blank)
     * @param expectedErrorFragment expected text fragment in the error message
     */
    @Test(groups = {"ui", "regression"},
            dataProvider = "missingFields",
            description = "Missing required field shows the corresponding required-field error")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Each required field, when omitted, must produce its own specific error message.")
    public void missingRequiredFieldShowsError(final String firstName,
                                                final String lastName,
                                                final String zipCode,
                                                final String expectedErrorFragment) {
        final CheckoutStepOnePage step1 = reachCheckoutStepOne();
        step1.enterCustomerInfo(firstName, lastName, zipCode).continueToOverview();

        assertThat(step1.isErrorDisplayed())
                .as("error displayed when first='%s', last='%s', zip='%s'",
                        firstName, lastName, zipCode)
                .isTrue();
        assertThat(step1.getErrorMessage())
                .as("error message content")
                .containsIgnoringCase(expectedErrorFragment);
    }

    /**
     * Asserts that submitting completely blank form (all three fields empty)
     * shows an error about the first required field.
     */
    @Test(groups = {"ui", "regression"},
            description = "All fields blank shows first-name required error")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Submitting a completely blank checkout form must trigger the first-name required error.")
    public void allFieldsBlankShowsFirstNameError() {
        final CheckoutStepOnePage step1 = reachCheckoutStepOne();
        step1.enterCustomerInfo("", "", "").continueToOverview();

        assertThat(step1.isErrorDisplayed()).isTrue();
        assertThat(step1.getErrorMessage())
                .containsIgnoringCase("First Name is required");
    }

    /**
     * Asserts that a very long first name (100 characters) is accepted without
     * triggering any validation error, meaning the field has no enforced max length.
     */
    @Test(groups = {"ui", "regression"},
            description = "Very long first name (100 chars) does not trigger a validation error")
    @Severity(SeverityLevel.NORMAL)
    @Description("Boundary: a first name of 100 characters must not cause a checkout form error.")
    public void veryLongFirstNameIsAccepted() {
        final String longName = "A".repeat(100);
        final CheckoutStepOnePage step1 = reachCheckoutStepOne();
        step1.enterCustomerInfo(longName, LAST, ZIP).continueToOverview();

        // SauceDemo does not enforce a max-length; the form should proceed
        assertThat(step1.isErrorDisplayed())
                .as("no error for very long first name")
                .isFalse();
    }

    /**
     * Asserts that a very long last name (100 characters) is accepted without
     * triggering any validation error.
     */
    @Test(groups = {"ui", "regression"},
            description = "Very long last name (100 chars) does not trigger a validation error")
    @Severity(SeverityLevel.NORMAL)
    @Description("Boundary: a last name of 100 characters must not cause a checkout form error.")
    public void veryLongLastNameIsAccepted() {
        final String longName = "B".repeat(100);
        final CheckoutStepOnePage step1 = reachCheckoutStepOne();
        step1.enterCustomerInfo(FIRST, longName, ZIP).continueToOverview();

        assertThat(step1.isErrorDisplayed())
                .as("no error for very long last name")
                .isFalse();
    }

    /**
     * Asserts that a numeric-only zip code passes checkout form validation.
     */
    @Test(groups = {"ui", "regression"},
            description = "Numeric-only zip code is accepted without error")
    @Severity(SeverityLevel.NORMAL)
    @Description("Boundary: a purely numeric postal code must be accepted by the checkout form.")
    public void numericOnlyZipCodeIsAccepted() {
        final CheckoutStepOnePage step1 = reachCheckoutStepOne();
        step1.enterCustomerInfo(FIRST, LAST, "99999").continueToOverview();

        assertThat(step1.isErrorDisplayed())
                .as("no error for numeric zip code")
                .isFalse();
    }

    /**
     * Asserts that special characters in the name fields are accepted without
     * triggering a validation error (SauceDemo has no whitelist on name fields).
     */
    @Test(groups = {"ui", "regression"},
            description = "Special characters in first/last name fields are accepted")
    @Severity(SeverityLevel.NORMAL)
    @Description("Boundary: special characters in name fields must not trigger a validation error.")
    public void specialCharactersInNameFieldsAreAccepted() {
        final CheckoutStepOnePage step1 = reachCheckoutStepOne();
        step1.enterCustomerInfo("O'Brien-Müller", "St. John-Smith", ZIP).continueToOverview();

        assertThat(step1.isErrorDisplayed())
                .as("no error for special characters in names")
                .isFalse();
    }

    /**
     * Asserts that clicking «Cancel» on the checkout step-one page returns to
     * the cart with the previously added item still present.
     */
    @Test(groups = {"ui", "regression"},
            description = "Cancel on checkout step-one returns to cart with items intact")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicking Cancel on the checkout form must return to the cart page without data loss.")
    public void cancelOnCheckoutStepOneReturnsToCart() {
        final CheckoutStepOnePage step1 = reachCheckoutStepOne();
        final CartPage cart = step1.cancel();

        assertThat(cart.getItemCount())
                .as("cart items still present after cancel")
                .isEqualTo(1);
    }
}
