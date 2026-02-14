package ua.kpi.sc.user.adapter;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.common.security.PartnerLevel;
import ua.kpi.sc.common.security.UserDetailsPort;
import ua.kpi.sc.common.security.UserPrincipal;
import ua.kpi.sc.user.entity.PartnerMember;
import ua.kpi.sc.user.entity.User;
import ua.kpi.sc.user.repository.PartnerMemberRepository;
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
    private final PartnerMemberRepository partnerMemberRepository;

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
    @Transactional
    public void updatePassword(UUID userId, String passwordHash) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setPasswordHash(passwordHash);
            userRepository.save(user);
        });
    }

    @Override
    @Transactional
    public void assignPartnerLevel(UUID userId, UUID partnerId, PartnerLevel level, UUID assignedBy) {
        Optional<PartnerMember> existing = partnerMemberRepository.findByUserIdAndPartnerId(userId, partnerId);
        if (existing.isPresent()) {
            PartnerMember member = existing.get();
            member.setLevel(level);
            partnerMemberRepository.save(member);
        } else {
            PartnerMember member = PartnerMember.builder()
                    .userId(userId)
                    .partnerId(partnerId)
                    .level(level)
                    .assignedBy(assignedBy)
                    .build();
            partnerMemberRepository.save(member);
        }
    }

    @Override
    @Transactional
    public void removePartnerLevel(UUID userId, UUID partnerId) {
        partnerMemberRepository.deleteByUserIdAndPartnerId(userId, partnerId);
    }

    @Override
    @Transactional
    public void updateUserTier(UUID userId, CapabilityTier tier) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setCapabilityTier(tier);
            userRepository.save(user);
        });
    }

    @Override
    @Transactional
    public void updateUserActiveStatus(UUID userId, boolean active) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setActive(active);
            userRepository.save(user);
        });
    }

    @Override
    @Transactional
    public Optional<UserPrincipal> updateUserProfile(UUID userId, String firstName, String lastName) {
        return userRepository.findById(userId).map(user -> {
            user.setFirstName(firstName);
            user.setLastName(lastName);
            User saved = userRepository.save(user);
            return toPrincipal(saved);
        });
    }

    private UserPrincipal toPrincipal(User user) {
        Map<UUID, PartnerLevel> partnerRoles = partnerMemberRepository.findByUserId(user.getId())
                .stream()
                .collect(Collectors.toMap(PartnerMember::getPartnerId, PartnerMember::getLevel));

        return UserPrincipal.builder()
                .id(user.getId())
                .email(user.getEmail())
                .password(user.getPasswordHash())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .tier(user.getCapabilityTier())
                .active(user.isActive())
                .partnerRoles(partnerRoles)
                .build();
    }
}
