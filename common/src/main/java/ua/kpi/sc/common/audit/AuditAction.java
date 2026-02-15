package ua.kpi.sc.common.audit;

/**
 * Enumeration of all audit action types tracked across the system.
 *
 * @since 0.4.0
 */
public enum AuditAction {

    // CRUD actions
    CREATED,
    UPDATED,
    DELETED,

    // Feature flag actions
    TOGGLED,
    BULK_TOGGLED,
    OVERRIDE_ADDED,
    OVERRIDE_REMOVED,

    // Auth actions
    LOGIN,
    LOGIN_FAILED,
    LOGOUT,
    REGISTER,
    PASSWORD_CHANGED,
    PASSWORD_RESET_REQUESTED,
    PASSWORD_RESET_COMPLETED,
    OAUTH_LOGIN,
    OAUTH_LINKED,

    // Audit system actions
    AUDIT_EXPORTED
}
