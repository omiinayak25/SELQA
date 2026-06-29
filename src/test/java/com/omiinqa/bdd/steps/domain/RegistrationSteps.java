package com.omiinqa.bdd.steps.domain;

import com.omiinqa.bdd.support.DomainWorld;
import com.omiinqa.reference.identity.Account;
import com.omiinqa.reference.identity.RegistrationService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the reference identity / registration domain.
 *
 * <p>Drives {@link RegistrationService} directly — no browser, no fakes. All
 * mutating calls are wrapped in {@link DomainWorld#run}/{@link DomainWorld#capture}
 * so the shared assertions in {@code CommonDomainSteps} work without redefinition.</p>
 */
public class RegistrationSteps {

    private static final String SVC   = "registrationService";
    private static final String TOKEN = "registrationLastToken";

    private RegistrationService service() {
        return DomainWorld.service(SVC, RegistrationService::new);
    }

    @Given("a clean registration service")
    public void cleanRegistrationService() {
        DomainWorld.put(SVC, new RegistrationService());
    }

    @When("I begin registration with username {string} email {string} password {string} and terms accepted {string}")
    public void beginRegistration(final String username, final String email,
                                  final String password, final String termsStr) {
        final boolean terms = Boolean.parseBoolean(termsStr);
        DomainWorld.run(() -> {
            final String token = service().beginRegistration(username, email, password, terms);
            DomainWorld.put(TOKEN, token);
        });
    }

    @When("I verify the registration email with the issued token")
    public void verifyWithIssuedToken() {
        final String token = DomainWorld.get(TOKEN);
        DomainWorld.run(() -> {
            final Account account = service().verifyEmail(token);
            DomainWorld.put("registrationLastAccount", account);
        });
    }

    @When("I verify the registration email with token {string}")
    public void verifyWithToken(final String token) {
        DomainWorld.run(() -> {
            final Account account = service().verifyEmail(token);
            DomainWorld.put("registrationLastAccount", account);
        });
    }

    @When("I advance the registration service tick by {int}")
    public void advanceRegistrationTick(final int amount) {
        service().advanceTick(amount);
    }

    @Then("the verified registration count is {int}")
    public void verifiedRegistrationCount(final int expected) {
        assertThat(service().verifiedCount()).isEqualTo(expected);
    }

    @Then("the pending registration count is {int}")
    public void pendingRegistrationCount(final int expected) {
        assertThat(service().pendingCount()).isEqualTo(expected);
    }

    @Then("the registered account {string} is email-verified")
    public void accountIsEmailVerified(final String email) {
        final Account account = service().findVerified(email);
        assertThat(account).as("no verified account for email: " + email).isNotNull();
        assertThat(account.isEmailVerified()).isTrue();
    }

    @Then("no verified account exists for email {string}")
    public void noVerifiedAccountForEmail(final String email) {
        assertThat(service().findVerified(email)).isNull();
    }

    @Then("the registration token is not null")
    public void registrationTokenIsNotNull() {
        assertThat((String) DomainWorld.get(TOKEN)).isNotNull().isNotBlank();
    }
}
