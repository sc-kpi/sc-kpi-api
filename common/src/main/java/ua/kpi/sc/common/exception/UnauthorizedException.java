package ua.kpi.sc.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a request lacks valid authentication credentials.
 * Results in an HTTP 401 Unauthorized response.
 *
 * @see GlobalExceptionHandler
 * @since 0.1.0
 */
public class UnauthorizedException extends ApiException {

    /**
     * Creates an unauthorized exception with the given detail message.
     *
     * @param message description of the authentication failure
     */
    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
