package com.omiinqa.bdd.steps.domain;

import com.omiinqa.bdd.support.DomainWorld;
import com.omiinqa.reference.identity.Account;
import com.omiinqa.reference.identity.AuthService;
import com.omiinqa.reference.identity.ProfileService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the reference identity / profile domain.
 *
 * <p>Drives {@link ProfileService} directly; reuses an {@link AuthService} to
 * register accounts and obtain real account-ids. All mutating calls flow through
 * {@link DomainWorld} so generic assertions in {@code CommonDomainSteps} work.</p>
 */
public class ProfileSteps {

    private static final String SVC      = "profileService";
    private static final String AUTH_SVC = "profileAuthService";
    private static final String ACCT_ID  = "profileAccountId";
    private static final String EMAIL_TOKEN = "profileEmailChangeToken";

    private ProfileService service() {
        return DomainWorld.service(SVC, ProfileService::new);
    }

    private AuthService authService() {
        return DomainWorld.service(AUTH_SVC, AuthService::new);
    }

    @Given("a clean profile service")
    public void cleanProfileService() {
        DomainWorld.put(SVC, new ProfileService());
        DomainWorld.put(AUTH_SVC, new AuthService());
    }

    @Given("a profile account for user {string} with email {string} and password {string}")
    public void profileAccountForUser(final String username, final String email, final String password) {
        final Account account = authService().register(username, email, password);
        service().registerAccount(account);
        DomainWorld.put(ACCT_ID, account.getId());
    }

    @When("I update the profile name to first {string} last {string}")
    public void updateProfileName(final String first, final String last) {
        final long id = DomainWorld.<Long>get(ACCT_ID);
        DomainWorld.run(() -> service().updateName(id, first, last));
    }

    @When("I update the profile display name to {string}")
    public void updateProfileDisplayName(final String displayName) {
        final long id = DomainWorld.<Long>get(ACCT_ID);
        DomainWorld.run(() -> service().updateDisplayName(id, displayName));
    }

    @When("I update the profile phone to {string}")
    public void updateProfilePhone(final String phone) {
        final long id = DomainWorld.<Long>get(ACCT_ID);
        DomainWorld.run(() -> service().updatePhone(id, phone));
    }

    @When("I update the profile bio to a string of {int} characters")
    public void updateProfileBioToLength(final int length) {
        final long id = DomainWorld.<Long>get(ACCT_ID);
        final String bio = "x".repeat(length);
        DomainWorld.run(() -> service().updateBio(id, bio));
    }

    @When("I update the profile bio to {string}")
    public void updateProfileBio(final String bio) {
        final long id = DomainWorld.<Long>get(ACCT_ID);
        DomainWorld.run(() -> service().updateBio(id, bio));
    }

    @When("I request a profile email change to {string}")
    public void requestProfileEmailChange(final String newEmail) {
        final long id = DomainWorld.<Long>get(ACCT_ID);
        DomainWorld.run(() -> {
            final String token = service().requestEmailChange(id, newEmail);
            DomainWorld.put(EMAIL_TOKEN, token);
        });
    }

    @When("I confirm the profile email change with the issued token")
    public void confirmProfileEmailChangeWithIssuedToken() {
        final long id = DomainWorld.<Long>get(ACCT_ID);
        final String token = DomainWorld.get(EMAIL_TOKEN);
        DomainWorld.run(() -> service().confirmEmailChange(id, token));
    }

    @When("I confirm the profile email change with token {string}")
    public void confirmProfileEmailChangeWithToken(final String token) {
        final long id = DomainWorld.<Long>get(ACCT_ID);
        DomainWorld.run(() -> service().confirmEmailChange(id, token));
    }

    @Then("the profile first name is {string}")
    public void profileFirstNameIs(final String expected) {
        final long id = DomainWorld.<Long>get(ACCT_ID);
        assertThat(service().get(id).firstName).isEqualTo(expected);
    }

    @Then("the profile last name is {string}")
    public void profileLastNameIs(final String expected) {
        final long id = DomainWorld.<Long>get(ACCT_ID);
        assertThat(service().get(id).lastName).isEqualTo(expected);
    }

    @Then("the profile display name is {string}")
    public void profileDisplayNameIs(final String expected) {
        final long id = DomainWorld.<Long>get(ACCT_ID);
        assertThat(service().get(id).displayName).isEqualTo(expected);
    }

    @Then("the profile phone is {string}")
    public void profilePhoneIs(final String expected) {
        final long id = DomainWorld.<Long>get(ACCT_ID);
        assertThat(service().get(id).phone).isEqualTo(expected);
    }

    @Then("the profile email is {string}")
    public void profileEmailIs(final String expected) {
        final long id = DomainWorld.<Long>get(ACCT_ID);
        assertThat(service().get(id).email).isEqualTo(expected);
    }

    @Then("the profile bio length is {int}")
    public void profileBioLengthIs(final int expected) {
        final long id = DomainWorld.<Long>get(ACCT_ID);
        final String bio = service().get(id).bio;
        assertThat(bio).isNotNull();
        assertThat(bio.length()).isEqualTo(expected);
    }
}
