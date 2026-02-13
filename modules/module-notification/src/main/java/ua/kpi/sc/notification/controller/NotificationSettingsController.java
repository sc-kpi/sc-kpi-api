package ua.kpi.sc.notification.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.common.security.RequireTier;

/**
 * REST controller for user notification preference management.
 *
 * @since 0.1.0
 */
@RestController
@RequestMapping("/api/v1/notifications/settings")
@Tag(name = "Notifications", description = "Notification settings endpoints")
@RequireTier(CapabilityTier.BASIC)
public class NotificationSettingsController {
}
