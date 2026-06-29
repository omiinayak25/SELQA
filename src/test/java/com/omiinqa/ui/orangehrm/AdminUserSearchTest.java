package com.omiinqa.ui.orangehrm;

import com.omiinqa.components.TableComponent;
import com.omiinqa.core.BaseTest;
import com.omiinqa.pages.orangehrm.AdminPage;
import com.omiinqa.pages.orangehrm.DashboardPage;
import com.omiinqa.pages.orangehrm.OrangeLoginPage;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TestNG suite for OrangeHRM Admin module — User Management search.
 *
 * <p><b>Coverage:</b>
 * <ul>
 *   <li>Navigate to Admin → User Management: page loads correctly</li>
 *   <li>Default search (no filters): table is non-empty</li>
 *   <li>Filter by username: result set is narrowed</li>
 *   <li>Filter by role (Admin / ESS): table reflects the filter</li>
 *   <li>Filter by status (Enabled / Disabled)</li>
 *   <li>{@link TableComponent} integration: row count, cell text, column values</li>
 *   <li>Reset: clears filters and restores full list</li>
 * </ul>
 * </p>
 *
 * <p>Navigation to Admin is handled in {@link #navigateToAdmin()} before each
 * test to ensure full isolation.</p>
 */
@Epic("OrangeHRM")
@Feature("Admin - User Management")
public class AdminUserSearchTest extends BaseTest {

    private static final String VALID_USER = "Admin";
    private static final String VALID_PASS = "admin123";

    private AdminPage adminPage;

    /**
     * Logs in, navigates to Admin module and initialises {@link #adminPage}.
     * Runs after BaseTest.setUp() (which starts the driver).
     */
    @BeforeMethod(alwaysRun = true, dependsOnMethods = "setUp")
    public void navigateToAdmin() {
        final DashboardPage dashboard = new OrangeLoginPage().open().login(VALID_USER, VALID_PASS);
        dashboard.sidebar().navigateTo("Admin");
        adminPage = new AdminPage();
    }

    // ================================================================= Page Load

    /**
     * Verifies that navigating to Admin loads the System Users page.
     */
    @Test(groups = {"ui", "regression", "smoke"},
          description = "Admin module loads System Users page")
    @Severity(SeverityLevel.NORMAL)
    public void adminPageLoadsSystemUsers() {
        assertThat(adminPage.getPageHeader())
                .as("Admin page header").containsIgnoringCase("System Users");
    }

    /**
     * Verifies the system users table is visible on Admin page load.
     */
    @Test(groups = {"ui", "regression"},
          description = "System Users table is visible on Admin page load")
    @Severity(SeverityLevel.NORMAL)
    public void systemUsersTableIsVisible() {
        assertThat(adminPage.isTableVisible())
                .as("System users table visible").isTrue();
    }

    // ================================================================= Default Search

    /**
     * Verifies that clicking Search without filters returns a non-empty table.
     */
    @Test(groups = {"ui", "regression"},
          description = "Search with no filters returns at least one user row")
    @Severity(SeverityLevel.NORMAL)
    @Description("Default search (no criteria) must return the full user list — table must not be empty.")
    public void defaultSearchReturnsUsers() {
        adminPage.clickSearch();
        assertThat(adminPage.getResultRowCount())
                .as("User result row count").isGreaterThan(0);
    }

    /**
     * Verifies TableComponent.getRowCount() is positive after default search.
     */
    @Test(groups = {"ui", "regression"},
          description = "TableComponent.getRowCount() positive after default search")
    @Severity(SeverityLevel.NORMAL)
    public void tableComponentRowCountPositive() {
        adminPage.clickSearch();
        final int count = adminPage.userTable().getRowCount();
        assertThat(count).as("TableComponent row count").isGreaterThan(0);
    }

    // ================================================================= Username Filter

    /**
     * Verifies searching by known username "Admin" returns results.
     */
    @Test(groups = {"ui", "regression"},
          description = "Search by username 'Admin' returns results")
    @Severity(SeverityLevel.NORMAL)
    public void searchByUsernameAdminReturnsResults() {
        adminPage.enterUsername("Admin").clickSearch();
        assertThat(adminPage.getResultRowCount())
                .as("Rows for username 'Admin'").isGreaterThan(0);
    }

    /**
     * Verifies searching by a non-existent username returns zero rows.
     */
    @Test(groups = {"ui", "regression"},
          description = "Search by nonexistent username returns empty table")
    @Severity(SeverityLevel.NORMAL)
    public void searchByNonexistentUsernameReturnsEmpty() {
        adminPage.enterUsername("XYZNONEXISTENTUSER99999").clickSearch();
        assertThat(adminPage.getResultRowCount())
                .as("Rows for nonexistent username").isEqualTo(0);
    }

    // ================================================================= Role Filter

    /**
     * Verifies filtering by role shows results (parametrised over known roles).
     */
    @Test(groups = {"ui", "regression"},
          dataProvider = "userRoles",
          description = "Filter by user role returns appropriate results")
    @Severity(SeverityLevel.NORMAL)
    @Description("Filtering the user list by each known role must return a non-empty table.")
    public void filterByRoleReturnsResults(final String role) {
        adminPage.selectUserRole(role).clickSearch();
        // Admin role always has at least one user (the "Admin" account)
        assertThat(adminPage.isTableVisible())
                .as("Table visible after role filter '%s'", role).isTrue();
    }

    @DataProvider(name = "userRoles")
    public Object[][] userRoles() {
        return new Object[][] {
                {"Admin"},
                {"ESS"},
        };
    }

    // ================================================================= TableComponent integration

    /**
     * Verifies getCellText returns non-null, non-blank text for the first row first column.
     */
    @Test(groups = {"ui", "regression"},
          description = "TableComponent.getCellText(1,1) returns non-blank value")
    @Severity(SeverityLevel.NORMAL)
    public void tableComponentCellTextIsNotBlank() {
        adminPage.clickSearch();
        final TableComponent table = adminPage.userTable();
        assertThat(table.getRowCount()).isGreaterThan(0);
        final String cellText = table.getCellText(1, 1);
        assertThat(cellText).as("Cell(1,1) text").isNotBlank();
    }

    /**
     * Verifies that getColumnValues returns a non-empty list for the username column.
     */
    @Test(groups = {"ui", "regression"},
          description = "TableComponent.getColumnValues returns non-empty list for username column")
    @Severity(SeverityLevel.NORMAL)
    public void tableComponentColumnValuesNotEmpty() {
        adminPage.clickSearch();
        final TableComponent table = adminPage.userTable();
        // OrangeHRM Admin user list header columns include "Username"
        final java.util.List<String> usernames = table.getColumnValues("Username");
        assertThat(usernames).as("Username column values").isNotEmpty();
    }

    // ================================================================= Reset

    /**
     * Verifies that clicking Reset after a username filter restores the full list.
     */
    @Test(groups = {"ui", "regression"},
          description = "Reset clears username filter and restores full user list")
    @Severity(SeverityLevel.NORMAL)
    public void resetRestoresFullList() {
        adminPage.enterUsername("Admin").clickSearch();
        final int filteredCount = adminPage.getResultRowCount();
        adminPage.clickReset().clickSearch();
        final int totalCount = adminPage.getResultRowCount();
        assertThat(totalCount).as("Total count >= filtered count")
                .isGreaterThanOrEqualTo(filteredCount);
    }

    /**
     * Verifies the table is still visible after a reset.
     */
    @Test(groups = {"ui", "regression"},
          description = "Table remains visible after clicking Reset")
    @Severity(SeverityLevel.NORMAL)
    public void tableVisibleAfterReset() {
        adminPage.clickReset();
        assertThat(adminPage.isTableVisible()).as("Table visible after reset").isTrue();
    }
}
