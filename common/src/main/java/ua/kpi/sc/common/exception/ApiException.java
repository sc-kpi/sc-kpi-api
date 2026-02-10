package ua.kpi.sc.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base exception for all API-specific errors in the SC-KPI application.
 *
 * <p>Subclasses map to specific HTTP status codes and are translated into
 * RFC 9457 {@link org.springframework.http.ProblemDetail} responses by
 * {@link GlobalExceptionHandler}.
 *
 * @param status  HTTP status code that this exception maps to
 * @param message human-readable error detail included in the response body
 * @see GlobalExceptionHandler#handleApiException(ApiException, org.springframework.web.context.request.WebRequest)
 * @since 0.1.0
 */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    /**
     * Creates an API exception with the given HTTP status and detail message.
     *
     * @param status  the HTTP status code for the error response
     * @param message the detail message explaining the error
     */
    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
