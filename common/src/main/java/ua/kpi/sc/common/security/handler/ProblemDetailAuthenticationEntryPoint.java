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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Handles {@link AuthenticationException} by writing an RFC 9457 {@link ProblemDetail}
 * response with HTTP 401 Unauthorized status.
 *
 * <p>Used as the authentication entry point in the security filter chain to ensure
 * consistent error response formatting when unauthenticated requests reach
 * protected endpoints.
 *
 * @see ProblemDetailAccessDeniedHandler
 * @since 0.1.0
 */
@Component
public class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    private final String apiBaseUrl;

    /**
     * Creates the entry point with required dependencies.
     *
     * @param objectMapper Jackson mapper for serializing the response body
     * @param apiBaseUrl   base URL for constructing error type URIs
     */
    public ProblemDetailAuthenticationEntryPoint(ObjectMapper objectMapper,
                                                @Value("${app.api-base-url}") String apiBaseUrl) {
        this.objectMapper = objectMapper;
        this.apiBaseUrl = apiBaseUrl;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Authentication required");
        problem.setTitle("Unauthorized");
        problem.setType(URI.create(apiBaseUrl + "/errors/401"));
        problem.setInstance(URI.create(request.getRequestURI()));

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
