package com.omiinqa.reference.identity;

import lombok.Builder;
import lombok.Data;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A user account in the reference identity domain. Holds credentials, lifecycle
 * status, role assignments and the failed-login counter that drives lockout.
 */
@Data
@Builder
public class Account {

    public enum Status { PENDING, ACTIVE, LOCKED, DISABLED }

    private long id;
    private String username;
    private String email;
    private String password;
    private boolean emailVerified;
    private boolean mfaEnabled;

    @Builder.Default
    private Status status = Status.ACTIVE;

    @Builder.Default
    private int failedLoginAttempts = 0;

    @Builder.Default
    private Set<String> roles = new LinkedHashSet<>();

    public boolean hasRole(final String role) {
        return roles.contains(role);
    }
}
