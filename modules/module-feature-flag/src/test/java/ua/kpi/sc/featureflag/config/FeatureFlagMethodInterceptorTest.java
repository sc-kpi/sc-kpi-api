package ua.kpi.sc.featureflag.config;

import java.lang.reflect.Method;
import java.util.UUID;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import ua.kpi.sc.common.exception.ResourceNotFoundException;
import ua.kpi.sc.common.featureflag.FeatureFlag;
import ua.kpi.sc.common.featureflag.FeatureFlagChecker;
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.common.security.UserPrincipal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeatureFlagMethodInterceptorTest {

    @Mock
    private FeatureFlagChecker checker;
    @Mock
    private MethodInvocation invocation;

    private FeatureFlagMethodInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new FeatureFlagMethodInterceptor(checker);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void invoke_proceedsWhenFlagEnabled() throws Throwable {
        Method method = AnnotatedService.class.getMethod("flaggedMethod");
        when(invocation.getMethod()).thenReturn(method);
        when(checker.isEnabled(eq("test.feature"), any(), any())).thenReturn(true);
        when(invocation.proceed()).thenReturn("result");

        Object result = interceptor.invoke(invocation);

        assertThat(result).isEqualTo("result");
        verify(invocation).proceed();
    }

    @Test
    void invoke_throwsWhenFlagDisabled() throws Throwable {
        Method method = AnnotatedService.class.getMethod("flaggedMethod");
        when(invocation.getMethod()).thenReturn(method);
        when(checker.isEnabled(eq("test.feature"), any(), any())).thenReturn(false);

        assertThatThrownBy(() -> interceptor.invoke(invocation))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(invocation, never()).proceed();
    }

    @Test
    void invoke_extractsUserFromSecurityContext() throws Throwable {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = UserPrincipal.builder()
                .id(userId).email("test@test.ua").tier(CapabilityTier.ADMIN).active(true).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        Method method = AnnotatedService.class.getMethod("flaggedMethod");
        when(invocation.getMethod()).thenReturn(method);
        when(checker.isEnabled("test.feature", userId, CapabilityTier.ADMIN.getLevel())).thenReturn(true);
        when(invocation.proceed()).thenReturn("ok");

        interceptor.invoke(invocation);

        verify(checker).isEnabled("test.feature", userId, CapabilityTier.ADMIN.getLevel());
    }

    @Test
    void invoke_proceedsWithoutAnnotation() throws Throwable {
        Method method = AnnotatedService.class.getMethod("noFlagMethod");
        when(invocation.getMethod()).thenReturn(method);
        when(invocation.proceed()).thenReturn("ok");

        Object result = interceptor.invoke(invocation);

        assertThat(result).isEqualTo("ok");
    }

    @Test
    void invoke_classLevelAnnotation_evaluatesFlag() throws Throwable {
        Method method = ClassAnnotatedService.class.getMethod("someMethod");
        when(invocation.getMethod()).thenReturn(method);
        when(checker.isEnabled(eq("class.feature"), any(), any())).thenReturn(true);
        when(invocation.proceed()).thenReturn("ok");

        Object result = interceptor.invoke(invocation);

        assertThat(result).isEqualTo("ok");
        verify(checker).isEnabled(eq("class.feature"), any(), any());
    }

    @Test
    void invoke_methodAnnotationOverridesClassAnnotation() throws Throwable {
        Method method = ClassAnnotatedService.class.getMethod("overriddenMethod");
        when(invocation.getMethod()).thenReturn(method);
        when(checker.isEnabled(eq("method.override"), any(), any())).thenReturn(true);
        when(invocation.proceed()).thenReturn("overridden");

        Object result = interceptor.invoke(invocation);

        assertThat(result).isEqualTo("overridden");
        verify(checker).isEnabled(eq("method.override"), any(), any());
    }

    @Test
    void invoke_noAuthentication_passesNullUserContext() throws Throwable {
        Method method = AnnotatedService.class.getMethod("flaggedMethod");
        when(invocation.getMethod()).thenReturn(method);
        when(checker.isEnabled("test.feature", null, null)).thenReturn(true);
        when(invocation.proceed()).thenReturn("ok");

        interceptor.invoke(invocation);

        verify(checker).isEnabled("test.feature", null, null);
    }

    static class AnnotatedService {
        @FeatureFlag("test.feature")
        public String flaggedMethod() {
            return "result";
        }

        public String noFlagMethod() {
            return "ok";
        }
    }

    @FeatureFlag("class.feature")
    static class ClassAnnotatedService {
        public String someMethod() {
            return "ok";
        }

        @FeatureFlag("method.override")
        public String overriddenMethod() {
            return "overridden";
        }
    }
}
