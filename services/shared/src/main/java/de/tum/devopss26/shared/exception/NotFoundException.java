package de.tum.devopss26.shared.exception;

/**
 * Exception thrown when a requested resource could not be found (HTTP 404).
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
