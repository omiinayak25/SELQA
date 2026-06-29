package com.omiinqa.bdd.steps;

import com.omiinqa.bdd.context.ScenarioContext;
import com.omiinqa.businessflows.CheckoutFlow;
import com.omiinqa.pages.saucedemo.CheckoutCompletePage;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the SauceDemo checkout journey. The heavy orchestration
 * is delegated to the {@link CheckoutFlow} Facade — steps stay declarative.
 */
public class CheckoutSteps {

    @When("the customer purchases the following items:")
    public void customerPurchases(final List<String> items) {
        // First column of the data table (header row ignored by Cucumber for List<String>).
        final CheckoutCompletePage complete =
                CheckoutFlow.completePurchase(items, "Omkar", "Nayak", "411001");
        ScenarioContext.put(ScenarioContext.CHECKOUT_COMPLETE, complete);
    }

    @Then("the order confirmation is shown")
    public void orderConfirmationShown() {
        final CheckoutCompletePage complete =
                ScenarioContext.get(ScenarioContext.CHECKOUT_COMPLETE);
        assertThat(complete.getCompleteHeaderText())
                .containsIgnoringCase("Thank you for your order");
    }
}
