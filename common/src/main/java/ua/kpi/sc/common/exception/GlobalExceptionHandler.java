package ua.kpi.sc.common.exception;

import java.net.URI;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Centralized exception handler that translates all exceptions into
 * RFC 9457 {@link ProblemDetail} responses.
 *
 * <p>Extends {@link ResponseEntityExceptionHandler} to inherit handling of standard
 * Spring MVC exceptions, and adds handlers for the application-specific
 * {@link ApiException} hierarchy. Every response is enriched with a {@code type} URI
 * (based on {@code app.api-base-url}) and an {@code instance} URI matching the
 * request path.
 *
 * @see ApiException
 * @since 0.1.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final URI ABOUT_BLANK = URI.create("about:blank");

    private final String apiBaseUrl;

    /**
     * Creates the handler with the configured API base URL used for error type URIs.
     *
     * @param apiBaseUrl base URL for constructing {@code type} links (e.g. {@code https://api.example.com})
     */
    public GlobalExceptionHandler(@Value("${app.api-base-url}") String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    /**
     * Enriches every {@link ProblemDetail} response with a {@code type} URI and
     * {@code instance} URI if not already set.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(
                ex, body, headers, status, request);
        if (response.getBody() instanceof ProblemDetail problem) {
            if (problem.getType() == null || ABOUT_BLANK.equals(problem.getType())) {
                problem.setType(URI.create(apiBaseUrl + "/errors/" + status.value()));
            }
            if (problem.getInstance() == null) {
                setInstance(problem, request);
            }
        }
        return response;
    }

    /**
     * Handles all {@link ApiException} subclasses, mapping them to their declared HTTP status.
     *
     * @param ex      the application-specific exception
     * @param request the current web request
     * @return a {@link ProblemDetail} response with the exception's status and message
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Object> handleApiException(ApiException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        problem.setTitle(ex.getStatus().getReasonPhrase());
        return handleExceptionInternal(ex, problem, new HttpHeaders(), ex.getStatus(), request);
    }

    /**
     * Handles access denied errors from method-level security (e.g. {@code @RequireTier}).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, "Access denied");
        problem.setTitle("Forbidden");
        return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.FORBIDDEN, request);
    }

    /**
     * Catch-all handler for unexpected exceptions not covered by more specific handlers.
     * Logs the full stack trace and returns a generic 500 response to avoid leaking internals.
     *
     * @param ex      the unhandled exception
     * @param request the current web request
     * @return a 500 {@link ProblemDetail} response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception ex, WebRequest request) {
        log.error("Unhandled exception", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problem.setTitle("Internal Server Error");
        return handleExceptionInternal(
                ex, problem, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    /**
     * Handles bean validation failures from {@code @Valid} annotated parameters,
     * returning field-level error details.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validation Error");
        problem.setType(URI.create(apiBaseUrl + "/errors/validation"));
        problem.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                .map(e -> Map.of("field", e.getField(), "message",
                        e.getDefaultMessage() != null ? e.getDefaultMessage() : "invalid"))
                .toList());
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /**
     * Handles malformed request bodies (e.g. invalid JSON syntax).
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Malformed request body");
        problem.setTitle("Bad Request");
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /**
     * Handles missing required request parameters (e.g. {@code ?page} when required).
     */
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Required parameter '%s' is missing".formatted(ex.getParameterName()));
        problem.setTitle("Bad Request");
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /**
     * Handles unsupported HTTP methods (e.g. PATCH on an endpoint that only supports GET).
     */
    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.METHOD_NOT_ALLOWED,
                "HTTP method '%s' is not supported for this endpoint".formatted(ex.getMethod()));
        problem.setTitle("Method Not Allowed");
        return handleExceptionInternal(ex, problem, headers, HttpStatus.METHOD_NOT_ALLOWED, request);
    }

    /**
     * Handles type conversion failures in request parameters or path variables
     * (e.g. passing a string where a number is expected).
     */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String detail = ex.getRequiredType() != null
                ? "Parameter '%s' should be of type '%s'".formatted(
                        ex.getPropertyName(), ex.getRequiredType().getSimpleName())
                : "Type mismatch for parameter '%s'".formatted(ex.getPropertyName());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Bad Request");
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    private void setInstance(ProblemDetail problem, WebRequest request) {
        if (request instanceof ServletWebRequest servletRequest
                && servletRequest.getRequest().getRequestURI() != null) {
            problem.setInstance(URI.create(servletRequest.getRequest().getRequestURI()));
        }
    }
}
