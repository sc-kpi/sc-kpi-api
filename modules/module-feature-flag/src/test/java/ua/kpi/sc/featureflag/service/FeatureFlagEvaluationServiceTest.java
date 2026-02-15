package ua.kpi.sc.featureflag.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.kpi.sc.featureflag.config.FeatureFlagProperties;
import ua.kpi.sc.featureflag.entity.FeatureFlag;
import ua.kpi.sc.featureflag.entity.FeatureFlagOverride;
import ua.kpi.sc.featureflag.entity.OverrideType;
import ua.kpi.sc.featureflag.repository.FeatureFlagRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeatureFlagEvaluationServiceTest {

    @Mock
    private FeatureFlagRepository flagRepository;

    private FeatureFlagProperties properties;
    private FeatureFlagEvaluationService service;

    @BeforeEach
    void setUp() {
        properties = new FeatureFlagProperties();
        service = new FeatureFlagEvaluationService(flagRepository, properties);
    }

    @Test
    void evaluate_configOverrideTakesPrecedence() {
        properties.setOverrides(Map.of("test.flag", false));

        boolean result = service.evaluate("test.flag", UUID.randomUUID(), 5);

        assertThat(result).isFalse();
    }

    @Test
    void evaluate_userOverrideTakesPrecedenceOverTierAndGlobal() {
        UUID userId = UUID.randomUUID();
        FeatureFlag flag = FeatureFlag.builder().key("test.flag").enabled(true).rolloutPercentage(100).build();

        FeatureFlagOverride tierOverride = FeatureFlagOverride.builder()
                .overrideType(OverrideType.TIER).tierLevel(5).enabled(true).build();
        FeatureFlagOverride userOverride = FeatureFlagOverride.builder()
                .overrideType(OverrideType.USER).userId(userId).enabled(false).build();
        flag.setOverrides(List.of(tierOverride, userOverride));

        when(flagRepository.findByKeyWithOverrides("test.flag")).thenReturn(Optional.of(flag));

        boolean result = service.evaluate("test.flag", userId, 5);

        assertThat(result).isFalse();
    }

    @Test
    void evaluate_tierOverrideTakesPrecedenceOverGlobal() {
        UUID userId = UUID.randomUUID();
        FeatureFlag flag = FeatureFlag.builder().key("test.flag").enabled(true).rolloutPercentage(100).build();

        FeatureFlagOverride tierOverride = FeatureFlagOverride.builder()
                .overrideType(OverrideType.TIER).tierLevel(1).enabled(false).build();
        flag.setOverrides(List.of(tierOverride));

        when(flagRepository.findByKeyWithOverrides("test.flag")).thenReturn(Optional.of(flag));

        boolean result = service.evaluate("test.flag", userId, 1);

        assertThat(result).isFalse();
    }

    @Test
    void evaluate_percentageRolloutIsDeterministic() {
        UUID userId = UUID.randomUUID();
        FeatureFlag flag = FeatureFlag.builder().key("test.flag").enabled(true).rolloutPercentage(50).build();
        flag.setOverrides(List.of());
        when(flagRepository.findByKeyWithOverrides("test.flag")).thenReturn(Optional.of(flag));

        boolean result1 = service.evaluate("test.flag", userId, null);
        boolean result2 = service.evaluate("test.flag", userId, null);

        assertThat(result1).isEqualTo(result2);
    }

    @Test
    void evaluate_globalDefault() {
        FeatureFlag flag = FeatureFlag.builder().key("test.flag").enabled(false).rolloutPercentage(100).build();
        flag.setOverrides(List.of());
        when(flagRepository.findByKeyWithOverrides("test.flag")).thenReturn(Optional.of(flag));

        boolean result = service.evaluate("test.flag", null, null);

        assertThat(result).isFalse();
    }

    @Test
    void evaluate_configDefaultWhenFlagNotInDb() {
        properties.setDefaults(Map.of("unknown.flag", true));
        when(flagRepository.findByKeyWithOverrides("unknown.flag")).thenReturn(Optional.empty());

        boolean result = service.evaluate("unknown.flag", null, null);

        assertThat(result).isTrue();
    }

    @Test
    void evaluate_returnsFalseWhenNoFlagAndNoDefault() {
        when(flagRepository.findByKeyWithOverrides("missing.flag")).thenReturn(Optional.empty());

        boolean result = service.evaluate("missing.flag", null, null);

        assertThat(result).isFalse();
    }

    @Test
    void evaluateAll_returnsAllFlagStates() {
        FeatureFlag flag1 = FeatureFlag.builder().key("flag.one").enabled(true).rolloutPercentage(100).build();
        flag1.setOverrides(List.of());
        FeatureFlag flag2 = FeatureFlag.builder().key("flag.two").enabled(false).rolloutPercentage(100).build();
        flag2.setOverrides(List.of());

        when(flagRepository.findAllWithOverrides()).thenReturn(List.of(flag1, flag2));
        when(flagRepository.findByKeyWithOverrides("flag.one")).thenReturn(Optional.of(flag1));
        when(flagRepository.findByKeyWithOverrides("flag.two")).thenReturn(Optional.of(flag2));

        Map<String, Boolean> result = service.evaluateAll(null, null);

        assertThat(result).containsEntry("flag.one", true);
        assertThat(result).containsEntry("flag.two", false);
    }

    @Test
    void evaluateAll_mixedFlags_returnsCorrectMap() {
        FeatureFlag enabledFlag = FeatureFlag.builder().key("enabled.flag").enabled(true).rolloutPercentage(100).build();
        enabledFlag.setOverrides(List.of());
        FeatureFlag disabledFlag = FeatureFlag.builder().key("disabled.flag").enabled(false).rolloutPercentage(100).build();
        disabledFlag.setOverrides(List.of());

        when(flagRepository.findAllWithOverrides()).thenReturn(List.of(enabledFlag, disabledFlag));
        when(flagRepository.findByKeyWithOverrides("enabled.flag")).thenReturn(Optional.of(enabledFlag));
        when(flagRepository.findByKeyWithOverrides("disabled.flag")).thenReturn(Optional.of(disabledFlag));

        Map<String, Boolean> result = service.evaluateAll(UUID.randomUUID(), 1);

        assertThat(result).hasSize(2);
    }

    @Test
    void evaluate_percentageRollout_0percent_alwaysFalse() {
        UUID userId = UUID.randomUUID();
        FeatureFlag flag = FeatureFlag.builder().key("rollout.flag").enabled(true).rolloutPercentage(0).build();
        flag.setOverrides(List.of());
        when(flagRepository.findByKeyWithOverrides("rollout.flag")).thenReturn(Optional.of(flag));

        boolean result = service.evaluate("rollout.flag", userId, null);

        assertThat(result).isFalse();
    }

    @Test
    void evaluate_percentageRollout_100percent_alwaysTrue() {
        UUID userId = UUID.randomUUID();
        FeatureFlag flag = FeatureFlag.builder().key("full.flag").enabled(true).rolloutPercentage(100).build();
        flag.setOverrides(List.of());
        when(flagRepository.findByKeyWithOverrides("full.flag")).thenReturn(Optional.of(flag));

        boolean result = service.evaluate("full.flag", userId, null);

        assertThat(result).isTrue();
    }

    @Test
    void evaluate_nullUserId_skipsUserOverrideAndPercentage() {
        FeatureFlag flag = FeatureFlag.builder().key("test.flag").enabled(true).rolloutPercentage(50).build();

        FeatureFlagOverride userOverride = FeatureFlagOverride.builder()
                .overrideType(OverrideType.USER).userId(UUID.randomUUID()).enabled(false).build();
        flag.setOverrides(List.of(userOverride));

        when(flagRepository.findByKeyWithOverrides("test.flag")).thenReturn(Optional.of(flag));

        // With null userId, user override is skipped and percentage rollout is skipped,
        // so falls through to global enabled state
        boolean result = service.evaluate("test.flag", null, null);

        assertThat(result).isTrue();
    }

    @Test
    void evaluate_nullTierLevel_skipsTierOverride() {
        UUID userId = UUID.randomUUID();
        FeatureFlag flag = FeatureFlag.builder().key("test.flag").enabled(false).rolloutPercentage(100).build();

        FeatureFlagOverride tierOverride = FeatureFlagOverride.builder()
                .overrideType(OverrideType.TIER).tierLevel(5).enabled(true).build();
        flag.setOverrides(List.of(tierOverride));

        when(flagRepository.findByKeyWithOverrides("test.flag")).thenReturn(Optional.of(flag));

        // With null tierLevel, tier override is skipped, so falls through to global enabled=false
        boolean result = service.evaluate("test.flag", userId, null);

        assertThat(result).isFalse();
    }
}
