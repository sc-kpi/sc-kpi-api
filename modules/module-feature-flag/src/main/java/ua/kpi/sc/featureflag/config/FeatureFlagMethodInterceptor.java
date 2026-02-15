package ua.kpi.sc.featureflag.config;

import java.util.UUID;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import ua.kpi.sc.common.exception.ResourceNotFoundException;
import ua.kpi.sc.common.featureflag.FeatureFlag;
import ua.kpi.sc.common.featureflag.FeatureFlagChecker;
import ua.kpi.sc.common.security.UserPrincipal;

/**
 * AOP method interceptor that evaluates {@link FeatureFlag} annotations
 * before allowing method execution.
 *
 * @since 0.3.0
 */
public class FeatureFlagMethodInterceptor implements MethodInterceptor {

    private final FeatureFlagChecker checker;

    public FeatureFlagMethodInterceptor(FeatureFlagChecker checker) {
        this.checker = checker;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        FeatureFlag annotation = invocation.getMethod().getAnnotation(FeatureFlag.class);
        if (annotation == null) {
            annotation = AnnotationUtils.findAnnotation(
                    invocation.getMethod().getDeclaringClass(), FeatureFlag.class);
        }
        if (annotation == null) {
            return invocation.proceed();
        }

        String key = annotation.value();
        UUID userId = null;
        Integer tierLevel = null;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            userId = principal.getId();
            tierLevel = principal.getTier().getLevel();
        }

        if (!checker.isEnabled(key, userId, tierLevel)) {
            throw new ResourceNotFoundException("Feature '%s' is not available".formatted(key));
        }

        return invocation.proceed();
    }
}
