package ua.kpi.sc.ratelimit.controller;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.common.security.UserPrincipal;
import ua.kpi.sc.ratelimit.dto.CreateRateLimitRuleRequest;
import ua.kpi.sc.ratelimit.dto.RateLimitRuleResponse;
import ua.kpi.sc.ratelimit.dto.RateLimitStatsResponse;
import ua.kpi.sc.ratelimit.dto.RateLimitToggleRequest;
import ua.kpi.sc.ratelimit.dto.UpdateRateLimitRuleRequest;
import ua.kpi.sc.ratelimit.entity.RateLimitScope;
import ua.kpi.sc.ratelimit.service.RateLimitService;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitAdminControllerTest {

    @Mock
    private RateLimitService rateLimitService;

    @InjectMocks
    private RateLimitAdminController controller;

    private final UserPrincipal adminPrincipal = UserPrincipal.builder()
            .id(UUID.randomUUID())
            .email("admin@test.kpi.ua")
            .tier(CapabilityTier.ADMIN)
            .active(true)
            .build();

    @Test
    void listRules_delegatesToService() {
        var pageable = PageRequest.of(0, 20);
        var expected = new PageImpl<>(List.of(buildResponse()));
        when(rateLimitService.listRules(pageable)).thenReturn(expected);

        Page<RateLimitRuleResponse> result = controller.listRules(pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void createRule_delegatesToService() {
        var request = new CreateRateLimitRuleRequest(
                "test.rule", null, "/api/**", null,
                60, 60, 90, RateLimitScope.IP, null, null, null, null, 1, true);
        var expected = buildResponse();
        when(rateLimitService.createRule(eq(request), eq(adminPrincipal))).thenReturn(expected);

        RateLimitRuleResponse result = controller.createRule(request, adminPrincipal);

        assertThat(result.name()).isEqualTo("test.rule");
    }

    @Test
    void getRule_delegatesToService() {
        UUID id = UUID.randomUUID();
        var expected = buildResponse();
        when(rateLimitService.getRule(id)).thenReturn(expected);

        RateLimitRuleResponse result = controller.getRule(id);

        assertThat(result.name()).isEqualTo("test.rule");
    }

    @Test
    void updateRule_delegatesToService() {
        UUID id = UUID.randomUUID();
        var request = new UpdateRateLimitRuleRequest(
                "Updated", null, null, null, null, null,
                null, null, null, null, null, null, null);
        var expected = buildResponse();
        when(rateLimitService.updateRule(eq(id), eq(request), eq(adminPrincipal))).thenReturn(expected);

        controller.updateRule(id, request, adminPrincipal);

        verify(rateLimitService).updateRule(id, request, adminPrincipal);
    }

    @Test
    void toggleRule_delegatesToService() {
        UUID id = UUID.randomUUID();
        var request = new RateLimitToggleRequest(false);
        var expected = buildResponse();
        when(rateLimitService.toggleRule(eq(id), eq(request), eq(adminPrincipal))).thenReturn(expected);

        controller.toggleRule(id, request, adminPrincipal);

        verify(rateLimitService).toggleRule(id, request, adminPrincipal);
    }

    @Test
    void deleteRule_delegatesToService() {
        UUID id = UUID.randomUUID();

        controller.deleteRule(id, adminPrincipal);

        verify(rateLimitService).deleteRule(id, adminPrincipal);
    }

    @Test
    void getStats_delegatesToService() {
        var expected = new RateLimitStatsResponse(42L, 100L, Map.of());
        when(rateLimitService.getStats()).thenReturn(expected);

        RateLimitStatsResponse result = controller.getStats();

        assertThat(result.totalViolations()).isEqualTo(42L);
    }

    private RateLimitRuleResponse buildResponse() {
        return new RateLimitRuleResponse(
                UUID.randomUUID(), "test.rule", "Test rule", "/api/**", null,
                60, 60, 90, RateLimitScope.IP, null, null, null, null,
                1, true, null, Instant.now(), Instant.now()
        );
    }
}
