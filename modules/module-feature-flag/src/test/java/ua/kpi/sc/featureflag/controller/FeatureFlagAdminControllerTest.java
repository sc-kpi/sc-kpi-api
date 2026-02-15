package ua.kpi.sc.featureflag.controller;

import java.time.Instant;
import java.util.List;
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
import ua.kpi.sc.featureflag.dto.BulkToggleRequest;
import ua.kpi.sc.featureflag.dto.CreateFeatureFlagRequest;
import ua.kpi.sc.featureflag.dto.CreateOverrideRequest;
import ua.kpi.sc.featureflag.dto.FeatureFlagResponse;
import ua.kpi.sc.featureflag.dto.OverrideResponse;
import ua.kpi.sc.featureflag.dto.ToggleFeatureFlagRequest;
import ua.kpi.sc.featureflag.dto.UpdateFeatureFlagRequest;
import ua.kpi.sc.featureflag.entity.OverrideType;
import ua.kpi.sc.featureflag.service.FeatureFlagService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeatureFlagAdminControllerTest {

    @Mock
    private FeatureFlagService featureFlagService;

    @InjectMocks
    private FeatureFlagAdminController controller;

    private static final UUID FLAG_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID OVERRIDE_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");

    private UserPrincipal adminPrincipal() {
        return UserPrincipal.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000002"))
                .email("admin@kpi.ua").password("h").firstName("A").lastName("U")
                .tier(CapabilityTier.ADMIN).active(true).build();
    }

    private FeatureFlagResponse sampleFlagResponse() {
        return new FeatureFlagResponse(
                FLAG_ID, "test.flag", "Test Flag", "A test flag",
                true, null, 100, List.of(),
                adminPrincipal().getId(), Instant.now(), Instant.now()
        );
    }

    @Test
    void listFlags_delegatesToServiceWithPageable() {
        var pageable = PageRequest.of(0, 20);
        var page = new PageImpl<>(List.of(sampleFlagResponse()));
        when(featureFlagService.listFlags(pageable)).thenReturn(page);

        Page<FeatureFlagResponse> result = controller.listFlags(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().key()).isEqualTo("test.flag");
    }

    @Test
    void createFlag_validRequest_returnsCreatedAndDelegatesToService() {
        var principal = adminPrincipal();
        var request = new CreateFeatureFlagRequest("new.flag", "New Flag", "desc", true, null, 100);
        var expected = new FeatureFlagResponse(
                FLAG_ID, "new.flag", "New Flag", "desc",
                true, null, 100, List.of(),
                principal.getId(), Instant.now(), Instant.now()
        );
        when(featureFlagService.createFlag(request, principal)).thenReturn(expected);

        FeatureFlagResponse result = controller.createFlag(request, principal);

        assertThat(result.key()).isEqualTo("new.flag");
        verify(featureFlagService).createFlag(request, principal);
    }

    @Test
    void getFlag_existingId_returnsFlagResponse() {
        when(featureFlagService.getFlag(FLAG_ID)).thenReturn(sampleFlagResponse());

        FeatureFlagResponse result = controller.getFlag(FLAG_ID);

        assertThat(result.id()).isEqualTo(FLAG_ID);
        assertThat(result.key()).isEqualTo("test.flag");
    }

    @Test
    void updateFlag_validRequest_delegatesToService() {
        var principal = adminPrincipal();
        var request = new UpdateFeatureFlagRequest("Updated Name", "Updated desc", null, null, null);
        var expected = new FeatureFlagResponse(
                FLAG_ID, "test.flag", "Updated Name", "Updated desc",
                true, null, 100, List.of(),
                principal.getId(), Instant.now(), Instant.now()
        );
        when(featureFlagService.updateFlag(FLAG_ID, request, principal)).thenReturn(expected);

        FeatureFlagResponse result = controller.updateFlag(FLAG_ID, request, principal);

        assertThat(result.name()).isEqualTo("Updated Name");
        verify(featureFlagService).updateFlag(FLAG_ID, request, principal);
    }

    @Test
    void toggleFlag_delegatesToServiceWithRequest() {
        var principal = adminPrincipal();
        var request = new ToggleFeatureFlagRequest(false, "Disable for maintenance");
        var expected = new FeatureFlagResponse(
                FLAG_ID, "test.flag", "Test Flag", "A test flag",
                false, null, 100, List.of(),
                principal.getId(), Instant.now(), Instant.now()
        );
        when(featureFlagService.toggleFlag(FLAG_ID, request, principal)).thenReturn(expected);

        FeatureFlagResponse result = controller.toggleFlag(FLAG_ID, request, principal);

        assertThat(result.enabled()).isFalse();
        verify(featureFlagService).toggleFlag(FLAG_ID, request, principal);
    }

    @Test
    void deleteFlag_delegatesToService() {
        var principal = adminPrincipal();

        controller.deleteFlag(FLAG_ID, principal);

        verify(featureFlagService).deleteFlag(FLAG_ID, principal);
    }

    @Test
    void addOverride_validRequest_returnsCreated() {
        var principal = adminPrincipal();
        var request = new CreateOverrideRequest(OverrideType.TIER, 5, null, false);
        var expected = new OverrideResponse(OVERRIDE_ID, OverrideType.TIER, 5, null, false);
        when(featureFlagService.addOverride(FLAG_ID, request, principal)).thenReturn(expected);

        OverrideResponse result = controller.addOverride(FLAG_ID, request, principal);

        assertThat(result.overrideType()).isEqualTo(OverrideType.TIER);
        assertThat(result.tierLevel()).isEqualTo(5);
        verify(featureFlagService).addOverride(FLAG_ID, request, principal);
    }

    @Test
    void removeOverride_delegatesToService() {
        var principal = adminPrincipal();

        controller.removeOverride(FLAG_ID, OVERRIDE_ID, principal);

        verify(featureFlagService).removeOverride(FLAG_ID, OVERRIDE_ID, principal);
    }

    @Test
    void bulkToggle_delegatesToService() {
        var principal = adminPrincipal();
        var request = new BulkToggleRequest(List.of("flag.a", "flag.b"), false, "Maintenance");
        var expected = List.of(
                new FeatureFlagResponse(UUID.randomUUID(), "flag.a", "Flag A", null, false, null, 100,
                        List.of(), principal.getId(), Instant.now(), Instant.now()),
                new FeatureFlagResponse(UUID.randomUUID(), "flag.b", "Flag B", null, false, null, 100,
                        List.of(), principal.getId(), Instant.now(), Instant.now())
        );
        when(featureFlagService.bulkToggle(request, principal)).thenReturn(expected);

        var result = controller.bulkToggle(request, principal);

        assertThat(result).hasSize(2);
        verify(featureFlagService).bulkToggle(request, principal);
    }

    @Test
    void listFlags_emptyPage_returnsEmptyContent() {
        var pageable = PageRequest.of(0, 20);
        var page = new PageImpl<>(List.<FeatureFlagResponse>of());
        when(featureFlagService.listFlags(pageable)).thenReturn(page);

        Page<FeatureFlagResponse> result = controller.listFlags(pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void addOverride_userOverride_delegatesToService() {
        var principal = adminPrincipal();
        UUID userId = UUID.randomUUID();
        var request = new CreateOverrideRequest(OverrideType.USER, null, userId, true);
        var expected = new OverrideResponse(OVERRIDE_ID, OverrideType.USER, null, userId, true);
        when(featureFlagService.addOverride(FLAG_ID, request, principal)).thenReturn(expected);

        OverrideResponse result = controller.addOverride(FLAG_ID, request, principal);

        assertThat(result.overrideType()).isEqualTo(OverrideType.USER);
        assertThat(result.userId()).isEqualTo(userId);
    }
}
