package ua.kpi.sc.user.adapter;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.common.security.UserPrincipal;
import ua.kpi.sc.user.entity.User;
import ua.kpi.sc.user.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsAdapterTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsAdapter adapter;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

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
    }

    @Test
    void loadByEmailReturnsEmptyForUnknown() {
        when(userRepository.findByEmail("unknown@kpi.ua")).thenReturn(Optional.empty());

        assertThat(adapter.loadByEmail("unknown@kpi.ua")).isEmpty();
    }

    @Test
    void loadByIdReturnsPrincipal() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser()));

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

        UserPrincipal principal = adapter.createUser("test@kpi.ua", "$2a$12$hashed", "Test", "User");

        assertThat(principal.getEmail()).isEqualTo("test@kpi.ua");
        assertThat(principal.getTier()).isEqualTo(CapabilityTier.BASIC);
    }
}
