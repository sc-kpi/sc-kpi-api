package ua.kpi.sc.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.kpi.sc.common.notification.NotificationCategory;
import ua.kpi.sc.common.notification.NotificationChannel;
import ua.kpi.sc.notification.dto.UpdatePreferencesRequest;
import ua.kpi.sc.notification.entity.NotificationPreferenceEntity;
import ua.kpi.sc.notification.repository.NotificationPreferenceRepository;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceTest {

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @InjectMocks
    private NotificationPreferenceService service;

    @Nested
    class GetPreferences {

        @Test
        void returnsDefaults_whenNoneExist() {
            UUID userId = UUID.randomUUID();
            when(preferenceRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

            var prefs = service.getPreferences(userId);

            // 4 categories x 2 channels = 8 default preferences
            assertThat(prefs).hasSize(8);
        }

        @Test
        void returnsExisting_whenPresent() {
            UUID userId = UUID.randomUUID();
            var entity = NotificationPreferenceEntity.builder()
                    .userId(userId).category("SECURITY").channel("IN_APP").enabled(true).build();
            when(preferenceRepository.findByUserId(userId)).thenReturn(List.of(entity));

            var prefs = service.getPreferences(userId);

            assertThat(prefs).hasSize(1);
            assertThat(prefs.getFirst().category()).isEqualTo("SECURITY");
        }

        @Test
        void defaultPreferences_disableEmailForFeatureFlag() {
            UUID userId = UUID.randomUUID();
            when(preferenceRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

            var prefs = service.getPreferences(userId);

            var featureFlagEmail = prefs.stream()
                    .filter(p -> "FEATURE_FLAG".equals(p.category()) && "EMAIL".equals(p.channel()))
                    .findFirst();
            assertThat(featureFlagEmail).isPresent();
            assertThat(featureFlagEmail.get().enabled()).isFalse();
        }

        @Test
        void defaultPreferences_enableInAppForAllCategories() {
            UUID userId = UUID.randomUUID();
            when(preferenceRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

            var prefs = service.getPreferences(userId);

            var inAppPrefs = prefs.stream()
                    .filter(p -> "IN_APP".equals(p.channel()))
                    .toList();
            assertThat(inAppPrefs).allMatch(p -> p.enabled());
        }

        @Test
        void returnsMultipleExisting() {
            UUID userId = UUID.randomUUID();
            var entity1 = NotificationPreferenceEntity.builder()
                    .userId(userId).category("SECURITY").channel("IN_APP").enabled(true).build();
            var entity2 = NotificationPreferenceEntity.builder()
                    .userId(userId).category("ADMIN").channel("EMAIL").enabled(false).build();
            when(preferenceRepository.findByUserId(userId)).thenReturn(List.of(entity1, entity2));

            var prefs = service.getPreferences(userId);

            assertThat(prefs).hasSize(2);
            assertThat(prefs.get(0).category()).isEqualTo("SECURITY");
            assertThat(prefs.get(1).channel()).isEqualTo("EMAIL");
            assertThat(prefs.get(1).enabled()).isFalse();
        }
    }

    @Nested
    class UpdatePreferences {

        @Test
        void createsNewPreference_whenNoneExists() {
            UUID userId = UUID.randomUUID();
            when(preferenceRepository.findByUserIdAndCategoryAndChannel(userId, "SECURITY", "IN_APP"))
                    .thenReturn(Optional.empty());
            when(preferenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var update = new UpdatePreferencesRequest.PreferenceUpdate("SECURITY", "IN_APP", false);
            var request = new UpdatePreferencesRequest(List.of(update));

            var results = service.updatePreferences(userId, request);

            assertThat(results).hasSize(1);
            assertThat(results.getFirst().category()).isEqualTo("SECURITY");
            assertThat(results.getFirst().channel()).isEqualTo("IN_APP");
            assertThat(results.getFirst().enabled()).isFalse();
            verify(preferenceRepository).save(any());
        }

        @Test
        void updatesExistingPreference() {
            UUID userId = UUID.randomUUID();
            var existing = NotificationPreferenceEntity.builder()
                    .userId(userId).category("ADMIN").channel("EMAIL").enabled(true).build();
            when(preferenceRepository.findByUserIdAndCategoryAndChannel(userId, "ADMIN", "EMAIL"))
                    .thenReturn(Optional.of(existing));
            when(preferenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var update = new UpdatePreferencesRequest.PreferenceUpdate("ADMIN", "EMAIL", false);
            var request = new UpdatePreferencesRequest(List.of(update));

            var results = service.updatePreferences(userId, request);

            assertThat(results).hasSize(1);
            assertThat(results.getFirst().enabled()).isFalse();
            assertThat(existing.isEnabled()).isFalse();
        }

        @Test
        void handlesMultipleUpdates() {
            UUID userId = UUID.randomUUID();
            when(preferenceRepository.findByUserIdAndCategoryAndChannel(any(), any(), any()))
                    .thenReturn(Optional.empty());
            when(preferenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var update1 = new UpdatePreferencesRequest.PreferenceUpdate("SECURITY", "IN_APP", true);
            var update2 = new UpdatePreferencesRequest.PreferenceUpdate("SYSTEM", "EMAIL", false);
            var request = new UpdatePreferencesRequest(List.of(update1, update2));

            var results = service.updatePreferences(userId, request);

            assertThat(results).hasSize(2);
        }
    }

    @Nested
    class IsChannelEnabled {

        @Test
        void returnsDefault_whenNoPreference() {
            UUID userId = UUID.randomUUID();
            when(preferenceRepository.findByUserIdAndCategoryAndChannel(any(), any(), any()))
                    .thenReturn(Optional.empty());

            // EMAIL for FEATURE_FLAG defaults to false
            assertThat(service.isChannelEnabled(userId, NotificationCategory.FEATURE_FLAG, NotificationChannel.EMAIL))
                    .isFalse();

            // IN_APP for SECURITY defaults to true
            assertThat(service.isChannelEnabled(userId, NotificationCategory.SECURITY, NotificationChannel.IN_APP))
                    .isTrue();
        }

        @Test
        void returnsStoredValue() {
            UUID userId = UUID.randomUUID();
            var entity = NotificationPreferenceEntity.builder()
                    .userId(userId).category("SECURITY").channel("IN_APP").enabled(false).build();
            when(preferenceRepository.findByUserIdAndCategoryAndChannel(userId, "SECURITY", "IN_APP"))
                    .thenReturn(Optional.of(entity));

            assertThat(service.isChannelEnabled(userId, NotificationCategory.SECURITY, NotificationChannel.IN_APP))
                    .isFalse();
        }

        @Test
        void returnsTrue_forEmailAndNonFeatureFlagCategory() {
            UUID userId = UUID.randomUUID();
            when(preferenceRepository.findByUserIdAndCategoryAndChannel(any(), any(), any()))
                    .thenReturn(Optional.empty());

            assertThat(service.isChannelEnabled(userId, NotificationCategory.SECURITY, NotificationChannel.EMAIL))
                    .isTrue();
            assertThat(service.isChannelEnabled(userId, NotificationCategory.ADMIN, NotificationChannel.EMAIL))
                    .isTrue();
            assertThat(service.isChannelEnabled(userId, NotificationCategory.SYSTEM, NotificationChannel.EMAIL))
                    .isTrue();
        }

        @Test
        void returnsStoredTrue_evenForFeatureFlagEmail() {
            UUID userId = UUID.randomUUID();
            var entity = NotificationPreferenceEntity.builder()
                    .userId(userId).category("FEATURE_FLAG").channel("EMAIL").enabled(true).build();
            when(preferenceRepository.findByUserIdAndCategoryAndChannel(userId, "FEATURE_FLAG", "EMAIL"))
                    .thenReturn(Optional.of(entity));

            assertThat(service.isChannelEnabled(userId, NotificationCategory.FEATURE_FLAG, NotificationChannel.EMAIL))
                    .isTrue();
        }
    }
}
