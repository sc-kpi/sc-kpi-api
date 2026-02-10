package ua.kpi.sc.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a client request is malformed or contains invalid data.
 * Results in an HTTP 400 Bad Request response.
 *
 * @see GlobalExceptionHandler
 * @since 0.1.0
 */
public class BadRequestException extends ApiException {

    /**
     * Creates a bad request exception with the given detail message.
     *
     * @param message description of what was invalid in the request
     */
    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
