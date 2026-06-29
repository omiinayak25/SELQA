package com.omiinqa.bdd.steps.domain;

import com.omiinqa.bdd.support.DomainWorld;
import com.omiinqa.reference.platform.AuditService;
import com.omiinqa.reference.platform.AuditService.AuditEntry;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the reference platform / audit-logs domain.
 *
 * <p>Drives the real {@link AuditService} (in-memory, append-only audit log) —
 * no external storage, no browser. Outcomes are recorded via {@link DomainWorld}
 * so shared assertions from {@code CommonDomainSteps} ("the operation succeeds",
 * "a domain error X is raised") work unchanged.</p>
 *
 * <p>All step text is prefixed with the noun <em>audit</em> / <em>Audit</em> to
 * guarantee global uniqueness across the full Cucumber step registry.</p>
 *
 * <p>Domain behaviour covered:</p>
 * <ul>
 *   <li>{@code AUDIT_BLANK} — any blank parameter to {@code record()}</li>
 *   <li>{@code AUDIT_IMMUTABLE} — delete attempt on an append-only log</li>
 *   <li>Query correctness: by actor, action, entity, time range</li>
 *   <li>Ordering: results are newest first</li>
 *   <li>Boundary: empty log, non-existent actor</li>
 * </ul>
 */
public class AuditSteps {

    private static final String SVC = "auditService";
    private static final String AUDIT_RESULTS = "audit.queryResults";

    private AuditService service() {
        return DomainWorld.service(SVC, AuditService::new);
    }

    // ─── Background ──────────────────────────────────────────────────────────

    /**
     * Resets the audit service to a clean, empty state at the start of each
     * scenario. Called from the feature Background.
     */
    @Given("a clean audit service")
    public void cleanAuditService() {
        DomainWorld.put(SVC, new AuditService());
    }

    // ─── Given — precondition records ────────────────────────────────────────

    /**
     * Records an audit event as a precondition (Given), bypassing
     * {@link DomainWorld#run} so that any unexpected validation error causes the
     * scenario to fail fast rather than silently swallowing it.
     */
    @Given("I record an audit event actor {string} action {string} entity {string} id {string}")
    public void givenAuditEvent(final String actor, final String action,
                                final String entity, final String entityId) {
        service().record(actor, action, entity, entityId);
    }

    // ─── When — mutating operations ──────────────────────────────────────────

    /**
     * Records an audit event as a When step, capturing any {@link com.omiinqa.reference.core.DomainException}
     * so that negative-path assertions in CommonDomainSteps can verify the error code.
     */
    @When("I audit record actor {string} action {string} entity {string} id {string}")
    public void auditRecord(final String actor, final String action,
                            final String entity, final String entityId) {
        DomainWorld.run(() -> service().record(actor, action, entity, entityId));
    }

    /**
     * Attempts to delete an audit record. The service is append-only; this must
     * always raise {@code AUDIT_IMMUTABLE}.
     */
    @When("I attempt to delete an audit record")
    public void auditAttemptDelete() {
        DomainWorld.run(() -> service().delete(1L));
    }

    // ─── When — query operations ─────────────────────────────────────────────

    /**
     * Queries the audit log by actor and stores the result list for subsequent
     * Then assertions. Any domain error is captured via {@link DomainWorld#capture}.
     */
    @When("I audit query by actor {string}")
    public void auditQueryByActor(final String actor) {
        final List<AuditEntry> results = DomainWorld.capture(() -> service().queryByActor(actor));
        DomainWorld.put(AUDIT_RESULTS, results);
    }

    /**
     * Queries the audit log by action verb and stores the result list.
     */
    @When("I audit query by action {string}")
    public void auditQueryByAction(final String action) {
        final List<AuditEntry> results = DomainWorld.capture(() -> service().queryByAction(action));
        DomainWorld.put(AUDIT_RESULTS, results);
    }

    /**
     * Queries the audit log by entity type and stores the result list.
     */
    @When("I audit query by entity {string}")
    public void auditQueryByEntity(final String entity) {
        final List<AuditEntry> results = DomainWorld.capture(() -> service().queryByEntity(entity));
        DomainWorld.put(AUDIT_RESULTS, results);
    }

    /**
     * Queries the audit log for events within the supplied inclusive ISO-8601
     * time range and stores the result list.
     *
     * @param fromIso ISO-8601 instant string for the lower bound (inclusive)
     * @param toIso   ISO-8601 instant string for the upper bound (inclusive)
     */
    @When("I audit query by time range {string} to {string}")
    public void auditQueryByTimeRange(final String fromIso, final String toIso) {
        final Instant from = Instant.parse(fromIso);
        final Instant to = Instant.parse(toIso);
        final List<AuditEntry> results = DomainWorld.capture(
                () -> service().queryByTimeRange(from, to));
        DomainWorld.put(AUDIT_RESULTS, results);
    }

    // ─── Then — result-list assertions ───────────────────────────────────────

    /**
     * Asserts that the last query returned exactly {@code expected} entries.
     */
    @Then("the audit result count is {int}")
    public void auditResultCount(final int expected) {
        final List<AuditEntry> results = DomainWorld.get(AUDIT_RESULTS);
        assertThat(results)
                .as("audit query result count")
                .isNotNull()
                .hasSize(expected);
    }

    /**
     * Asserts that the service's total record count equals {@code expected}.
     */
    @Then("the audit total count is {long}")
    public void auditTotalCount(final long expected) {
        assertThat(service().count())
                .as("audit total count")
                .isEqualTo(expected);
    }

    /**
     * Asserts that the last query result list is empty.
     */
    @Then("the audit result is empty")
    public void auditResultIsEmpty() {
        final List<AuditEntry> results = DomainWorld.get(AUDIT_RESULTS);
        assertThat(results)
                .as("audit query result should be empty")
                .isNotNull()
                .isEmpty();
    }

    /**
     * Asserts the actor on the first (newest) entry in the last query result.
     */
    @Then("the first audit result has actor {string}")
    public void firstAuditResultHasActor(final String expectedActor) {
        final List<AuditEntry> results = DomainWorld.get(AUDIT_RESULTS);
        assertThat(results)
                .as("audit query result must not be empty")
                .isNotNull()
                .isNotEmpty();
        assertThat(results.get(0).getActor())
                .as("actor of first audit result")
                .isEqualTo(expectedActor);
    }

    /**
     * Asserts the action verb on the first (newest) entry in the last query result.
     */
    @Then("the first audit result has action {string}")
    public void firstAuditResultHasAction(final String expectedAction) {
        final List<AuditEntry> results = DomainWorld.get(AUDIT_RESULTS);
        assertThat(results)
                .as("audit query result must not be empty")
                .isNotNull()
                .isNotEmpty();
        assertThat(results.get(0).getAction())
                .as("action of first audit result")
                .isEqualTo(expectedAction);
    }

    // ─── Then — latestFor assertions ─────────────────────────────────────────

    /**
     * Asserts that {@link AuditService#latestFor(String)} returns a present
     * entry whose action matches {@code expectedAction}.
     */
    @Then("the audit latest entity for {string} has action {string}")
    public void auditLatestEntityHasAction(final String entity, final String expectedAction) {
        final Optional<AuditEntry> latest = service().latestFor(entity);
        assertThat(latest)
                .as("expected a latest audit entry for entity '%s' but found none", entity)
                .isPresent();
        assertThat(latest.get().getAction())
                .as("action of latest audit entry for entity '%s'", entity)
                .isEqualTo(expectedAction);
    }
}
