package com.omiinqa.ui.saucedemo;

import com.omiinqa.core.BaseTest;
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
 * SauceDemo authentication tests — positive, negative and boundary coverage.
 * Assertions live here (never in the page objects), so the same pages serve
 * every scenario.
 */
@Epic("SauceDemo")
@Feature("Authentication")
public class LoginTest extends BaseTest {

    private static final String PASSWORD = "secret_sauce";

    @Test(groups = {"ui", "smoke", "regression"}, description = "Standard user logs in")
    @Severity(SeverityLevel.BLOCKER)
    @Description("A valid standard user reaches the products page with inventory loaded.")
    public void standardUserCanLogIn() {
        final ProductsPage products = new LoginPage().open().login("standard_user", PASSWORD);
        assertThat(products.isLoaded()).as("inventory loaded").isTrue();
        assertThat(products.getProductCount()).isEqualTo(6);
    }

    @Test(groups = {"ui", "regression"}, description = "Locked-out user is rejected")
    @Severity(SeverityLevel.CRITICAL)
    public void lockedOutUserIsRejected() {
        final LoginPage login = new LoginPage().open();
        login.login("locked_out_user", PASSWORD);
        assertThat(login.isErrorDisplayed()).isTrue();
        assertThat(login.getErrorMessage()).containsIgnoringCase("locked out");
    }

    @Test(groups = {"ui", "regression"},
            dataProvider = "invalidCredentials",
            description = "Invalid / empty credentials are rejected with a message")
    @Severity(SeverityLevel.NORMAL)
    public void invalidCredentialsAreRejected(final String user, final String pass,
                                              final String expectedFragment) {
        final LoginPage login = new LoginPage().open();
        login.login(user, pass);
        assertThat(login.isErrorDisplayed()).as("error shown for '%s'", user).isTrue();
        assertThat(login.getErrorMessage()).containsIgnoringCase(expectedFragment);
    }

    @DataProvider(name = "invalidCredentials")
    public Object[][] invalidCredentials() {
        return new Object[][] {
                {"standard_user", "wrong_password", "do not match"},
                {"ghost_user", "secret_sauce", "do not match"},
                {"", "secret_sauce", "Username is required"},
                {"standard_user", "", "Password is required"},
                {"", "", "Username is required"},
        };
    }
}
