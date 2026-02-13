package ua.kpi.sc.user.service;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.kpi.sc.common.exception.BadRequestException;
import ua.kpi.sc.common.exception.ConflictException;
import ua.kpi.sc.common.exception.ForbiddenException;
import ua.kpi.sc.common.exception.ResourceNotFoundException;
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.common.security.PartnerLevel;
import ua.kpi.sc.common.security.UserPrincipal;
import ua.kpi.sc.common.util.InputSanitizer;
import ua.kpi.sc.common.util.PasswordValidator;
import ua.kpi.sc.user.dto.AssignPartnerLevelRequest;
import ua.kpi.sc.user.dto.CreateUserRequest;
import ua.kpi.sc.user.dto.PartnerMemberResponse;
import ua.kpi.sc.user.dto.UpdateUserRequest;
import ua.kpi.sc.user.dto.UserListResponse;
import ua.kpi.sc.user.dto.UserResponse;
import ua.kpi.sc.user.entity.PartnerMember;
import ua.kpi.sc.user.entity.User;
import ua.kpi.sc.user.repository.PartnerMemberRepository;
import ua.kpi.sc.user.repository.UserRepository;
import ua.kpi.sc.user.repository.UserSpecifications;

/**
 * Service for user management operations (admin CRUD).
 *
 * @since 0.2.0
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PartnerMemberRepository partnerMemberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<UserListResponse> listUsers(Pageable pageable, String search, Integer tier, Boolean active) {
        boolean hasFilters = (search != null && !search.isBlank()) || tier != null || active != null;
        if (!hasFilters) {
            return userRepository.findAll(pageable).map(this::toListResponse);
        }

        Specification<User> spec = Specification.where(UserSpecifications.always());
        if (search != null && !search.isBlank()) {
            spec = spec.and(UserSpecifications.searchByKeyword(search));
        }
        if (tier != null) {
            spec = spec.and(UserSpecifications.hasTier(tier));
        }
        if (active != null) {
            spec = spec.and(UserSpecifications.isActive(active));
        }
        return userRepository.findAll(spec, pageable).map(this::toListResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return toFullResponse(user);
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request, UserPrincipal requester) {
        PasswordValidator.validateLength(request.password());
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already registered");
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(InputSanitizer.stripHtml(request.firstName()))
                .lastName(InputSanitizer.stripHtml(request.lastName()))
                .capabilityTier(CapabilityTier.fromLevel(request.tier()))
                .active(true)
                .build();
        User saved = userRepository.save(user);
        return toFullResponse(saved);
    }

    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request, UserPrincipal requester) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        user.setFirstName(InputSanitizer.stripHtml(request.firstName()));
        user.setLastName(InputSanitizer.stripHtml(request.lastName()));
        User saved = userRepository.save(user);
        return toFullResponse(saved);
    }

    @Transactional
    public UserResponse updateTier(UUID id, int tier, UserPrincipal requester) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        if (tier >= CapabilityTier.ADMIN.getLevel() && !requester.getTier().isAtLeast(CapabilityTier.ADMIN)) {
            throw new ForbiddenException("Only admins can assign admin tier");
        }

        user.setCapabilityTier(CapabilityTier.fromLevel(tier));
        User saved = userRepository.save(user);
        return toFullResponse(saved);
    }

    @Transactional
    public UserResponse updateStatus(UUID id, boolean active, UserPrincipal requester) {
        if (requester.getId().equals(id) && !active) {
            throw new BadRequestException("Cannot deactivate your own account");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        user.setActive(active);
        User saved = userRepository.save(user);
        return toFullResponse(saved);
    }

    @Transactional
    public void deleteUser(UUID id, UserPrincipal requester) {
        if (requester.getId().equals(id)) {
            throw new BadRequestException("Cannot delete your own account");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        user.setActive(false);
        userRepository.save(user);
    }

    @Transactional
    public PartnerMemberResponse assignPartnerLevel(UUID userId, AssignPartnerLevelRequest request, UserPrincipal requester) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", userId);
        }

        PartnerLevel level = parsePartnerLevel(request.level());

        var existing = partnerMemberRepository.findByUserIdAndPartnerId(userId, request.partnerId());
        PartnerMember member;
        if (existing.isPresent()) {
            member = existing.get();
            member.setLevel(level);
        } else {
            member = PartnerMember.builder()
                    .userId(userId)
                    .partnerId(request.partnerId())
                    .level(level)
                    .assignedBy(requester.getId())
                    .build();
        }
        PartnerMember saved = partnerMemberRepository.save(member);
        return new PartnerMemberResponse(saved.getPartnerId(), saved.getLevel().getValue(), saved.getAssignedAt());
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        PasswordValidator.validateLength(newPassword);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void removePartnerLevel(UUID userId, UUID partnerId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", userId);
        }
        partnerMemberRepository.deleteByUserIdAndPartnerId(userId, partnerId);
    }

    private PartnerLevel parsePartnerLevel(String value) {
        for (PartnerLevel level : PartnerLevel.values()) {
            if (level.getValue().equalsIgnoreCase(value)) {
                return level;
            }
        }
        throw new BadRequestException("Invalid partner level: " + value + ". Valid values: full, documents, basic");
    }

    private UserListResponse toListResponse(User user) {
        return new UserListResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getCapabilityTier().getLevel(),
                user.isActive()
        );
    }

    private UserResponse toFullResponse(User user) {
        var partnerRoles = partnerMemberRepository.findByUserId(user.getId()).stream()
                .map(pm -> new PartnerMemberResponse(pm.getPartnerId(), pm.getLevel().getValue(), pm.getAssignedAt()))
                .toList();

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getCapabilityTier().getLevel(),
                user.isActive(),
                user.getCreatedAt(),
                partnerRoles
        );
    }
}
