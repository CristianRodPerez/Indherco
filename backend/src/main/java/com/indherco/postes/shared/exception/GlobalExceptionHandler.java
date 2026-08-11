package com.indherco.postes.shared.exception;

import com.indherco.postes.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> handleApiException(ApiException exception, HttpServletRequest request) {
        ApiError error = error(exception.getStatus(), exception.getCode(), exception.getMessage(), request);
        return ResponseEntity.status(exception.getStatus()).body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> handleAuthentication(HttpServletRequest request) {
        ApiError error = error(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", "Usuario o contrasena incorrectos.", request);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> handleAccessDenied(HttpServletRequest request) {
        ApiError error = error(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "No tiene permiso para realizar esta accion.", request);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
            .map(this::formatFieldError)
            .collect(Collectors.joining(". "));
        ApiError error = error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(HttpServletRequest request) {
        ApiError error = error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Ocurrio un error inesperado.", request);
        return ResponseEntity.internalServerError().body(error);
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private ApiError error(HttpStatus status, String code, String message, HttpServletRequest request) {
        Object correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return new ApiError(
            LocalDateTime.now(),
            status.value(),
            code,
            message,
            request.getRequestURI(),
            correlationId == null ? "" : correlationId.toString()
        );
    }
}
