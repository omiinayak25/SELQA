package com.omiinqa.bdd.steps.domain;

import com.omiinqa.bdd.support.DomainWorld;
import com.omiinqa.reference.platform.EmailService;
import com.omiinqa.reference.platform.EmailService.BulkResult;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.datatable.DataTable;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the reference platform / email domain.
 *
 * <p>Drives the real {@link EmailService} (in-memory outbox) — no external SMTP,
 * no browser. Outcomes are recorded via {@link DomainWorld} so shared assertions
 * from {@code CommonDomainSteps} work unchanged. All step text is prefixed with
 * "email" to avoid Cucumber ambiguity with other domain steps.</p>
 *
 * <p>Domain-specific behaviour asserted here:</p>
 * <ul>
 *   <li>send() validation: EMAIL_BAD_RECIPIENT, EMAIL_BLANK_SUBJECT, EMAIL_BODY_TOO_LONG</li>
 *   <li>Outbox size, lastTo(), findByRecipient()</li>
 *   <li>bulkSend() partial-failure report: successCount, failureCount, error list</li>
 *   <li>clear() resets outbox to zero</li>
 *   <li>Boundary: body at exactly MAX_BODY_LENGTH (5000) passes; 5001 fails</li>
 * </ul>
 */
public class EmailSteps {

    private static final String SVC = "emailService";
    private static final String BULK_RESULT = "email.bulkResult";

    private EmailService service() {
        return DomainWorld.service(SVC, EmailService::new);
    }

    // ─── Background ──────────────────────────────────────────────────────────

    @Given("a clean email service")
    public void cleanEmailService() {
        DomainWorld.put(SVC, new EmailService());
    }

    // ─── When — send ─────────────────────────────────────────────────────────

    @When("I send an email to {string} with subject {string} and body {string}")
    public void sendEmail(final String to, final String subject, final String body) {
        DomainWorld.run(() -> service().send(to, subject, body));
    }

    /**
     * Sends an email whose body is a string of exactly {@code length} 'x' characters.
     * Used by boundary scenarios to assert behaviour at or around MAX_BODY_LENGTH.
     */
    @When("I send an email to {string} with subject {string} and body of length {int}")
    public void sendEmailWithBodyOfLength(final String to, final String subject, final int length) {
        final String body = "x".repeat(length);
        DomainWorld.run(() -> service().send(to, subject, body));
    }

    // ─── When — bulk send ────────────────────────────────────────────────────

    /**
     * Bulk-sends an email to a list of recipients supplied as a single-column DataTable.
     * Stores the {@link BulkResult} for subsequent assertions.
     */
    @When("I bulk send an email with subject {string} and body {string} to recipients:")
    public void bulkSend(final String subject, final String body, final DataTable table) {
        final List<String> recipients = table.asList();
        DomainWorld.run(() -> {
            final BulkResult result = service().bulkSend(recipients, subject, body);
            DomainWorld.put(BULK_RESULT, result);
        });
    }

    // ─── When — utility ──────────────────────────────────────────────────────

    @When("I clear the email outbox")
    public void clearEmailOutbox() {
        service().clear();
    }

    // ─── Then — outbox ───────────────────────────────────────────────────────

    @Then("the email outbox size is {int}")
    public void emailOutboxSize(final int expected) {
        assertThat(service().outboxSize())
                .as("email outbox size")
                .isEqualTo(expected);
    }

    @Then("the email last recipient is {string}")
    public void emailLastRecipient(final String expected) {
        assertThat(service().lastTo())
                .as("email last recipient")
                .isEqualTo(expected);
    }

    @Then("the email last recipient is null")
    public void emailLastRecipientIsNull() {
        assertThat(service().lastTo())
                .as("email last recipient when outbox is empty")
                .isNull();
    }

    @Then("finding emails by recipient {string} returns {int} email")
    public void findEmailsByRecipientSingular(final String to, final int expected) {
        assertThat(service().findByRecipient(to))
                .as("emails found for recipient '%s'", to)
                .hasSize(expected);
    }

    @Then("finding emails by recipient {string} returns {int} emails")
    public void findEmailsByRecipient(final String to, final int expected) {
        findEmailsByRecipientSingular(to, expected);
    }

    // ─── Then — bulk result ───────────────────────────────────────────────────

    @Then("the bulk send success count is {int}")
    public void bulkSendSuccessCount(final int expected) {
        final BulkResult result = DomainWorld.get(BULK_RESULT);
        assertThat(result)
                .as("bulk result must be present — was bulkSend called?")
                .isNotNull();
        assertThat(result.getSuccessCount())
                .as("bulk send success count")
                .isEqualTo(expected);
    }

    @Then("the bulk send failure count is {int}")
    public void bulkSendFailureCount(final int expected) {
        final BulkResult result = DomainWorld.get(BULK_RESULT);
        assertThat(result)
                .as("bulk result must be present — was bulkSend called?")
                .isNotNull();
        assertThat(result.getFailureCount())
                .as("bulk send failure count")
                .isEqualTo(expected);
    }

    @Then("the bulk send error list is not empty")
    public void bulkSendErrorListNotEmpty() {
        final BulkResult result = DomainWorld.get(BULK_RESULT);
        assertThat(result)
                .as("bulk result must be present — was bulkSend called?")
                .isNotNull();
        assertThat(result.getErrors())
                .as("bulk send error list")
                .isNotEmpty();
    }
}
