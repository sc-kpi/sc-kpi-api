package ua.kpi.sc.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import ua.kpi.sc.auth.dto.AuthUserResponse.PartnerRoleDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.kpi.sc.auth.config.JwtProperties;
import ua.kpi.sc.auth.dto.AuthUserResponse;
import ua.kpi.sc.auth.dto.LoginRequest;
import ua.kpi.sc.auth.dto.RegisterRequest;
import ua.kpi.sc.auth.entity.RefreshToken;
import ua.kpi.sc.auth.repository.RefreshTokenRepository;
import ua.kpi.sc.auth.security.JwtTokenProvider;
import ua.kpi.sc.common.exception.ConflictException;
import ua.kpi.sc.common.util.InputSanitizer;
import ua.kpi.sc.common.exception.ResourceNotFoundException;
import ua.kpi.sc.common.exception.UnauthorizedException;
import ua.kpi.sc.common.security.UserDetailsPort;
import ua.kpi.sc.common.security.UserPrincipal;

/**
 * Core authentication service handling register, login, logout, refresh, and user retrieval.
 *
 * @since 0.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserDetailsPort userDetailsPort;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    @Transactional
    public AuthResult register(RegisterRequest request) {
        if (userDetailsPort.existsByEmail(request.email())) {
            throw new ConflictException("Email already registered");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        String firstName = InputSanitizer.stripHtml(request.firstName());
        String lastName = InputSanitizer.stripHtml(request.lastName());
        UserPrincipal principal = userDetailsPort.createUser(
                request.email(), passwordHash, firstName, lastName);

        return createAuthResult(principal);
    }

    @Transactional
    public AuthResult login(LoginRequest request) {
        UserPrincipal principal = userDetailsPort.loadByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), principal.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (!principal.isEnabled()) {
            throw new UnauthorizedException("Account is disabled");
        }

        return createAuthResult(principal);
    }

    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    @Transactional
    public AuthResult refresh(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw new UnauthorizedException("Refresh token is required");
        }

        String tokenHash = hashToken(refreshTokenValue);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(storedToken);
            throw new UnauthorizedException("Refresh token expired");
        }

        // Rotate: delete old token
        refreshTokenRepository.delete(storedToken);

        UserPrincipal principal = userDetailsPort.loadById(storedToken.getUserId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        return createAuthResult(principal);
    }

    @Transactional(readOnly = true)
    public AuthUserResponse getMe(UUID userId) {
        UserPrincipal principal = userDetailsPort.loadById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return toResponse(principal);
    }

    private AuthResult createAuthResult(UserPrincipal principal) {
        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        String refreshTokenValue = jwtTokenProvider.generateRefreshToken();

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(principal.getId())
                .tokenHash(hashToken(refreshTokenValue))
                .expiresAt(Instant.now().plusMillis(jwtProperties.getRefreshExpiration()))
                .build();
        refreshTokenRepository.save(refreshToken);

        return new AuthResult(accessToken, refreshTokenValue, toResponse(principal));
    }

    private AuthUserResponse toResponse(UserPrincipal principal) {
        var partnerRoles = principal.getPartnerRoles().entrySet().stream()
                .map(e -> new PartnerRoleDto(e.getKey(), e.getValue().getValue()))
                .toList();

        return new AuthUserResponse(
                principal.getId(),
                principal.getEmail(),
                principal.getFirstName(),
                principal.getLastName(),
                principal.getTier().getLevel(),
                partnerRoles
        );
    }

    static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Carries the result of a successful authentication operation.
     */
    public record AuthResult(String accessToken, String refreshToken, AuthUserResponse user) {
    }
}
