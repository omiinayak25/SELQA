package com.omiinqa.pages.orangehrm;

import com.omiinqa.components.TableComponent;
import com.omiinqa.core.BasePage;
import com.omiinqa.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

/**
 * Page Object for the OrangeHRM PIM (Personnel Information Management) module.
 *
 * <p><b>Scope:</b> covers two functional areas within PIM:
 * <ol>
 *   <li>The <em>Add Employee</em> form ({@code /pim/addEmployee}) — captures
 *       name and employee-id fields then saves, returning a confirmation state.</li>
 *   <li>The <em>Employee List</em> view ({@code /pim/viewEmployeeList}) —
 *       search by name or ID and inspect the result grid via
 *       {@link TableComponent}.</li>
 * </ol>
 * </p>
 *
 * <p><b>Pattern:</b> Page Object Model (POM). All assertions are deferred to
 * test classes; this page only encapsulates locator logic and interaction
 * sequences. Returning {@code this} or a new page type enforces a fluent
 * navigation contract that the compiler validates at build time.</p>
 *
 * <p>OrangeHRM renders its UI with a Vue-based design system ({@code .oxd-*}
 * CSS classes). Locators are chosen to be label/attribute-based rather than
 * positional to survive minor DOM restructuring.</p>
 */
public class PimPage extends BasePage {

    // ---------------------------------------------------------------- Add Employee form locators
    /** "Add Employee" primary button on the Employee List toolbar. */
    private static final By ADD_EMPLOYEE_BTN    = By.xpath("//button[normalize-space()='Add']");
    private static final By FIRST_NAME_INPUT    = By.name("firstName");
    private static final By MIDDLE_NAME_INPUT   = By.name("middleName");
    private static final By LAST_NAME_INPUT     = By.name("lastName");
    /** The auto-generated employee ID field (editable). */
    private static final By EMPLOYEE_ID_INPUT   = By.xpath("//label[normalize-space()='Employee Id']/following::input[1]");
    private static final By SAVE_BTN            = By.xpath("//button[@type='submit' and normalize-space()='Save']");

    // ---------------------------------------------------------------- Employee List search locators
    private static final By SEARCH_NAME_INPUT   = By.xpath("//label[normalize-space()='Employee Name']/following::input[1]");
    private static final By SEARCH_ID_INPUT     = By.xpath("//label[normalize-space()='Employee Id']/following::input[1]");
    private static final By SEARCH_BTN          = By.xpath("//button[@type='submit' and normalize-space()='Search']");
    private static final By RESET_BTN           = By.xpath("//button[@type='button' and normalize-space()='Reset']");

    // ---------------------------------------------------------------- Result grid
    /** Container row holding employee records (.oxd-table-card inside .oxd-table). */
    private static final By RESULT_ROWS         = By.cssSelector(".oxd-table-body .oxd-table-row");
    /** Full table root element — passed to TableComponent. */
    private static final By TABLE_ROOT          = By.cssSelector(".oxd-table");

    // ---------------------------------------------------------------- Validation / toast
    private static final By REQUIRED_ALERT      = By.cssSelector(".oxd-input-group .oxd-input-field-error-message");
    private static final By TOAST_SUCCESS       = By.cssSelector(".oxd-toast--success");

    // ---------------------------------------------------------------- Breadcrumb / page header
    private static final By PAGE_HEADER         = By.cssSelector("h6.oxd-topbar-header-breadcrumb-module");

    // ================================================================= Add Employee Actions

    /**
     * Clicks the "Add" button on the Employee List toolbar to open the Add Employee form.
     *
     * @return {@code this} for chaining; caller stays on PimPage
     */
    public PimPage clickAddEmployee() {
        log.info("Clicking Add Employee button");
        click(ADD_EMPLOYEE_BTN);
        return this;
    }

    /**
     * Enters the employee's first name.
     *
     * @param firstName value to type (must not exceed field max-length)
     * @return {@code this}
     */
    public PimPage enterFirstName(final String firstName) {
        log.info("Entering first name: {}", firstName);
        type(FIRST_NAME_INPUT, firstName);
        return this;
    }

    /**
     * Enters the employee's middle name (optional on the form).
     *
     * @param middleName value to type; pass empty string to leave blank
     * @return {@code this}
     */
    public PimPage enterMiddleName(final String middleName) {
        log.info("Entering middle name: {}", middleName);
        type(MIDDLE_NAME_INPUT, middleName);
        return this;
    }

    /**
     * Enters the employee's last name.
     *
     * @param lastName value to type
     * @return {@code this}
     */
    public PimPage enterLastName(final String lastName) {
        log.info("Entering last name: {}", lastName);
        type(LAST_NAME_INPUT, lastName);
        return this;
    }

    /**
     * Clears and sets the employee ID field. OrangeHRM auto-generates an ID;
     * this method overwrites it with a caller-supplied value for predictable
     * test assertions.
     *
     * @param employeeId numeric string to use as the employee ID
     * @return {@code this}
     */
    public PimPage enterEmployeeId(final String employeeId) {
        log.info("Entering employee ID: {}", employeeId);
        type(EMPLOYEE_ID_INPUT, employeeId);
        return this;
    }

    /**
     * Submits the Add Employee form by clicking "Save".
     * On success OrangeHRM redirects to the personal details view.
     *
     * @return {@code this} — callers should then call {@link #isSuccessToastVisible()}
     *         or check {@link #currentUrl()} to confirm success
     */
    public PimPage saveEmployee() {
        log.info("Saving new employee");
        click(SAVE_BTN);
        return this;
    }

    // ================================================================= Employee List / Search Actions

    /**
     * Types a name (or partial name) into the Employee Name search field.
     *
     * @param name search term
     * @return {@code this}
     */
    public PimPage searchByName(final String name) {
        log.info("Searching employee by name: {}", name);
        type(SEARCH_NAME_INPUT, name);
        return this;
    }

    /**
     * Types an employee ID into the Employee Id search field.
     *
     * @param employeeId exact or partial ID to search
     * @return {@code this}
     */
    public PimPage searchById(final String employeeId) {
        log.info("Searching employee by ID: {}", employeeId);
        type(SEARCH_ID_INPUT, employeeId);
        return this;
    }

    /**
     * Submits the search filter form.
     *
     * @return {@code this}
     */
    public PimPage clickSearch() {
        log.info("Clicking Search button");
        click(SEARCH_BTN);
        return this;
    }

    /**
     * Resets the search filter form to its default state.
     *
     * @return {@code this}
     */
    public PimPage clickReset() {
        log.info("Clicking Reset button");
        click(RESET_BTN);
        return this;
    }

    // ================================================================= Query Methods

    /**
     * Returns the count of result rows currently visible in the employee table.
     * Returns 0 when the table is empty or not yet rendered.
     *
     * @return visible row count
     */
    public int getResultRowCount() {
        final List<WebElement> rows = driver().findElements(RESULT_ROWS);
        return rows.size();
    }

    /**
     * Returns the page header/breadcrumb text to verify the current PIM sub-view.
     *
     * @return breadcrumb text, e.g. "Employee List" or "Add Employee"
     */
    public String getPageHeader() {
        return getText(PAGE_HEADER);
    }

    /**
     * @return {@code true} when a success toast notification is visible after saving
     */
    public boolean isSuccessToastVisible() {
        return isDisplayed(TOAST_SUCCESS, Duration.ofSeconds(6));
    }

    /**
     * Returns the first required-field validation error message visible on the form.
     * Useful for asserting that empty-required-field attempts are correctly rejected.
     *
     * @return error text; empty string if no error visible
     */
    public String getFirstValidationError() {
        if (!isDisplayed(REQUIRED_ALERT)) {
            return "";
        }
        return getText(REQUIRED_ALERT);
    }

    /**
     * @return {@code true} when at least one required-field validation error is shown
     */
    public boolean isValidationErrorDisplayed() {
        return isDisplayed(REQUIRED_ALERT);
    }

    /**
     * Returns the auto-generated or manually set value in the Employee Id input.
     * Useful for capturing IDs before saving to use in later search scenarios.
     *
     * @return current value of the Employee Id field
     */
    public String getEmployeeIdFieldValue() {
        return getAttribute(EMPLOYEE_ID_INPUT, "value");
    }

    /**
     * Provides a {@link TableComponent} scoped to the employee result grid.
     * The component is constructed with the live table root element; callers
     * should not cache the result across page navigations.
     *
     * @return a {@link TableComponent} for the employee list table
     */
    public TableComponent employeeTable() {
        final WebElement tableRoot = WaitUtils.visible(driver(), TABLE_ROOT);
        return new TableComponent(tableRoot);
    }

    /**
     * @return {@code true} when the table root element is present and visible
     */
    public boolean isTableVisible() {
        return isDisplayed(TABLE_ROOT);
    }
}
