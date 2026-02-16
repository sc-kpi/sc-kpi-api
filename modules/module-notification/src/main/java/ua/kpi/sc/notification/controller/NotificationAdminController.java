package ua.kpi.sc.notification.controller;

import java.time.Instant;
import java.util.Map;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ua.kpi.sc.common.featureflag.FeatureFlag;
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.common.security.RequireTier;
import ua.kpi.sc.common.security.UserPrincipal;
import ua.kpi.sc.notification.dto.BroadcastRequest;
import ua.kpi.sc.notification.dto.NotificationResponse;
import ua.kpi.sc.notification.dto.NotificationStatsResponse;
import ua.kpi.sc.notification.service.NotificationAdminService;

/**
 * REST controller for admin notification management.
 *
 * @since 0.5.0
 */
@RestController
@RequestMapping("/api/v1/admin/notifications")
@Tag(name = "Notification Admin", description = "Admin notification management endpoints")
@RequireTier(CapabilityTier.ADMIN)
@FeatureFlag("notifications.enabled")
@RequiredArgsConstructor
public class NotificationAdminController {

    private final NotificationAdminService adminService;

    @GetMapping
    public Page<NotificationResponse> getAllNotifications(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return adminService.getAllNotifications(category, search, from, to, pageable);
    }

    @PostMapping("/broadcast")
    @FeatureFlag("notifications.admin-broadcast")
    public Map<String, String> broadcast(@Valid @RequestBody BroadcastRequest request,
                                         @AuthenticationPrincipal UserPrincipal requester) {
        adminService.broadcast(request, requester);
        return Map.of("status", "accepted");
    }

    @GetMapping("/stats")
    public NotificationStatsResponse getStats() {
        return adminService.getStats();
    }

    @DeleteMapping("/cleanup")
    public Map<String, Object> cleanup(
            @RequestParam(defaultValue = "90") int days,
            @AuthenticationPrincipal UserPrincipal requester) {
        int deleted = adminService.cleanup(days, requester);
        return Map.of("deleted", deleted, "olderThanDays", days);
    }
}
