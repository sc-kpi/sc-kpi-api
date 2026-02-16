package ua.kpi.sc.user.adapter;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.common.security.UserQueryPort;
import ua.kpi.sc.user.entity.User;
import ua.kpi.sc.user.repository.UserRepository;

/**
 * Adapter implementing {@link UserQueryPort} using the user module's repository.
 *
 * @since 0.5.0
 */
@Component
public class UserQueryAdapter implements UserQueryPort {

    private final UserRepository userRepository;

    public UserQueryAdapter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<UUID> findActiveUserIdsByMinimumTier(CapabilityTier minimumTier) {
        return userRepository.findAll((root, query, cb) -> cb.and(
                cb.isTrue(root.get("active")),
                cb.greaterThanOrEqualTo(root.get("capabilityTier"), minimumTier)
        )).stream().map(User::getId).toList();
    }

    @Override
    public List<UUID> findAllActiveUserIds() {
        return userRepository.findAll((root, query, cb) ->
                cb.isTrue(root.get("active"))
        ).stream().map(User::getId).toList();
    }

    @Override
    public String getEmailById(UUID userId) {
        return userRepository.findById(userId)
                .map(User::getEmail)
                .orElse(null);
    }
}
