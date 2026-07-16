package de.tum.devopss26.shared.exception;

/**
 * Exception thrown when a request conflicts with the current state of the resource (HTTP 409).
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
