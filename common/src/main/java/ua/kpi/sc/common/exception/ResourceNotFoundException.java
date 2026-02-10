package ua.kpi.sc.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested resource cannot be found.
 * Results in an HTTP 404 Not Found response.
 *
 * @see GlobalExceptionHandler
 * @since 0.1.0
 */
public class ResourceNotFoundException extends ApiException {

    /**
     * Creates a not-found exception with the given detail message.
     *
     * @param message description of the missing resource
     */
    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }

    /**
     * Creates a not-found exception for a specific resource type and identifier.
     *
     * @param resource the type of resource (e.g. "User", "Project")
     * @param id       the identifier that was not found
     */
    public ResourceNotFoundException(String resource, Object id) {
        super(HttpStatus.NOT_FOUND, "%s not found with id: %s".formatted(resource, id));
    }
}
