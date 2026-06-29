package com.omiinqa.reference.access;

import lombok.Builder;
import lombok.Data;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A named role carrying a set of fine-grained permission strings.
 *
 * <p>Permissions follow the pattern {@code resource:action}. The wildcard
 * {@code admin:*} grants every permission check.</p>
 *
 * <p>Error codes raised by {@link AccessControlService}: {@code AC_DENIED},
 * {@code AC_ROLE_UNKNOWN}, {@code AC_USER_UNKNOWN}, {@code AC_ROLE_EXISTS},
 * {@code AC_BLANK}.</p>
 */
@Data
@Builder
public class Role {

    private String name;

    @Builder.Default
    private Set<String> permissions = new LinkedHashSet<>();

    /**
     * Returns {@code true} when this role explicitly carries {@code permission}
     * or holds the super-admin wildcard {@link Permission#ADMIN_ALL}.
     */
    public boolean grantsPermission(final String permission) {
        return permissions.contains(Permission.ADMIN_ALL)
                || permissions.contains(permission);
    }
}
