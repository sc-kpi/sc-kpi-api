package ua.kpi.sc.common.security.authorization;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.common.security.UserPrincipal;

/**
 * URL-level {@link AuthorizationManager} that grants or denies access based on the
 * authenticated user's {@link CapabilityTier}.
 *
 * <p>Used in the security filter chain to protect URL patterns with tier-based rules.
 * Factory methods {@link #requireTier(int)} and {@link #requireTier(CapabilityTier)}
 * provide convenient construction for filter chain configuration.
 *
 * @see CapabilityTier
 * @see TierMethodAuthorizationManager
 * @since 0.1.0
 */
public class TierAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final int requiredTier;

    /**
     * Creates a manager that requires tier level 0 (effectively allows all authenticated users).
     */
    public TierAuthorizationManager() {
        this.requiredTier = 0;
    }

    /**
     * Creates a manager requiring the specified numeric tier level.
     *
     * @param requiredTier the minimum tier level
     */
    public TierAuthorizationManager(int requiredTier) {
        this.requiredTier = requiredTier;
    }

    /**
     * Creates a manager requiring the specified capability tier.
     *
     * @param requiredTier the minimum capability tier
     */
    public TierAuthorizationManager(CapabilityTier requiredTier) {
        this.requiredTier = requiredTier.getLevel();
    }

    @Override
    public AuthorizationResult authorize(Supplier<? extends @Nullable Authentication> authentication, RequestAuthorizationContext context) {
        Authentication auth = authentication.get();
        if (auth == null || !auth.isAuthenticated()) {
            return new AuthorizationDecision(false);
        }
        if (auth.getPrincipal() instanceof UserPrincipal principal) {
            return new AuthorizationDecision(principal.getTier().getLevel() >= requiredTier);
        }
        return new AuthorizationDecision(false);
    }

    /**
     * Factory method for creating a manager with the given numeric tier level.
     *
     * @param tier the minimum tier level
     * @return a new authorization manager
     */
    public static TierAuthorizationManager requireTier(int tier) {
        return new TierAuthorizationManager(tier);
    }

    /**
     * Factory method for creating a manager with the given capability tier.
     *
     * @param tier the minimum capability tier
     * @return a new authorization manager
     */
    public static TierAuthorizationManager requireTier(CapabilityTier tier) {
        return new TierAuthorizationManager(tier);
    }
}
