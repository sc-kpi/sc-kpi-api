package ua.kpi.sc.user.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ua.kpi.sc.common.security.CapabilityTier;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController controller;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PARTNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    private UserPrincipal adminPrincipal() {
        return UserPrincipal.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000002"))
                .email("admin@kpi.ua").password("h").firstName("A").lastName("U")
                .tier(CapabilityTier.ADMIN).active(true).build();
    }

    private UserPrincipal basicPrincipal() {
        return UserPrincipal.builder()
                .id(USER_ID)
                .email("basic@kpi.ua").password("h").firstName("B").lastName("U")
                .tier(CapabilityTier.BASIC).active(true).build();
    }

    @Test
    void listUsers_delegatesToServiceWithFilters() {
        var pageable = PageRequest.of(0, 20);
        var page = new PageImpl<>(List.of(
                new UserListResponse(USER_ID, "e@kpi.ua", "F", "L", 1, true)
        ));
        when(userService.listUsers(pageable, "test", 1, true)).thenReturn(page);

        var result = controller.listUsers(pageable, "test", 1, true);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void listUsers_delegatesToServiceWithNullFilters() {
        var pageable = PageRequest.of(0, 20);
        var page = new PageImpl<>(List.of(
                new UserListResponse(USER_ID, "e@kpi.ua", "F", "L", 1, true)
        ));
        when(userService.listUsers(pageable, null, null, null)).thenReturn(page);

        var result = controller.listUsers(pageable, null, null, null);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getUser_delegatesToService() {
        var response = new UserResponse(USER_ID, "e@kpi.ua", "F", "L", 1, true, Instant.now(), List.of());
        when(userService.getUserById(USER_ID)).thenReturn(response);

        var result = controller.getUser(USER_ID);

        assertThat(result.id()).isEqualTo(USER_ID);
    }

    @Test
    void getMe_delegatesToService() {
        var principal = basicPrincipal();
        var response = new UserResponse(USER_ID, "basic@kpi.ua", "B", "U", 1, true, Instant.now(), List.of());
        when(userService.getUserById(USER_ID)).thenReturn(response);

        var result = controller.getMe(principal);

        assertThat(result.id()).isEqualTo(USER_ID);
    }

    @Test
    void updateSelf_delegatesToService() {
        var request = new UpdateUserRequest("NewFirst", "NewLast");
        var principal = basicPrincipal();
        var response = new UserResponse(USER_ID, "basic@kpi.ua", "NewFirst", "NewLast", 1, true, Instant.now(), List.of());
        when(userService.updateUser(USER_ID, request, principal)).thenReturn(response);

        var result = controller.updateSelf(request, principal);

        assertThat(result.firstName()).isEqualTo("NewFirst");
    }

    @Test
    void changePassword_delegatesToService() {
        var request = new ChangePasswordRequest("oldPass123", "newPass456");
        var principal = basicPrincipal();

        controller.changePassword(request, principal);

        verify(userService).changePassword(USER_ID, "oldPass123", "newPass456");
    }

    @Test
    void createUser_delegatesToService() {
        var request = new CreateUserRequest("e@kpi.ua", "password1", "F", "L", 1);
        var response = new UserResponse(USER_ID, "e@kpi.ua", "F", "L", 1, true, Instant.now(), List.of());
        var principal = adminPrincipal();
        when(userService.createUser(request, principal)).thenReturn(response);

        var result = controller.createUser(request, principal);

        assertThat(result.email()).isEqualTo("e@kpi.ua");
    }

    @Test
    void updateUser_delegatesToService() {
        var request = new UpdateUserRequest("F", "L");
        var response = new UserResponse(USER_ID, "e@kpi.ua", "F", "L", 1, true, Instant.now(), List.of());
        var principal = adminPrincipal();
        when(userService.updateUser(USER_ID, request, principal)).thenReturn(response);

        var result = controller.updateUser(USER_ID, request, principal);

        assertThat(result).isNotNull();
    }

    @Test
    void updateTier_delegatesToService() {
        var request = new UpdateTierRequest(3);
        var response = new UserResponse(USER_ID, "e@kpi.ua", "F", "L", 3, true, Instant.now(), List.of());
        var principal = adminPrincipal();
        when(userService.updateTier(USER_ID, 3, principal)).thenReturn(response);

        var result = controller.updateTier(USER_ID, request, principal);

        assertThat(result.capabilityTier()).isEqualTo(3);
    }

    @Test
    void updateStatus_delegatesToService() {
        var request = new UpdateStatusRequest(false);
        var response = new UserResponse(USER_ID, "e@kpi.ua", "F", "L", 1, false, Instant.now(), List.of());
        var principal = adminPrincipal();
        when(userService.updateStatus(USER_ID, false, principal)).thenReturn(response);

        var result = controller.updateStatus(USER_ID, request, principal);

        assertThat(result.active()).isFalse();
    }

    @Test
    void deleteUser_delegatesToService() {
        var principal = adminPrincipal();

        controller.deleteUser(USER_ID, principal);

        verify(userService).deleteUser(USER_ID, principal);
    }

    @Test
    void assignPartnerLevel_delegatesToService() {
        var request = new AssignPartnerLevelRequest(PARTNER_ID, "full");
        var response = new PartnerMemberResponse(PARTNER_ID, "full", Instant.now());
        var principal = adminPrincipal();
        when(userService.assignPartnerLevel(USER_ID, request, principal)).thenReturn(response);

        var result = controller.assignPartnerLevel(USER_ID, request, principal);

        assertThat(result.partnerId()).isEqualTo(PARTNER_ID);
    }

    @Test
    void removePartnerLevel_delegatesToService() {
        controller.removePartnerLevel(USER_ID, PARTNER_ID);

        verify(userService).removePartnerLevel(USER_ID, PARTNER_ID);
    }
}
