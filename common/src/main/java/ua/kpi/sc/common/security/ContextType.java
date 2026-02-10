package ua.kpi.sc.common.security;

/**
 * Defines the authorization scope in which a user's permissions are evaluated.
 *
 * <ul>
 *   <li>{@link #GLOBAL} — system-wide permissions (e.g. admin operations)</li>
 *   <li>{@link #DEPARTMENT} — scoped to a specific department</li>
 *   <li>{@link #PROJECT} — scoped to a specific project</li>
 *   <li>{@link #PARTNER} — scoped to a partner organization's access level</li>
 * </ul>
 *
 * @see DepartmentRole
 * @see ProjectRole
 * @see PartnerLevel
 * @since 0.1.0
 */
public enum ContextType {
    /** System-wide permissions (e.g. admin operations). */
    GLOBAL,
    /** Permissions scoped to a specific department. */
    DEPARTMENT,
    /** Permissions scoped to a specific project. */
    PROJECT,
    /** Permissions scoped to a partner organization's access level. */
    PARTNER
}
