package ua.kpi.sc.featureflag.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ua.kpi.sc.common.audit.AuditEvent;
import ua.kpi.sc.common.audit.AuditPublisher;
import ua.kpi.sc.common.notification.NotificationPublisher;
import ua.kpi.sc.common.exception.BadRequestException;
import ua.kpi.sc.common.exception.ConflictException;
import ua.kpi.sc.common.exception.ResourceNotFoundException;
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.common.security.UserPrincipal;
import ua.kpi.sc.featureflag.dto.BulkToggleRequest;
import ua.kpi.sc.featureflag.dto.CreateFeatureFlagRequest;
import ua.kpi.sc.featureflag.dto.CreateOverrideRequest;
import ua.kpi.sc.featureflag.dto.FeatureFlagResponse;
import ua.kpi.sc.featureflag.dto.ToggleFeatureFlagRequest;
import ua.kpi.sc.featureflag.dto.UpdateFeatureFlagRequest;
import ua.kpi.sc.featureflag.entity.FeatureFlag;
import ua.kpi.sc.featureflag.entity.FeatureFlagOverride;
import ua.kpi.sc.featureflag.entity.OverrideType;
import ua.kpi.sc.featureflag.repository.FeatureFlagOverrideRepository;
import ua.kpi.sc.featureflag.repository.FeatureFlagRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeatureFlagServiceTest {

    @Mock
    private FeatureFlagRepository flagRepository;
    @Mock
    private FeatureFlagOverrideRepository overrideRepository;
    @Mock
    private AuditPublisher auditPublisher;
    @Mock
    private NotificationPublisher notificationPublisher;
    @Mock
    private FeatureFlagEvaluationService evaluationService;

    @InjectMocks
    private FeatureFlagService service;

    private final UserPrincipal adminPrincipal = UserPrincipal.builder()
            .id(UUID.randomUUID())
            .email("admin@test.kpi.ua")
            .tier(CapabilityTier.ADMIN)
            .active(true)
            .build();

    @Test
    void createFlag_success() {
        var request = new CreateFeatureFlagRequest("test.flag", "Test Flag", "A test flag", true, null, 100);
        when(flagRepository.existsByKey("test.flag")).thenReturn(false);

        FeatureFlag saved = FeatureFlag.builder()
                .id(UUID.randomUUID()).key("test.flag").name("Test Flag")
                .description("A test flag").enabled(true).rolloutPercentage(100)
                .createdBy(adminPrincipal.getId()).build();
        when(flagRepository.save(any(FeatureFlag.class))).thenReturn(saved);

        FeatureFlagResponse response = service.createFlag(request, adminPrincipal);

        assertThat(response.key()).isEqualTo("test.flag");
        verify(auditPublisher).publish(any(AuditEvent.class));
        verify(evaluationService).evictCache();
    }

    @Test
    void createFlag_duplicateKeyThrowsConflict() {
        var request = new CreateFeatureFlagRequest("test.flag", "Test Flag", null, true, null, 100);
        when(flagRepository.existsByKey("test.flag")).thenReturn(true);

        assertThatThrownBy(() -> service.createFlag(request, adminPrincipal))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void toggleFlag_success() {
        UUID flagId = UUID.randomUUID();
        FeatureFlag flag = FeatureFlag.builder()
                .id(flagId).key("test.flag").name("Test").enabled(true).rolloutPercentage(100).build();
        when(flagRepository.findById(flagId)).thenReturn(Optional.of(flag));
        when(flagRepository.save(any(FeatureFlag.class))).thenReturn(flag);

        var request = new ToggleFeatureFlagRequest(false, "Emergency disable");
        FeatureFlagResponse response = service.toggleFlag(flagId, request, adminPrincipal);

        verify(auditPublisher).publish(any(AuditEvent.class));
        verify(evaluationService).evictCache();
    }

    @Test
    void deleteFlag_notFoundThrows() {
        UUID flagId = UUID.randomUUID();
        when(flagRepository.findById(flagId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteFlag(flagId, adminPrincipal))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteFlag_success() {
        UUID flagId = UUID.randomUUID();
        FeatureFlag flag = FeatureFlag.builder()
                .id(flagId).key("test.flag").name("Test").enabled(true).rolloutPercentage(100).build();
        when(flagRepository.findById(flagId)).thenReturn(Optional.of(flag));

        service.deleteFlag(flagId, adminPrincipal);

        verify(flagRepository).delete(flag);
        verify(auditPublisher).publish(any(AuditEvent.class));
        verify(evaluationService).evictCache();
    }

    @Test
    void updateFlag_updatesAllFields() {
        UUID flagId = UUID.randomUUID();
        FeatureFlag flag = FeatureFlag.builder()
                .id(flagId).key("test.flag").name("Old Name").description("Old desc")
                .enabled(true).environment("dev").rolloutPercentage(100).build();
        when(flagRepository.findById(flagId)).thenReturn(Optional.of(flag));
        when(flagRepository.save(any(FeatureFlag.class))).thenReturn(flag);

        var request = new UpdateFeatureFlagRequest("New Name", "New desc", false, "prod", 50);
        FeatureFlagResponse response = service.updateFlag(flagId, request, adminPrincipal);

        verify(evaluationService).evictCache();
    }

    @Test
    void addOverride_tierOverride() {
        UUID flagId = UUID.randomUUID();
        FeatureFlag flag = FeatureFlag.builder()
                .id(flagId).key("test.flag").name("Test").enabled(true).rolloutPercentage(100).build();
        when(flagRepository.findById(flagId)).thenReturn(Optional.of(flag));

        FeatureFlagOverride saved = FeatureFlagOverride.builder()
                .id(UUID.randomUUID()).flag(flag).overrideType(OverrideType.TIER)
                .tierLevel(5).enabled(false).build();
        when(overrideRepository.save(any(FeatureFlagOverride.class))).thenReturn(saved);

        var request = new CreateOverrideRequest(OverrideType.TIER, 5, null, false);
        var response = service.addOverride(flagId, request, adminPrincipal);

        assertThat(response.overrideType()).isEqualTo(OverrideType.TIER);
        assertThat(response.tierLevel()).isEqualTo(5);
        verify(evaluationService).evictCache();
    }

    @Test
    void addOverride_tierWithoutLevelThrows() {
        UUID flagId = UUID.randomUUID();
        FeatureFlag flag = FeatureFlag.builder()
                .id(flagId).key("test.flag").name("Test").enabled(true).rolloutPercentage(100).build();
        when(flagRepository.findById(flagId)).thenReturn(Optional.of(flag));

        var request = new CreateOverrideRequest(OverrideType.TIER, null, null, false);
        assertThatThrownBy(() -> service.addOverride(flagId, request, adminPrincipal))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void addOverride_userWithoutIdThrows() {
        UUID flagId = UUID.randomUUID();
        FeatureFlag flag = FeatureFlag.builder()
                .id(flagId).key("test.flag").name("Test").enabled(true).rolloutPercentage(100).build();
        when(flagRepository.findById(flagId)).thenReturn(Optional.of(flag));

        var request = new CreateOverrideRequest(OverrideType.USER, null, null, false);
        assertThatThrownBy(() -> service.addOverride(flagId, request, adminPrincipal))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void removeOverride_success() {
        UUID flagId = UUID.randomUUID();
        UUID overrideId = UUID.randomUUID();
        FeatureFlag flag = FeatureFlag.builder()
                .id(flagId).key("test.flag").name("Test").enabled(true).rolloutPercentage(100).build();
        FeatureFlagOverride override = FeatureFlagOverride.builder()
                .id(overrideId).flag(flag).overrideType(OverrideType.TIER).tierLevel(5).enabled(false).build();

        when(flagRepository.findById(flagId)).thenReturn(Optional.of(flag));
        when(overrideRepository.findById(overrideId)).thenReturn(Optional.of(override));

        service.removeOverride(flagId, overrideId, adminPrincipal);

        verify(overrideRepository).delete(override);
        verify(evaluationService).evictCache();
    }

    @Test
    void removeOverride_wrongFlagThrows() {
        UUID flagId = UUID.randomUUID();
        UUID otherFlagId = UUID.randomUUID();
        UUID overrideId = UUID.randomUUID();

        FeatureFlag flag = FeatureFlag.builder()
                .id(flagId).key("test.flag").name("Test").enabled(true).rolloutPercentage(100).build();
        FeatureFlag otherFlag = FeatureFlag.builder()
                .id(otherFlagId).key("other.flag").name("Other").enabled(true).rolloutPercentage(100).build();
        FeatureFlagOverride override = FeatureFlagOverride.builder()
                .id(overrideId).flag(otherFlag).overrideType(OverrideType.TIER).tierLevel(5).enabled(false).build();

        when(flagRepository.findById(flagId)).thenReturn(Optional.of(flag));
        when(overrideRepository.findById(overrideId)).thenReturn(Optional.of(override));

        assertThatThrownBy(() -> service.removeOverride(flagId, overrideId, adminPrincipal))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void bulkToggle_success() {
        FeatureFlag flag1 = FeatureFlag.builder()
                .id(UUID.randomUUID()).key("flag.1").name("Flag 1").enabled(true).rolloutPercentage(100).build();
        FeatureFlag flag2 = FeatureFlag.builder()
                .id(UUID.randomUUID()).key("flag.2").name("Flag 2").enabled(true).rolloutPercentage(100).build();

        when(flagRepository.findByKey("flag.1")).thenReturn(Optional.of(flag1));
        when(flagRepository.findByKey("flag.2")).thenReturn(Optional.of(flag2));
        when(flagRepository.save(any(FeatureFlag.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new BulkToggleRequest(List.of("flag.1", "flag.2"), false, "Maintenance");
        var result = service.bulkToggle(request, adminPrincipal);

        assertThat(result).hasSize(2);
    }

    @Test
    void listFlags_returnsPaginatedResults() {
        var pageable = PageRequest.of(0, 20);
        FeatureFlag flag = FeatureFlag.builder()
                .id(UUID.randomUUID()).key("test.flag").name("Test").enabled(true).rolloutPercentage(100).build();
        when(flagRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(flag)));

        Page<FeatureFlagResponse> result = service.listFlags(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().key()).isEqualTo("test.flag");
    }

    @Test
    void getFlag_success() {
        UUID flagId = UUID.randomUUID();
        FeatureFlag flag = FeatureFlag.builder()
                .id(flagId).key("test.flag").name("Test").enabled(true).rolloutPercentage(100).build();
        when(flagRepository.findById(flagId)).thenReturn(Optional.of(flag));

        FeatureFlagResponse response = service.getFlag(flagId);

        assertThat(response.key()).isEqualTo("test.flag");
    }

    @Test
    void getFlagByKey_success() {
        FeatureFlag flag = FeatureFlag.builder()
                .id(UUID.randomUUID()).key("test.flag").name("Test").enabled(true).rolloutPercentage(100).build();
        when(flagRepository.findByKey("test.flag")).thenReturn(Optional.of(flag));

        FeatureFlagResponse response = service.getFlagByKey("test.flag");

        assertThat(response.key()).isEqualTo("test.flag");
    }

    @Test
    void getFlagByKey_notFoundThrows() {
        when(flagRepository.findByKey("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getFlagByKey("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getFlag_nonExistentId_throwsNotFoundException() {
        UUID flagId = UUID.randomUUID();
        when(flagRepository.findById(flagId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getFlag(flagId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateFlag_nonExistentId_throwsNotFoundException() {
        UUID flagId = UUID.randomUUID();
        when(flagRepository.findById(flagId)).thenReturn(Optional.empty());
        var request = new UpdateFeatureFlagRequest("Name", null, null, null, null);

        assertThatThrownBy(() -> service.updateFlag(flagId, request, adminPrincipal))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void toggleFlag_enablesDisabledFlag() {
        UUID flagId = UUID.randomUUID();
        FeatureFlag flag = FeatureFlag.builder()
                .id(flagId).key("test.flag").name("Test").enabled(false).rolloutPercentage(100).build();
        when(flagRepository.findById(flagId)).thenReturn(Optional.of(flag));
        when(flagRepository.save(any(FeatureFlag.class))).thenReturn(flag);

        var request = new ToggleFeatureFlagRequest(true, "Re-enable");
        service.toggleFlag(flagId, request, adminPrincipal);

        assertThat(flag.isEnabled()).isTrue();
        verify(evaluationService).evictCache();
    }

    @Test
    void toggleFlag_disablesEnabledFlag() {
        UUID flagId = UUID.randomUUID();
        FeatureFlag flag = FeatureFlag.builder()
                .id(flagId).key("test.flag").name("Test").enabled(true).rolloutPercentage(100).build();
        when(flagRepository.findById(flagId)).thenReturn(Optional.of(flag));
        when(flagRepository.save(any(FeatureFlag.class))).thenReturn(flag);

        var request = new ToggleFeatureFlagRequest(false, "Emergency disable");
        service.toggleFlag(flagId, request, adminPrincipal);

        assertThat(flag.isEnabled()).isFalse();
        verify(evaluationService).evictCache();
    }

    @Test
    void addOverride_userOverride_savesCorrectly() {
        UUID flagId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        FeatureFlag flag = FeatureFlag.builder()
                .id(flagId).key("test.flag").name("Test").enabled(true).rolloutPercentage(100).build();
        when(flagRepository.findById(flagId)).thenReturn(Optional.of(flag));

        FeatureFlagOverride saved = FeatureFlagOverride.builder()
                .id(UUID.randomUUID()).flag(flag).overrideType(OverrideType.USER)
                .userId(userId).enabled(true).build();
        when(overrideRepository.save(any(FeatureFlagOverride.class))).thenReturn(saved);

        var request = new CreateOverrideRequest(OverrideType.USER, null, userId, true);
        var response = service.addOverride(flagId, request, adminPrincipal);

        assertThat(response.overrideType()).isEqualTo(OverrideType.USER);
        assertThat(response.userId()).isEqualTo(userId);
        verify(evaluationService).evictCache();
    }

    @Test
    void removeOverride_nonExistentOverride_throwsNotFoundException() {
        UUID flagId = UUID.randomUUID();
        UUID overrideId = UUID.randomUUID();
        FeatureFlag flag = FeatureFlag.builder()
                .id(flagId).key("test.flag").name("Test").enabled(true).rolloutPercentage(100).build();
        when(flagRepository.findById(flagId)).thenReturn(Optional.of(flag));
        when(overrideRepository.findById(overrideId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeOverride(flagId, overrideId, adminPrincipal))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateFlag_changesOnlyName() {
        UUID flagId = UUID.randomUUID();
        FeatureFlag flag = FeatureFlag.builder()
                .id(flagId).key("test.flag").name("Old Name").description("Old desc")
                .enabled(true).environment("dev").rolloutPercentage(100).build();
        when(flagRepository.findById(flagId)).thenReturn(Optional.of(flag));
        when(flagRepository.save(any(FeatureFlag.class))).thenReturn(flag);

        var request = new UpdateFeatureFlagRequest("New Name", null, null, null, null);
        service.updateFlag(flagId, request, adminPrincipal);

        assertThat(flag.getName()).isEqualTo("New Name");
        assertThat(flag.getDescription()).isEqualTo("Old desc");
    }

    @Test
    void bulkToggle_flagNotFoundThrows() {
        when(flagRepository.findByKey("missing.flag")).thenReturn(Optional.empty());

        var request = new BulkToggleRequest(List.of("missing.flag"), false, "Maintenance");

        assertThatThrownBy(() -> service.bulkToggle(request, adminPrincipal))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
