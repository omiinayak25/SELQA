package com.omiinqa.pages.orangehrm;

import com.omiinqa.core.BasePage;
import org.openqa.selenium.By;

import java.time.Duration;

/**
 * Page Object for the OrangeHRM "My Info" module ({@code /pim/viewMyDetails}).
 *
 * <p><b>Scope:</b> the personal details section of My Info, which contains the
 * employee's own name fields, gender, marital status, and nationality.
 * This page is typically accessed from the top navigation profile link or the
 * sidebar after logging in.</p>
 *
 * <p><b>Rationale for a separate page:</b> My Info has its own URL path and
 * form layout that differs from the PIM employee-management forms. Splitting it
 * into its own class keeps both pages focused and ensures changes to one do not
 * inadvertently affect locators in the other (Open/Closed Principle).</p>
 *
 * <p><b>POM contract:</b> no assertions — returns field values and visibility
 * states for tests to assert against.</p>
 */
public class MyInfoPage extends BasePage {

    // ---------------------------------------------------------------- Personal detail form locators
    private static final By FIRST_NAME_INPUT  = By.name("firstName");
    private static final By MIDDLE_NAME_INPUT = By.name("middleName");
    private static final By LAST_NAME_INPUT   = By.name("lastName");

    // Marital Status dropdown
    private static final By MARITAL_STATUS_DROPDOWN = By.xpath(
            "//label[normalize-space()='Marital Status']/following::div[contains(@class,'oxd-select-text')][1]");

    // Dropdown option selector (shared pattern across OrangeHRM)
    private static final By DROPDOWN_OPTION   = By.cssSelector(
            ".oxd-select-dropdown .oxd-select-option");

    // Save button (first Save on personal details tab)
    private static final By SAVE_BTN          = By.xpath(
            "//button[@type='submit' and normalize-space()='Save']");

    // Success toast
    private static final By TOAST_SUCCESS     = By.cssSelector(".oxd-toast--success");

    // Page header
    private static final By PAGE_HEADER       = By.cssSelector(
            "h6.oxd-topbar-header-breadcrumb-module");

    // ================================================================= Query Methods

    /**
     * @return the breadcrumb header text, e.g. {@code "My Info"}
     */
    public String getPageHeader() {
        return getText(PAGE_HEADER);
    }

    /**
     * @return the current value in the First Name input field
     */
    public String getFirstName() {
        return getAttribute(FIRST_NAME_INPUT, "value");
    }

    /**
     * @return the current value in the Last Name input field
     */
    public String getLastName() {
        return getAttribute(LAST_NAME_INPUT, "value");
    }

    /**
     * @return the current value in the Middle Name input field; may be empty string
     */
    public String getMiddleName() {
        return getAttribute(MIDDLE_NAME_INPUT, "value");
    }

    /**
     * @return {@code true} when the First Name input field is visible (page fully loaded)
     */
    public boolean isLoaded() {
        return isDisplayed(FIRST_NAME_INPUT, Duration.ofSeconds(8));
    }

    /**
     * @return {@code true} when a success toast appears after saving personal details
     */
    public boolean isSuccessToastVisible() {
        return isDisplayed(TOAST_SUCCESS, Duration.ofSeconds(6));
    }

    // ================================================================= Action Methods

    /**
     * Clears and sets the First Name field.
     *
     * @param firstName new value to enter
     * @return {@code this}
     */
    public MyInfoPage enterFirstName(final String firstName) {
        log.info("MyInfo — entering first name: {}", firstName);
        type(FIRST_NAME_INPUT, firstName);
        return this;
    }

    /**
     * Clears and sets the Last Name field.
     *
     * @param lastName new value to enter
     * @return {@code this}
     */
    public MyInfoPage enterLastName(final String lastName) {
        log.info("MyInfo — entering last name: {}", lastName);
        type(LAST_NAME_INPUT, lastName);
        return this;
    }

    /**
     * Clears and sets the Middle Name field.
     *
     * @param middleName new value; pass empty string to clear
     * @return {@code this}
     */
    public MyInfoPage enterMiddleName(final String middleName) {
        log.info("MyInfo — entering middle name: {}", middleName);
        type(MIDDLE_NAME_INPUT, middleName);
        return this;
    }

    /**
     * Submits personal details by clicking the Save button.
     *
     * @return {@code this}
     */
    public MyInfoPage savePersonalDetails() {
        log.info("MyInfo — saving personal details");
        click(SAVE_BTN);
        return this;
    }
}
