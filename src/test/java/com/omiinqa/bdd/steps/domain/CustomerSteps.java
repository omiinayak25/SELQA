package com.omiinqa.bdd.steps.domain;

import com.omiinqa.bdd.support.DomainWorld;
import com.omiinqa.reference.access.AccessControlService;
import com.omiinqa.reference.access.CustomerService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the customer domain.
 *
 * <p>Scenarios verify that customers can only access their own resources, cannot
 * escalate privileges, and that cross-customer data access is rejected with
 * {@code AC_FORBIDDEN_RESOURCE}.</p>
 */
public class CustomerSteps {

    private static final String SVC      = "acl.service";
    private static final String CUST_SVC = "customer.service";

    private AccessControlService acl() {
        return DomainWorld.service(SVC, AccessControlService::new);
    }

    private CustomerService customerService() {
        return DomainWorld.service(CUST_SVC, () -> new CustomerService(acl()));
    }

    // -------------------------------------------------------------------------
    // Given
    // -------------------------------------------------------------------------

    @Given("a clean customer service")
    public void cleanCustomerService() {
        final AccessControlService acl = new AccessControlService();
        DomainWorld.put(SVC, acl);
        DomainWorld.put(CUST_SVC, new CustomerService(acl));
    }

    @Given("a registered customer {string} with email {string}")
    public void registeredCustomer(final String username, final String email) {
        customerService().registerCustomer(username, email);
    }

    @Given("customer {string} has placed an order for {string}")
    public void customerPlacedOrder(final String username, final String item) {
        final String orderId = customerService().placeOrder(username, item);
        DomainWorld.put("customer.lastOrder." + username, orderId);
    }

    @Given("a non-customer actor {string} with role {string}")
    public void nonCustomerActorWithRole(final String username, final String role) {
        acl().registerUser(username);
        acl().assignRole(username, role);
    }

    // -------------------------------------------------------------------------
    // When
    // -------------------------------------------------------------------------

    @When("customer {string} views their own profile")
    public void customerViewsOwnProfile(final String username) {
        DomainWorld.capture(() -> customerService().viewProfile(username, username));
    }

    @When("customer {string} attempts to view profile of {string}")
    public void customerAttemptsViewProfile(final String actor, final String target) {
        DomainWorld.run(() -> customerService().viewProfile(actor, target));
    }

    @When("customer {string} updates their display name to {string}")
    public void customerUpdatesDisplayName(final String username, final String newName) {
        DomainWorld.run(() -> customerService().updateProfile(username, username, newName));
    }

    @When("customer {string} attempts to update profile of {string} to {string}")
    public void customerAttemptsUpdateOtherProfile(final String actor, final String target,
                                                    final String newName) {
        DomainWorld.run(() -> customerService().updateProfile(actor, target, newName));
    }

    @When("customer {string} places an order for {string}")
    public void customerPlacesOrder(final String username, final String item) {
        DomainWorld.run(() -> {
            final String orderId = customerService().placeOrder(username, item);
            DomainWorld.put("customer.lastOrder." + username, orderId);
        });
    }

    @When("customer {string} views their orders")
    public void customerViewsOrders(final String username) {
        DomainWorld.capture(() -> customerService().viewOrders(username, username));
    }

    @When("customer {string} attempts to view orders of {string}")
    public void customerAttemptsViewOrdersOf(final String actor, final String target) {
        DomainWorld.run(() -> customerService().viewOrders(actor, target));
    }

    @When("customer {string} cancels their last order")
    public void customerCancelsLastOrder(final String username) {
        final String orderId = DomainWorld.get("customer.lastOrder." + username);
        DomainWorld.run(() -> customerService().cancelOrder(username, orderId));
    }

    @When("customer {string} attempts to cancel order {string}")
    public void customerAttemptsCancelOrder(final String username, final String orderId) {
        DomainWorld.run(() -> customerService().cancelOrder(username, orderId));
    }

    @When("actor {string} attempts to place an order for {string}")
    public void actorAttemptsPlaceOrder(final String actor, final String item) {
        DomainWorld.run(() -> customerService().placeOrder(actor, item));
    }

    // -------------------------------------------------------------------------
    // Then
    // -------------------------------------------------------------------------

    @Then("customer {string} exists in the system")
    public void customerExistsInSystem(final String username) {
        assertThat(customerService().customerExists(username))
                .as("customer %s should exist", username)
                .isTrue();
    }

    @Then("customer {string} has {int} orders")
    public void customerHasOrders(final String username, final int count) {
        assertThat(customerService().orderCount(username))
                .as("order count for %s", username)
                .isEqualTo(count);
    }

    @Then("customer {string} profile display name is {string}")
    public void customerProfileDisplayName(final String username, final String expected) {
        final CustomerService.CustomerProfile p =
                customerService().viewProfile(username, username);
        assertThat(p.getDisplayName())
                .as("display name of %s", username)
                .isEqualTo(expected);
    }
}
