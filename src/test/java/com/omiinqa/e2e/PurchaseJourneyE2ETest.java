package com.omiinqa.e2e;

import com.omiinqa.businessflows.CheckoutFlow;
import com.omiinqa.core.BaseTest;
import com.omiinqa.pages.saucedemo.CheckoutCompletePage;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end purchase journey: login → add items → cart → checkout → finish.
 * Orchestration is delegated to the {@link CheckoutFlow} Facade; the test only
 * asserts the externally-observable outcome.
 */
@Epic("SauceDemo")
@Feature("E2E Purchase")
public class PurchaseJourneyE2ETest extends BaseTest {

    @Test(groups = {"e2e", "regression"})
    @Severity(SeverityLevel.BLOCKER)
    public void customerCanCompleteAPurchase() {
        final List<String> cart = List.of("Sauce Labs Backpack", "Sauce Labs Bolt T-Shirt");

        final CheckoutCompletePage complete =
                CheckoutFlow.completePurchase(cart, "Omkar", "Nayak", "411001");

        assertThat(complete.getCompleteHeaderText())
                .containsIgnoringCase("Thank you for your order");
        assertThat(complete.getCompleteText()).isNotBlank();
    }
}
