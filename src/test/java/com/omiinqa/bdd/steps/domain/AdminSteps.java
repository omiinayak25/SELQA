package com.omiinqa.bdd.steps.domain;

import com.omiinqa.bdd.support.DomainWorld;
import com.omiinqa.reference.access.AccessControlService;
import com.omiinqa.reference.access.AdminService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the admin domain.
 *
 * <p>Each scenario gets a fresh {@link AdminService} backed by a shared
 * {@link AccessControlService}. Admin operations are gated by permission checks
 * inside the service; non-admin actors receive {@code AC_DENIED}.</p>
 */
public class AdminSteps {

    private static final String SVC     = "acl.service";
    private static final String ADM_SVC = "admin.service";

    private AccessControlService acl() {
        return DomainWorld.service(SVC, AccessControlService::new);
    }

    private AdminService adminService() {
        return DomainWorld.service(ADM_SVC, () -> new AdminService(acl()));
    }

    // -------------------------------------------------------------------------
    // Given
    // -------------------------------------------------------------------------

    @Given("a clean admin service")
    public void cleanAdminService() {
        final AccessControlService acl = new AccessControlService();
        DomainWorld.put(SVC, acl);
        DomainWorld.put(ADM_SVC, new AdminService(acl));
    }

    @Given("an admin actor {string}")
    public void adminActor(final String username) {
        acl().registerUser(username);
        acl().assignRole(username, AccessControlService.ROLE_ADMIN);
    }

    @Given("a non-admin actor {string} with role {string}")
    public void nonAdminActor(final String username, final String role) {
        acl().registerUser(username);
        acl().assignRole(username, role);
    }

    @Given("a managed user {string} created by admin {string}")
    public void managedUserCreated(final String target, final String admin) {
        adminService().createUser(admin, target);
    }

    // -------------------------------------------------------------------------
    // When
    // -------------------------------------------------------------------------

    @When("admin {string} creates user {string}")
    public void adminCreatesUser(final String actor, final String username) {
        DomainWorld.run(() -> adminService().createUser(actor, username));
    }

    @When("admin {string} disables user {string}")
    public void adminDisablesUser(final String actor, final String username) {
        DomainWorld.run(() -> adminService().disableUser(actor, username));
    }

    @When("admin {string} assigns role {string} to user {string}")
    public void adminAssignsRole(final String actor, final String role, final String username) {
        DomainWorld.run(() -> adminService().assignRole(actor, username, role));
    }

    @When("actor {string} attempts to create user {string}")
    public void actorAttemptsCreateUser(final String actor, final String username) {
        DomainWorld.run(() -> adminService().createUser(actor, username));
    }

    @When("actor {string} attempts to disable user {string}")
    public void actorAttemptsDisableUser(final String actor, final String username) {
        DomainWorld.run(() -> adminService().disableUser(actor, username));
    }

    @When("actor {string} attempts to view audit config")
    public void actorAttemptsViewAuditConfig(final String actor) {
        DomainWorld.capture(() -> adminService().viewAuditConfig(actor));
    }

    @When("actor {string} attempts to assign role {string} to user {string}")
    public void actorAttemptsAssignRole(final String actor, final String role,
                                         final String username) {
        DomainWorld.run(() -> adminService().assignRole(actor, username, role));
    }

    // -------------------------------------------------------------------------
    // Then
    // -------------------------------------------------------------------------

    @Then("managed user {string} exists")
    public void managedUserExists(final String username) {
        assertThat(adminService().userExists(username))
                .as("managed user %s should exist", username)
                .isTrue();
    }

    @Then("managed user {string} is disabled")
    public void managedUserIsDisabled(final String username) {
        assertThat(adminService().isDisabled(username))
                .as("user %s should be disabled", username)
                .isTrue();
    }

    @Then("managed user {string} is not disabled")
    public void managedUserIsNotDisabled(final String username) {
        assertThat(adminService().isDisabled(username))
                .as("user %s should not be disabled", username)
                .isFalse();
    }

    @Then("the audit log contains {int} entries")
    public void auditLogContainsEntries(final int count) {
        assertThat(adminService().auditLog()).hasSize(count);
    }

    @Then("the audit log is not empty")
    public void auditLogIsNotEmpty() {
        assertThat(adminService().auditLog()).isNotEmpty();
    }

    @Then("the audit log entry {int} contains {string}")
    public void auditLogEntryContains(final int index, final String fragment) {
        final List<String> log = adminService().auditLog();
        assertThat(log).hasSizeGreaterThan(index - 1);
        assertThat(log.get(index - 1)).containsIgnoringCase(fragment);
    }
}
