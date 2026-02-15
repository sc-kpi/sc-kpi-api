package ua.kpi.sc.featureflag.config;

import org.aopalliance.aop.Advice;
import org.springframework.aop.Advisor;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import ua.kpi.sc.common.featureflag.FeatureFlag;
import ua.kpi.sc.common.featureflag.FeatureFlagChecker;

@Configuration
public class FeatureFlagInterceptorConfig {

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    Advisor featureFlagAdvisor(FeatureFlagChecker checker) {
        Advice advice = new FeatureFlagMethodInterceptor(checker);

        Pointcut methodPointcut = new AnnotationMatchingPointcut(null, FeatureFlag.class, true);
        Pointcut classPointcut = new AnnotationMatchingPointcut(FeatureFlag.class, true);
        Pointcut combined = new ComposablePointcut(methodPointcut).union(classPointcut);

        DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(combined, advice);
        advisor.setOrder(800);
        return advisor;
    }
}
