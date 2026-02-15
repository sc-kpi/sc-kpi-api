package ua.kpi.sc.featureflag.controller;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.common.security.UserPrincipal;
import ua.kpi.sc.featureflag.dto.FeatureFlagEvaluationResponse;
import ua.kpi.sc.featureflag.service.FeatureFlagEvaluationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeatureFlagControllerTest {

    @Mock
    private FeatureFlagEvaluationService evaluationService;

    @InjectMocks
    private FeatureFlagController controller;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private UserPrincipal authenticatedPrincipal() {
        return UserPrincipal.builder()
                .id(USER_ID)
                .email("user@kpi.ua").password("h").firstName("U").lastName("P")
                .tier(CapabilityTier.BASIC).active(true).build();
    }

    @Test
    void evaluateAll_authenticated_returnsMapFromService() {
        var principal = authenticatedPrincipal();
        var expected = Map.of("flag.a", true, "flag.b", false);
        when(evaluationService.evaluateAll(USER_ID, CapabilityTier.BASIC.getLevel())).thenReturn(expected);

        var result = controller.evaluateAll(principal);

        assertThat(result).isEqualTo(expected);
        verify(evaluationService).evaluateAll(USER_ID, CapabilityTier.BASIC.getLevel());
    }

    @Test
    void evaluateAll_unauthenticated_returnsMapWithNullUserContext() {
        var expected = Map.of("flag.a", true);
        when(evaluationService.evaluateAll(null, null)).thenReturn(expected);

        var result = controller.evaluateAll(null);

        assertThat(result).isEqualTo(expected);
        verify(evaluationService).evaluateAll(null, null);
    }

    @Test
    void evaluateSingle_enabled_returnsTrue() {
        var principal = authenticatedPrincipal();
        when(evaluationService.evaluate("test.flag", USER_ID, CapabilityTier.BASIC.getLevel())).thenReturn(true);

        FeatureFlagEvaluationResponse result = controller.evaluate("test.flag", principal);

        assertThat(result.key()).isEqualTo("test.flag");
        assertThat(result.enabled()).isTrue();
    }

    @Test
    void evaluateSingle_disabled_returnsFalse() {
        var principal = authenticatedPrincipal();
        when(evaluationService.evaluate("test.flag", USER_ID, CapabilityTier.BASIC.getLevel())).thenReturn(false);

        FeatureFlagEvaluationResponse result = controller.evaluate("test.flag", principal);

        assertThat(result.key()).isEqualTo("test.flag");
        assertThat(result.enabled()).isFalse();
    }

    @Test
    void evaluateSingle_nullPrincipal_evaluatesWithoutUserContext() {
        when(evaluationService.evaluate("test.flag", null, null)).thenReturn(true);

        FeatureFlagEvaluationResponse result = controller.evaluate("test.flag", null);

        assertThat(result.enabled()).isTrue();
        verify(evaluationService).evaluate("test.flag", null, null);
    }

    @Test
    void evaluateAll_emptyFlags_returnsEmptyMap() {
        when(evaluationService.evaluateAll(null, null)).thenReturn(Map.of());

        var result = controller.evaluateAll(null);

        assertThat(result).isEmpty();
    }

    @Test
    void evaluateAll_multipleFlags_returnsAllEvaluations() {
        var principal = authenticatedPrincipal();
        var expected = Map.of("flag.a", true, "flag.b", false, "flag.c", true);
        when(evaluationService.evaluateAll(USER_ID, CapabilityTier.BASIC.getLevel())).thenReturn(expected);

        var result = controller.evaluateAll(principal);

        assertThat(result).hasSize(3);
        assertThat(result).containsEntry("flag.a", true);
        assertThat(result).containsEntry("flag.b", false);
        assertThat(result).containsEntry("flag.c", true);
    }

    @Test
    void evaluateSingle_preservesKeyInResponse() {
        when(evaluationService.evaluate("my.feature.flag", null, null)).thenReturn(false);

        FeatureFlagEvaluationResponse result = controller.evaluate("my.feature.flag", null);

        assertThat(result.key()).isEqualTo("my.feature.flag");
    }
}
