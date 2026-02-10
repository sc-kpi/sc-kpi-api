package ua.kpi.sc.common.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Roles a user can hold within a department, each mapping to an effective
 * {@link CapabilityTier} level.
 *
 * <ul>
 *   <li>{@link #HEAD} — department head (tier 4 / {@link CapabilityTier#SENIOR})</li>
 *   <li>{@link #CONTENT_MANAGER} — manages department content (tier 3 / {@link CapabilityTier#ADVANCED})</li>
 *   <li>{@link #MEMBER} — regular department member (tier 2 / {@link CapabilityTier#INTERNAL})</li>
 * </ul>
 *
 * @see ContextType#DEPARTMENT
 * @since 0.1.0
 */
@Getter
@RequiredArgsConstructor
public enum DepartmentRole {

    /** Department head with senior-level capabilities (tier 4). */
    HEAD(4, "head"),
    /** Content manager with advanced capabilities (tier 3). */
    CONTENT_MANAGER(3, "content_manager"),
    /** Regular department member with internal access (tier 2). */
    MEMBER(2, "member");

    private final int effectiveTier;
    private final String value;
}
