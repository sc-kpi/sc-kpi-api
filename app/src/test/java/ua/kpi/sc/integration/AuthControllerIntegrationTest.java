package ua.kpi.sc.integration;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import ua.kpi.sc.auth.dto.LoginRequest;
import ua.kpi.sc.auth.dto.RegisterRequest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String LOGOUT_URL = "/api/v1/auth/logout";
    private static final String REFRESH_URL = "/api/v1/auth/refresh";
    private static final String ME_URL = "/api/v1/auth/me";

    private int userCounter = 0;

    private RegisterRequest uniqueRegisterRequest() {
        userCounter++;
        return new RegisterRequest(
                "inttest" + userCounter + System.nanoTime() + "@kpi.ua",
                "password123",
                "Test",
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
        Cookie cookie = result.getResponse().getCookie("access_token");
        return cookie != null ? cookie.getValue() : null;
    }

    private String extractRefreshToken(MvcResult result) {
        Cookie cookie = result.getResponse().getCookie("refresh_token");
        return cookie != null ? cookie.getValue() : null;
    }

    @Nested
    class RegisterTests {

        @Test
        void registerReturns201WithCookiesAndUser() throws Exception {
            RegisterRequest request = uniqueRegisterRequest();

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value(request.email()))
                    .andExpect(jsonPath("$.firstName").value("Test"))
                    .andExpect(jsonPath("$.lastName").value("User"))
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(cookie().exists("access_token"))
                    .andExpect(cookie().exists("refresh_token"))
                    .andExpect(cookie().httpOnly("access_token", true))
                    .andExpect(cookie().httpOnly("refresh_token", true));
        }

        @Test
        void registerDuplicateEmailReturns409() throws Exception {
            RegisterRequest request = uniqueRegisterRequest();
            registerUser(request);

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        void registerInvalidEmailReturns400() throws Exception {
            RegisterRequest request = new RegisterRequest("not-an-email", "password123", "A", "B");

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void registerShortPasswordReturns400() throws Exception {
            RegisterRequest request = new RegisterRequest("valid@kpi.ua", "short", "A", "B");

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class LoginTests {

        @Test
        void loginReturns200WithCookies() throws Exception {
            RegisterRequest reg = uniqueRegisterRequest();
            registerUser(reg);

            LoginRequest login = new LoginRequest(reg.email(), reg.password());

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(login)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(reg.email()))
                    .andExpect(cookie().exists("access_token"))
                    .andExpect(cookie().exists("refresh_token"));
        }

        @Test
        void loginWrongPasswordReturns401() throws Exception {
            RegisterRequest reg = uniqueRegisterRequest();
            registerUser(reg);

            LoginRequest login = new LoginRequest(reg.email(), "wrongpassword");

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(login)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void loginNonExistentEmailReturns401() throws Exception {
            LoginRequest login = new LoginRequest("nobody@kpi.ua", "password123");

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(login)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class MeTests {

        @Test
        void meWithValidTokenReturns200() throws Exception {
            MvcResult regResult = registerUser(uniqueRegisterRequest());
            String accessToken = extractAccessToken(regResult);

            mockMvc.perform(get(ME_URL)
                            .cookie(new Cookie("access_token", accessToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").exists())
                    .andExpect(jsonPath("$.firstName").exists());
        }

        @Test
        void meWithBearerHeaderReturns200() throws Exception {
            MvcResult regResult = registerUser(uniqueRegisterRequest());
            String accessToken = extractAccessToken(regResult);

            mockMvc.perform(get(ME_URL)
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").exists());
        }

        @Test
        void meWithoutTokenReturns401() throws Exception {
            mockMvc.perform(get(ME_URL))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class LogoutTests {

        @Test
        void logoutClearsCookies() throws Exception {
            MvcResult regResult = registerUser(uniqueRegisterRequest());
            String accessToken = extractAccessToken(regResult);

            mockMvc.perform(post(LOGOUT_URL)
                            .cookie(new Cookie("access_token", accessToken)))
                    .andExpect(status().isOk())
                    .andExpect(cookie().maxAge("access_token", 0))
                    .andExpect(cookie().maxAge("refresh_token", 0));
        }
    }

    @Nested
    class RefreshTests {

        @Test
        void refreshWithValidTokenReturns200() throws Exception {
            MvcResult regResult = registerUser(uniqueRegisterRequest());
            String refreshToken = extractRefreshToken(regResult);

            mockMvc.perform(post(REFRESH_URL)
                            .cookie(new Cookie("refresh_token", refreshToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").exists())
                    .andExpect(cookie().exists("access_token"))
                    .andExpect(cookie().exists("refresh_token"));
        }

        @Test
        void refreshWithoutCookieReturns401() throws Exception {
            mockMvc.perform(post(REFRESH_URL))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void refreshWithInvalidTokenReturns401() throws Exception {
            mockMvc.perform(post(REFRESH_URL)
                            .cookie(new Cookie("refresh_token", "invalid-token-value")))
                    .andExpect(status().isUnauthorized());
        }
    }
}
