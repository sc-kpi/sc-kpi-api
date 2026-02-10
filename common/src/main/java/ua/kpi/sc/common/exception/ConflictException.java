package ua.kpi.sc.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a request conflicts with the current state of a resource
 * (e.g. duplicate email, concurrent modification).
 * Results in an HTTP 409 Conflict response.
 *
 * @see GlobalExceptionHandler
 * @since 0.1.0
 */
public class ConflictException extends ApiException {

    /**
     * Creates a conflict exception with the given detail message.
     *
     * @param message description of the conflict
     */
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
