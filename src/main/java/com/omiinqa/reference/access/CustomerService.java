package com.omiinqa.reference.access;

import com.omiinqa.reference.core.DomainException;
import com.omiinqa.reference.core.Validations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Customer-scoped operations that enforce two key boundaries:
 * <ol>
 *   <li>A customer may only read <em>their own</em> profile and orders; accessing
 *       another customer's data raises {@code AC_FORBIDDEN_RESOURCE}.</li>
 *   <li>Elevated operations (admin/manager actions) raise {@code AC_DENIED} for
 *       any actor without the required permission.</li>
 * </ol>
 *
 * <p>Error codes (asserted by scenarios):
 * <ul>
 *   <li>{@code CUST_NOT_FOUND}        — target customer unknown to this service</li>
 *   <li>{@code CUST_ORDER_NOT_FOUND}  — order not found for that customer</li>
 *   <li>{@code AC_FORBIDDEN_RESOURCE} — cross-customer data access attempt</li>
 *   <li>{@code AC_DENIED}             — insufficient permissions for operation</li>
 * </ul>
 * </p>
 */
public class CustomerService {

    private final AccessControlService acl;
    private final Map<String, CustomerProfile> profiles = new HashMap<>();
    private final Map<String, List<String>>    orders   = new HashMap<>();

    public CustomerService(final AccessControlService acl) {
        this.acl = acl;
    }

    // -------------------------------------------------------------------------
    // Profile management
    // -------------------------------------------------------------------------

    /**
     * Register a customer profile (called during test setup). Automatically
     * registers the user with the ACL and assigns the CUSTOMER role.
     *
     * @throws DomainException {@code AC_BLANK} if username is blank
     */
    public CustomerProfile registerCustomer(final String username, final String email) {
        Validations.requireNotBlank(username, "username", "AC_BLANK");
        acl.registerUser(username);
        acl.assignRole(username, AccessControlService.ROLE_CUSTOMER);
        final CustomerProfile p = new CustomerProfile(username, email);
        profiles.put(username, p);
        orders.put(username, new ArrayList<>());
        return p;
    }

    /**
     * View own profile — restricted to the owning customer.
     *
     * @throws DomainException {@code AC_DENIED}             if actor lacks profile:read
     * @throws DomainException {@code CUST_NOT_FOUND}        if actor unknown in this service
     * @throws DomainException {@code AC_FORBIDDEN_RESOURCE} if actor != targetUsername
     */
    public CustomerProfile viewProfile(final String actor, final String targetUsername) {
        acl.require(actor, Permission.PROFILE_READ);
        requireCustomer(actor);
        if (!actor.equals(targetUsername)) {
            throw new DomainException("AC_FORBIDDEN_RESOURCE",
                    "Customer '" + actor + "' may not view profile of '" + targetUsername + "'");
        }
        return profiles.get(actor);
    }

    /**
     * Update own profile display name.
     *
     * @throws DomainException {@code AC_DENIED}             if actor lacks profile:update
     * @throws DomainException {@code CUST_NOT_FOUND}        if actor unknown in this service
     * @throws DomainException {@code AC_FORBIDDEN_RESOURCE} if actor != targetUsername
     * @throws DomainException {@code AC_BLANK}              if newDisplayName is blank
     */
    public void updateProfile(final String actor, final String targetUsername,
                               final String newDisplayName) {
        acl.require(actor, Permission.PROFILE_UPDATE);
        requireCustomer(actor);
        if (!actor.equals(targetUsername)) {
            throw new DomainException("AC_FORBIDDEN_RESOURCE",
                    "Customer '" + actor + "' may not update profile of '" + targetUsername + "'");
        }
        Validations.requireNotBlank(newDisplayName, "displayName", "AC_BLANK");
        profiles.get(actor).setDisplayName(newDisplayName);
    }

    // -------------------------------------------------------------------------
    // Order management
    // -------------------------------------------------------------------------

    /**
     * Place an order for the acting customer.
     *
     * @throws DomainException {@code AC_DENIED}      if actor lacks order:place
     * @throws DomainException {@code CUST_NOT_FOUND} if actor not registered as customer
     * @throws DomainException {@code AC_BLANK}       if item is blank
     * @return the generated order ID
     */
    public String placeOrder(final String actor, final String item) {
        acl.require(actor, Permission.ORDER_PLACE);
        requireCustomer(actor);
        Validations.requireNotBlank(item, "item", "AC_BLANK");
        final String orderId = "ORD-" + actor.toUpperCase() + "-" + (orders.get(actor).size() + 1);
        orders.get(actor).add(orderId);
        return orderId;
    }

    /**
     * View orders belonging to a customer — restricted to the owning customer.
     *
     * @throws DomainException {@code AC_DENIED}             if actor lacks order:read
     * @throws DomainException {@code CUST_NOT_FOUND}        if actor unknown in this service
     * @throws DomainException {@code AC_FORBIDDEN_RESOURCE} if actor != targetUsername
     */
    public List<String> viewOrders(final String actor, final String targetUsername) {
        acl.require(actor, Permission.ORDER_READ);
        requireCustomer(actor);
        if (!actor.equals(targetUsername)) {
            throw new DomainException("AC_FORBIDDEN_RESOURCE",
                    "Customer '" + actor + "' may not view orders of '" + targetUsername + "'");
        }
        return Collections.unmodifiableList(orders.get(actor));
    }

    /**
     * Cancel an order — actor must own the order.
     *
     * @throws DomainException {@code AC_DENIED}           if actor lacks order:cancel
     * @throws DomainException {@code CUST_NOT_FOUND}      if actor unknown in this service
     * @throws DomainException {@code CUST_ORDER_NOT_FOUND} if order not in actor's list
     */
    public void cancelOrder(final String actor, final String orderId) {
        acl.require(actor, Permission.ORDER_CANCEL);
        requireCustomer(actor);
        final List<String> myOrders = orders.get(actor);
        if (!myOrders.remove(orderId)) {
            throw new DomainException("CUST_ORDER_NOT_FOUND",
                    "Order '" + orderId + "' not found for customer '" + actor + "'");
        }
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /** Returns {@code true} when a customer profile has been registered. */
    public boolean customerExists(final String username) {
        return profiles.containsKey(username);
    }

    /** Returns the current order count for the named customer, or 0 if unknown. */
    public int orderCount(final String username) {
        final List<String> o = orders.get(username);
        return o == null ? 0 : o.size();
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private void requireCustomer(final String username) {
        if (!profiles.containsKey(username)) {
            throw new DomainException("CUST_NOT_FOUND",
                    "Customer profile not found: " + username);
        }
    }

    // -------------------------------------------------------------------------
    // Inner type
    // -------------------------------------------------------------------------

    /** Lightweight customer profile tracked by CustomerService. */
    public static class CustomerProfile {
        private final String username;
        private final String email;
        private String displayName;

        public CustomerProfile(final String username, final String email) {
            this.username = username;
            this.email = email;
            this.displayName = username;
        }

        public String getUsername()    { return username; }
        public String getEmail()       { return email; }
        public String getDisplayName() { return displayName; }

        public void setDisplayName(final String displayName) {
            this.displayName = displayName;
        }
    }
}
