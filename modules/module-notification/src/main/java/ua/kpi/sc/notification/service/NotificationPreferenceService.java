package ua.kpi.sc.notification.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.kpi.sc.common.notification.NotificationCategory;
import ua.kpi.sc.common.notification.NotificationChannel;
import ua.kpi.sc.notification.dto.NotificationPreferenceResponse;
import ua.kpi.sc.notification.dto.UpdatePreferencesRequest;
import ua.kpi.sc.notification.entity.NotificationPreferenceEntity;
import ua.kpi.sc.notification.repository.NotificationPreferenceRepository;

/**
 * Service for managing user notification preferences.
 *
 * @since 0.5.0
 */
@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    @Transactional(readOnly = true)
    public List<NotificationPreferenceResponse> getPreferences(UUID userId) {
        List<NotificationPreferenceEntity> existing = preferenceRepository.findByUserId(userId);
        if (existing.isEmpty()) {
            return getDefaultPreferences();
        }
        return existing.stream().map(this::toResponse).toList();
    }

    @Transactional
    public List<NotificationPreferenceResponse> updatePreferences(UUID userId, UpdatePreferencesRequest request) {
        List<NotificationPreferenceResponse> results = new ArrayList<>();
        for (var update : request.preferences()) {
            var entity = preferenceRepository
                    .findByUserIdAndCategoryAndChannel(userId, update.category(), update.channel())
                    .orElseGet(() -> NotificationPreferenceEntity.builder()
                            .userId(userId)
                            .category(update.category())
                            .channel(update.channel())
                            .build());
            entity.setEnabled(update.enabled());
            preferenceRepository.save(entity);
            results.add(toResponse(entity));
        }
        return results;
    }

    @Transactional(readOnly = true)
    public boolean isChannelEnabled(UUID userId, NotificationCategory category, NotificationChannel channel) {
        return preferenceRepository
                .findByUserIdAndCategoryAndChannel(userId, category.name(), channel.name())
                .map(NotificationPreferenceEntity::isEnabled)
                .orElse(getDefaultEnabled(category, channel));
    }

    private boolean getDefaultEnabled(NotificationCategory category, NotificationChannel channel) {
        // All channels enabled by default, except EMAIL for FEATURE_FLAG
        if (channel == NotificationChannel.EMAIL && category == NotificationCategory.FEATURE_FLAG) {
            return false;
        }
        return true;
    }

    private List<NotificationPreferenceResponse> getDefaultPreferences() {
        List<NotificationPreferenceResponse> defaults = new ArrayList<>();
        for (NotificationCategory category : NotificationCategory.values()) {
            for (NotificationChannel channel : NotificationChannel.values()) {
                defaults.add(new NotificationPreferenceResponse(
                        category.name(), channel.name(), getDefaultEnabled(category, channel)));
            }
        }
        return defaults;
    }

    private NotificationPreferenceResponse toResponse(NotificationPreferenceEntity entity) {
        return new NotificationPreferenceResponse(entity.getCategory(), entity.getChannel(), entity.isEnabled());
    }
}
