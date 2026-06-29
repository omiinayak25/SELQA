package com.omiinqa.visual;

import com.omiinqa.businessflows.LoginFlow;
import com.omiinqa.core.BaseTest;
import com.omiinqa.pages.saucedemo.LoginPage;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Visual-regression layer. Each test captures a page and compares it to a stored
 * baseline within a pixel tolerance; the first run for a key bootstraps the
 * baseline (and passes). Dynamic regions are masked via ignore-rectangles so
 * volatile content does not produce false diffs.
 */
@Epic("Visual")
@Feature("Baseline regression")
public class VisualRegressionTest extends BaseTest {

    @Test(groups = {"visual", "regression"})
    public void loginPageMatchesBaseline() {
        new LoginPage().open();
        final VisualComparisonResult result =
                VisualValidator.forKey("saucedemo-login").capture(driver());
        assertThat(result.matched())
                .as("login page visual (diff=%.3f%%)", result.diffRatio() * 100)
                .isTrue();
    }

    @Test(groups = {"visual", "regression"})
    public void inventoryPageMatchesBaselineIgnoringDynamicCart() {
        LoginFlow.loginAsStandardUser();
        // Mask the cart badge region (top-right) which varies with cart state.
        final VisualComparisonResult result = VisualValidator.forKey("saucedemo-inventory")
                .ignoreRegion(1820, 0, 100, 80)
                .tolerance(0.01)
                .capture(driver());
        assertThat(result.matched())
                .as("inventory visual (diff=%.3f%%)", result.diffRatio() * 100)
                .isTrue();
    }
}
