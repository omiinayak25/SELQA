package com.omiinqa.bdd.steps.domain;

import com.omiinqa.bdd.support.DomainWorld;
import com.omiinqa.reference.core.DomainException;
import com.omiinqa.reference.core.Validations;
import com.omiinqa.reference.security.InputGuard;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the reference <b>validation-rules</b> domain.
 *
 * <p>Exercises boundary conditions for email, password, length, and charset
 * validation rules via {@link Validations} and {@link InputGuard}. Every Then
 * asserts a concrete error code or boolean value — nothing trivially true.</p>
 *
 * <p>Step text is prefixed with "validation" or "val-rule" to be globally unique.</p>
 */
public class ValidationSteps {

    private static final String VALIDATION_BOOL = "validation.lastBool";

    // -----------------------------------------------------------------------
    // Background / Given
    // -----------------------------------------------------------------------

    @Given("a clean validation context")
    public void cleanValidationContext() {
        DomainWorld.put(VALIDATION_BOOL, null);
    }

    // -----------------------------------------------------------------------
    // Email validation — boolean queries (no exception)
    // -----------------------------------------------------------------------

    @When("I check validation email {string}")
    public void checkValidationEmail(final String email) {
        final boolean result = Validations.isValidEmail(email);
        DomainWorld.put(VALIDATION_BOOL, result);
    }

    @When("I validate validation email {string} with code {string}")
    public void validateValidationEmailWithCode(final String email, final String code) {
        DomainWorld.run(() -> Validations.requireValidEmail(email, code));
    }

    // -----------------------------------------------------------------------
    // Password validation — boolean queries (no exception)
    // -----------------------------------------------------------------------

    @When("I check validation password strength {string}")
    public void checkValidationPasswordStrength(final String password) {
        final boolean result = Validations.isStrongPassword(password);
        DomainWorld.put(VALIDATION_BOOL, result);
    }

    @When("I validate validation password {string} with code {string}")
    public void validateValidationPasswordWithCode(final String password, final String code) {
        DomainWorld.run(() -> Validations.requireStrongPassword(password, code));
    }

    // -----------------------------------------------------------------------
    // Blank field validation
    // -----------------------------------------------------------------------

    @When("I check validation blank on {string}")
    public void checkValidationBlank(final String value) {
        final boolean result = Validations.isBlank(value);
        DomainWorld.put(VALIDATION_BOOL, result);
    }

    @When("I validate validation not-blank field {string} value {string} code {string}")
    public void validateValidationNotBlank(final String field, final String value,
                                           final String code) {
        DomainWorld.run(() -> Validations.requireNotBlank(value, field, code));
    }

    // -----------------------------------------------------------------------
    // InputGuard — length boundary
    // -----------------------------------------------------------------------

    @When("I validate validation max-length {int} on field {string} with value of length {int}")
    public void validateValidationMaxLength(final int maxLen, final String field,
                                            final int valueLen) {
        final String value = "a".repeat(valueLen);
        DomainWorld.run(() -> InputGuard.requireMaxLength(value, field, maxLen));
    }

    @When("I validate validation required on field {string} with value {string}")
    public void validateValidationRequired(final String field, final String value) {
        final String actual = "<null>".equals(value) ? null : value;
        DomainWorld.run(() -> InputGuard.requireNotBlank(actual, field));
    }

    // -----------------------------------------------------------------------
    // InputGuard — charset boundary
    // -----------------------------------------------------------------------

    @When("I validate validation charset on field {string} with value {string}")
    public void validateValidationCharset(final String field, final String value) {
        DomainWorld.run(() -> InputGuard.requireSafeCharset(value, field));
    }

    // -----------------------------------------------------------------------
    // Combined assertSafe boundary
    // -----------------------------------------------------------------------

    @When("I run validation assertSafe on {string}")
    public void runValidationAssertSafe(final String input) {
        DomainWorld.run(() -> InputGuard.assertSafe(input));
    }

    // -----------------------------------------------------------------------
    // Assertions
    // -----------------------------------------------------------------------

    @Then("the validation result is {string}")
    public void theValidationResultIs(final String expected) {
        final Object val = DomainWorld.get(VALIDATION_BOOL);
        assertThat(val)
                .as("validation boolean result")
                .isNotNull();
        assertThat(String.valueOf(val)).isEqualTo(expected);
    }

    @Then("the validation error code is {string}")
    public void theValidationErrorCodeIs(final String expectedCode) {
        final DomainException error = DomainWorld.lastError();
        assertThat(error)
                .as("expected validation error '%s' but no error was raised", expectedCode)
                .isNotNull();
        assertThat(error.code())
                .as("validation error code")
                .isEqualTo(expectedCode);
    }

    @Then("no validation error is raised")
    public void noValidationErrorIsRaised() {
        final DomainException error = DomainWorld.lastError();
        assertThat(error)
                .as("expected no validation error but got: %s",
                        error == null ? "none" : error.code() + " / " + error.getMessage())
                .isNull();
    }
}
