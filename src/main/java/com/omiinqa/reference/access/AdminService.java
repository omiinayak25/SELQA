package com.omiinqa.reference.access;

import com.omiinqa.reference.core.DomainException;
import com.omiinqa.reference.core.Validations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin-only operations gated by {@link AccessControlService}.
 *
 * <p>Every mutating operation calls {@code AccessControlService.require(actor, "admin:*")}
 * first — a non-admin actor immediately receives {@code AC_DENIED}.</p>
 *
 * <p>Additional error codes:
 * <ul>
 *   <li>{@code ADMIN_USER_EXISTS}    — createUser called for existing username</li>
 *   <li>{@code ADMIN_USER_NOT_FOUND} — operation targets unknown managed user</li>
 *   <li>{@code ADMIN_BLANK}          — a required argument is blank</li>
 * </ul>
 * </p>
 */
public class AdminService {

    private final AccessControlService acl;
    private final Map<String, ManagedUser> users = new HashMap<>();
    private final List<String>             auditLog = new ArrayList<>();

    public AdminService(final AccessControlService acl) {
        this.acl = acl;
    }

    // -------------------------------------------------------------------------
    // Admin operations
    // -------------------------------------------------------------------------

    /**
     * Create a managed user record.
     *
     * @throws DomainException {@code AC_DENIED}         if actor is not admin
     * @throws DomainException {@code ADMIN_USER_EXISTS}  if username already managed
     * @throws DomainException {@code ADMIN_BLANK}        if username is blank
     */
    public ManagedUser createUser(final String actor, final String username) {
        acl.require(actor, Permission.ADMIN_ALL);
        Validations.requireNotBlank(username, "username", "ADMIN_BLANK");
        if (users.containsKey(username)) {
            throw new DomainException("ADMIN_USER_EXISTS", "User already exists: " + username);
        }
        acl.registerUser(username);
        final ManagedUser u = new ManagedUser(username);
        users.put(username, u);
        audit(actor, "CREATE_USER:" + username);
        return u;
    }

    /**
     * Disable a managed user.
     *
     * @throws DomainException {@code AC_DENIED}           if actor is not admin
     * @throws DomainException {@code ADMIN_USER_NOT_FOUND} if target user not found
     */
    public void disableUser(final String actor, final String username) {
        acl.require(actor, Permission.ADMIN_ALL);
        final ManagedUser u = requireManagedUser(username);
        u.setDisabled(true);
        audit(actor, "DISABLE_USER:" + username);
    }

    /**
     * Assign a role to a managed user.
     *
     * @throws DomainException {@code AC_DENIED}           if actor is not admin
     * @throws DomainException {@code ADMIN_USER_NOT_FOUND} if target user not found
     * @throws DomainException {@code AC_ROLE_UNKNOWN}     if role not defined
     */
    public void assignRole(final String actor, final String username, final String roleName) {
        acl.require(actor, Permission.ADMIN_ALL);
        requireManagedUser(username);
        acl.assignRole(username, roleName);
        audit(actor, "ASSIGN_ROLE:" + roleName + "=>" + username);
    }

    /**
     * View the audit configuration — admin-only read operation.
     *
     * @throws DomainException {@code AC_DENIED} if actor is not admin
     * @return unmodifiable view of the audit log
     */
    public List<String> viewAuditConfig(final String actor) {
        acl.require(actor, Permission.ADMIN_ALL);
        return Collections.unmodifiableList(auditLog);
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    public boolean userExists(final String username) {
        return users.containsKey(username);
    }

    public boolean isDisabled(final String username) {
        final ManagedUser u = users.get(username);
        return u != null && u.isDisabled();
    }

    public List<String> auditLog() {
        return Collections.unmodifiableList(auditLog);
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private ManagedUser requireManagedUser(final String username) {
        final ManagedUser u = users.get(username);
        if (u == null) {
            throw new DomainException("ADMIN_USER_NOT_FOUND",
                    "Managed user not found: " + username);
        }
        return u;
    }

    private void audit(final String actor, final String event) {
        auditLog.add("[" + actor + "] " + event);
    }

    // -------------------------------------------------------------------------
    // Inner type
    // -------------------------------------------------------------------------

    /** Lightweight managed-user record tracked by AdminService. */
    public static class ManagedUser {
        private final String username;
        private boolean disabled;

        public ManagedUser(final String username) {
            this.username = username;
        }

        public String getUsername() { return username; }
        public boolean isDisabled() { return disabled; }
        public void setDisabled(final boolean disabled) { this.disabled = disabled; }
    }
}
