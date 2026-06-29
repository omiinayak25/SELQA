package com.omiinqa.bdd.steps.domain;

import com.omiinqa.bdd.support.DomainWorld;
import com.omiinqa.reference.identity.Account;
import com.omiinqa.reference.identity.AuthService;
import com.omiinqa.reference.identity.MfaService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the reference identity / MFA domain.
 *
 * <p>Drives {@link MfaService} directly. An {@link AuthService} is used to
 * provision real accounts so the MFA service has concrete account-ids. All
 * mutating calls flow through {@link DomainWorld} so shared assertions work.</p>
 */
public class MfaSteps {

    private static final String SVC         = "mfaService";
    private static final String AUTH_SVC    = "mfaAuthService";
    private static final String ACCT_ID     = "mfaAccountId";
    private static final String ENROL_RESULT = "mfaEnrolmentResult";

    private MfaService service() {
        return DomainWorld.service(SVC, MfaService::new);
    }

    private AuthService authService() {
        return DomainWorld.service(AUTH_SVC, AuthService::new);
    }

    @Given("a clean MFA service")
    public void cleanMfaService() {
        DomainWorld.put(SVC, new MfaService());
        DomainWorld.put(AUTH_SVC, new AuthService());
    }

    @Given("an MFA-managed account for user {string} with email {string} and password {string}")
    public void mfaManagedAccount(final String username, final String email, final String password) {
        final Account account = authService().register(username, email, password);
        DomainWorld.put(ACCT_ID, account.getId());
    }

    @When("I enrol the account in MFA")
    public void enrolAccountInMfa() {
        final long id = DomainWorld.<Long>get(ACCT_ID);
        DomainWorld.run(() -> {
            final MfaService.EnrolmentResult result = service().enrol(id);
            DomainWorld.put(ENROL_RESULT, result);
        });
    }

    @When("I verify the MFA code for the account")
    public void verifyMfaCode() {
        final long id = DomainWorld.<Long>get(ACCT_ID);
        DomainWorld.run(() -> service().verifyCode(id, service().currentExpectedCode(id)));
    }

    @When("I verify the MFA code {string} for the account")
    public void verifyMfaCodeValue(final String code) {
        final long id = DomainWorld.<Long>get(ACCT_ID);
        DomainWorld.run(() -> service().verifyCode(id, code));
    }

    @When("I use the first MFA backup code for the account")
    public void useFirstMfaBackupCode() {
        final long id = DomainWorld.<Long>get(ACCT_ID);
        DomainWorld.run(() -> {
            final String code = service().backupCodes(id).get(0);
            service().useBackupCode(id, code);
        });
    }

    @When("I use MFA backup code {string} for the account")
    public void useMfaBackupCodeValue(final String code) {
        final long id = DomainWorld.<Long>get(ACCT_ID);
        DomainWorld.run(() -> service().useBackupCode(id, code));
    }

    @When("I disable MFA for the account using the correct code")
    public void disableMfaWithCorrectCode() {
        final long id = DomainWorld.<Long>get(ACCT_ID);
        DomainWorld.run(() -> service().disable(id, service().currentExpectedCode(id)));
    }

    @When("I disable MFA for the account using code {string}")
    public void disableMfaWithCode(final String code) {
        final long id = DomainWorld.<Long>get(ACCT_ID);
        DomainWorld.run(() -> service().disable(id, code));
    }

    @When("I advance the MFA service tick by {int}")
    public void advanceMfaTick(final int amount) {
        service().advanceTick(amount);
    }

    @Then("the account is enrolled in MFA")
    public void accountIsEnrolledInMfa() {
        final long id = DomainWorld.<Long>get(ACCT_ID);
        assertThat(service().isEnrolled(id)).isTrue();
    }

    @Then("the account is not enrolled in MFA")
    public void accountIsNotEnrolledInMfa() {
        final long id = DomainWorld.<Long>get(ACCT_ID);
        assertThat(service().isEnrolled(id)).isFalse();
    }

    @Then("the MFA enrolment result contains {int} backup codes")
    public void enrolmentResultContainsBackupCodes(final int expected) {
        final MfaService.EnrolmentResult result = DomainWorld.get(ENROL_RESULT);
        assertThat(result).isNotNull();
        assertThat(result.backupCodes).hasSize(expected);
    }

    @Then("the MFA backup codes are all distinct")
    public void mfaBackupCodesAreDistinct() {
        final long id = DomainWorld.<Long>get(ACCT_ID);
        final List<String> codes = service().backupCodes(id);
        assertThat(codes).doesNotHaveDuplicates();
    }

    @Then("the MFA enrolment secret is not null")
    public void mfaSecretIsNotNull() {
        final MfaService.EnrolmentResult result = DomainWorld.get(ENROL_RESULT);
        assertThat(result).isNotNull();
        assertThat(result.secret).isNotNull().isNotBlank();
    }
}
