package ua.kpi.sc.common.security.handler;

import java.io.IOException;
import java.net.URI;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Handles {@link AccessDeniedException} by writing an RFC 9457 {@link ProblemDetail}
 * response with HTTP 403 Forbidden status.
 *
 * <p>Used as the access-denied handler in the security filter chain to ensure
 * consistent error response formatting for authorization failures that occur
 * before reaching controller-level exception handling.
 *
 * @see ProblemDetailAuthenticationEntryPoint
 * @since 0.1.0
 */
@Component
public class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final String apiBaseUrl;

    /**
     * Creates the handler with required dependencies.
     *
     * @param objectMapper Jackson mapper for serializing the response body
     * @param apiBaseUrl   base URL for constructing error type URIs
     */
    public ProblemDetailAccessDeniedHandler(ObjectMapper objectMapper,
                                           @Value("${app.api-base-url}") String apiBaseUrl) {
        this.objectMapper = objectMapper;
        this.apiBaseUrl = apiBaseUrl;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, "Access denied");
        problem.setTitle("Forbidden");
        problem.setType(URI.create(apiBaseUrl + "/errors/403"));
        problem.setInstance(URI.create(request.getRequestURI()));

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
