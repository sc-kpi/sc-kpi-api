package ua.kpi.sc.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Spring-managed bean for SpEL-based permission checks in {@code @PreAuthorize} expressions.
 *
 * <p>Registered as {@code "permissionChecker"} so it can be referenced in SpEL:
 * <pre>{@code
 * @PreAuthorize("@permissionChecker.hasTier(T(ua.kpi.sc.common.security.CapabilityTier).SENIOR)")
 * public void sensitiveOperation() { ... }
 * }</pre>
 *
 * @see CapabilityTier
 * @see UserPrincipal
 * @since 0.1.0
 */
@Component("permissionChecker")
public class PermissionChecker {

    /**
     * Checks whether the current user's tier level meets or exceeds the required level.
     *
     * @param requiredTier the minimum tier level (numeric)
     * @return {@code true} if the current user has sufficient tier, {@code false} otherwise
     */
    public boolean hasTier(int requiredTier) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return false;
        }
        return principal.getTier().getLevel() >= requiredTier;
    }

    /**
     * Checks whether the current user's tier meets or exceeds the required tier.
     *
     * @param requiredTier the minimum capability tier
     * @return {@code true} if the current user has sufficient tier
     */
    public boolean hasTier(CapabilityTier requiredTier) {
        return hasTier(requiredTier.getLevel());
    }

    /** Returns {@code true} if the current user is an administrator (tier 5). */
    public boolean isAdmin() {
        return hasTier(CapabilityTier.ADMIN.getLevel());
    }

    /** Returns {@code true} if the current user is at least senior level (tier 4+). */
    public boolean isSenior() {
        return hasTier(CapabilityTier.SENIOR.getLevel());
    }

    /** Returns {@code true} if the current user is at least advanced level (tier 3+). */
    public boolean isAdvanced() {
        return hasTier(CapabilityTier.ADVANCED.getLevel());
    }

    /** Returns {@code true} if the current user has internal access (tier 2+). */
    public boolean isInternal() {
        return hasTier(CapabilityTier.INTERNAL.getLevel());
    }

    /** Returns {@code true} if the current user is at least basic level (tier 1+). */
    public boolean isBasic() {
        return hasTier(CapabilityTier.BASIC.getLevel());
    }
}
