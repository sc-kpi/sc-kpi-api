package ua.kpi.sc.user.adapter;

import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.common.security.UserDetailsPort;
import ua.kpi.sc.common.security.UserPrincipal;
import ua.kpi.sc.user.entity.User;
import ua.kpi.sc.user.repository.UserRepository;

/**
 * Adapter implementing {@link UserDetailsPort} using JPA persistence.
 * Bridges the auth module with the user module following hexagonal architecture.
 *
 * @since 0.1.0
 */
@Component
@RequiredArgsConstructor
public class UserDetailsAdapter implements UserDetailsPort {

    private final UserRepository userRepository;

    @Override
    public Optional<UserPrincipal> loadByEmail(String email) {
        return userRepository.findByEmail(email).map(this::toPrincipal);
    }

    @Override
    public Optional<UserPrincipal> loadById(UUID id) {
        return userRepository.findById(id).map(this::toPrincipal);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public UserPrincipal createUser(String email, String passwordHash,
                                    String firstName, String lastName) {
        User user = User.builder()
                .email(email)
                .passwordHash(passwordHash)
                .firstName(firstName)
                .lastName(lastName)
                .capabilityTier(CapabilityTier.BASIC)
                .active(true)
                .build();
        User saved = userRepository.save(user);
        return toPrincipal(saved);
    }

    @Override
    public void updatePassword(UUID userId, String passwordHash) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setPasswordHash(passwordHash);
            userRepository.save(user);
        });
    }

    private UserPrincipal toPrincipal(User user) {
        return UserPrincipal.builder()
                .id(user.getId())
                .email(user.getEmail())
                .password(user.getPasswordHash())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .tier(user.getCapabilityTier())
                .active(user.isActive())
                .build();
    }
}
