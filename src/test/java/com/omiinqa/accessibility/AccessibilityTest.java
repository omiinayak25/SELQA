package com.omiinqa.accessibility;

import com.deque.html.axecore.results.Results;
import com.omiinqa.businessflows.LoginFlow;
import com.omiinqa.core.BaseTest;
import com.omiinqa.pages.saucedemo.LoginPage;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Accessibility (axe-core, WCAG 2.1 A/AA) scans across key SauceDemo pages.
 *
 * <p>Demo apps carry known accessibility debt, so most checks assert against a
 * <em>violation budget</em> (track-and-reduce) rather than zero — except the
 * critical/serious bar, which is the realistic gate for shipping. Budgets are
 * explicit so regressions (a new violation pushing past the cap) fail loudly.</p>
 */
@Epic("Accessibility")
@Feature("WCAG 2.1 AA")
public class AccessibilityTest extends BaseTest {

    @Test(groups = {"accessibility", "regression"})
    @Severity(SeverityLevel.NORMAL)
    public void loginPageHasNoCriticalOrSeriousViolations() {
        new LoginPage().open();
        final Results results = AccessibilityScanner.wcag21aa().scan(driver());
        AccessibilityValidator.of(results).hasNoCriticalOrSeriousViolations();
    }

    @Test(groups = {"accessibility", "regression"})
    public void inventoryPageWithinViolationBudget() {
        LoginFlow.loginAsStandardUser();
        final Results results = AccessibilityScanner.wcag21aa().scan(driver());
        AccessibilityValidator.of(results).violationsAtMost(15);
    }

    @Test(groups = {"accessibility", "regression"})
    public void cartPageScanProducesResults() {
        LoginFlow.loginAsStandardUser().openCart();
        final Results results = AccessibilityScanner.wcag21aa().scan(driver());
        assertThat(results.getViolations()).as("scan returned a violation list").isNotNull();
        AccessibilityValidator.of(results).hasNoCriticalOrSeriousViolations();
    }

    @Test(groups = {"accessibility", "regression"})
    public void loginFormComponentScopeScan() {
        new LoginPage().open();
        final Results results = AccessibilityScanner.wcag21aa()
                .include("#login_button_container")
                .scan(driver());
        AccessibilityValidator.of(results).violationsAtMost(10);
    }
}
