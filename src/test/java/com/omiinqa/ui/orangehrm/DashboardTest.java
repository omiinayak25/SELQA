package com.omiinqa.ui.orangehrm;

import com.omiinqa.components.NavigationSidebar;
import com.omiinqa.core.BaseTest;
import com.omiinqa.pages.orangehrm.DashboardPage;
import com.omiinqa.pages.orangehrm.OrangeLoginPage;
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
 * TestNG suite verifying the OrangeHRM Dashboard state and Navigation Sidebar.
 *
 * <p><b>Coverage:</b>
 * <ul>
 *   <li>Dashboard loads after login and shows the correct header text</li>
 *   <li>Sidebar contains expected top-level module labels (Admin, PIM, Leave,
 *       Time, Recruitment)</li>
 *   <li>Navigating to each module via the sidebar updates the active menu item
 *       and changes the browser URL to the expected path fragment</li>
 * </ul>
 * </p>
 *
 * <p>Each test logs in fresh to avoid state leakage between methods; the
 * per-method driver lifecycle is managed by {@link BaseTest}.</p>
 */
@Epic("OrangeHRM")
@Feature("Dashboard")
public class DashboardTest extends BaseTest {

    private static final String VALID_USER = "Admin";
    private static final String VALID_PASS = "admin123";

    /** Helper: log in and return a ready DashboardPage. */
    private DashboardPage loginAndGetDashboard() {
        return new OrangeLoginPage().open().login(VALID_USER, VALID_PASS);
    }

    // ================================================================= Dashboard State

    /**
     * Verifies the dashboard is loaded (header visible) immediately after login.
     */
    @Test(groups = {"ui", "regression", "smoke"},
          description = "Dashboard loads successfully after Admin login")
    @Severity(SeverityLevel.BLOCKER)
    public void dashboardLoadsAfterLogin() {
        final DashboardPage dashboard = loginAndGetDashboard();
        assertThat(dashboard.isLoaded()).as("Dashboard header visible").isTrue();
    }

    /**
     * Verifies the breadcrumb title reads "Dashboard".
     */
    @Test(groups = {"ui", "regression"},
          description = "Dashboard breadcrumb title equals 'Dashboard'")
    @Severity(SeverityLevel.NORMAL)
    public void dashboardTitleIsCorrect() {
        final DashboardPage dashboard = loginAndGetDashboard();
        assertThat(dashboard.getDashboardTitle())
                .as("Dashboard breadcrumb title").isEqualToIgnoringCase("Dashboard");
    }

    // ================================================================= Sidebar Labels

    /**
     * Verifies the sidebar contains all five required core module labels.
     */
    @Test(groups = {"ui", "regression"},
          description = "Sidebar contains Admin, PIM, Leave, Time, Recruitment labels")
    @Severity(SeverityLevel.NORMAL)
    @Description("At minimum the sidebar must expose the five core OrangeHRM modules.")
    public void sidebarContainsRequiredModules() {
        final NavigationSidebar sidebar = loginAndGetDashboard().sidebar();
        final List<String> labels = sidebar.getMenuItemLabels();

        assertThat(labels).as("Sidebar menu labels")
                .anySatisfy(l -> assertThat(l).containsIgnoringCase("Admin"))
                .anySatisfy(l -> assertThat(l).containsIgnoringCase("PIM"))
                .anySatisfy(l -> assertThat(l).containsIgnoringCase("Leave"))
                .anySatisfy(l -> assertThat(l).containsIgnoringCase("Time"))
                .anySatisfy(l -> assertThat(l).containsIgnoringCase("Recruitment"));
    }

    /**
     * Verifies the sidebar exposes at least five distinct menu items.
     */
    @Test(groups = {"ui", "regression"},
          description = "Sidebar has at least 5 menu items")
    @Severity(SeverityLevel.NORMAL)
    public void sidebarHasAtLeastFiveItems() {
        final NavigationSidebar sidebar = loginAndGetDashboard().sidebar();
        assertThat(sidebar.getMenuItemLabels().size())
                .as("Sidebar item count").isGreaterThanOrEqualTo(5);
    }

    /**
     * Verifies sidebar labels are non-blank strings.
     */
    @Test(groups = {"ui", "regression"},
          description = "All sidebar menu item labels are non-blank")
    @Severity(SeverityLevel.NORMAL)
    public void sidebarLabelsAreNonBlank() {
        final NavigationSidebar sidebar = loginAndGetDashboard().sidebar();
        assertThat(sidebar.getMenuItemLabels())
                .as("All sidebar labels non-blank")
                .allSatisfy(label -> assertThat(label).isNotBlank());
    }

    // ================================================================= Navigation (parametrised)

    /**
     * Verifies navigating to each sidebar module changes the browser URL.
     */
    @Test(groups = {"ui", "regression"},
          dataProvider = "sidebarModules",
          description = "Navigating to a sidebar module updates the URL")
    @Severity(SeverityLevel.NORMAL)
    @Description("Each sidebar nav click should change the URL to the expected path fragment.")
    public void navigatingToModuleUpdatesUrl(final String menuLabel, final String urlFragment) {
        final DashboardPage dashboard = loginAndGetDashboard();
        dashboard.sidebar().navigateTo(menuLabel);
        assertThat(driver().getCurrentUrl())
                .as("URL after navigating to '%s'", menuLabel)
                .containsIgnoringCase(urlFragment);
    }

    @DataProvider(name = "sidebarModules")
    public Object[][] sidebarModules() {
        return new Object[][] {
                {"Admin",       "admin"},
                {"PIM",         "pim"},
                {"Leave",       "leave"},
                {"Time",        "time"},
                {"Recruitment", "recruitment"},
        };
    }

    /**
     * Verifies that navigating to Admin marks Admin as the active sidebar item.
     */
    @Test(groups = {"ui", "regression"},
          description = "Active sidebar item is 'Admin' after navigating to Admin")
    @Severity(SeverityLevel.NORMAL)
    public void activeMenuUpdatesOnAdminNavigation() {
        final DashboardPage dashboard = loginAndGetDashboard();
        dashboard.sidebar().navigateTo("Admin");
        final String activeLabel = dashboard.sidebar().getActiveMenuLabel();
        assertThat(activeLabel).as("Active sidebar label").containsIgnoringCase("Admin");
    }

    /**
     * Verifies that navigating to PIM marks PIM as the active sidebar item.
     */
    @Test(groups = {"ui", "regression"},
          description = "Active sidebar item is 'PIM' after navigating to PIM")
    @Severity(SeverityLevel.NORMAL)
    public void activeMenuUpdatesOnPimNavigation() {
        final DashboardPage dashboard = loginAndGetDashboard();
        dashboard.sidebar().navigateTo("PIM");
        final String activeLabel = dashboard.sidebar().getActiveMenuLabel();
        assertThat(activeLabel).as("Active sidebar label after PIM navigation")
                .containsIgnoringCase("PIM");
    }
}
