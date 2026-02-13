package ua.kpi.sc.user.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import ua.kpi.sc.common.exception.BadRequestException;
import ua.kpi.sc.common.exception.ConflictException;
import ua.kpi.sc.common.exception.ForbiddenException;
import ua.kpi.sc.common.exception.ResourceNotFoundException;
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.common.security.PartnerLevel;
import ua.kpi.sc.common.security.UserPrincipal;
import ua.kpi.sc.user.dto.AssignPartnerLevelRequest;
import ua.kpi.sc.user.dto.CreateUserRequest;
import ua.kpi.sc.user.dto.UpdateUserRequest;
import ua.kpi.sc.user.dto.UserResponse;
import ua.kpi.sc.user.entity.PartnerMember;
import ua.kpi.sc.user.entity.User;
import ua.kpi.sc.user.repository.PartnerMemberRepository;
import ua.kpi.sc.user.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PartnerMemberRepository partnerMemberRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID PARTNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    private User testUser() {
        return User.builder()
                .id(USER_ID)
                .email("test@kpi.ua")
                .passwordHash("$2a$12$hashed")
                .firstName("Test")
                .lastName("User")
                .capabilityTier(CapabilityTier.BASIC)
                .active(true)
                .createdAt(Instant.now())
                .build();
    }

    private UserPrincipal adminPrincipal() {
        return UserPrincipal.builder()
                .id(ADMIN_ID)
                .email("admin@kpi.ua")
                .password("hashed")
                .firstName("Admin")
                .lastName("User")
                .tier(CapabilityTier.ADMIN)
                .active(true)
                .build();
    }

    private UserPrincipal seniorPrincipal() {
        return UserPrincipal.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000003"))
                .email("senior@kpi.ua")
                .password("hashed")
                .firstName("Senior")
                .lastName("User")
                .tier(CapabilityTier.SENIOR)
                .active(true)
                .build();
    }

    @Test
    void listUsers_returnsPage() {
        var pageable = PageRequest.of(0, 20);
        var page = new PageImpl<>(List.of(testUser()));
        when(userRepository.findAll(pageable)).thenReturn(page);

        var result = userService.listUsers(pageable, null, null, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().email()).isEqualTo("test@kpi.ua");
    }

    @Test
    void listUsersWithSearchFilter() {
        var pageable = PageRequest.of(0, 20);
        var page = new PageImpl<>(List.of(testUser()));
        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        var result = userService.listUsers(pageable, "test", null, null);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void listUsersWithTierFilter() {
        var pageable = PageRequest.of(0, 20);
        var page = new PageImpl<>(List.of(testUser()));
        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        var result = userService.listUsers(pageable, null, 1, null);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void listUsersWithActiveFilter() {
        var pageable = PageRequest.of(0, 20);
        var page = new PageImpl<>(List.of(testUser()));
        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        var result = userService.listUsers(pageable, null, null, true);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getUserById_returnsFullResponse() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser()));
        when(partnerMemberRepository.findByUserId(USER_ID)).thenReturn(List.of());

        UserResponse response = userService.getUserById(USER_ID);

        assertThat(response.id()).isEqualTo(USER_ID);
        assertThat(response.email()).isEqualTo("test@kpi.ua");
        assertThat(response.partnerRoles()).isEmpty();
    }

    @Test
    void getUserById_notFound_throws() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createUser_success() {
        var request = new CreateUserRequest("new@kpi.ua", "password123", "New", "User", 1);
        when(userRepository.existsByEmail("new@kpi.ua")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$12$encoded");
        User saved = testUser();
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(partnerMemberRepository.findByUserId(USER_ID)).thenReturn(List.of());

        UserResponse response = userService.createUser(request, adminPrincipal());

        assertThat(response.email()).isEqualTo("test@kpi.ua");
    }

    @Test
    void createUser_duplicateEmail_throwsConflict() {
        var request = new CreateUserRequest("dup@kpi.ua", "password123", "Dup", "User", 1);
        when(userRepository.existsByEmail("dup@kpi.ua")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request, adminPrincipal()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateUser_success() {
        var request = new UpdateUserRequest("Updated", "Name");
        User user = testUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(partnerMemberRepository.findByUserId(USER_ID)).thenReturn(List.of());

        UserResponse response = userService.updateUser(USER_ID, request, adminPrincipal());

        assertThat(response).isNotNull();
    }

    @Test
    void updateTier_nonAdminCannotSetAdmin_throwsForbidden() {
        User user = testUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.updateTier(USER_ID, 5, seniorPrincipal()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateTier_adminCanSetAdmin() {
        User user = testUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(partnerMemberRepository.findByUserId(USER_ID)).thenReturn(List.of());

        UserResponse response = userService.updateTier(USER_ID, 5, adminPrincipal());

        assertThat(response).isNotNull();
    }

    @Test
    void updateStatus_cannotDeactivateSelf_throwsBadRequest() {
        assertThatThrownBy(() -> userService.updateStatus(ADMIN_ID, false, adminPrincipal()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot deactivate your own account");
    }

    @Test
    void updateStatus_success() {
        User user = testUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(partnerMemberRepository.findByUserId(USER_ID)).thenReturn(List.of());

        UserResponse response = userService.updateStatus(USER_ID, false, adminPrincipal());

        assertThat(response).isNotNull();
    }

    @Test
    void deleteUser_cannotDeleteSelf_throwsBadRequest() {
        assertThatThrownBy(() -> userService.deleteUser(ADMIN_ID, adminPrincipal()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot delete your own account");
    }

    @Test
    void deleteUser_deactivatesUser() {
        User user = testUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.deleteUser(USER_ID, adminPrincipal());

        verify(userRepository).save(any(User.class));
    }

    @Test
    void changePassword_success() {
        User user = testUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPass", "$2a$12$hashed")).thenReturn(true);
        when(passwordEncoder.encode("newPass123")).thenReturn("$2a$12$newHash");
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.changePassword(USER_ID, "oldPass", "newPass123");

        verify(userRepository).save(any(User.class));
    }

    @Test
    void changePassword_wrongCurrentPassword_throwsBadRequest() {
        User user = testUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPass", "$2a$12$hashed")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(USER_ID, "wrongPass", "newPass123"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Current password is incorrect");
    }

    @Test
    void changePassword_userNotFound_throws() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changePassword(USER_ID, "old", "new12345"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void changePassword_tooLongPassword_throwsBadRequest() {
        String longPassword = "A".repeat(73);

        assertThatThrownBy(() -> userService.changePassword(USER_ID, "oldPass", longPassword))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Password must be between 8 and 72 characters");
    }

    @Test
    void changePassword_tooShortPassword_throwsBadRequest() {
        assertThatThrownBy(() -> userService.changePassword(USER_ID, "oldPass", "short"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Password must be between 8 and 72 characters");
    }

    @Test
    void assignPartnerLevel_success() {
        var request = new AssignPartnerLevelRequest(PARTNER_ID, "full");
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(partnerMemberRepository.findByUserIdAndPartnerId(USER_ID, PARTNER_ID)).thenReturn(Optional.empty());
        PartnerMember saved = PartnerMember.builder()
                .userId(USER_ID).partnerId(PARTNER_ID).level(PartnerLevel.FULL)
                .assignedAt(Instant.now()).assignedBy(ADMIN_ID).build();
        when(partnerMemberRepository.save(any(PartnerMember.class))).thenReturn(saved);

        var response = userService.assignPartnerLevel(USER_ID, request, adminPrincipal());

        assertThat(response.partnerId()).isEqualTo(PARTNER_ID);
        assertThat(response.level()).isEqualTo("full");
    }

    @Test
    void assignPartnerLevel_updatesExisting() {
        var request = new AssignPartnerLevelRequest(PARTNER_ID, "documents");
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        PartnerMember existing = PartnerMember.builder()
                .userId(USER_ID).partnerId(PARTNER_ID).level(PartnerLevel.BASIC)
                .assignedAt(Instant.now()).build();
        when(partnerMemberRepository.findByUserIdAndPartnerId(USER_ID, PARTNER_ID)).thenReturn(Optional.of(existing));
        when(partnerMemberRepository.save(any(PartnerMember.class))).thenReturn(existing);

        var response = userService.assignPartnerLevel(USER_ID, request, adminPrincipal());

        assertThat(response).isNotNull();
    }

    @Test
    void assignPartnerLevel_invalidLevel_throwsBadRequest() {
        var request = new AssignPartnerLevelRequest(PARTNER_ID, "invalid");
        when(userRepository.existsById(USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> userService.assignPartnerLevel(USER_ID, request, adminPrincipal()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid partner level");
    }

    @Test
    void assignPartnerLevel_userNotFound_throws() {
        var request = new AssignPartnerLevelRequest(PARTNER_ID, "full");
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> userService.assignPartnerLevel(USER_ID, request, adminPrincipal()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removePartnerLevel_success() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);

        userService.removePartnerLevel(USER_ID, PARTNER_ID);

        verify(partnerMemberRepository).deleteByUserIdAndPartnerId(USER_ID, PARTNER_ID);
    }

    @Test
    void removePartnerLevel_userNotFound_throws() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> userService.removePartnerLevel(USER_ID, PARTNER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
