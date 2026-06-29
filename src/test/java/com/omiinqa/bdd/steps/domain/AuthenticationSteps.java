package com.omiinqa.bdd.steps.domain;

import com.omiinqa.bdd.support.DomainWorld;
import com.omiinqa.reference.identity.Account;
import com.omiinqa.reference.identity.AuthService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the reference identity / authentication domain.
 *
 * <p>These steps execute the real {@link AuthService} and assert concrete
 * business outcomes — no browser, no fakes. Generic success/error assertions are
 * provided by {@code CommonDomainSteps}; outcomes are recorded via
 * {@link DomainWorld}.</p>
 */
public class AuthenticationSteps {

    private static final String SVC = "authService";
    private static final String ACCOUNT = "authLastAccount";

    private AuthService service() {
        return DomainWorld.service(SVC, AuthService::new);
    }

    @Given("a clean authentication service")
    public void cleanAuthService() {
        DomainWorld.put(SVC, new AuthService());
    }

    @Given("a registered user with username {string} email {string} and password {string}")
    public void registeredUser(final String username, final String email, final String password) {
        DomainWorld.put(ACCOUNT, service().register(username, email, password));
    }

    @When("I register a user with username {string} email {string} and password {string}")
    public void registerUser(final String username, final String email, final String password) {
        DomainWorld.run(() -> DomainWorld.put(ACCOUNT, service().register(username, email, password)));
    }

    @When("I authenticate as {string} with password {string}")
    public void authenticate(final String identifier, final String password) {
        DomainWorld.run(() -> DomainWorld.put(ACCOUNT, service().login(identifier, password)));
    }

    @When("I authenticate {int} times as {string} with wrong password {string}")
    public void authenticateRepeatedly(final int times, final String identifier, final String password) {
        for (int i = 0; i < times; i++) {
            DomainWorld.run(() -> service().login(identifier, password));
        }
    }

    @Then("the account {string} has status {string}")
    public void accountHasStatus(final String identifier, final String status) {
        final Account account = service().find(identifier)
                .orElseThrow(() -> new AssertionError("account not found: " + identifier));
        assertThat(account.getStatus().name()).isEqualTo(status);
    }

    @Then("the registered account count is {int}")
    public void registeredAccountCount(final int expected) {
        assertThat(service().accountCount()).isEqualTo(expected);
    }
}
