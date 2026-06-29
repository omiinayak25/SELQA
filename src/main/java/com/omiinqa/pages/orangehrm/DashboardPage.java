package com.omiinqa.pages.orangehrm;

import com.omiinqa.components.NavigationSidebar;
import com.omiinqa.core.BasePage;
import com.omiinqa.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Page Object for the OrangeHRM main dashboard ({@code /dashboard/index}).
 *
 * <p><b>Composition:</b> the left-hand navigation is exposed via
 * {@link NavigationSidebar}, a reusable {@link com.omiinqa.core.BaseComponent}.
 * DashboardPage does not extend it — it contains it. This keeps the page class
 * focused on dashboard-specific state while delegating sidebar navigation to a
 * dedicated, independently-testable component.</p>
 *
 * <p><b>POM contract:</b> all state is exposed via getters; no assertions are
 * made inside this class.</p>
 */
public class DashboardPage extends BasePage {

    // ---------------------------------------------------------------- locators
    private static final By DASHBOARD_HEADER  = By.cssSelector("h6.oxd-topbar-header-breadcrumb-module");
    private static final By SIDEBAR_ROOT      = By.cssSelector("nav.oxd-sidepanel-body, aside");
    private static final By WIDGET_CONTAINER  = By.cssSelector(".oxd-grid-item");
    private static final By USER_DROPDOWN     = By.className("oxd-userdropdown");
    private static final By LOGOUT_LINK       = By.cssSelector(".oxd-userdropdown-tab ~ ul li:last-child a");

    // ----------------------------------------------------------------- queries

    /**
     * @return the breadcrumb title shown at the top of the page (e.g.
     *         {@code "Dashboard"})
     */
    public String getDashboardTitle() {
        return getText(DASHBOARD_HEADER);
    }

    /**
     * @return {@code true} when the dashboard header is visible, indicating a
     *         successful post-login load
     */
    public boolean isLoaded() {
        return isDisplayed(DASHBOARD_HEADER);
    }

    // ----------------------------------------------------------------- actions

    /**
     * Provides the {@link NavigationSidebar} component bound to the left-hand
     * navigation panel. Use this to navigate to any OrangeHRM module.
     *
     * @return a lazily constructed {@link NavigationSidebar}
     */
    public NavigationSidebar sidebar() {
        final WebElement sidebarRoot = WaitUtils.visible(driver(), SIDEBAR_ROOT);
        return new NavigationSidebar(sidebarRoot);
    }

    /**
     * Logs out of OrangeHRM by clicking the user dropdown then the logout link.
     *
     * @return the {@link OrangeLoginPage}
     */
    public OrangeLoginPage logout() {
        log.info("Logging out of OrangeHRM");
        click(USER_DROPDOWN);
        click(LOGOUT_LINK);
        return new OrangeLoginPage();
    }
}
