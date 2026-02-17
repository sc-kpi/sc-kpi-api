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
import ua.kpi.sc.common.security.RequireMfa;

/**
 * Registers the {@link MfaMethodAuthorizationManager} as a method interceptor
 * that fires before method execution for any bean annotated with {@link RequireMfa}.
 *
 * <p>The interceptor runs at order 750, after the tier interceptor (700).
 *
 * @see RequireMfa
 * @see MfaMethodAuthorizationManager
 * @since 0.6.0
 */
@Configuration
public class MethodSecurityMfaConfig {

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    AuthorizationManagerBeforeMethodInterceptor requireMfaInterceptor() {
        AuthorizationManager<MethodInvocation> manager = new MfaMethodAuthorizationManager();

        Pointcut methodPointcut = new AnnotationMatchingPointcut(null, RequireMfa.class, true);
        Pointcut classPointcut = new AnnotationMatchingPointcut(RequireMfa.class, true);
        Pointcut combined = new ComposablePointcut(methodPointcut).union(classPointcut);

        AuthorizationManagerBeforeMethodInterceptor interceptor =
                new AuthorizationManagerBeforeMethodInterceptor(combined, manager);
        interceptor.setOrder(750);
        return interceptor;
    }
}
