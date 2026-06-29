package com.omiinqa.bdd.steps;

import com.omiinqa.bdd.context.ScenarioContext;
import com.omiinqa.pages.saucedemo.LoginPage;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for advanced authentication scenarios that extend the
 * foundation {@link LoginSteps} with security edge cases (injection,
 * XSS) and state-boundary checks (logout/revisit).
 *
 * <p>These steps deliberately use slightly different phrasing from
 * {@link LoginSteps} to avoid Cucumber ambiguous-step collisions while
 * still exercising the same underlying page objects.</p>
 */
public class AuthenticationAdvancedSteps {

    @When("the user attempts login with username {string} and password {string}")
    public void userAttemptsLoginWithUsernameAndPassword(final String username, final String password) {
        final LoginPage login = ScenarioContext.get(ScenarioContext.LOGIN_PAGE);
        login.login(username, password);
        // The login page stays active on failure; keep reference in context
        ScenarioContext.put(ScenarioContext.LOGIN_PAGE, login);
    }

    @Then("a login error is displayed")
    public void aLoginErrorIsDisplayed() {
        final LoginPage login = ScenarioContext.get(ScenarioContext.LOGIN_PAGE);
        assertThat(login.isErrorDisplayed())
                .as("A login error banner should be visible for rejected credentials")
                .isTrue();
    }
}
