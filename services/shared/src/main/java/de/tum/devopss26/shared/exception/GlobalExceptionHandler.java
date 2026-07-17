package de.tum.devopss26.shared.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Every handler returns an empty {@link ResponseEntity} with the appropriate status code —
 * the exception message and stack trace are logged server-side and never leaked to clients.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles {@link NotFoundException} and returns a 404 Not Found response.
     *
     * @param ex the exception that was thrown
     * @return an empty response entity with HTTP 404 status
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Void> handleNotFoundException(NotFoundException ex) {
        log.atError().setCause(ex).log("Resource not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    /**
     * Handles {@link ConflictException} and returns a 409 Conflict response.
     *
     * @param ex the exception that was thrown
     * @return an empty response entity with HTTP 409 status
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Void> handleConflictException(ConflictException ex) {
        log.atError().setCause(ex).log("Conflict error");
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    /**
     * Handles {@link ForbiddenException} and returns a 403 Forbidden response.
     *
     * @param ex the exception that was thrown
     * @return an empty response entity with HTTP 403 status
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Void> handleForbiddenException(ForbiddenException ex) {
        log.atError().setCause(ex).log("Forbidden error");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    /**
     * Handles bad request scenarios including {@link BadRequestException}, malformed HTTP
     * messages, type mismatches, missing parameters, and {@code @Valid} validation failures.
     * Returns a 400 Bad Request response.
     *
     * @param ex the exception that was thrown
     * @return an empty response entity with HTTP 400 status
     */
    @ExceptionHandler({
            BadRequestException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.web.bind.MethodArgumentNotValidException.class
    })
    public ResponseEntity<Void> handleBadRequestException(Exception ex) {
        log.atError().setCause(ex).log("Bad request");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    /**
     * Catch-all handler for any unhandled exception. Returns a 500 Internal Server Error
     * response without leaking stack trace details to the client.
     *
     * @param ex the exception that was thrown
     * @return an empty response entity with HTTP 500 status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Void> handleGeneralException(Exception ex) {
        log.atError().setCause(ex).log("An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
