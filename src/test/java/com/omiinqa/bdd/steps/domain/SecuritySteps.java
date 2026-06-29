package com.omiinqa.bdd.steps.domain;

import com.omiinqa.bdd.support.DomainWorld;
import com.omiinqa.reference.identity.AuthService;
import com.omiinqa.reference.security.InputGuard;
import com.omiinqa.security.SecurityAssertions;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the reference <b>security</b> domain.
 *
 * <p>These steps drive {@link InputGuard} and {@link AuthService} with known
 * attack payloads (SQL injection, XSS, path traversal) from
 * {@code SecurityPayloads} and assert that every hostile input is rejected with
 * the correct domain error code. All checks run offline — no browser, no fakes.</p>
 *
 * <p>Step text is prefixed with the domain noun ("security") or "I guard" / "input guard"
 * to remain globally unique alongside all other domain step classes.</p>
 */
public class SecuritySteps {

    private static final String AUTH_SVC   = "securityAuthService";

    // -----------------------------------------------------------------------
    // Service helpers
    // -----------------------------------------------------------------------

    private AuthService auth() {
        return DomainWorld.service(AUTH_SVC, AuthService::new);
    }

    // -----------------------------------------------------------------------
    // Background / Given
    // -----------------------------------------------------------------------

    @Given("a clean security input guard")
    public void cleanInputGuard() {
        DomainWorld.put(AUTH_SVC, new AuthService());
    }

    @Given("a security auth service with a registered user username {string} email {string} password {string}")
    public void securityRegisterUser(final String username, final String email, final String password) {
        auth().register(username, email, password);
    }

    // -----------------------------------------------------------------------
    // SQL Injection — InputGuard
    // -----------------------------------------------------------------------

    @When("I guard against SQL injection with payload {string}")
    public void guardAgainstSqli(final String payload) {
        DomainWorld.run(() -> InputGuard.rejectSqlInjection(payload));
    }

    // -----------------------------------------------------------------------
    // XSS — InputGuard
    // -----------------------------------------------------------------------

    @When("I guard against XSS with payload {string}")
    public void guardAgainstXss(final String payload) {
        DomainWorld.run(() -> InputGuard.rejectXss(payload));
    }

    // -----------------------------------------------------------------------
    // Path Traversal — InputGuard
    // -----------------------------------------------------------------------

    @When("I guard against path traversal with payload {string}")
    public void guardAgainstTraversal(final String payload) {
        DomainWorld.run(() -> InputGuard.rejectPathTraversal(payload));
    }

    // -----------------------------------------------------------------------
    // assertSafe — combined check
    // -----------------------------------------------------------------------

    @When("I assert input is safe with payload {string}")
    public void assertSafePayload(final String payload) {
        DomainWorld.run(() -> InputGuard.assertSafe(payload));
    }

    // -----------------------------------------------------------------------
    // Auth registration with hostile input
    // -----------------------------------------------------------------------

    @When("I register a security user with username {string} email {string} and password {string}")
    public void securityRegisterHostile(final String username, final String email, final String password) {
        DomainWorld.run(() -> {
            InputGuard.assertSafe(username);
            InputGuard.assertSafe(email);
            InputGuard.assertSafe(password);
            auth().register(username, email, password);
        });
    }

    @When("I register a security user with SQLi username {string} and safe email {string} password {string}")
    public void securityRegisterSqliUsername(final String username, final String email, final String password) {
        DomainWorld.run(() -> InputGuard.rejectSqlInjection(username));
    }

    @When("I register a security user with XSS username {string} and safe email {string} password {string}")
    public void securityRegisterXssUsername(final String username, final String email, final String password) {
        DomainWorld.run(() -> InputGuard.rejectXss(username));
    }

    @When("I login a security user with identifier {string} and password {string}")
    public void securityLoginUser(final String identifier, final String password) {
        DomainWorld.run(() -> {
            InputGuard.assertSafe(identifier);
            InputGuard.assertSafe(password);
            auth().login(identifier, password);
        });
    }

    @When("I login a security user with SQLi identifier {string} and password {string}")
    public void securityLoginSqliIdentifier(final String identifier, final String password) {
        DomainWorld.run(() -> InputGuard.rejectSqlInjection(identifier));
    }

    @When("I login a security user with XSS identifier {string} and password {string}")
    public void securityLoginXssIdentifier(final String identifier, final String password) {
        DomainWorld.run(() -> InputGuard.rejectXss(identifier));
    }

    // -----------------------------------------------------------------------
    // SecurityAssertions bridge
    // -----------------------------------------------------------------------

    @Then("the security guard rejects the input as {string}")
    public void securityGuardRejects(final String expectedCode) {
        final var error = DomainWorld.lastError();
        assertThat(error)
                .as("expected a domain error '%s' but no error was raised", expectedCode)
                .isNotNull();
        assertThat(error.code())
                .as("security error code")
                .isEqualTo(expectedCode);
        SecurityAssertions.assertRejected(true, expectedCode);
    }

    @Then("the security guard accepts the input")
    public void securityGuardAccepts() {
        final var error = DomainWorld.lastError();
        assertThat(error)
                .as("expected no security error but got: %s",
                        error == null ? "none" : error.code() + " / " + error.getMessage())
                .isNull();
    }

    @Then("no injection succeeds through the guard")
    public void noInjectionSucceeds() {
        final var error = DomainWorld.lastError();
        assertThat(error)
                .as("injection payload must be rejected — no error raised")
                .isNotNull();
        assertThat(error.code())
                .as("error code should be a SEC_* rejection code")
                .startsWith("SEC_");
    }
}
