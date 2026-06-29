package com.omiinqa.performance;

import com.omiinqa.businessflows.LoginFlow;
import com.omiinqa.core.BaseTest;
import com.omiinqa.pages.saucedemo.LoginPage;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance-smoke layer: asserts navigation-timing budgets on key pages so a
 * gross performance regression fails the functional suite. Not a load test —
 * a fast, per-page guardrail using the browser Performance API.
 */
@Epic("Performance")
@Feature("Navigation timing budgets")
public class PerformanceSmokeTest extends BaseTest {

    @Test(groups = {"performance", "regression"})
    public void loginPageLoadsWithinBudget() {
        new LoginPage().open();
        PerformanceValidator.of(driver())
                .pageLoadWithin(PerformanceBudget.PAGE_LOAD)
                .domInteractiveWithin(PerformanceBudget.DOM_INTERACTIVE);
    }

    @Test(groups = {"performance", "regression"})
    public void inventoryPageLoadsWithinBudget() {
        LoginFlow.loginAsStandardUser();
        PerformanceValidator.of(driver()).pageLoadWithin(PerformanceBudget.PAGE_LOAD);
    }

    @Test(groups = {"performance", "regression"})
    public void navigationTimingMetricsAreCaptured() {
        new LoginPage().open();
        final PerformanceMetrics metrics = PerformanceMetrics.capture(driver());
        assertThat(metrics.pageLoadMillis()).as("page-load metric available").isGreaterThanOrEqualTo(0);
        assertThat(metrics.timeToFirstByteMillis()).isGreaterThanOrEqualTo(0);
    }

    @Test(groups = {"performance", "regression"})
    public void performanceGlitchUserStillWithinGenerousBudget() {
        // The performance_glitch_user is intentionally slow; assert it still
        // completes within a generous ceiling so we catch true hangs.
        LoginFlow.loginAsPerformanceGlitchUser();
        PerformanceValidator.of(driver())
                .within(new PerformanceBudget("glitch-load", 15_000),
                        PerformanceMetrics.capture(driver()).pageLoadMillis());
    }
}
