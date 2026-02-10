package ua.kpi.sc.common.security.authorization;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.common.security.UserPrincipal;

class TierAuthorizationManagerTest {

    private final RequestAuthorizationContext context =
            new RequestAuthorizationContext(new MockHttpServletRequest());

    @Test
    void defaultConstructor_anyAuthenticatedPrincipalPasses() {
        var manager = new TierAuthorizationManager();
        var principal = buildPrincipal(CapabilityTier.GUEST);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        AuthorizationResult result = manager.authorize(() -> auth, context);
        assertThat(result.isGranted()).isTrue();
    }

    @Test
    void authorize_nullAuth_denied() {
        var manager = new TierAuthorizationManager(1);

        AuthorizationResult result = manager.authorize(() -> null, context);
        assertThat(result.isGranted()).isFalse();
    }

    @Test
    void authorize_unauthenticated_denied() {
        var manager = new TierAuthorizationManager(1);
        var auth = new UsernamePasswordAuthenticationToken("user", "pass");
        auth.setAuthenticated(false);

        AuthorizationResult result = manager.authorize(() -> auth, context);
        assertThat(result.isGranted()).isFalse();
    }

    @Test
    void authorize_nonUserPrincipal_denied() {
        var manager = new TierAuthorizationManager(1);
        var auth = new UsernamePasswordAuthenticationToken("stringPrincipal", null, java.util.List.of());

        AuthorizationResult result = manager.authorize(() -> auth, context);
        assertThat(result.isGranted()).isFalse();
    }

    @Test
    void authorize_sufficientTier_granted() {
        var manager = new TierAuthorizationManager(3);
        var principal = buildPrincipal(CapabilityTier.ADMIN);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        AuthorizationResult result = manager.authorize(() -> auth, context);
        assertThat(result.isGranted()).isTrue();
    }

    @Test
    void authorize_insufficientTier_denied() {
        var manager = new TierAuthorizationManager(5);
        var principal = buildPrincipal(CapabilityTier.BASIC);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        AuthorizationResult result = manager.authorize(() -> auth, context);
        assertThat(result.isGranted()).isFalse();
    }

    @Test
    void requireTier_returnsConfiguredInstance() {
        var manager = TierAuthorizationManager.requireTier(4);
        var principal = buildPrincipal(CapabilityTier.SENIOR);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        AuthorizationResult result = manager.authorize(() -> auth, context);
        assertThat(result.isGranted()).isTrue();
    }

    @Test
    void requireTierEnum_returnsConfiguredInstance() {
        var manager = TierAuthorizationManager.requireTier(CapabilityTier.SENIOR);
        var principal = buildPrincipal(CapabilityTier.SENIOR);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        AuthorizationResult result = manager.authorize(() -> auth, context);
        assertThat(result.isGranted()).isTrue();
    }

    @Test
    void requireTierEnum_insufficientTier_denied() {
        var manager = TierAuthorizationManager.requireTier(CapabilityTier.ADMIN);
        var principal = buildPrincipal(CapabilityTier.BASIC);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        AuthorizationResult result = manager.authorize(() -> auth, context);
        assertThat(result.isGranted()).isFalse();
    }

    private UserPrincipal buildPrincipal(CapabilityTier tier) {
        return UserPrincipal.builder()
                .email("test@kpi.ua")
                .tier(tier)
                .active(true)
                .build();
    }
}
