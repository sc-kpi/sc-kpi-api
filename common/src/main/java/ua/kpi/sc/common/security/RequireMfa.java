package ua.kpi.sc.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declarative annotation that enforces two-factor authentication for accessing
 * a controller method or all methods of a controller class.
 *
 * <p>When placed on a method, only users with 2FA enabled are granted access.
 * When placed on a class, it applies to all methods unless overridden.
 *
 * <p>Example usage:
 * <pre>{@code
 * @RequireMfa
 * public ResponseEntity<Void> deleteUser(@PathVariable UUID id) { ... }
 * }</pre>
 *
 * @see ua.kpi.sc.common.security.authorization.MfaMethodAuthorizationManager
 * @since 0.6.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireMfa {
}
