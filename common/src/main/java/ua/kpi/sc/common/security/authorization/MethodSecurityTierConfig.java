package ua.kpi.sc.common.security.authorization;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import ua.kpi.sc.common.security.RequireTier;

/**
 * Registers the {@link TierMethodAuthorizationManager} as a method interceptor
 * that fires before method execution for any bean annotated with {@link RequireTier}.
 *
 * <p>The interceptor runs at order 700, which is after Spring Security's built-in
 * {@code @PreAuthorize} interceptor (order 500) but before {@code @PostAuthorize} (order 600+).
 *
 * @see RequireTier
 * @see TierMethodAuthorizationManager
 * @since 0.1.0
 */
@Configuration
public class MethodSecurityTierConfig {

    /**
     * Creates the AOP interceptor that enforces {@link RequireTier} annotations.
     *
     * @return the configured method interceptor
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    AuthorizationManagerBeforeMethodInterceptor requireTierInterceptor() {
        AuthorizationManager<MethodInvocation> manager = new TierMethodAuthorizationManager();

        Pointcut methodPointcut = new AnnotationMatchingPointcut(null, RequireTier.class, true);
        Pointcut classPointcut = new AnnotationMatchingPointcut(RequireTier.class, true);
        Pointcut combined = new ComposablePointcut(methodPointcut).union(classPointcut);

        AuthorizationManagerBeforeMethodInterceptor interceptor =
                new AuthorizationManagerBeforeMethodInterceptor(combined, manager);
        interceptor.setOrder(700);
        return interceptor;
    }
}
