package ua.kpi.sc.user.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.common.security.PartnerLevel;
import ua.kpi.sc.common.security.UserPrincipal;
import ua.kpi.sc.user.entity.PartnerMember;
import ua.kpi.sc.user.entity.User;
import ua.kpi.sc.user.repository.PartnerMemberRepository;
import ua.kpi.sc.user.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsAdapterTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PartnerMemberRepository partnerMemberRepository;

    @InjectMocks
    private UserDetailsAdapter adapter;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
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
                .build();
    }

    @Test
    void loadByEmailReturnsPrincipal() {
        when(userRepository.findByEmail("test@kpi.ua")).thenReturn(Optional.of(testUser()));
        when(partnerMemberRepository.findByUserId(USER_ID)).thenReturn(List.of());

        Optional<UserPrincipal> result = adapter.loadByEmail("test@kpi.ua");

        assertThat(result).isPresent();
        UserPrincipal principal = result.get();
        assertThat(principal.getId()).isEqualTo(USER_ID);
        assertThat(principal.getEmail()).isEqualTo("test@kpi.ua");
        assertThat(principal.getPassword()).isEqualTo("$2a$12$hashed");
        assertThat(principal.getFirstName()).isEqualTo("Test");
        assertThat(principal.getLastName()).isEqualTo("User");
        assertThat(principal.getTier()).isEqualTo(CapabilityTier.BASIC);
        assertThat(principal.isActive()).isTrue();
        assertThat(principal.getPartnerRoles()).isEmpty();
    }

    @Test
    void loadByEmailReturnsPrincipalWithPartnerRoles() {
        PartnerMember pm = PartnerMember.builder()
                .userId(USER_ID)
                .partnerId(PARTNER_ID)
                .level(PartnerLevel.FULL)
                .build();
        when(userRepository.findByEmail("test@kpi.ua")).thenReturn(Optional.of(testUser()));
        when(partnerMemberRepository.findByUserId(USER_ID)).thenReturn(List.of(pm));

        Optional<UserPrincipal> result = adapter.loadByEmail("test@kpi.ua");

        assertThat(result).isPresent();
        assertThat(result.get().getPartnerRoles()).containsEntry(PARTNER_ID, PartnerLevel.FULL);
    }

    @Test
    void loadByEmailReturnsEmptyForUnknown() {
        when(userRepository.findByEmail("unknown@kpi.ua")).thenReturn(Optional.empty());

        assertThat(adapter.loadByEmail("unknown@kpi.ua")).isEmpty();
    }

    @Test
    void loadByIdReturnsPrincipal() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser()));
        when(partnerMemberRepository.findByUserId(USER_ID)).thenReturn(List.of());

        assertThat(adapter.loadById(USER_ID)).isPresent();
    }

    @Test
    void existsByEmailDelegates() {
        when(userRepository.existsByEmail("test@kpi.ua")).thenReturn(true);

        assertThat(adapter.existsByEmail("test@kpi.ua")).isTrue();
    }

    @Test
    void createUserSavesAndReturnsPrincipal() {
        User saved = testUser();
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(partnerMemberRepository.findByUserId(USER_ID)).thenReturn(List.of());

        UserPrincipal principal = adapter.createUser("test@kpi.ua", "$2a$12$hashed", "Test", "User");

        assertThat(principal.getEmail()).isEqualTo("test@kpi.ua");
        assertThat(principal.getTier()).isEqualTo(CapabilityTier.BASIC);
    }

    @Test
    void updatePasswordUpdatesUser() {
        when(userRepository.updatePasswordById(USER_ID, "$2a$12$newHash")).thenReturn(1);

        adapter.updatePassword(USER_ID, "$2a$12$newHash");

        verify(userRepository).updatePasswordById(USER_ID, "$2a$12$newHash");
    }

    @Test
    void assignPartnerLevelCreatesNew() {
        when(partnerMemberRepository.findByUserIdAndPartnerId(USER_ID, PARTNER_ID)).thenReturn(Optional.empty());
        when(partnerMemberRepository.save(any(PartnerMember.class))).thenAnswer(inv -> inv.getArgument(0));

        adapter.assignPartnerLevel(USER_ID, PARTNER_ID, PartnerLevel.FULL, UUID.randomUUID());

        verify(partnerMemberRepository).save(any(PartnerMember.class));
    }

    @Test
    void assignPartnerLevelUpdatesExisting() {
        PartnerMember existing = PartnerMember.builder()
                .userId(USER_ID).partnerId(PARTNER_ID).level(PartnerLevel.BASIC).build();
        when(partnerMemberRepository.findByUserIdAndPartnerId(USER_ID, PARTNER_ID)).thenReturn(Optional.of(existing));
        when(partnerMemberRepository.save(any(PartnerMember.class))).thenReturn(existing);

        adapter.assignPartnerLevel(USER_ID, PARTNER_ID, PartnerLevel.FULL, UUID.randomUUID());

        assertThat(existing.getLevel()).isEqualTo(PartnerLevel.FULL);
    }

    @Test
    void removePartnerLevelDelegates() {
        adapter.removePartnerLevel(USER_ID, PARTNER_ID);

        verify(partnerMemberRepository).deleteByUserIdAndPartnerId(USER_ID, PARTNER_ID);
    }

    @Test
    void updateUserTierUpdatesAndSaves() {
        User user = testUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        adapter.updateUserTier(USER_ID, CapabilityTier.ADMIN);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUserActiveStatusUpdatesAndSaves() {
        User user = testUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        adapter.updateUserActiveStatus(USER_ID, false);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUserProfileUpdatesAndReturnsPrincipal() {
        User user = testUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(partnerMemberRepository.findByUserId(USER_ID)).thenReturn(List.of());

        Optional<UserPrincipal> result = adapter.updateUserProfile(USER_ID, "New", "Name");

        assertThat(result).isPresent();
    }

    @Test
    void updateUserProfileNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThat(adapter.updateUserProfile(USER_ID, "New", "Name")).isEmpty();
    }
}
