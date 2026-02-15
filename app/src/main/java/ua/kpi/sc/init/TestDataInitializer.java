package ua.kpi.sc.init;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.featureflag.entity.FeatureFlag;
import ua.kpi.sc.featureflag.repository.FeatureFlagRepository;
import ua.kpi.sc.user.entity.User;
import ua.kpi.sc.user.repository.UserRepository;

@Component
@Profile({"dev", "test"})
@RequiredArgsConstructor
public class TestDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FeatureFlagRepository featureFlagRepository;

    @Override
    @Transactional
    public void run(String... args) {
        seedUser("admin@test.kpi.ua", "TestAdmin123!", "Test", "Admin", CapabilityTier.ADMIN);
        seedUser("basic@test.kpi.ua", "TestBasic123!", "Test", "Basic", CapabilityTier.BASIC);
        seedFeatureFlags();
    }

    private void seedFeatureFlags() {
        seedFlag("audit.system", "Audit System", "Centralized audit logging system", true);
        seedFlag("auth.oauth2.google", "Google OAuth", "Google OAuth 2.0 authentication", true);
        seedFlag("auth.password-reset", "Password Reset", "Password reset via email", true);
        seedFlag("auth.registration", "Registration", "New user registration", true);
        seedFlag("notifications.telegram", "Telegram Notifications", "Telegram bot integration", false);
        seedFlag("engagements.clubs", "Clubs", "Student clubs feature", true);
        seedFlag("engagements.projects", "Projects", "Student projects feature", true);
        seedFlag("council.departments", "Departments", "Council departments feature", true);
        seedFlag("documents.management", "Documents", "Document management feature", true);
        seedFlag("user.partner-levels", "Partner Levels", "Partner level assignment", true);
    }

    private void seedFlag(String key, String name, String description, boolean enabled) {
        if (featureFlagRepository.existsByKey(key)) {
            return;
        }
        featureFlagRepository.save(FeatureFlag.builder()
                .key(key)
                .name(name)
                .description(description)
                .enabled(enabled)
                .rolloutPercentage(100)
                .build());
    }

    private void seedUser(String email, String password, String firstName, String lastName,
                          CapabilityTier tier) {
        if (userRepository.existsByEmail(email)) {
            return;
        }
        userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .firstName(firstName)
                .lastName(lastName)
                .capabilityTier(tier)
                .active(true)
                .build());
    }
}
