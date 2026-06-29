package com.omiinqa.bdd.steps.domain;

import com.omiinqa.bdd.support.DomainWorld;
import com.omiinqa.reference.identity.Account;
import com.omiinqa.reference.identity.AuthService;
import com.omiinqa.reference.identity.OtpService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the reference identity / OTP domain.
 *
 * <p>Drives {@link OtpService} directly. An {@link AuthService} provisions real
 * accounts so OTP operations have a concrete account-id. All mutations flow
 * through {@link DomainWorld} so shared assertions work without redefinition.</p>
 */
public class OtpSteps {

    private static final String SVC      = "otpService";
    private static final String AUTH_SVC = "otpAuthService";
    private static final String ACCT_ID  = "otpAccountId";
    private static final String OTP_CODE = "otpLastCode";

    private OtpService service() {
        return DomainWorld.service(SVC, OtpService::new);
    }

    private AuthService authService() {
        return DomainWorld.service(AUTH_SVC, AuthService::new);
    }

    @Given("a clean OTP service")
    public void cleanOtpService() {
        DomainWorld.put(SVC, new OtpService());
        DomainWorld.put(AUTH_SVC, new AuthService());
    }

    @Given("an OTP-managed account for user {string} with email {string} and password {string}")
    public void otpManagedAccount(final String username, final String email, final String password) {
        final Account account = authService().register(username, email, password);
        DomainWorld.put(ACCT_ID, account.getId());
    }

    @When("I generate an OTP for the account")
    public void generateOtpForAccount() {
        final long id = DomainWorld.<Long>get(ACCT_ID);
        DomainWorld.run(() -> {
            final String code = service().generate(id);
            DomainWorld.put(OTP_CODE, code);
        });
    }

    @When("I verify the OTP with the generated code")
    public void verifyOtpWithGeneratedCode() {
        final long id = DomainWorld.<Long>get(ACCT_ID);
        final String code = DomainWorld.get(OTP_CODE);
        DomainWorld.run(() -> service().verify(id, code));
    }

    @When("I verify the OTP with code {string}")
    public void verifyOtpWithCode(final String code) {
        final long id = DomainWorld.<Long>get(ACCT_ID);
        DomainWorld.run(() -> service().verify(id, code));
    }

    @When("I verify the OTP {int} times with wrong code {string}")
    public void verifyOtpRepeatedlyWithWrongCode(final int times, final String code) {
        final long id = DomainWorld.<Long>get(ACCT_ID);
        for (int i = 0; i < times; i++) {
            DomainWorld.run(() -> service().verify(id, code));
        }
    }

    @When("I advance the OTP service tick by {int}")
    public void advanceOtpTick(final int amount) {
        service().advanceTick(amount);
    }

    @Then("the OTP code is not null")
    public void otpCodeIsNotNull() {
        assertThat((String) DomainWorld.get(OTP_CODE)).isNotNull().isNotBlank();
    }

    @Then("the OTP code is a 6-digit numeric string")
    public void otpCodeIsSixDigitNumeric() {
        final String code = DomainWorld.get(OTP_CODE);
        assertThat(code).matches("\\d{6}");
    }

    @Then("the OTP code for the account is {string}")
    public void otpCodeForAccountIs(final String expected) {
        final long id = DomainWorld.<Long>get(ACCT_ID);
        assertThat(service().currentCode(id)).isEqualTo(expected);
    }
}
