package com.omiinqa.bdd.steps.domain;

import com.omiinqa.bdd.support.DomainWorld;
import com.omiinqa.reference.identity.Account;
import com.omiinqa.reference.identity.AuthService;
import com.omiinqa.reference.identity.SessionService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the reference identity / session-management domain.
 *
 * <p>Drives {@link SessionService} directly. An {@link AuthService} is used to
 * provision real accounts so the session service has a valid account-id. All
 * mutations are wrapped in {@link DomainWorld} so shared assertions work.</p>
 */
public class SessionSteps {

    private static final String SVC       = "sessionService";
    private static final String AUTH_SVC  = "sessionAuthService";
    private static final String ACCT_ID   = "sessionAccountId";
    private static final String LAST_TOKEN = "sessionLastToken";

    private SessionService service() {
        return DomainWorld.service(SVC, SessionService::new);
    }

    private AuthService authService() {
        return DomainWorld.service(AUTH_SVC, AuthService::new);
    }

    @Given("a clean session service")
    public void cleanSessionService() {
        DomainWorld.put(SVC, new SessionService());
        DomainWorld.put(AUTH_SVC, new AuthService());
    }

    @Given("a session-managed account for user {string} with email {string} and password {string}")
    public void sessionManagedAccount(final String username, final String email, final String password) {
        final Account account = authService().register(username, email, password);
        DomainWorld.put(ACCT_ID, account.getId());
    }

    @When("I create a session for the account")
    public void createSession() {
        final long id = DomainWorld.<Long>get(ACCT_ID);
        DomainWorld.run(() -> {
            final String token = service().createSession(id);
            DomainWorld.put(LAST_TOKEN, token);
        });
    }

    @When("I create {int} sessions for the account")
    public void createMultipleSessions(final int count) {
        final long id = DomainWorld.<Long>get(ACCT_ID);
        for (int i = 0; i < count; i++) {
            DomainWorld.run(() -> {
                final String token = service().createSession(id);
                DomainWorld.put(LAST_TOKEN, token);
            });
        }
    }

    @When("I validate the last session token")
    public void validateLastSessionToken() {
        final String token = DomainWorld.get(LAST_TOKEN);
        DomainWorld.run(() -> service().validate(token));
    }

    @When("I validate session token {string}")
    public void validateSessionToken(final String token) {
        DomainWorld.run(() -> service().validate(token));
    }

    @When("I revoke the last session token")
    public void revokeLastSessionToken() {
        final String token = DomainWorld.get(LAST_TOKEN);
        DomainWorld.run(() -> service().revoke(token));
    }

    @When("I revoke all sessions for the account")
    public void revokeAllSessionsForAccount() {
        final long id = DomainWorld.<Long>get(ACCT_ID);
        DomainWorld.run(() -> service().revokeAll(id));
    }

    @When("I advance the session service tick by {int}")
    public void advanceSessionTick(final int amount) {
        service().advanceTick(amount);
    }

    @Then("the session token is not null")
    public void sessionTokenIsNotNull() {
        assertThat((String) DomainWorld.get(LAST_TOKEN)).isNotNull().isNotBlank();
    }

    @Then("the live session count for the account is {int}")
    public void liveSessionCountIs(final int expected) {
        final long id = DomainWorld.<Long>get(ACCT_ID);
        assertThat(service().liveSessionCount(id)).isEqualTo(expected);
    }

    @Then("the validated session belongs to the account")
    public void validatedSessionBelongsToAccount() {
        final long expected = DomainWorld.<Long>get(ACCT_ID);
        final String token = DomainWorld.get(LAST_TOKEN);
        assertThat(service().validate(token)).isEqualTo(expected);
    }
}
