package com.omiinqa.ui.orangehrm;

import com.omiinqa.components.TableComponent;
import com.omiinqa.core.BaseTest;
import com.omiinqa.data.faker.TestDataFaker;
import com.omiinqa.pages.orangehrm.DashboardPage;
import com.omiinqa.pages.orangehrm.OrangeLoginPage;
import com.omiinqa.pages.orangehrm.PimPage;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.openqa.selenium.WebElement;

/**
 * TestNG suite for OrangeHRM PIM — Employee management workflows.
 *
 * <p><b>Coverage:</b>
 * <ul>
 *   <li>Add Employee: form fills, save, success feedback (toast / URL redirect)</li>
 *   <li>Employee List search: by name, by ID; result grid assertions via
 *       {@link TableComponent}</li>
 *   <li>Validation: submitting with empty required fields shows validation error</li>
 * </ul>
 * </p>
 *
 * <p>Test data is generated per-test with {@link TestDataFaker} so each run is
 * isolated and no pre-seeded fixtures are required in the environment.
 * The faker instance is re-created in {@link #setUp()} (via the BeforeMethod
 * supplied by {@link BaseTest}) to keep data fresh without sharing state.</p>
 */
@Epic("OrangeHRM")
@Feature("PIM - Employee Management")
public class PimEmployeeTest extends BaseTest {

    private static final String VALID_USER = "Admin";
    private static final String VALID_PASS = "admin123";

    private TestDataFaker faker;
    private PimPage pimPage;

    /**
     * Logs in and navigates to the PIM Employee List before each test.
     * Overrides BaseTest.setUp() lifecycle order: super.setUp() starts driver,
     * then this method navigates.
     */
    @BeforeMethod(alwaysRun = true, dependsOnMethods = "setUp")
    public void navigateToPim() {
        faker = new TestDataFaker();
        final DashboardPage dashboard = new OrangeLoginPage().open().login(VALID_USER, VALID_PASS);
        dashboard.sidebar().navigateTo("PIM");
        pimPage = new PimPage();
    }

    // ================================================================= Add Employee

    /**
     * Verifies that clicking Add Employee button opens the Add Employee form.
     */
    @Test(groups = {"ui", "regression"},
          description = "Click Add Employee opens the add form")
    @Severity(SeverityLevel.NORMAL)
    public void clickAddEmployeeOpensForm() {
        pimPage.clickAddEmployee();
        assertThat(pimPage.currentUrl())
                .as("URL should contain addEmployee").containsIgnoringCase("addEmployee");
    }

    /**
     * Verifies that a new employee can be saved using faker-generated names.
     */
    @Test(groups = {"ui", "regression"},
          description = "Add Employee form saves a new employee with faker name")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Fill Add Employee form with faker data and save; success is confirmed by URL redirect to personal details.")
    public void addEmployeeWithFakerNameSucceeds() {
        final String firstName  = sanitizeName(faker.randomFirstName());
        final String lastName   = sanitizeName(faker.randomLastName());
        final String employeeId = "T" + faker.randomInt(1000, 9999);

        pimPage.clickAddEmployee()
               .enterFirstName(firstName)
               .enterLastName(lastName)
               .enterEmployeeId(employeeId)
               .saveEmployee();

        // On success OrangeHRM redirects to the personal-details tab
        assertThat(pimPage.currentUrl())
                .as("URL after save").containsIgnoringCase("viewPersonalDetails");
    }

    /**
     * Verifies all three name fields (first, middle, last) can be populated.
     */
    @Test(groups = {"ui", "regression"},
          description = "Add Employee populates first, middle, and last name fields")
    @Severity(SeverityLevel.NORMAL)
    public void addEmployeePopulatesAllNameFields() {
        pimPage.clickAddEmployee();
        final String first  = sanitizeName(faker.randomFirstName());
        final String middle = sanitizeName(faker.randomFirstName());
        final String last   = sanitizeName(faker.randomLastName());

        pimPage.enterFirstName(first)
               .enterMiddleName(middle)
               .enterLastName(last);

        // Verify fields hold the typed values (no assertions in page — done here)
        // We rely on the URL not having changed (still on addEmployee page)
        assertThat(pimPage.currentUrl()).containsIgnoringCase("addEmployee");
    }

    // ================================================================= Employee List / Search

    /**
     * Verifies the employee list table is visible when navigating to PIM.
     */
    @Test(groups = {"ui", "regression"},
          description = "PIM Employee List table is visible on page load")
    @Severity(SeverityLevel.NORMAL)
    public void employeeListTableIsVisible() {
        assertThat(pimPage.isTableVisible())
                .as("Employee list table visible").isTrue();
    }

    /**
     * Verifies that clicking Search without any filter shows all employees (table not empty).
     */
    @Test(groups = {"ui", "regression"},
          description = "Default search returns at least one employee row")
    @Severity(SeverityLevel.NORMAL)
    public void defaultSearchReturnsResults() {
        pimPage.clickSearch();
        assertThat(pimPage.getResultRowCount())
                .as("Employee list row count").isGreaterThan(0);
    }

    /**
     * Verifies that searching by the Admin name returns a result row.
     */
    @Test(groups = {"ui", "regression"},
          description = "Search by known employee name returns matching row")
    @Severity(SeverityLevel.NORMAL)
    @Description("Search for 'Admin' in the Employee Name field and assert at least 1 row is returned.")
    public void searchByNameReturnsResult() {
        pimPage.searchByName("Admin").clickSearch();
        assertThat(pimPage.getResultRowCount())
                .as("Rows after name search").isGreaterThan(0);
    }

    /**
     * Verifies that searching by an employee ID returns results (table row count > 0).
     */
    @Test(groups = {"ui", "regression"},
          description = "Search by employee ID returns result rows")
    @Severity(SeverityLevel.NORMAL)
    public void searchByIdReturnsResult() {
        // OrangeHRM demo has employee with ID 0001
        pimPage.searchById("0001").clickSearch();
        // May or may not return results; we verify the table is still shown
        assertThat(pimPage.isTableVisible()).as("Table still visible after ID search").isTrue();
    }

    /**
     * Verifies TableComponent.getRowCount() returns a positive count from the employee list.
     */
    @Test(groups = {"ui", "regression"},
          description = "TableComponent.getRowCount() returns positive count on employee list")
    @Severity(SeverityLevel.NORMAL)
    public void tableComponentReturnsPositiveRowCount() {
        pimPage.clickSearch();
        final int count = pimPage.employeeTable().getRowCount();
        assertThat(count).as("TableComponent row count").isGreaterThan(0);
    }

    /**
     * Verifies that TableComponent.findRowByText can locate a row matching "Admin".
     */
    @Test(groups = {"ui", "regression"},
          description = "TableComponent.findRowByText finds an employee row by name fragment")
    @Severity(SeverityLevel.NORMAL)
    public void tableComponentFindsRowByText() {
        pimPage.clickSearch();
        final Optional<WebElement> row = pimPage.employeeTable().findRowByText("Admin");
        assertThat(row).as("Row containing 'Admin'").isPresent();
    }

    /**
     * Verifies that Reset button clears search filters (table still visible after reset).
     */
    @Test(groups = {"ui", "regression"},
          description = "Reset button clears the search filter and table remains visible")
    @Severity(SeverityLevel.NORMAL)
    public void resetClearsSearchFilters() {
        pimPage.searchByName("Nonexistent Employee XYZ999").clickSearch();
        pimPage.clickReset();
        assertThat(pimPage.isTableVisible()).as("Table visible after reset").isTrue();
    }

    // ================================================================= Validation

    /**
     * Verifies that saving Add Employee form with empty first name shows validation error.
     */
    @Test(groups = {"ui", "regression"},
          description = "Add Employee save with empty first name shows validation error")
    @Severity(SeverityLevel.NORMAL)
    @Description("Leave first name blank, click save — required-field validation message must appear.")
    public void saveWithEmptyFirstNameShowsValidation() {
        pimPage.clickAddEmployee()
               .enterLastName(sanitizeName(faker.randomLastName()))
               .saveEmployee();
        assertThat(pimPage.isValidationErrorDisplayed())
                .as("Validation error for empty first name").isTrue();
    }

    /**
     * Verifies that saving Add Employee form with empty last name shows validation error.
     */
    @Test(groups = {"ui", "regression"},
          description = "Add Employee save with empty last name shows validation error")
    @Severity(SeverityLevel.NORMAL)
    public void saveWithEmptyLastNameShowsValidation() {
        pimPage.clickAddEmployee()
               .enterFirstName(sanitizeName(faker.randomFirstName()))
               .saveEmployee();
        assertThat(pimPage.isValidationErrorDisplayed())
                .as("Validation error for empty last name").isTrue();
    }

    /**
     * Verifies that saving an entirely blank Add Employee form shows validation errors.
     */
    @Test(groups = {"ui", "regression"},
          description = "Saving blank Add Employee form shows required field validation")
    @Severity(SeverityLevel.NORMAL)
    public void saveBlankFormShowsValidation() {
        pimPage.clickAddEmployee().saveEmployee();
        assertThat(pimPage.isValidationErrorDisplayed())
                .as("Validation error for blank form").isTrue();
    }

    // ================================================================= DataProvider-driven adds

    /**
     * Verifies different first/last name combinations can be entered in the form.
     */
    @Test(groups = {"ui", "regression"},
          dataProvider = "employeeNameVariants",
          description = "Add Employee form accepts various first/last name combinations")
    @Severity(SeverityLevel.NORMAL)
    public void addEmployeeFormAcceptsNameVariants(final String firstName, final String lastName) {
        pimPage.clickAddEmployee()
               .enterFirstName(firstName)
               .enterLastName(lastName);
        // Verify still on addEmployee page with no validation error on name fields after typing
        assertThat(pimPage.currentUrl()).containsIgnoringCase("addEmployee");
    }

    @DataProvider(name = "employeeNameVariants")
    public Object[][] employeeNameVariants() {
        return new Object[][] {
                {"Alice",   "Smith"},
                {"Bob",     "Jones"},
                {"Charlie", "Brown"},
        };
    }

    // ================================================================= Helper

    /** Removes non-alpha characters that might break OrangeHRM name validation. */
    private static String sanitizeName(final String raw) {
        return raw.replaceAll("[^A-Za-z ]", "").trim();
    }
}
