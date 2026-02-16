package ua.kpi.sc.notification.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.kpi.sc.common.audit.AuditAction;
import ua.kpi.sc.common.audit.AuditEntityType;
import ua.kpi.sc.common.audit.AuditEventBuilder;
import ua.kpi.sc.common.audit.AuditPublisher;
import ua.kpi.sc.common.featureflag.FeatureFlag;
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.common.security.RequireTier;
import ua.kpi.sc.common.security.UserPrincipal;
import ua.kpi.sc.notification.dto.NotificationPreferenceResponse;
import ua.kpi.sc.notification.dto.UpdatePreferencesRequest;
import ua.kpi.sc.notification.service.NotificationPreferenceService;

/**
 * REST controller for user notification preference management.
 *
 * @since 0.5.0
 */
@RestController
@RequestMapping("/api/v1/notifications/preferences")
@Tag(name = "Notification Preferences", description = "User notification preference endpoints")
@RequireTier(CapabilityTier.BASIC)
@FeatureFlag("notifications.enabled")
@RequiredArgsConstructor
public class NotificationPreferencesController {

    private final NotificationPreferenceService preferenceService;
    private final AuditPublisher auditPublisher;

    @GetMapping
    public List<NotificationPreferenceResponse> getPreferences(@AuthenticationPrincipal UserPrincipal user) {
        return preferenceService.getPreferences(user.getId());
    }

    @PutMapping
    public List<NotificationPreferenceResponse> updatePreferences(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody UpdatePreferencesRequest request) {
        var result = preferenceService.updatePreferences(user.getId(), request);

        auditPublisher.publish(AuditEventBuilder.builder()
                .actor(user)
                .action(AuditAction.NOTIFICATION_PREFERENCES_UPDATED)
                .entityType(AuditEntityType.NOTIFICATION_PREFERENCES)
                .entityId(user.getId())
                .entityName(user.getEmail())
                .sourceModule("notification")
                .build());

        return result;
    }
}
