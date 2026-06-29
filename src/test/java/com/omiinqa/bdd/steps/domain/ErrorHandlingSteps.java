package com.omiinqa.bdd.steps.domain;

import com.omiinqa.bdd.support.DomainWorld;
import com.omiinqa.reference.core.DomainException;
import com.omiinqa.reference.core.Validations;
import com.omiinqa.reference.identity.AuthService;
import com.omiinqa.reference.security.InputGuard;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

// InputGuard is a utility class (private constructor); all methods are called statically

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the reference <b>error-handling</b> domain.
 *
 * <p>Systematically maps bad inputs across {@link AuthService},
 * {@link Validations}, and {@link InputGuard} to their exact domain error codes.
 * Every scenario runs real business logic — no browser, no fakes.</p>
 *
 * <p>Step text is prefixed with "error handling" or the short noun "error-domain"
 * to stay globally unique.</p>
 */
public class ErrorHandlingSteps {

    private static final String AUTH_SVC = "errorAuthService";

    // -----------------------------------------------------------------------
    // Service helpers
    // -----------------------------------------------------------------------

    private AuthService auth() {
        return DomainWorld.service(AUTH_SVC, AuthService::new);
    }

    // -----------------------------------------------------------------------
    // Background / Given
    // -----------------------------------------------------------------------

    @Given("a clean error-handling service")
    public void cleanErrorHandlingService() {
        DomainWorld.put(AUTH_SVC, new AuthService());
    }

    @Given("an error-domain registered user username {string} email {string} password {string}")
    public void errorDomainRegisterUser(final String username, final String email,
                                        final String password) {
        auth().register(username, email, password);
    }

    // -----------------------------------------------------------------------
    // AuthService — registration error mapping
    // -----------------------------------------------------------------------

    @When("I attempt error-domain registration with username {string} email {string} password {string}")
    public void errorDomainRegistration(final String username, final String email,
                                        final String password) {
        DomainWorld.run(() -> auth().register(username, email, password));
    }

    // -----------------------------------------------------------------------
    // AuthService — login error mapping
    // -----------------------------------------------------------------------

    @When("I attempt error-domain login with identifier {string} password {string}")
    public void errorDomainLogin(final String identifier, final String password) {
        DomainWorld.run(() -> auth().login(identifier, password));
    }

    @When("I attempt error-domain login {int} times with identifier {string} password {string}")
    public void errorDomainLoginRepeated(final int times, final String identifier,
                                         final String password) {
        for (int i = 0; i < times; i++) {
            DomainWorld.run(() -> auth().login(identifier, password));
        }
    }

    // -----------------------------------------------------------------------
    // Validations — structural checks
    // -----------------------------------------------------------------------

    @When("I validate error-domain email {string}")
    public void errorDomainValidateEmail(final String email) {
        DomainWorld.run(() -> Validations.requireValidEmail(email, "AUTH_BAD_EMAIL"));
    }

    @When("I validate error-domain password {string}")
    public void errorDomainValidatePassword(final String password) {
        DomainWorld.run(() -> Validations.requireStrongPassword(password, "AUTH_WEAK_PASSWORD"));
    }

    @When("I validate error-domain blank field {string} named {string}")
    public void errorDomainValidateBlank(final String value, final String field) {
        DomainWorld.run(() -> Validations.requireNotBlank(value, field, "AUTH_BLANK"));
    }

    // -----------------------------------------------------------------------
    // InputGuard — security rejection error mapping
    // -----------------------------------------------------------------------

    @When("I guard error-domain input {string} against SQL injection")
    public void errorDomainGuardSqli(final String input) {
        DomainWorld.run(() -> InputGuard.rejectSqlInjection(input));
    }

    @When("I guard error-domain input {string} against XSS")
    public void errorDomainGuardXss(final String input) {
        DomainWorld.run(() -> InputGuard.rejectXss(input));
    }

    @When("I guard error-domain input {string} against path traversal")
    public void errorDomainGuardTraversal(final String input) {
        DomainWorld.run(() -> InputGuard.rejectPathTraversal(input));
    }

    @When("I guard error-domain input {string} with assertSafe")
    public void errorDomainAssertSafe(final String input) {
        DomainWorld.run(() -> InputGuard.assertSafe(input));
    }

    @When("I check error-domain input {string} max-length {int} field {string}")
    public void errorDomainMaxLength(final String input, final int maxLen, final String field) {
        DomainWorld.run(() -> InputGuard.requireMaxLength(input, field, maxLen));
    }

    @When("I check error-domain input {string} charset field {string}")
    public void errorDomainCharset(final String input, final String field) {
        DomainWorld.run(() -> InputGuard.requireSafeCharset(input, field));
    }

    // -----------------------------------------------------------------------
    // Assertions
    // -----------------------------------------------------------------------

    @Then("the error-domain error code is {string}")
    public void errorDomainCodeIs(final String expectedCode) {
        final DomainException error = DomainWorld.lastError();
        assertThat(error)
                .as("expected domain error '%s' but no error was raised", expectedCode)
                .isNotNull();
        assertThat(error.code())
                .as("domain error code")
                .isEqualTo(expectedCode);
    }

    @Then("the error-domain operation is successful")
    public void errorDomainSuccess() {
        final DomainException error = DomainWorld.lastError();
        assertThat(error)
                .as("expected success but got error: %s",
                        error == null ? "none" : error.code() + " / " + error.getMessage())
                .isNull();
    }

    @Then("the error-domain email validation result is {string}")
    public void errorDomainEmailValidationResult(final String expected) {
        final boolean isValid = Validations.isValidEmail(
                DomainWorld.<String>get("errorDomainLastEmail"));
        assertThat(String.valueOf(isValid)).isEqualTo(expected);
    }
}
