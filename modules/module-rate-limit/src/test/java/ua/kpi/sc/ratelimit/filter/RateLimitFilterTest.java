package ua.kpi.sc.ratelimit.filter;

import java.util.List;
import java.util.UUID;

import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;
import ua.kpi.sc.common.featureflag.FeatureFlagChecker;
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.common.security.UserPrincipal;
import ua.kpi.sc.ratelimit.config.RateLimitProperties;
import ua.kpi.sc.ratelimit.entity.RateLimitRule;
import ua.kpi.sc.ratelimit.entity.RateLimitScope;
import ua.kpi.sc.ratelimit.service.RateLimitEvaluationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private RateLimitEvaluationService evaluationService;
    @Mock
    private FeatureFlagChecker featureFlagChecker;
    @Mock
    private FilterChain filterChain;

    private RateLimitProperties properties;
    private RateLimitFilter filter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        properties = new RateLimitProperties();
        properties.setEnabled(true);
        properties.setExcludedPatterns(List.of("/actuator/**"));
        objectMapper = new ObjectMapper();
        filter = new RateLimitFilter(properties, evaluationService, featureFlagChecker, objectMapper);
    }

    @Test
    void doFilter_disabledProperty_skips() throws Exception {
        properties.setEnabled(false);
        var request = new MockHttpServletRequest("GET", "/api/v1/users");
        request.setServletPath("/api/v1/users");
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(evaluationService, never()).resolveRule(any(), any(), any(), any(), any());
    }

    @Test
    void doFilter_featureFlagDisabled_skips() throws Exception {
        when(featureFlagChecker.isEnabled(eq("rate-limiting.enabled"), any(), any())).thenReturn(false);
        var request = new MockHttpServletRequest("GET", "/api/v1/users");
        request.setServletPath("/api/v1/users");
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_noMatchingRule_proceeds() throws Exception {
        when(featureFlagChecker.isEnabled(eq("rate-limiting.enabled"), any(), any())).thenReturn(true);
        when(evaluationService.resolveRule(any(), any(), any(), any(), any())).thenReturn(null);
        var request = new MockHttpServletRequest("GET", "/api/v1/users");
        request.setServletPath("/api/v1/users");
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_withinLimit_setsHeaders() throws Exception {
        when(featureFlagChecker.isEnabled(eq("rate-limiting.enabled"), any(), any())).thenReturn(true);
        RateLimitRule rule = buildRule();
        when(evaluationService.resolveRule(any(), any(), any(), any(), any())).thenReturn(rule);
        ConsumptionProbe probe = mock(ConsumptionProbe.class);
        when(probe.isConsumed()).thenReturn(true);
        when(probe.getRemainingTokens()).thenReturn(9L);
        when(probe.getNanosToWaitForReset()).thenReturn(60_000_000_000L);
        when(evaluationService.tryConsume(any(), any(), any(), any())).thenReturn(probe);

        var request = new MockHttpServletRequest("GET", "/api/v1/users");
        request.setServletPath("/api/v1/users");
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("90");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("9");
    }

    @Test
    void doFilter_exceededLimit_returns429() throws Exception {
        when(featureFlagChecker.isEnabled(eq("rate-limiting.enabled"), any(), any())).thenReturn(true);
        RateLimitRule rule = buildRule();
        when(evaluationService.resolveRule(any(), any(), any(), any(), any())).thenReturn(rule);
        ConsumptionProbe probe = mock(ConsumptionProbe.class);
        when(probe.isConsumed()).thenReturn(false);
        when(probe.getRemainingTokens()).thenReturn(0L);
        when(probe.getNanosToWaitForRefill()).thenReturn(30_000_000_000L);
        when(evaluationService.tryConsume(any(), any(), any(), any())).thenReturn(probe);

        var request = new MockHttpServletRequest("GET", "/api/v1/users");
        request.setServletPath("/api/v1/users");
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("30");
        assertThat(response.getContentType()).isEqualTo("application/problem+json");
        verify(evaluationService).recordViolation(rule.getEndpointPattern());
    }

    @Test
    void doFilter_whitelistedIp_skips() throws Exception {
        properties.setWhitelistedIps(List.of("10.0.0.1"));
        when(featureFlagChecker.isEnabled(eq("rate-limiting.enabled"), any(), any())).thenReturn(true);
        var request = new MockHttpServletRequest("GET", "/api/v1/users");
        request.setServletPath("/api/v1/users");
        request.setRemoteAddr("10.0.0.1");
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(evaluationService, never()).resolveRule(any(), any(), any(), any(), any());
    }

    @Test
    void doFilter_extractsClientIpFromXForwardedFor() throws Exception {
        when(featureFlagChecker.isEnabled(eq("rate-limiting.enabled"), any(), any())).thenReturn(true);
        when(evaluationService.resolveRule(any(), any(), eq("1.2.3.4"), any(), any())).thenReturn(null);
        var request = new MockHttpServletRequest("GET", "/api/v1/users");
        request.setServletPath("/api/v1/users");
        request.addHeader("X-Forwarded-For", "1.2.3.4, 5.6.7.8");
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(evaluationService).resolveRule(any(), any(), eq("1.2.3.4"), any(), any());
    }

    @Test
    void shouldNotFilter_swaggerPaths() {
        var request = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
        request.setServletPath("/swagger-ui/index.html");

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldNotFilter_apiDocsPaths() {
        var request = new MockHttpServletRequest("GET", "/v3/api-docs");
        request.setServletPath("/v3/api-docs");

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldNotFilter_healthEndpoint() {
        var request = new MockHttpServletRequest("GET", "/actuator/health");
        request.setServletPath("/actuator/health");

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void doFilter_withAuthenticatedUser_passesUserContext() throws Exception {
        UserPrincipal principal = UserPrincipal.builder()
                .id(UUID.randomUUID())
                .email("user@test.kpi.ua")
                .tier(CapabilityTier.BASIC)
                .active(true)
                .build();
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(featureFlagChecker.isEnabled(eq("rate-limiting.enabled"), eq(principal.getId()),
                eq(principal.getTier().getLevel()))).thenReturn(true);
        when(evaluationService.resolveRule(any(), any(), any(), eq(principal.getId()),
                eq(principal.getTier().getLevel()))).thenReturn(null);

        var request = new MockHttpServletRequest("GET", "/api/v1/users");
        request.setServletPath("/api/v1/users");
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    private RateLimitRule buildRule() {
        return RateLimitRule.builder()
                .id(UUID.randomUUID())
                .name("test.rule")
                .endpointPattern("/api/v1/**")
                .limitPerPeriod(60)
                .periodSeconds(60)
                .burstCapacity(90)
                .scope(RateLimitScope.IP)
                .priority(1)
                .enabled(true)
                .build();
    }
}
