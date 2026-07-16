package de.tum.devopss26.shared.exception;

/**
 * Exception thrown when a user is not authorized to access a resource (HTTP 403).
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
