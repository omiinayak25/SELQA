package com.omiinqa.ui.orangehrm;

import com.omiinqa.core.BaseTest;
import com.omiinqa.pages.orangehrm.DashboardPage;
import com.omiinqa.pages.orangehrm.LoginAssistPage;
import com.omiinqa.pages.orangehrm.OrangeLoginPage;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TestNG suite for OrangeHRM authentication flows.
 *
 * <p><b>Coverage:</b>
 * <ul>
 *   <li>Positive: valid Admin login reaches Dashboard and shows correct title</li>
 *   <li>Negative: invalid credentials, empty username, empty password, and fully
 *       empty forms show an error message (parametrised via {@code @DataProvider})</li>
 *   <li>Session: logout from Dashboard returns to the login URL</li>
 *   <li>Navigation: "Forgot your password?" link is visible and navigates correctly</li>
 * </ul>
 * </p>
 *
 * <p>Assertions reside exclusively in this test class, never in page objects
 * (POM contract). Each test obtains a fresh browser session via the
 * {@link BaseTest} per-method lifecycle.</p>
 */
@Epic("OrangeHRM")
@Feature("Authentication")
public class OrangeAuthenticationTest extends BaseTest {

    private static final String VALID_USER = "Admin";
    private static final String VALID_PASS = "admin123";

    // ================================================================= Positive

    /**
     * Verifies that the Admin account can log in and the Dashboard loads.
     */
    @Test(groups = {"ui", "regression", "smoke"},
          description = "Admin login with valid credentials reaches Dashboard")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Open login page, submit Admin / admin123, assert Dashboard header is visible.")
    public void validAdminLoginReachesDashboard() {
        final DashboardPage dashboard = new OrangeLoginPage().open().login(VALID_USER, VALID_PASS);
        assertThat(dashboard.isLoaded())
                .as("Dashboard header visible after valid login").isTrue();
    }

    /**
     * Verifies the dashboard page title text equals "Dashboard" after successful login.
     */
    @Test(groups = {"ui", "regression"},
          description = "Dashboard title is 'Dashboard' after Admin login")
    @Severity(SeverityLevel.NORMAL)
    public void dashboardTitleCorrectAfterLogin() {
        final DashboardPage dashboard = new OrangeLoginPage().open().login(VALID_USER, VALID_PASS);
        assertThat(dashboard.getDashboardTitle())
                .as("Dashboard page title").isEqualToIgnoringCase("Dashboard");
    }

    // ================================================================= Negative (parametrised)

    /**
     * Verifies that invalid credential combinations each produce an error alert.
     */
    @Test(groups = {"ui", "regression"},
          dataProvider = "invalidCredentials",
          description = "Invalid credentials show an error alert")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Negative auth: an error message must appear for every invalid credential combination.")
    public void invalidCredentialsShowError(final String user, final String pass,
                                            final String expectedFragment) {
        final OrangeLoginPage loginPage = new OrangeLoginPage().open();
        loginPage.login(user, pass);
        assertThat(loginPage.isErrorDisplayed())
                .as("Error displayed for user='%s'", user).isTrue();
        assertThat(loginPage.getErrorMessage())
                .as("Error message content").containsIgnoringCase(expectedFragment);
    }

    @DataProvider(name = "invalidCredentials")
    public Object[][] invalidCredentials() {
        return new Object[][] {
                {"Admin",   "wrongpass",   "Invalid"},
                {"unknown", "admin123",    "Invalid"},
                {"",        "admin123",    "Required"},
                {"Admin",   "",            "Required"},
                {"",        "",            "Required"},
        };
    }

    // ================================================================= Empty Fields (individual)

    /**
     * Verifies that submitting with an empty username shows a required-field error.
     */
    @Test(groups = {"ui", "regression"},
          description = "Empty username field produces a required-field error")
    @Severity(SeverityLevel.NORMAL)
    public void emptyUsernameShowsError() {
        final OrangeLoginPage loginPage = new OrangeLoginPage().open();
        loginPage.login("", VALID_PASS);
        assertThat(loginPage.isErrorDisplayed()).as("Error for empty username").isTrue();
    }

    /**
     * Verifies that submitting with an empty password shows a required-field error.
     */
    @Test(groups = {"ui", "regression"},
          description = "Empty password field produces a required-field error")
    @Severity(SeverityLevel.NORMAL)
    public void emptyPasswordShowsError() {
        final OrangeLoginPage loginPage = new OrangeLoginPage().open();
        loginPage.login(VALID_USER, "");
        assertThat(loginPage.isErrorDisplayed()).as("Error for empty password").isTrue();
    }

    // ================================================================= Logout

    /**
     * Verifies that logging out from Dashboard navigates back to the login URL.
     */
    @Test(groups = {"ui", "regression"},
          description = "Logout from Dashboard returns user to login page")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Login as Admin, invoke logout, assert the resulting URL contains 'auth'.")
    public void logoutReturnsToLoginPage() {
        final OrangeLoginPage loginPage = new OrangeLoginPage()
                .open()
                .login(VALID_USER, VALID_PASS)
                .logout();
        assertThat(loginPage.currentUrl())
                .as("URL after logout").containsIgnoringCase("auth");
    }

    // ================================================================= Forgot Password link

    /**
     * Verifies the "Forgot your password?" link is visible on the login screen.
     */
    @Test(groups = {"ui", "regression"},
          description = "'Forgot your password?' link is visible on login page")
    @Severity(SeverityLevel.NORMAL)
    public void forgotPasswordLinkIsVisible() {
        new OrangeLoginPage().open();
        final LoginAssistPage assist = new LoginAssistPage();
        assertThat(assist.isForgotPasswordLinkVisible())
                .as("Forgot password link visible on login page").isTrue();
    }

    /**
     * Verifies that clicking the forgot-password link navigates to the reset page.
     */
    @Test(groups = {"ui", "regression"},
          description = "Clicking 'Forgot your password?' navigates to the reset request page")
    @Severity(SeverityLevel.NORMAL)
    public void forgotPasswordLinkNavigatesToResetPage() {
        new OrangeLoginPage().open();
        final LoginAssistPage assist = new LoginAssistPage();
        assist.clickForgotPasswordLink();
        assertThat(assist.getUrl())
                .as("URL after clicking forgot-password link")
                .containsIgnoringCase("requestPasswordResetCode");
    }
}
