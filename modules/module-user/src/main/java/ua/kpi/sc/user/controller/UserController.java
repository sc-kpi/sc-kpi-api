package ua.kpi.sc.user.controller;

import java.util.UUID;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ua.kpi.sc.common.featureflag.FeatureFlag;
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.common.security.RequireMfa;
import ua.kpi.sc.common.security.RequireTier;
import ua.kpi.sc.common.security.UserPrincipal;
import ua.kpi.sc.user.dto.AssignPartnerLevelRequest;
import ua.kpi.sc.user.dto.ChangePasswordRequest;
import ua.kpi.sc.user.dto.CreateUserRequest;
import ua.kpi.sc.user.dto.PartnerMemberResponse;
import ua.kpi.sc.user.dto.UpdateStatusRequest;
import ua.kpi.sc.user.dto.UpdateTierRequest;
import ua.kpi.sc.user.dto.UpdateUserRequest;
import ua.kpi.sc.user.dto.UserListResponse;
import ua.kpi.sc.user.dto.UserResponse;
import ua.kpi.sc.user.service.UserService;

/**
 * REST controller for user profile and account management.
 *
 * @since 0.1.0
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User management endpoints")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @RequireTier(CapabilityTier.SENIOR)
    public Page<UserListResponse> listUsers(
            Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer tier,
            @RequestParam(required = false) Boolean active) {
        return userService.listUsers(pageable, search, tier, active);
    }

    @GetMapping("/me")
    @RequireTier(CapabilityTier.BASIC)
    public UserResponse getMe(@AuthenticationPrincipal UserPrincipal requester) {
        return userService.getUserById(requester.getId());
    }

    @PatchMapping("/me")
    @RequireTier(CapabilityTier.BASIC)
    public UserResponse updateSelf(
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal UserPrincipal requester) {
        return userService.updateUser(requester.getId(), request, requester);
    }

    @PatchMapping("/me/password")
    @RequireTier(CapabilityTier.BASIC)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserPrincipal requester) {
        userService.changePassword(requester.getId(), request.currentPassword(), request.newPassword());
    }

    @GetMapping("/{id}")
    @RequireTier(CapabilityTier.SENIOR)
    public UserResponse getUser(@PathVariable UUID id) {
        return userService.getUserById(id);
    }

    @PostMapping
    @RequireTier(CapabilityTier.ADMIN)
    @RequireMfa
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal UserPrincipal requester) {
        return userService.createUser(request, requester);
    }

    @PatchMapping("/{id}")
    @RequireTier(CapabilityTier.ADMIN)
    public UserResponse updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal UserPrincipal requester) {
        return userService.updateUser(id, request, requester);
    }

    @PatchMapping("/{id}/tier")
    @RequireTier(CapabilityTier.ADMIN)
    @RequireMfa
    public UserResponse updateTier(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTierRequest request,
            @AuthenticationPrincipal UserPrincipal requester) {
        return userService.updateTier(id, request.tier(), requester);
    }

    @PatchMapping("/{id}/status")
    @RequireTier(CapabilityTier.ADMIN)
    @RequireMfa
    public UserResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request,
            @AuthenticationPrincipal UserPrincipal requester) {
        return userService.updateStatus(id, request.active(), requester);
    }

    @DeleteMapping("/{id}")
    @RequireTier(CapabilityTier.ADMIN)
    @RequireMfa
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal requester) {
        userService.deleteUser(id, requester);
    }

    @PostMapping("/{id}/partners")
    @RequireTier(CapabilityTier.ADMIN)
    @RequireMfa
    @ResponseStatus(HttpStatus.CREATED)
    @FeatureFlag("user.partner-levels")
    public PartnerMemberResponse assignPartnerLevel(
            @PathVariable UUID id,
            @Valid @RequestBody AssignPartnerLevelRequest request,
            @AuthenticationPrincipal UserPrincipal requester) {
        return userService.assignPartnerLevel(id, request, requester);
    }

    @DeleteMapping("/{id}/partners/{partnerId}")
    @RequireTier(CapabilityTier.ADMIN)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @FeatureFlag("user.partner-levels")
    public void removePartnerLevel(
            @PathVariable UUID id,
            @PathVariable UUID partnerId) {
        userService.removePartnerLevel(id, partnerId);
    }
}
