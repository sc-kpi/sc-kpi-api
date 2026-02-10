package ua.kpi.sc.common.security.handler;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

class ProblemDetailAuthenticationEntryPointTest {

    private ProblemDetailAuthenticationEntryPoint entryPoint;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        entryPoint = new ProblemDetailAuthenticationEntryPoint(objectMapper, "https://api.example.com");
    }

    @Test
    void commence_writes401ProblemDetail() throws Exception {
        var request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/profile");
        var response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("Bad credentials"));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        ProblemDetail problem = objectMapper.readValue(response.getContentAsString(), ProblemDetail.class);
        assertThat(problem.getTitle()).isEqualTo("Unauthorized");
        assertThat(problem.getDetail()).isEqualTo("Authentication required");
        assertThat(problem.getType().toString()).isEqualTo("https://api.example.com/errors/401");
        assertThat(problem.getInstance().toString()).isEqualTo("/api/v1/profile");
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }
}
