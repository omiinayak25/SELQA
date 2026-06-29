package com.omiinqa.bdd.steps.domain;

import com.omiinqa.bdd.support.DomainWorld;
import com.omiinqa.reference.access.AccessControlService;
import com.omiinqa.reference.access.Role;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the role-based-access-control (RBAC) domain.
 *
 * <p>Steps drive the real {@link AccessControlService}. Outcomes are recorded via
 * {@link DomainWorld} so shared assertions in {@code CommonDomainSteps} apply.
 * All step text is prefixed with domain nouns to avoid Cucumber ambiguity.</p>
 */
public class RbacSteps {

    private static final String SVC = "acl.service";

    private AccessControlService service() {
        return DomainWorld.service(SVC, AccessControlService::new);
    }

    // -------------------------------------------------------------------------
    // Given — setup
    // -------------------------------------------------------------------------

    @Given("a clean access-control service")
    public void cleanAclService() {
        DomainWorld.put(SVC, new AccessControlService());
    }

    @Given("a registered principal {string}")
    public void registeredPrincipal(final String username) {
        service().registerUser(username);
    }

    @Given("principal {string} is assigned role {string}")
    public void principalAssignedRole(final String username, final String role) {
        service().assignRole(username, role);
    }

    // -------------------------------------------------------------------------
    // When — mutating actions
    // -------------------------------------------------------------------------

    @When("I define a role {string} with permissions {string}")
    public void defineRoleWithPermissions(final String roleName, final String permsCsv) {
        DomainWorld.run(() -> {
            final String[] perms = permsCsv.split(",\\s*");
            service().defineRole(roleName, perms);
        });
    }

    @When("I register principal {string}")
    public void registerPrincipal(final String username) {
        DomainWorld.run(() -> service().registerUser(username));
    }

    @When("I assign role {string} to principal {string}")
    public void assignRoleToPrincipal(final String role, final String username) {
        DomainWorld.run(() -> service().assignRole(username, role));
    }

    @When("I revoke role {string} from principal {string}")
    public void revokeRoleFromPrincipal(final String role, final String username) {
        DomainWorld.run(() -> service().revokeRole(username, role));
    }

    @When("I check permission {string} for principal {string}")
    public void checkPermissionForPrincipal(final String permission, final String username) {
        DomainWorld.capture(() -> service().hasPermission(username, permission));
    }

    // -------------------------------------------------------------------------
    // Then — assertions
    // -------------------------------------------------------------------------

    @Then("user {string} has permission {string}")
    public void userHasPermission(final String username, final String permission) {
        assertThat(service().hasPermission(username, permission))
                .as("expected %s to have permission %s", username, permission)
                .isTrue();
    }

    @Then("user {string} is denied permission {string}")
    public void userIsDeniedPermission(final String username, final String permission) {
        assertThat(service().hasPermission(username, permission))
                .as("expected %s to be denied permission %s", username, permission)
                .isFalse();
    }

    @Then("role {string} is defined")
    public void roleIsDefined(final String roleName) {
        assertThat(service().isRoleDefined(roleName))
                .as("role %s should be defined", roleName)
                .isTrue();
    }

    @Then("principal {string} has role {string}")
    public void principalHasRole(final String username, final String role) {
        final Set<String> roles = service().rolesOf(username);
        assertThat(roles).as("roles of %s", username).contains(role);
    }

    @Then("principal {string} does not have role {string}")
    public void principalDoesNotHaveRole(final String username, final String role) {
        final Set<String> roles = service().rolesOf(username);
        assertThat(roles).as("roles of %s", username).doesNotContain(role);
    }

    @Then("the standard roles ADMIN, MANAGER, CUSTOMER, GUEST are seeded")
    public void standardRolesSeeded() {
        assertThat(service().isRoleDefined("ADMIN")).isTrue();
        assertThat(service().isRoleDefined("MANAGER")).isTrue();
        assertThat(service().isRoleDefined("CUSTOMER")).isTrue();
        assertThat(service().isRoleDefined("GUEST")).isTrue();
    }

    @Then("role {string} grants permission {string}")
    public void roleGrantsPermission(final String roleName, final String permission) {
        final Role role = service().getRole(roleName);
        assertThat(role.grantsPermission(permission))
                .as("role %s should grant %s", roleName, permission)
                .isTrue();
    }

    @Then("role {string} does not grant permission {string}")
    public void roleDoesNotGrantPermission(final String roleName, final String permission) {
        final Role role = service().getRole(roleName);
        assertThat(role.grantsPermission(permission))
                .as("role %s should not grant %s", roleName, permission)
                .isFalse();
    }
}
