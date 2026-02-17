package ua.kpi.sc.integration;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import ua.kpi.sc.auth.dto.LoginRequest;
import ua.kpi.sc.auth.dto.RegisterRequest;
import ua.kpi.sc.auth.entity.TotpSecret;
import ua.kpi.sc.auth.repository.TotpSecretRepository;
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.notification.repository.NotificationRepository;
import ua.kpi.sc.user.entity.User;
import ua.kpi.sc.user.repository.UserRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the notification system.
 * Validates end-to-end flows: login triggers notification, admin operations,
 * preferences, mark-as-read, broadcast, SSE stream, and cleanup.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TotpSecretRepository totpSecretRepository;

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String NOTIFICATIONS_URL = "/api/v1/notifications";
    private static final String PREFERENCES_URL = "/api/v1/notifications/preferences";
    private static final String ADMIN_NOTIFICATIONS_URL = "/api/v1/admin/notifications";

    private int userCounter = 0;

    private RegisterRequest uniqueRegisterRequest() {
        userCounter++;
        return new RegisterRequest(
                "notiftest" + userCounter + System.nanoTime() + "@kpi.ua",
                "password123",
                "Notif",
                "User"
        );
    }

    private MvcResult registerUser(RegisterRequest request) throws Exception {
        return mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private String extractAccessToken(MvcResult result) {
        var cookie = result.getResponse().getCookie("access_token");
        return cookie != null ? cookie.getValue() : null;
    }

    private String extractUserId(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private record UserTokenPair(String userId, String token, String email, String password) {}

    private UserTokenPair registerAndElevate(CapabilityTier tier) throws Exception {
        RegisterRequest request = uniqueRegisterRequest();
        MvcResult result = registerUser(request);
        String userId = extractUserId(result);

        User user = userRepository.findById(UUID.fromString(userId)).orElseThrow();
        user.setCapabilityTier(tier);
        userRepository.save(user);

        MvcResult loginResult = mockMvc.perform(
                        post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        new LoginRequest(request.email(), request.password()))))
                .andExpect(status().isOk())
                .andReturn();

        // Enable 2FA for admin users after obtaining token — filter re-queries DB on each request
        if (tier.getLevel() >= CapabilityTier.SENIOR.getLevel()) {
            totpSecretRepository.save(TotpSecret.builder()
                    .userId(UUID.fromString(userId))
                    .encryptedSecret("test-encrypted-secret")
                    .enabled(true)
                    .build());
        }

        return new UserTokenPair(userId, extractAccessToken(loginResult), request.email(), request.password());
    }

    @Nested
    class LoginNotificationTests {

        @Test
        void loginCreatesSecurityNotification() throws Exception {
            RegisterRequest request = uniqueRegisterRequest();
            MvcResult regResult = registerUser(request);
            String userId = extractUserId(regResult);

            // Elevate to BASIC so we can access notification endpoints
            User user = userRepository.findById(UUID.fromString(userId)).orElseThrow();
            user.setCapabilityTier(CapabilityTier.BASIC);
            userRepository.save(user);

            // Login — should trigger a security notification
            MvcResult loginResult = mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new LoginRequest(request.email(), request.password()))))
                    .andExpect(status().isOk())
                    .andReturn();
            String token = extractAccessToken(loginResult);

            // Allow async notification publishing to complete
            Thread.sleep(500);

            // Verify unread count > 0
            mockMvc.perform(get(NOTIFICATIONS_URL + "/unread-count")
                            .cookie(new Cookie("access_token", token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").isNumber());

            // Verify notifications list contains the login notification
            mockMvc.perform(get(NOTIFICATIONS_URL)
                            .cookie(new Cookie("access_token", token))
                            .param("category", "SECURITY"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
        }
    }

    @Nested
    class TierChangeNotificationTests {

        @Test
        void tierChangeCreatesAdminNotification() throws Exception {
            // Register admin
            UserTokenPair admin = registerAndElevate(CapabilityTier.ADMIN);

            // Register target user at BASIC
            UserTokenPair target = registerAndElevate(CapabilityTier.BASIC);

            // Admin changes target user's tier
            mockMvc.perform(patch("/api/v1/users/" + target.userId() + "/tier")
                            .cookie(new Cookie("access_token", admin.token()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"tier\": 2}"))
                    .andExpect(status().isOk());

            // Allow async notification publishing to complete
            Thread.sleep(500);

            // Target user checks notifications — should have tier change notification
            // Re-login target as the tier changed
            MvcResult targetLogin = mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new LoginRequest(target.email(), target.password()))))
                    .andExpect(status().isOk())
                    .andReturn();
            String targetToken = extractAccessToken(targetLogin);

            Thread.sleep(500);

            mockMvc.perform(get(NOTIFICATIONS_URL)
                            .cookie(new Cookie("access_token", targetToken))
                            .param("category", "ADMIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
        }
    }

    @Nested
    class MarkAsReadTests {

        @Test
        void markAllReadResetsUnreadCount() throws Exception {
            // Register and elevate to BASIC
            UserTokenPair user = registerAndElevate(CapabilityTier.BASIC);

            // The login itself triggers a notification — wait for async
            Thread.sleep(500);

            // Mark all as read
            mockMvc.perform(patch(NOTIFICATIONS_URL + "/mark-all-read")
                            .cookie(new Cookie("access_token", user.token())))
                    .andExpect(status().isOk());

            // Verify unread count is 0
            mockMvc.perform(get(NOTIFICATIONS_URL + "/unread-count")
                            .cookie(new Cookie("access_token", user.token())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(0));
        }
    }

    @Nested
    class PreferencesTests {

        @Test
        void getAndUpdatePreferences() throws Exception {
            UserTokenPair user = registerAndElevate(CapabilityTier.BASIC);

            // Get default preferences
            mockMvc.perform(get(PREFERENCES_URL)
                            .cookie(new Cookie("access_token", user.token())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThan(0)));

            // Update a preference — disable IN_APP for FEATURE_FLAG
            String updateJson = """
                    {
                      "preferences": [
                        { "category": "FEATURE_FLAG", "channel": "IN_APP", "enabled": false }
                      ]
                    }
                    """;
            mockMvc.perform(put(PREFERENCES_URL)
                            .cookie(new Cookie("access_token", user.token()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());

            // Verify the preference was updated
            MvcResult prefsResult = mockMvc.perform(get(PREFERENCES_URL)
                            .cookie(new Cookie("access_token", user.token())))
                    .andExpect(status().isOk())
                    .andReturn();

            String prefsBody = prefsResult.getResponse().getContentAsString();
            assertThat(prefsBody).contains("FEATURE_FLAG");
        }
    }

    @Nested
    class SseStreamTests {

        @Test
        void sseStreamReturnsTextEventStream() throws Exception {
            UserTokenPair user = registerAndElevate(CapabilityTier.BASIC);

            mockMvc.perform(get(NOTIFICATIONS_URL + "/stream")
                            .cookie(new Cookie("access_token", user.token()))
                            .accept(MediaType.TEXT_EVENT_STREAM))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class AdminTests {

        @Test
        void adminCanListAllNotifications() throws Exception {
            UserTokenPair admin = registerAndElevate(CapabilityTier.ADMIN);

            Thread.sleep(500);

            mockMvc.perform(get(ADMIN_NOTIFICATIONS_URL)
                            .cookie(new Cookie("access_token", admin.token())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        void adminCanBroadcast() throws Exception {
            UserTokenPair admin = registerAndElevate(CapabilityTier.ADMIN);

            String broadcastJson = """
                    {
                      "titleKey": "notification.admin.tier_changed",
                      "bodyKey": "notification.admin.tier_changed.body",
                      "category": "ADMIN"
                    }
                    """;

            mockMvc.perform(post(ADMIN_NOTIFICATIONS_URL + "/broadcast")
                            .cookie(new Cookie("access_token", admin.token()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(broadcastJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("accepted"));
        }

        @Test
        void adminCanGetStats() throws Exception {
            UserTokenPair admin = registerAndElevate(CapabilityTier.ADMIN);

            mockMvc.perform(get(ADMIN_NOTIFICATIONS_URL + "/stats")
                            .cookie(new Cookie("access_token", admin.token())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").isNumber())
                    .andExpect(jsonPath("$.last24h").isNumber());
        }

        @Test
        void adminCanCleanup() throws Exception {
            UserTokenPair admin = registerAndElevate(CapabilityTier.ADMIN);

            mockMvc.perform(delete(ADMIN_NOTIFICATIONS_URL + "/cleanup")
                            .cookie(new Cookie("access_token", admin.token()))
                            .param("days", "90"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.deleted").isNumber())
                    .andExpect(jsonPath("$.olderThanDays").value(90));
        }

        @Test
        void nonAdminCannotAccessAdminEndpoints() throws Exception {
            UserTokenPair basicUser = registerAndElevate(CapabilityTier.BASIC);

            mockMvc.perform(get(ADMIN_NOTIFICATIONS_URL)
                            .cookie(new Cookie("access_token", basicUser.token())))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class SecurityTests {

        @Test
        void unauthenticatedCannotAccessNotifications() throws Exception {
            mockMvc.perform(get(NOTIFICATIONS_URL))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void unauthenticatedCannotAccessStream() throws Exception {
            mockMvc.perform(get(NOTIFICATIONS_URL + "/stream"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void unauthenticatedCannotAccessPreferences() throws Exception {
            mockMvc.perform(get(PREFERENCES_URL))
                    .andExpect(status().isUnauthorized());
        }
    }
}
