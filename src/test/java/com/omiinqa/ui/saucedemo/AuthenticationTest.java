package com.omiinqa.ui.saucedemo;

import com.omiinqa.businessflows.LoginFlow;
import com.omiinqa.core.BaseTest;
import com.omiinqa.data.factory.CredentialsFactory;
import com.omiinqa.data.model.Credentials;
import com.omiinqa.pages.saucedemo.LoginPage;
import com.omiinqa.pages.saucedemo.ProductsPage;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive authentication tests for SauceDemo covering all four user personas
 * (data-driven via {@code @DataProvider}), wrong-password and blank-field boundary
 * scenarios, session persistence after login, logout, and re-login workflows.
 *
 * <p><b>Design:</b> Each test method follows the Arrange-Act-Assert pattern with all
 * assertions confined to this class. Page objects and flows are consumed but never
 * modified. The {@code allUsers} data provider drives the 4-persona matrix so adding
 * a new SauceDemo user persona requires only a single row change here.</p>
 *
 * <p>Groups {@code ui} and {@code regression} apply to every method; the
 * {@code smoke} group is reserved for the single critical happy-path check.</p>
 */
@Epic("SauceDemo")
@Feature("Authentication")
public class AuthenticationTest extends BaseTest {

    private static final String PASSWORD = "secret_sauce";

    // --------------------------------------------------------- data providers

    /**
     * Provides the four SauceDemo user personas together with the flag indicating
     * whether login is expected to succeed.
     *
     * @return matrix of [username, password, expectSuccess]
     */
    @DataProvider(name = "allUsers")
    public Object[][] allUsers() {
        return new Object[][] {
                {"standard_user",           PASSWORD, true},
                {"problem_user",            PASSWORD, true},
                {"performance_glitch_user", PASSWORD, true},
                {"locked_out_user",         PASSWORD, false},
        };
    }

    /**
     * Provides blank-field boundary combinations and the expected error fragment.
     *
     * @return matrix of [username, password, expectedErrorFragment]
     */
    @DataProvider(name = "blankFieldCases")
    public Object[][] blankFieldCases() {
        return new Object[][] {
                {"",              PASSWORD,  "Username is required"},
                {"standard_user", "",        "Password is required"},
                {"",              "",        "Username is required"},
        };
    }

    // --------------------------------------------------------- test methods

    /**
     * Verifies that each of the four SauceDemo user personas either lands on the
     * products page (success=true) or stays on the login page with an error
     * (success=false). Drives all four rows from {@code allUsers}.
     *
     * @param username      SauceDemo username
     * @param password      SauceDemo password
     * @param expectSuccess whether login should succeed
     */
    @Test(groups = {"ui", "regression"},
            dataProvider = "allUsers",
            description = "All four user personas: success or rejection as expected")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Data-driven login covering all four SauceDemo user personas.")
    public void allUserPersonasLoginBehaviour(final String username,
                                               final String password,
                                               final boolean expectSuccess) {
        final LoginPage loginPage = new LoginPage().open();
        final ProductsPage products = loginPage.login(username, password);

        if (expectSuccess) {
            assertThat(products.isLoaded())
                    .as("Products page should be loaded for user '%s'", username)
                    .isTrue();
        } else {
            assertThat(loginPage.isErrorDisplayed())
                    .as("Error should be displayed for user '%s'", username)
                    .isTrue();
            assertThat(loginPage.getErrorMessage())
                    .as("Error message for locked-out user")
                    .containsIgnoringCase("locked out");
        }
    }

    /**
     * Asserts that submitting a completely wrong password for a valid username
     * displays the credentials-mismatch error and does not reach the products page.
     */
    @Test(groups = {"ui", "regression"},
            description = "Wrong password for valid username shows mismatch error")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Login with correct username but wrong password must show an error.")
    public void wrongPasswordShowsMismatchError() {
        final Credentials creds = CredentialsFactory.validUsernameWrongPassword();
        final LoginPage loginPage = new LoginPage().open();
        loginPage.login(creds.getUsername(), creds.getPassword());

        assertThat(loginPage.isErrorDisplayed()).as("error shown").isTrue();
        assertThat(loginPage.getErrorMessage()).containsIgnoringCase("do not match");
    }

    /**
     * Asserts that an entirely unknown username/password pair is rejected with the
     * credentials-mismatch error message.
     */
    @Test(groups = {"ui", "regression"},
            description = "Unknown user is rejected with credentials mismatch error")
    @Severity(SeverityLevel.NORMAL)
    @Description("Login with a non-existent user account must be rejected.")
    public void unknownUserIsRejected() {
        final Credentials creds = CredentialsFactory.invalidUser();
        final LoginPage loginPage = new LoginPage().open();
        loginPage.login(creds.getUsername(), creds.getPassword());

        assertThat(loginPage.isErrorDisplayed()).isTrue();
        assertThat(loginPage.getErrorMessage()).containsIgnoringCase("do not match");
    }

    /**
     * Verifies blank-field boundary cases: each of the three blank combinations
     * must produce an error whose text contains the expected fragment.
     *
     * @param username              username field value (may be blank)
     * @param password              password field value (may be blank)
     * @param expectedErrorFragment fragment expected in the error message
     */
    @Test(groups = {"ui", "regression"},
            dataProvider = "blankFieldCases",
            description = "Blank username or password produces the relevant required-field error")
    @Severity(SeverityLevel.NORMAL)
    @Description("Blank field validation: each missing field triggers its own error message.")
    public void blankFieldsProduceRequiredFieldErrors(final String username,
                                                       final String password,
                                                       final String expectedErrorFragment) {
        final LoginPage loginPage = new LoginPage().open();
        loginPage.login(username, password);

        assertThat(loginPage.isErrorDisplayed())
                .as("error shown for username='%s', password='%s'", username, password)
                .isTrue();
        assertThat(loginPage.getErrorMessage())
                .as("error message fragment")
                .containsIgnoringCase(expectedErrorFragment);
    }

    /**
     * Confirms that after a successful login the browser URL contains the inventory
     * path, demonstrating a valid session has been established.
     */
    @Test(groups = {"ui", "smoke", "regression"},
            description = "Session is established: URL contains inventory path after login")
    @Severity(SeverityLevel.BLOCKER)
    @Description("After login, the active URL must contain the inventory page path.")
    public void sessionEstablishedAfterLogin() {
        final ProductsPage products = LoginFlow.loginAsStandardUser();

        assertThat(products.isLoaded()).as("products page loaded").isTrue();
        assertThat(products.currentUrl())
                .as("URL contains inventory path")
                .contains("inventory");
    }

    /**
     * Exercises logout via the burger menu: after logout the browser should no
     * longer be on the inventory page (implying the session has been cleared).
     */
    @Test(groups = {"ui", "regression"},
            description = "Logout via burger menu navigates away from inventory")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Clicking Logout in the burger menu must end the session.")
    public void logoutNavigatesAwayFromInventory() {
        final ProductsPage products = LoginFlow.loginAsStandardUser();
        assertThat(products.isLoaded()).as("logged in").isTrue();

        products.header().logout();

        // After logout the URL must no longer point to inventory
        assertThat(products.currentUrl())
                .as("URL should not contain inventory after logout")
                .doesNotContain("inventory");
    }

    /**
     * Verifies that a user can re-login successfully after logging out: the
     * products page must load correctly on the second login.
     */
    @Test(groups = {"ui", "regression"},
            description = "Re-login after logout succeeds and loads the inventory")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Logout followed by a fresh login must restore full inventory access.")
    public void reLoginAfterLogoutSucceeds() {
        // First login
        final ProductsPage products = LoginFlow.loginAsStandardUser();
        products.header().logout();

        // Second login
        final ProductsPage productsAgain = new LoginPage().open().login("standard_user", PASSWORD);
        assertThat(productsAgain.isLoaded())
                .as("inventory loaded after re-login")
                .isTrue();
        assertThat(productsAgain.getProductCount())
                .as("all 6 products visible after re-login")
                .isEqualTo(6);
    }

    /**
     * Asserts that the page title on the login page is set to the expected value,
     * verifying basic page identity before credential submission.
     */
    @Test(groups = {"ui", "regression"},
            description = "Login page title is correct before any interaction")
    @Severity(SeverityLevel.MINOR)
    @Description("The browser tab title for the login page must match the expected value.")
    public void loginPageHasCorrectTitle() {
        final LoginPage loginPage = new LoginPage().open();

        assertThat(loginPage.pageTitle())
                .as("login page browser title")
                .isEqualToIgnoringCase("Swag Labs");
    }
}
