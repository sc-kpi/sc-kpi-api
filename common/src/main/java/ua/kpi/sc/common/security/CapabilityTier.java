package ua.kpi.sc.common.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Hierarchical capability tiers that control access to application features.
 *
 * <p>Each tier has a numeric level (0–5). Authorization checks compare the user's
 * tier level against the required level — a user with a higher-level tier can access
 * all resources available to lower tiers.
 *
 * <p>Tier hierarchy (lowest to highest):
 * <ol start="0">
 *   <li>{@link #GUEST} — unauthenticated or unverified users</li>
 *   <li>{@link #BASIC} — registered users with minimal access</li>
 *   <li>{@link #INTERNAL} — members with internal access (department/project members)</li>
 *   <li>{@link #ADVANCED} — users with extended capabilities (content managers)</li>
 *   <li>{@link #SENIOR} — senior roles (department heads, project leads)</li>
 *   <li>{@link #ADMIN} — full system administrators</li>
 * </ol>
 *
 * @see RequireTier
 * @see UserPrincipal#getTier()
 * @since 0.1.0
 */
@Getter
@RequiredArgsConstructor
public enum CapabilityTier {

    GUEST(0, "Guest"),
    BASIC(1, "Basic"),
    INTERNAL(2, "Internal Access"),
    ADVANCED(3, "Advanced"),
    SENIOR(4, "Senior"),
    ADMIN(5, "Administrator");

    private final int level;
    private final String displayName;

    /**
     * Returns the tier corresponding to the given numeric level.
     * Falls back to {@link #GUEST} if no tier matches the level.
     *
     * @param level the numeric tier level (0–5)
     * @return the matching tier, or {@link #GUEST} if not found
     */
    public static CapabilityTier fromLevel(int level) {
        for (CapabilityTier tier : values()) {
            if (tier.level == level) {
                return tier;
            }
        }
        return GUEST;
    }

    /**
     * Checks whether this tier meets or exceeds the required tier.
     *
     * @param required the minimum tier needed
     * @return {@code true} if this tier's level is greater than or equal to the required level
     */
    public boolean isAtLeast(CapabilityTier required) {
        return this.level >= required.level;
    }
}
