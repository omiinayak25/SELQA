package com.omiinqa.security;

import com.omiinqa.core.BaseTest;
import com.omiinqa.pages.saucedemo.LoginPage;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.openqa.selenium.Cookie;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Security test layer for the UI under test (SauceDemo).
 *
 * <p>Confirms the application neutralizes injection input rather than executing
 * or reflecting it, that session cookies exist after auth, and that no obvious
 * secret leaks into page source. These are authorized, educational probes
 * against a public demo app — see {@link SecurityPayloads}.</p>
 */
@Epic("Security")
@Feature("Input handling & session")
public class SecurityTest extends BaseTest {

    @DataProvider(name = "sqlInjection")
    public Object[][] sqlInjection() {
        return SecurityPayloads.SQL_INJECTION.stream().map(p -> new Object[]{p}).toArray(Object[][]::new);
    }

    @DataProvider(name = "xss")
    public Object[][] xss() {
        return SecurityPayloads.XSS.stream().map(p -> new Object[]{p}).toArray(Object[][]::new);
    }

    @Test(groups = {"security", "regression"}, dataProvider = "sqlInjection")
    @Severity(SeverityLevel.CRITICAL)
    public void sqlInjectionInLoginIsRejected(final String payload) {
        final LoginPage login = new LoginPage().open();
        login.login(payload, payload);
        // App must NOT authenticate; it stays on login showing an error.
        SecurityAssertions.assertRejected(login.isErrorDisplayed(), payload);
        assertThat(login.currentUrl()).doesNotContain("inventory");
    }

    @Test(groups = {"security", "regression"}, dataProvider = "xss")
    public void xssPayloadIsNotReflectedVerbatim(final String payload) {
        final LoginPage login = new LoginPage().open();
        login.login(payload, "secret_sauce");
        SecurityAssertions.assertPayloadNotReflected(payload, driver().getPageSource());
    }

    @Test(groups = {"security", "regression"})
    public void sessionCookieExistsAfterLogin() {
        new LoginPage().open().login("standard_user", "secret_sauce");
        final Cookie session = driver().manage().getCookieNamed("session-username");
        assertThat(session).as("SauceDemo session cookie present").isNotNull();
    }

    @Test(groups = {"security", "regression"})
    public void pageSourceDoesNotLeakSecrets() {
        new LoginPage().open();
        SecurityAssertions.assertNoSensitiveDataInSource(driver().getPageSource());
    }
}
