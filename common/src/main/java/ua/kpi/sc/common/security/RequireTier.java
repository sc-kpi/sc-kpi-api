package ua.kpi.sc.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declarative annotation that enforces a minimum {@link CapabilityTier} for accessing
 * a controller method or all methods of a controller class.
 *
 * <p>When placed on a method, only users whose tier is at least the specified value
 * are granted access. When placed on a class, it applies to all methods unless
 * overridden by a method-level annotation.
 *
 * <p>Example usage:
 * <pre>{@code
 * @RequireTier(CapabilityTier.SENIOR)
 * public ResponseEntity<Report> generateReport() { ... }
 * }</pre>
 *
 * @see CapabilityTier
 * @see ua.kpi.sc.common.security.authorization.TierMethodAuthorizationManager
 * @since 0.1.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireTier {
    /**
     * The minimum capability tier required.
     *
     * @return the required tier
     */
    CapabilityTier value();
}
