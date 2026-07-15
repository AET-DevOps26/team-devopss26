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

    /** 404 — requested domain entity does not exist. */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Void> handleNotFoundException(NotFoundException ex) {
        log.atError().setCause(ex).log("Resource not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    /** 409 — duplicate resources or state conflicts. */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Void> handleConflictException(ConflictException ex) {
        log.atError().setCause(ex).log("Conflict error");
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    /** 403 — authenticated user lacks permission. */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Void> handleForbiddenException(ForbiddenException ex) {
        log.atError().setCause(ex).log("Forbidden error");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    /**
     * 400 — malformed or invalid client input. Catches bad request body, type mismatches,
     * missing parameters, and {@code @Valid} validation failures.
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

    /** 500 — catch-all safety net, never leaks stack traces to the client. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Void> handleGeneralException(Exception ex) {
        log.atError().setCause(ex).log("An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
