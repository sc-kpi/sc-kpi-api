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
import org.springframework.security.access.AccessDeniedException;

class ProblemDetailAccessDeniedHandlerTest {

    private ProblemDetailAccessDeniedHandler handler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        handler = new ProblemDetailAccessDeniedHandler(objectMapper, "https://api.example.com");
    }

    @Test
    void handle_writes403ProblemDetail() throws Exception {
        var request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/admin/users");
        var response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("Forbidden"));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        ProblemDetail problem = objectMapper.readValue(response.getContentAsString(), ProblemDetail.class);
        assertThat(problem.getTitle()).isEqualTo("Forbidden");
        assertThat(problem.getDetail()).isEqualTo("Access denied");
        assertThat(problem.getType().toString()).isEqualTo("https://api.example.com/errors/403");
        assertThat(problem.getInstance().toString()).isEqualTo("/api/v1/admin/users");
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }
}
