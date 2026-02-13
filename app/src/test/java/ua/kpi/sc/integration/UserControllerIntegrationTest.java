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
import ua.kpi.sc.auth.dto.RegisterRequest;
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.user.entity.User;
import ua.kpi.sc.user.repository.UserRepository;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String USERS_URL = "/api/v1/users";

    private int userCounter = 0;

    private RegisterRequest uniqueRegisterRequest() {
        userCounter++;
        return new RegisterRequest(
                "usertest" + userCounter + System.nanoTime() + "@kpi.ua",
                "password123",
                "Test",
                "User"
        );
    }

    private RegisterRequest uniqueRegisterRequest(String firstName, String lastName) {
        userCounter++;
        return new RegisterRequest(
                "usertest" + userCounter + System.nanoTime() + "@kpi.ua",
                "password123",
                firstName,
                lastName
        );
    }

    private MvcResult registerUser(RegisterRequest request) throws Exception {
        return mockMvc.perform(
                        post(REGISTER_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private String extractAccessToken(MvcResult result) {
        Cookie cookie = result.getResponse().getCookie("access_token");
        return cookie != null ? cookie.getValue() : null;
    }

    private String extractUserId(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private String registerAndGetAdminToken() throws Exception {
        RegisterRequest request = uniqueRegisterRequest();
        MvcResult result = registerUser(request);
        String userId = extractUserId(result);

        User user = userRepository.findById(UUID.fromString(userId)).orElseThrow();
        user.setCapabilityTier(CapabilityTier.ADMIN);
        userRepository.save(user);

        // Re-login to get a token with updated tier
        MvcResult loginResult = mockMvc.perform(
                        post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        new ua.kpi.sc.auth.dto.LoginRequest(request.email(), request.password()))))
                .andExpect(status().isOk())
                .andReturn();

        return extractAccessToken(loginResult);
    }

    private record UserTokenPair(String userId, String token, String email, String password) {}

    private UserTokenPair registerAndGetAdminTokenWithDetails() throws Exception {
        RegisterRequest request = uniqueRegisterRequest();
        MvcResult result = registerUser(request);
        String userId = extractUserId(result);

        User user = userRepository.findById(UUID.fromString(userId)).orElseThrow();
        user.setCapabilityTier(CapabilityTier.ADMIN);
        userRepository.save(user);

        MvcResult loginResult = mockMvc.perform(
                        post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        new ua.kpi.sc.auth.dto.LoginRequest(request.email(), request.password()))))
                .andExpect(status().isOk())
                .andReturn();

        return new UserTokenPair(userId, extractAccessToken(loginResult), request.email(), request.password());
    }

    private String registerTargetUser() throws Exception {
        RegisterRequest request = uniqueRegisterRequest();
        MvcResult result = registerUser(request);
        return extractUserId(result);
    }

    private String registerTargetUserWithName(String firstName, String lastName) throws Exception {
        RegisterRequest request = uniqueRegisterRequest(firstName, lastName);
        MvcResult result = registerUser(request);
        return extractUserId(result);
    }

    @Nested
    class ListUsersTests {

        @Test
        void listUsersReturns200() throws Exception {
            String adminToken = registerAndGetAdminToken();

            mockMvc.perform(get(USERS_URL)
                            .cookie(new Cookie("access_token", adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").isNumber());
        }

        @Test
        void listUsersWithPaginationParams() throws Exception {
            String adminToken = registerAndGetAdminToken();
            registerTargetUser();

            mockMvc.perform(get(USERS_URL + "?page=0&size=1")
                            .cookie(new Cookie("access_token", adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.totalPages").isNumber());
        }

        @Test
        void listUsersWithoutAuthReturns401() throws Exception {
            mockMvc.perform(get(USERS_URL))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class GetUserByIdTests {

        @Test
        void getUserByIdReturns200() throws Exception {
            String adminToken = registerAndGetAdminToken();
            String targetUserId = registerTargetUser();

            mockMvc.perform(get(USERS_URL + "/" + targetUserId)
                            .cookie(new Cookie("access_token", adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(targetUserId))
                    .andExpect(jsonPath("$.email").isString())
                    .andExpect(jsonPath("$.firstName").isString())
                    .andExpect(jsonPath("$.lastName").isString())
                    .andExpect(jsonPath("$.capabilityTier").isNumber())
                    .andExpect(jsonPath("$.active").isBoolean())
                    .andExpect(jsonPath("$.partnerRoles").isArray());
        }

        @Test
        void getNonExistentUserReturns404() throws Exception {
            String adminToken = registerAndGetAdminToken();

            mockMvc.perform(get(USERS_URL + "/" + UUID.randomUUID())
                            .cookie(new Cookie("access_token", adminToken)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void getUserWithoutAuthReturns401() throws Exception {
            mockMvc.perform(get(USERS_URL + "/" + UUID.randomUUID()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class CreateUserTests {

        @Test
        void createUserReturns201() throws Exception {
            String adminToken = registerAndGetAdminToken();

            mockMvc.perform(post(USERS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"newcreated%d@kpi.ua","password":"password123","firstName":"Created","lastName":"User","tier":1}
                                    """.formatted(System.nanoTime()))
                            .cookie(new Cookie("access_token", adminToken)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isString())
                    .andExpect(jsonPath("$.email").isString())
                    .andExpect(jsonPath("$.active").value(true));
        }

        @Test
        void createUserDuplicateEmailReturns409() throws Exception {
            String adminToken = registerAndGetAdminToken();
            String email = "dup" + System.nanoTime() + "@kpi.ua";

            // Create first user
            mockMvc.perform(post(USERS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"%s","password":"password123","firstName":"A","lastName":"B","tier":1}
                                    """.formatted(email))
                            .cookie(new Cookie("access_token", adminToken)))
                    .andExpect(status().isCreated());

            // Create duplicate
            mockMvc.perform(post(USERS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"%s","password":"password123","firstName":"C","lastName":"D","tier":1}
                                    """.formatted(email))
                            .cookie(new Cookie("access_token", adminToken)))
                    .andExpect(status().isConflict());
        }

        @Test
        void createUserInvalidEmailReturns400() throws Exception {
            String adminToken = registerAndGetAdminToken();

            mockMvc.perform(post(USERS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"not-an-email","password":"password123","firstName":"A","lastName":"B","tier":1}
                                    """)
                            .cookie(new Cookie("access_token", adminToken)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void createUserWithoutAuthReturns401() throws Exception {
            mockMvc.perform(post(USERS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"x@kpi.ua","password":"password123","firstName":"A","lastName":"B","tier":1}
                                    """))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class DeleteUserTests {

        @Test
        void deleteUserReturns204() throws Exception {
            String adminToken = registerAndGetAdminToken();
            String targetUserId = registerTargetUser();

            mockMvc.perform(delete(USERS_URL + "/" + targetUserId)
                            .cookie(new Cookie("access_token", adminToken)))
                    .andExpect(status().isNoContent());
        }

        @Test
        void deletedUserIsDeactivated() throws Exception {
            String adminToken = registerAndGetAdminToken();
            String targetUserId = registerTargetUser();

            mockMvc.perform(delete(USERS_URL + "/" + targetUserId)
                            .cookie(new Cookie("access_token", adminToken)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get(USERS_URL + "/" + targetUserId)
                            .cookie(new Cookie("access_token", adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(false));
        }

        @Test
        void deleteNonExistentUserReturns404() throws Exception {
            String adminToken = registerAndGetAdminToken();

            mockMvc.perform(delete(USERS_URL + "/" + UUID.randomUUID())
                            .cookie(new Cookie("access_token", adminToken)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void deleteSelfReturns400() throws Exception {
            var admin = registerAndGetAdminTokenWithDetails();

            mockMvc.perform(delete(USERS_URL + "/" + admin.userId())
                            .cookie(new Cookie("access_token", admin.token())))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class AssignPartnerLevelTests {

        @Test
        void assignPartnerLevelReturns201() throws Exception {
            String adminToken = registerAndGetAdminToken();
            String targetUserId = registerTargetUser();
            UUID partnerId = UUID.randomUUID();

            mockMvc.perform(post(USERS_URL + "/" + targetUserId + "/partners")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"partnerId":"%s","level":"basic"}
                                    """.formatted(partnerId))
                            .cookie(new Cookie("access_token", adminToken)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.partnerId").value(partnerId.toString()))
                    .andExpect(jsonPath("$.level").value("basic"))
                    .andExpect(jsonPath("$.assignedAt").isString());
        }

        @Test
        void assignPartnerLevelUpserts() throws Exception {
            String adminToken = registerAndGetAdminToken();
            String targetUserId = registerTargetUser();
            UUID partnerId = UUID.randomUUID();

            // Assign basic
            mockMvc.perform(post(USERS_URL + "/" + targetUserId + "/partners")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"partnerId":"%s","level":"basic"}
                                    """.formatted(partnerId))
                            .cookie(new Cookie("access_token", adminToken)))
                    .andExpect(status().isCreated());

            // Upsert to full
            mockMvc.perform(post(USERS_URL + "/" + targetUserId + "/partners")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"partnerId":"%s","level":"full"}
                                    """.formatted(partnerId))
                            .cookie(new Cookie("access_token", adminToken)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.level").value("full"));
        }

        @Test
        void assignInvalidLevelReturns400() throws Exception {
            String adminToken = registerAndGetAdminToken();
            String targetUserId = registerTargetUser();

            mockMvc.perform(post(USERS_URL + "/" + targetUserId + "/partners")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"partnerId":"%s","level":"invalid"}
                                    """.formatted(UUID.randomUUID()))
                            .cookie(new Cookie("access_token", adminToken)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void assignToNonExistentUserReturns404() throws Exception {
            String adminToken = registerAndGetAdminToken();

            mockMvc.perform(post(USERS_URL + "/" + UUID.randomUUID() + "/partners")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"partnerId":"%s","level":"basic"}
                                    """.formatted(UUID.randomUUID()))
                            .cookie(new Cookie("access_token", adminToken)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class RemovePartnerLevelTests {

        @Test
        void removePartnerLevelReturns204() throws Exception {
            String adminToken = registerAndGetAdminToken();
            String targetUserId = registerTargetUser();
            UUID partnerId = UUID.randomUUID();

            // Assign first
            mockMvc.perform(post(USERS_URL + "/" + targetUserId + "/partners")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"partnerId":"%s","level":"basic"}
                                    """.formatted(partnerId))
                            .cookie(new Cookie("access_token", adminToken)))
                    .andExpect(status().isCreated());

            // Remove
            mockMvc.perform(delete(USERS_URL + "/" + targetUserId + "/partners/" + partnerId)
                            .cookie(new Cookie("access_token", adminToken)))
                    .andExpect(status().isNoContent());
        }

        @Test
        void removedPartnerGoneFromUser() throws Exception {
            String adminToken = registerAndGetAdminToken();
            String targetUserId = registerTargetUser();
            UUID partnerId = UUID.randomUUID();

            // Assign
            mockMvc.perform(post(USERS_URL + "/" + targetUserId + "/partners")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"partnerId":"%s","level":"full"}
                                    """.formatted(partnerId))
                            .cookie(new Cookie("access_token", adminToken)))
                    .andExpect(status().isCreated());

            // Remove
            mockMvc.perform(delete(USERS_URL + "/" + targetUserId + "/partners/" + partnerId)
                            .cookie(new Cookie("access_token", adminToken)))
                    .andExpect(status().isNoContent());

            // Verify partner is gone
            mockMvc.perform(get(USERS_URL + "/" + targetUserId)
                            .cookie(new Cookie("access_token", adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.partnerRoles.length()").value(0));
        }
    }

    @Nested
    class SearchFilterTests {

        @Test
        void searchByFirstName() throws Exception {
            String adminToken = registerAndGetAdminToken();
            // Use a unique alphabetic-only name to pass validation
            String uniqueName = "Knownxyz" + Long.toHexString(System.nanoTime()).replaceAll("[0-9]", "a");
            registerTargetUserWithName(uniqueName, "Surname");

            mockMvc.perform(get(USERS_URL + "?search=" + uniqueName)
                            .cookie(new Cookie("access_token", adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].firstName").value(uniqueName));
        }

        @Test
        void searchIsCaseInsensitive() throws Exception {
            String adminToken = registerAndGetAdminToken();
            String uniqueName = "Uniqsrch" + Long.toHexString(System.nanoTime()).replaceAll("[0-9]", "b");
            registerTargetUserWithName(uniqueName, "Last");

            mockMvc.perform(get(USERS_URL + "?search=" + uniqueName.toUpperCase())
                            .cookie(new Cookie("access_token", adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1));
        }

        @Test
        void filterByTier() throws Exception {
            String adminToken = registerAndGetAdminToken();
            String targetUserId = registerTargetUser();

            // Set target user to tier 3
            mockMvc.perform(patch(USERS_URL + "/" + targetUserId + "/tier")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"tier":3}
                                    """)
                            .cookie(new Cookie("access_token", adminToken)))
                    .andExpect(status().isOk());

            mockMvc.perform(get(USERS_URL + "?tier=3")
                            .cookie(new Cookie("access_token", adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].capabilityTier").value(3));
        }

        @Test
        void filterByActiveStatus() throws Exception {
            String adminToken = registerAndGetAdminToken();
            String targetUserId = registerTargetUser();

            // Deactivate user
            mockMvc.perform(patch(USERS_URL + "/" + targetUserId + "/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"active":false}
                                    """)
                            .cookie(new Cookie("access_token", adminToken)))
                    .andExpect(status().isOk());

            mockMvc.perform(get(USERS_URL + "?active=false")
                            .cookie(new Cookie("access_token", adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").isNumber());
        }

        @Test
        void combinedSearchAndFilter() throws Exception {
            String adminToken = registerAndGetAdminToken();
            String uniqueName = "Cmbndflt" + Long.toHexString(System.nanoTime()).replaceAll("[0-9]", "c");
            registerTargetUserWithName(uniqueName, "Filter");

            mockMvc.perform(get(USERS_URL + "?search=" + uniqueName + "&tier=1&active=true")
                            .cookie(new Cookie("access_token", adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }

    @Nested
    class UpdateProfileTests {

        @Test
        void patchUserReturns200() throws Exception {
            String adminToken = registerAndGetAdminToken();
            String targetUserId = registerTargetUser();

            mockMvc.perform(patch(USERS_URL + "/" + targetUserId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"firstName":"Updated","lastName":"Name"}
                                    """)
                            .cookie(new Cookie("access_token", adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Updated"))
                    .andExpect(jsonPath("$.lastName").value("Name"));
        }

        @Test
        void putUserReturns405() throws Exception {
            String adminToken = registerAndGetAdminToken();
            String targetUserId = registerTargetUser();

            mockMvc.perform(put(USERS_URL + "/" + targetUserId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"firstName":"X","lastName":"Y"}
                                    """)
                            .cookie(new Cookie("access_token", adminToken)))
                    .andExpect(status().isMethodNotAllowed());
        }
    }

    @Nested
    class UpdateTierTests {

        @Test
        void patchTierReturns200() throws Exception {
            String adminToken = registerAndGetAdminToken();
            String targetUserId = registerTargetUser();

            mockMvc.perform(patch(USERS_URL + "/" + targetUserId + "/tier")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"tier":3}
                                    """)
                            .cookie(new Cookie("access_token", adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.capabilityTier").value(3));
        }

        @Test
        void putTierReturns405() throws Exception {
            String adminToken = registerAndGetAdminToken();
            String targetUserId = registerTargetUser();

            mockMvc.perform(put(USERS_URL + "/" + targetUserId + "/tier")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"tier":3}
                                    """)
                            .cookie(new Cookie("access_token", adminToken)))
                    .andExpect(status().isMethodNotAllowed());
        }
    }

    @Nested
    class UpdateStatusTests {

        @Test
        void patchStatusReturns200() throws Exception {
            String adminToken = registerAndGetAdminToken();
            String targetUserId = registerTargetUser();

            mockMvc.perform(patch(USERS_URL + "/" + targetUserId + "/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"active":false}
                                    """)
                            .cookie(new Cookie("access_token", adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(false));
        }

        @Test
        void putStatusReturns405() throws Exception {
            String adminToken = registerAndGetAdminToken();
            String targetUserId = registerTargetUser();

            mockMvc.perform(put(USERS_URL + "/" + targetUserId + "/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"active":false}
                                    """)
                            .cookie(new Cookie("access_token", adminToken)))
                    .andExpect(status().isMethodNotAllowed());
        }
    }

    @Nested
    class SelfProfileUpdateTests {

        @Test
        void patchMeReturns200() throws Exception {
            var admin = registerAndGetAdminTokenWithDetails();

            mockMvc.perform(patch(USERS_URL + "/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"firstName":"SelfUpdated","lastName":"MeName"}
                                    """)
                            .cookie(new Cookie("access_token", admin.token())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("SelfUpdated"))
                    .andExpect(jsonPath("$.lastName").value("MeName"));
        }

        @Test
        void patchMeWithBlankNameReturns400() throws Exception {
            var admin = registerAndGetAdminTokenWithDetails();

            mockMvc.perform(patch(USERS_URL + "/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"firstName":"","lastName":"Valid"}
                                    """)
                            .cookie(new Cookie("access_token", admin.token())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void patchMeWithoutAuthReturns401() throws Exception {
            mockMvc.perform(patch(USERS_URL + "/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"firstName":"X","lastName":"Y"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class ChangePasswordTests {

        @Test
        void changePasswordReturns204() throws Exception {
            var admin = registerAndGetAdminTokenWithDetails();

            mockMvc.perform(patch(USERS_URL + "/me/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"currentPassword":"password123","newPassword":"newPassword456"}
                                    """)
                            .cookie(new Cookie("access_token", admin.token())))
                    .andExpect(status().isNoContent());
        }

        @Test
        void canLoginWithNewPasswordAfterChange() throws Exception {
            var admin = registerAndGetAdminTokenWithDetails();

            mockMvc.perform(patch(USERS_URL + "/me/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"currentPassword":"password123","newPassword":"newPassword456"}
                                    """)
                            .cookie(new Cookie("access_token", admin.token())))
                    .andExpect(status().isNoContent());

            // Login with new password
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new ua.kpi.sc.auth.dto.LoginRequest(admin.email(), "newPassword456"))))
                    .andExpect(status().isOk());
        }

        @Test
        void changePasswordWrongCurrentReturns400() throws Exception {
            var admin = registerAndGetAdminTokenWithDetails();

            mockMvc.perform(patch(USERS_URL + "/me/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"currentPassword":"wrongPassword","newPassword":"newPassword456"}
                                    """)
                            .cookie(new Cookie("access_token", admin.token())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void changePasswordShortNewReturns400() throws Exception {
            var admin = registerAndGetAdminTokenWithDetails();

            mockMvc.perform(patch(USERS_URL + "/me/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"currentPassword":"password123","newPassword":"short"}
                                    """)
                            .cookie(new Cookie("access_token", admin.token())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void changePasswordWithoutAuthReturns401() throws Exception {
            mockMvc.perform(patch(USERS_URL + "/me/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"currentPassword":"x","newPassword":"newPassword456"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }
    }
}
