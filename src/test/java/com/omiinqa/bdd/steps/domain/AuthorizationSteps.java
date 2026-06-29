package com.omiinqa.bdd.steps.domain;

import com.omiinqa.bdd.support.DomainWorld;
import com.omiinqa.reference.access.AccessControlService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;

/**
 * Step definitions for the authorization domain (permission enforcement via
 * {@link AccessControlService#require}).
 *
 * <p>Reuses the shared {@link AccessControlService} keyed under the same world
 * slot as {@link RbacSteps}, so both step classes can cooperate in the same
 * scenario. Outcome assertions are delegated to {@code CommonDomainSteps}.</p>
 */
public class AuthorizationSteps {

    private static final String SVC = "acl.service";

    private AccessControlService service() {
        return DomainWorld.service(SVC, AccessControlService::new);
    }

    // -------------------------------------------------------------------------
    // Given
    // -------------------------------------------------------------------------

    @Given("a clean authorization service")
    public void cleanAuthorizationService() {
        DomainWorld.put(SVC, new AccessControlService());
    }

    @Given("principal {string} has been registered with role {string}")
    public void principalRegisteredWithRole(final String username, final String role) {
        service().registerUser(username);
        service().assignRole(username, role);
    }

    // -------------------------------------------------------------------------
    // When
    // -------------------------------------------------------------------------

    @When("I require permission {string} for principal {string}")
    public void requirePermissionForPrincipal(final String permission, final String username) {
        DomainWorld.run(() -> service().require(username, permission));
    }

    @When("I require permission {string} for unknown principal {string}")
    public void requirePermissionForUnknownPrincipal(final String permission,
                                                       final String username) {
        DomainWorld.run(() -> service().require(username, permission));
    }
}
