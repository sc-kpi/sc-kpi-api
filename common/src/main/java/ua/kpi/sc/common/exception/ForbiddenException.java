package ua.kpi.sc.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an authenticated user lacks sufficient permissions for the requested operation.
 * Results in an HTTP 403 Forbidden response.
 *
 * @see GlobalExceptionHandler
 * @since 0.1.0
 */
public class ForbiddenException extends ApiException {

    /**
     * Creates a forbidden exception with the given detail message.
     *
     * @param message description of the missing permission
     */
    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
