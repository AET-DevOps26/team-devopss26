package de.tum.devopss26.shared.exception;

/**
 * Exception thrown when a request contains invalid data or parameters (HTTP 400).
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
