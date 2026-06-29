package com.omiinqa.reference.access;

/**
 * Named permission constants used across the access-control domain.
 * Permissions follow the pattern {@code resource:action} e.g. {@code order:read}.
 */
public final class Permission {

    // Admin permissions
    public static final String ADMIN_ALL      = "admin:*";
    public static final String USER_CREATE    = "user:create";
    public static final String USER_DELETE    = "user:delete";
    public static final String USER_DISABLE   = "user:disable";
    public static final String USER_READ      = "user:read";
    public static final String ROLE_ASSIGN    = "role:assign";
    public static final String ROLE_REVOKE    = "role:revoke";
    public static final String AUDIT_VIEW     = "audit:view";

    // Manager permissions
    public static final String ORDER_READ     = "order:read";
    public static final String ORDER_MANAGE   = "order:manage";
    public static final String REPORT_VIEW    = "report:view";

    // Customer permissions
    public static final String PROFILE_READ   = "profile:read";
    public static final String PROFILE_UPDATE = "profile:update";
    public static final String ORDER_PLACE    = "order:place";
    public static final String ORDER_CANCEL   = "order:cancel";

    // Guest permissions
    public static final String CATALOG_READ   = "catalog:read";

    private Permission() {
    }
}
