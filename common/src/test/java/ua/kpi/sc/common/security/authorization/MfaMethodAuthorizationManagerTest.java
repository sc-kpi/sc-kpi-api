package ua.kpi.sc.common.security.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationResult;
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.common.security.RequireMfa;
import ua.kpi.sc.common.security.UserPrincipal;

class MfaMethodAuthorizationManagerTest {

    private MfaMethodAuthorizationManager manager;

    @BeforeEach
    void setUp() {
        manager = new MfaMethodAuthorizationManager();
    }

    @Test
    void noAnnotation_permits() throws Exception {
        MethodInvocation invocation = mockInvocation(UnannotatedService.class, "open");

        AuthorizationResult result = manager.authorize(() -> null, invocation);
        assertThat(result.isGranted()).isTrue();
    }

    @Test
    void methodAnnotation_mfaEnabled_granted() throws Exception {
        MethodInvocation invocation = mockInvocation(AnnotatedMethodService.class, "restricted");
        var principal = buildPrincipal(true);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        AuthorizationResult result = manager.authorize(() -> auth, invocation);
        assertThat(result.isGranted()).isTrue();
    }

    @Test
    void methodAnnotation_mfaDisabled_denied() throws Exception {
        MethodInvocation invocation = mockInvocation(AnnotatedMethodService.class, "restricted");
        var principal = buildPrincipal(false);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        AuthorizationResult result = manager.authorize(() -> auth, invocation);
        assertThat(result.isGranted()).isFalse();
    }

    @Test
    void classAnnotation_mfaEnabled_granted() throws Exception {
        MethodInvocation invocation = mockInvocation(AnnotatedClassService.class, "anyMethod");
        var principal = buildPrincipal(true);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        AuthorizationResult result = manager.authorize(() -> auth, invocation);
        assertThat(result.isGranted()).isTrue();
    }

    @Test
    void classAnnotation_mfaDisabled_denied() throws Exception {
        MethodInvocation invocation = mockInvocation(AnnotatedClassService.class, "anyMethod");
        var principal = buildPrincipal(false);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        AuthorizationResult result = manager.authorize(() -> auth, invocation);
        assertThat(result.isGranted()).isFalse();
    }

    @Test
    void nullAuthentication_denied() throws Exception {
        MethodInvocation invocation = mockInvocation(AnnotatedMethodService.class, "restricted");

        AuthorizationResult result = manager.authorize(() -> null, invocation);
        assertThat(result.isGranted()).isFalse();
    }

    @Test
    void nonUserPrincipal_denied() throws Exception {
        MethodInvocation invocation = mockInvocation(AnnotatedMethodService.class, "restricted");
        var auth = new UsernamePasswordAuthenticationToken("stringPrincipal", null, java.util.List.of());

        AuthorizationResult result = manager.authorize(() -> auth, invocation);
        assertThat(result.isGranted()).isFalse();
    }

    private MethodInvocation mockInvocation(Class<?> clazz, String methodName) throws Exception {
        Method method = clazz.getMethod(methodName);
        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getMethod()).thenReturn(method);
        return invocation;
    }

    private UserPrincipal buildPrincipal(boolean twoFactorEnabled) {
        return UserPrincipal.builder()
                .email("test@kpi.ua")
                .tier(CapabilityTier.BASIC)
                .active(true)
                .twoFactorEnabled(twoFactorEnabled)
                .build();
    }

    // Test fixture classes

    static class UnannotatedService {
        public void open() {}
    }

    static class AnnotatedMethodService {
        @RequireMfa
        public void restricted() {}
    }

    @RequireMfa
    static class AnnotatedClassService {
        public void anyMethod() {}
    }
}
