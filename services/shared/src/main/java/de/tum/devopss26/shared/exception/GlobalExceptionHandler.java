package de.tum.devopss26.shared.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global {@link RestControllerAdvice} that maps custom and framework exceptions
 * to appropriate HTTP status codes.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles {@link NotFoundException} and returns HTTP 404.
     *
     * @param ex the exception
     * @return a 404 response with no body
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Void> handleNotFoundException(NotFoundException ex) {
        log.atError().setCause(ex).log("Resource not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    /**
     * Handles {@link ConflictException} and returns HTTP 409.
     *
     * @param ex the exception
     * @return a 409 response with no body
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Void> handleConflictException(ConflictException ex) {
        log.atError().setCause(ex).log("Conflict error");
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    /**
     * Handles {@link ForbiddenException} and returns HTTP 403.
     *
     * @param ex the exception
     * @return a 403 response with no body
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Void> handleForbiddenException(ForbiddenException ex) {
        log.atError().setCause(ex).log("Forbidden error");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    /**
     * Handles bad request errors ({@link BadRequestException} and framework
     * exceptions such as unreadable messages, type mismatches, missing parameters,
     * or validation failures) and returns HTTP 400.
     *
     * @param ex the exception
     * @return a 400 response with no body
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
     * Catch-all handler for any unhandled exception, returning HTTP 500.
     *
     * @param ex the exception
     * @return a 500 response with no body
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Void> handleGeneralException(Exception ex) {
        log.atError().setCause(ex).log("An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
