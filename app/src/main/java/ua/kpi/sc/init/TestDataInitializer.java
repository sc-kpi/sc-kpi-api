package ua.kpi.sc.init;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.user.entity.User;
import ua.kpi.sc.user.repository.UserRepository;

@Component
@Profile({"dev", "test"})
@RequiredArgsConstructor
public class TestDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedUser("admin@test.kpi.ua", "TestAdmin123!", "Test", "Admin", CapabilityTier.ADMIN);
        seedUser("basic@test.kpi.ua", "TestBasic123!", "Test", "Basic", CapabilityTier.BASIC);
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
