package ua.kpi.sc.common.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Roles a user can hold within a project, each mapping to an effective
 * {@link CapabilityTier} level.
 *
 * <ul>
 *   <li>{@link #LEAD} — project lead (tier 4 / {@link CapabilityTier#SENIOR})</li>
 *   <li>{@link #MEMBER} — project member (tier 2 / {@link CapabilityTier#INTERNAL})</li>
 * </ul>
 *
 * @see ContextType#PROJECT
 * @since 0.1.0
 */
@Getter
@RequiredArgsConstructor
public enum ProjectRole {

    /** Project lead with senior-level capabilities (tier 4). */
    LEAD(4, "lead"),
    /** Regular project member with internal access (tier 2). */
    MEMBER(2, "member");

    private final int effectiveTier;
    private final String value;
}
