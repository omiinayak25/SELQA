package com.omiinqa.bdd.steps.domain;

import com.omiinqa.bdd.support.DomainWorld;
import com.omiinqa.reference.identity.Account;
import com.omiinqa.reference.identity.AuthService;
import com.omiinqa.reference.identity.PasswordService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the reference identity / password-management domain.
 *
 * <p>Drives {@link PasswordService} directly. An {@link AuthService} is used
 * to provision real accounts so the password service has a valid account-id to
 * work with. All mutations are wrapped in {@link DomainWorld} so shared
 * assertions work without redefinition.</p>
 */
public class PasswordSteps {

    private static final String SVC       = "passwordService";
    private static final String AUTH_SVC  = "passwordAuthService";
    private static final String ACCT_ID   = "passwordAccountId";
    private static final String RST_TOKEN = "passwordResetToken";

    private PasswordService service() {
        return DomainWorld.service(SVC, PasswordService::new);
    }

    private AuthService authService() {
        return DomainWorld.service(AUTH_SVC, AuthService::new);
    }

    @Given("a clean password service")
    public void cleanPasswordService() {
        DomainWorld.put(SVC, new PasswordService());
        DomainWorld.put(AUTH_SVC, new AuthService());
    }

    @Given("a password-managed account for user {string} with email {string} and password {string}")
    public void passwordManagedAccount(final String username, final String email, final String password) {
        final Account account = authService().register(username, email, password);
        service().registerAccount(account);
        DomainWorld.put(ACCT_ID, account.getId());
    }

    @When("I change the account password from {string} to {string}")
    public void changeAccountPassword(final String oldPwd, final String newPwd) {
        final long id = DomainWorld.<Long>get(ACCT_ID);
        DomainWorld.run(() -> service().changePassword(id, oldPwd, newPwd));
    }

    @When("I issue a password reset token for the account")
    public void issuePasswordResetToken() {
        final long id = DomainWorld.<Long>get(ACCT_ID);
        DomainWorld.run(() -> {
            final String token = service().issueResetToken(id);
            DomainWorld.put(RST_TOKEN, token);
        });
    }

    @When("I reset the account password to {string} using the issued reset token")
    public void resetPasswordWithIssuedToken(final String newPwd) {
        final String token = DomainWorld.get(RST_TOKEN);
        DomainWorld.run(() -> service().resetPassword(token, newPwd));
    }

    @When("I reset the account password to {string} using reset token {string}")
    public void resetPasswordWithToken(final String newPwd, final String token) {
        DomainWorld.run(() -> service().resetPassword(token, newPwd));
    }

    @When("I advance the password service tick by {int}")
    public void advancePasswordTick(final int amount) {
        service().advanceTick(amount);
    }

    @Then("the stored account password is {string}")
    public void storedAccountPasswordIs(final String expected) {
        final long id = DomainWorld.<Long>get(ACCT_ID);
        assertThat(service().currentPassword(id)).isEqualTo(expected);
    }

    @Then("the password reset token is not null")
    public void passwordResetTokenIsNotNull() {
        assertThat((String) DomainWorld.get(RST_TOKEN)).isNotNull().isNotBlank();
    }
}
