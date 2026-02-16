package ua.kpi.sc.notification.controller;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ua.kpi.sc.common.featureflag.FeatureFlag;
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.common.security.RequireTier;
import ua.kpi.sc.common.security.UserPrincipal;
import ua.kpi.sc.notification.config.NotificationProperties;
import ua.kpi.sc.notification.dto.MarkReadRequest;
import ua.kpi.sc.notification.dto.NotificationResponse;
import ua.kpi.sc.notification.dto.UnreadCountResponse;
import ua.kpi.sc.notification.service.NotificationService;
import ua.kpi.sc.notification.sse.SseEmitterRegistry;

/**
 * REST controller for user notification operations.
 *
 * @since 0.5.0
 */
@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "User notification endpoints")
@RequireTier(CapabilityTier.BASIC)
@FeatureFlag("notifications.enabled")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SseEmitterRegistry sseRegistry;
    private final NotificationProperties properties;

    @GetMapping
    public Page<NotificationResponse> getNotifications(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean read,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return notificationService.getUserNotifications(user.getId(), category, read, from, to, pageable);
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse getUnreadCount(@AuthenticationPrincipal UserPrincipal user) {
        return new UnreadCountResponse(notificationService.getUnreadCount(user.getId()));
    }

    @PatchMapping("/{id}/read")
    public void markAsRead(@AuthenticationPrincipal UserPrincipal user, @PathVariable UUID id) {
        notificationService.markAsRead(user.getId(), id);
    }

    @PatchMapping("/mark-read")
    public void markBatchAsRead(@AuthenticationPrincipal UserPrincipal user,
                                @Valid @RequestBody MarkReadRequest request) {
        notificationService.markBatchAsRead(user.getId(), request.ids());
    }

    @PatchMapping("/mark-all-read")
    public void markAllAsRead(@AuthenticationPrincipal UserPrincipal user) {
        notificationService.markAllAsRead(user.getId());
    }

    @GetMapping("/stream")
    @FeatureFlag("notifications.sse")
    public SseEmitter stream(@AuthenticationPrincipal UserPrincipal user) {
        return sseRegistry.register(user.getId(), properties.sseTimeoutMs());
    }
}
