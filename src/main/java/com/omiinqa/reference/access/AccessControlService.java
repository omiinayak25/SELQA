package com.omiinqa.reference.access;

import com.omiinqa.reference.core.DomainException;
import com.omiinqa.reference.core.Validations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * In-memory role-based access-control service with real, asserted business rules.
 *
 * <p>State is per-instance so each BDD scenario gets an isolated world. Standard
 * roles are seeded at construction: {@code ADMIN} (wildcard), {@code MANAGER},
 * {@code CUSTOMER}, {@code GUEST}.</p>
 *
 * <p>Error codes (asserted by scenarios):
 * <ul>
 *   <li>{@code AC_DENIED}       — actor lacks the required permission</li>
 *   <li>{@code AC_ROLE_UNKNOWN} — referenced role does not exist</li>
 *   <li>{@code AC_USER_UNKNOWN} — referenced user does not exist</li>
 *   <li>{@code AC_ROLE_EXISTS}  — role name already defined</li>
 *   <li>{@code AC_BLANK}        — a required name argument is blank</li>
 * </ul>
 * </p>
 */
public class AccessControlService {

    // -------------------------------------------------------------------------
    // Standard role names
    // -------------------------------------------------------------------------
    public static final String ROLE_ADMIN    = "ADMIN";
    public static final String ROLE_MANAGER  = "MANAGER";
    public static final String ROLE_CUSTOMER = "CUSTOMER";
    public static final String ROLE_GUEST    = "GUEST";

    private final Map<String, Role>       roles       = new HashMap<>();
    private final Map<String, Set<String>> userRoles  = new HashMap<>();
    private final Set<String>             knownUsers  = new HashSet<>();

    public AccessControlService() {
        seedStandardRoles();
    }

    // -------------------------------------------------------------------------
    // Seeding
    // -------------------------------------------------------------------------

    private void seedStandardRoles() {
        defineRole(ROLE_ADMIN,
                Permission.ADMIN_ALL);
        defineRole(ROLE_MANAGER,
                Permission.ORDER_READ,
                Permission.ORDER_MANAGE,
                Permission.REPORT_VIEW,
                Permission.USER_READ);
        defineRole(ROLE_CUSTOMER,
                Permission.PROFILE_READ,
                Permission.PROFILE_UPDATE,
                Permission.ORDER_PLACE,
                Permission.ORDER_CANCEL,
                Permission.ORDER_READ,
                Permission.CATALOG_READ);
        defineRole(ROLE_GUEST,
                Permission.CATALOG_READ);
    }

    // -------------------------------------------------------------------------
    // Role management
    // -------------------------------------------------------------------------

    /**
     * Define a new named role with the supplied permissions.
     *
     * @throws DomainException {@code AC_BLANK}      if name is blank
     * @throws DomainException {@code AC_ROLE_EXISTS} if already defined
     */
    public Role defineRole(final String name, final String... permissions) {
        Validations.requireNotBlank(name, "role name", "AC_BLANK");
        if (roles.containsKey(name)) {
            throw new DomainException("AC_ROLE_EXISTS", "Role already defined: " + name);
        }
        final Set<String> perms = new LinkedHashSet<>(Arrays.asList(permissions));
        final Role role = Role.builder().name(name).permissions(perms).build();
        roles.put(name, role);
        return role;
    }

    // -------------------------------------------------------------------------
    // User registration (lightweight — just marks a user as known)
    // -------------------------------------------------------------------------

    /**
     * Register a user so the service recognises them.
     *
     * @throws DomainException {@code AC_BLANK} if username is blank
     */
    public void registerUser(final String username) {
        Validations.requireNotBlank(username, "username", "AC_BLANK");
        knownUsers.add(username);
    }

    // -------------------------------------------------------------------------
    // Role assignment / revocation
    // -------------------------------------------------------------------------

    /**
     * Assign a role to a user.
     *
     * @throws DomainException {@code AC_USER_UNKNOWN} if user not registered
     * @throws DomainException {@code AC_ROLE_UNKNOWN} if role not defined
     */
    public void assignRole(final String username, final String roleName) {
        requireKnownUser(username);
        requireKnownRole(roleName);
        userRoles.computeIfAbsent(username, k -> new HashSet<>()).add(roleName);
    }

    /**
     * Revoke a role from a user.
     *
     * @throws DomainException {@code AC_USER_UNKNOWN} if user not registered
     * @throws DomainException {@code AC_ROLE_UNKNOWN} if role not defined
     */
    public void revokeRole(final String username, final String roleName) {
        requireKnownUser(username);
        requireKnownRole(roleName);
        final Set<String> assigned = userRoles.getOrDefault(username, new HashSet<>());
        assigned.remove(roleName);
    }

    // -------------------------------------------------------------------------
    // Permission checks
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} when the user holds at least one role that grants
     * the permission.
     *
     * @throws DomainException {@code AC_USER_UNKNOWN} if user not registered
     */
    public boolean hasPermission(final String username, final String permission) {
        requireKnownUser(username);
        final Set<String> assigned = userRoles.getOrDefault(username, new HashSet<>());
        return assigned.stream()
                .map(roles::get)
                .filter(r -> r != null)
                .anyMatch(r -> r.grantsPermission(permission));
    }

    /**
     * Assert that the actor holds the permission; throw if not.
     *
     * @throws DomainException {@code AC_USER_UNKNOWN} if user not registered
     * @throws DomainException {@code AC_ROLE_UNKNOWN} if a referenced role is gone
     * @throws DomainException {@code AC_DENIED}       if no role grants the permission
     */
    public void require(final String username, final String permission) {
        if (!hasPermission(username, permission)) {
            throw new DomainException("AC_DENIED",
                    "User '" + username + "' lacks permission: " + permission);
        }
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    public boolean isRoleDefined(final String name) {
        return roles.containsKey(name);
    }

    public boolean isUserKnown(final String username) {
        return knownUsers.contains(username);
    }

    public Set<String> rolesOf(final String username) {
        requireKnownUser(username);
        return new HashSet<>(userRoles.getOrDefault(username, new HashSet<>()));
    }

    public Role getRole(final String name) {
        requireKnownRole(name);
        return roles.get(name);
    }

    // -------------------------------------------------------------------------
    // Internal guards
    // -------------------------------------------------------------------------

    private void requireKnownUser(final String username) {
        if (!knownUsers.contains(username)) {
            throw new DomainException("AC_USER_UNKNOWN",
                    "User not registered: " + username);
        }
    }

    private void requireKnownRole(final String roleName) {
        if (!roles.containsKey(roleName)) {
            throw new DomainException("AC_ROLE_UNKNOWN",
                    "Role not defined: " + roleName);
        }
    }
}
