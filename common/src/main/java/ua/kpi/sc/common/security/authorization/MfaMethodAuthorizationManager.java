package ua.kpi.sc.common.security.authorization;

import java.util.function.Supplier;

import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import ua.kpi.sc.common.security.RequireMfa;
import ua.kpi.sc.common.security.UserPrincipal;

/**
 * Method-level {@link AuthorizationManager} that enforces {@link RequireMfa} annotations.
 *
 * <p>Checks for the annotation on the invoked method first; if absent, falls back to the
 * declaring class. If no annotation is found at either level, access is granted by default.
 *
 * @see RequireMfa
 * @see MethodSecurityMfaConfig
 * @since 0.6.0
 */
public class MfaMethodAuthorizationManager implements AuthorizationManager<MethodInvocation> {

    @Override
    public AuthorizationResult authorize(Supplier<? extends @Nullable Authentication> authentication, MethodInvocation invocation) {
        RequireMfa annotation = invocation.getMethod().getAnnotation(RequireMfa.class);
        if (annotation == null) {
            annotation = AnnotationUtils.findAnnotation(invocation.getMethod().getDeclaringClass(), RequireMfa.class);
        }
        if (annotation == null) {
            return new AuthorizationDecision(true);
        }

        Authentication auth = authentication.get();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return new AuthorizationDecision(false);
        }
        return new AuthorizationDecision(principal.isTwoFactorEnabled());
    }
}
