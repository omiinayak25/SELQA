package com.omiinqa.pages.orangehrm;

import com.omiinqa.components.TableComponent;
import com.omiinqa.core.BasePage;
import com.omiinqa.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Page Object for the OrangeHRM Admin module — User Management section.
 *
 * <p><b>Scope:</b> the "System Users" list and its search filter panel
 * ({@code /admin/viewSystemUsers}). Exposes interactions for entering search
 * criteria (username, role, status) and reading the result grid via
 * {@link TableComponent}.</p>
 *
 * <p><b>Why a dedicated page?</b> The Admin module has its own URL namespace,
 * search form, and table structure that differ enough from PIM to justify a
 * separate page object. Keeping admin interactions isolated prevents locator
 * leakage and makes each page object independently understandable (Single
 * Responsibility Principle).</p>
 *
 * <p><b>POM contract:</b> no assertions; state exposed via getter methods.</p>
 */
public class AdminPage extends BasePage {

    // ---------------------------------------------------------------- Search form locators
    private static final By USERNAME_INPUT     = By.xpath(
            "//label[normalize-space()='Username']/following::input[1]");
    private static final By USER_ROLE_DROPDOWN = By.xpath(
            "//label[normalize-space()='User Role']/following::div[contains(@class,'oxd-select-text')][1]");
    private static final By STATUS_DROPDOWN    = By.xpath(
            "//label[normalize-space()='Status']/following::div[contains(@class,'oxd-select-text')][1]");
    private static final By SEARCH_BTN         = By.xpath(
            "//button[@type='submit' and normalize-space()='Search']");
    private static final By RESET_BTN          = By.xpath(
            "//button[@type='button' and normalize-space()='Reset']");

    // ---------------------------------------------------------------- Dropdown option locators
    /** Options rendered inside an open .oxd-select-dropdown panel. */
    private static final By DROPDOWN_OPTION    = By.cssSelector(
            ".oxd-select-dropdown .oxd-select-option");

    // ---------------------------------------------------------------- Table / result locators
    private static final By TABLE_ROOT         = By.cssSelector(".oxd-table");
    private static final By RESULT_ROWS        = By.cssSelector(
            ".oxd-table-body .oxd-table-row");

    // ---------------------------------------------------------------- Page header
    private static final By PAGE_HEADER        = By.cssSelector(
            "h6.oxd-topbar-header-breadcrumb-module");

    // ================================================================= Search Actions

    /**
     * Enters the username search term into the Username filter field.
     *
     * @param username partial or full username to filter by
     * @return {@code this}
     */
    public AdminPage enterUsername(final String username) {
        log.info("Admin search — entering username: {}", username);
        type(USERNAME_INPUT, username);
        return this;
    }

    /**
     * Selects a user role from the User Role dropdown.
     * Opens the dropdown wrapper then clicks the matching option by visible text.
     *
     * @param role visible role text, e.g. {@code "Admin"} or {@code "ESS"}
     * @return {@code this}
     */
    public AdminPage selectUserRole(final String role) {
        log.info("Admin search — selecting user role: {}", role);
        click(USER_ROLE_DROPDOWN);
        driver().findElements(DROPDOWN_OPTION).stream()
                .filter(opt -> opt.getText().trim().equalsIgnoreCase(role))
                .findFirst()
                .ifPresent(WebElement::click);
        return this;
    }

    /**
     * Selects an account status from the Status dropdown.
     *
     * @param status visible status text, e.g. {@code "Enabled"} or {@code "Disabled"}
     * @return {@code this}
     */
    public AdminPage selectStatus(final String status) {
        log.info("Admin search — selecting status: {}", status);
        click(STATUS_DROPDOWN);
        driver().findElements(DROPDOWN_OPTION).stream()
                .filter(opt -> opt.getText().trim().equalsIgnoreCase(status))
                .findFirst()
                .ifPresent(WebElement::click);
        return this;
    }

    /**
     * Submits the user search filter form.
     *
     * @return {@code this}
     */
    public AdminPage clickSearch() {
        log.info("Admin search — clicking Search");
        click(SEARCH_BTN);
        return this;
    }

    /**
     * Resets all search filters to their defaults.
     *
     * @return {@code this}
     */
    public AdminPage clickReset() {
        log.info("Admin search — clicking Reset");
        click(RESET_BTN);
        return this;
    }

    // ================================================================= Query Methods

    /**
     * @return current page header/breadcrumb text, e.g. {@code "System Users"}
     */
    public String getPageHeader() {
        return getText(PAGE_HEADER);
    }

    /**
     * @return number of visible result rows in the system users table
     */
    public int getResultRowCount() {
        return driver().findElements(RESULT_ROWS).size();
    }

    /**
     * @return {@code true} when the result table is visible on screen
     */
    public boolean isTableVisible() {
        return isDisplayed(TABLE_ROOT);
    }

    /**
     * Returns a {@link TableComponent} scoped to the system-users result table.
     * Always construct fresh on each call to avoid stale element references.
     *
     * @return a {@link TableComponent} for the admin user list
     */
    public TableComponent userTable() {
        final WebElement tableRoot = WaitUtils.visible(driver(), TABLE_ROOT);
        return new TableComponent(tableRoot);
    }
}
