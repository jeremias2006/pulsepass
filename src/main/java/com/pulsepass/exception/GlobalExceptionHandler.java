package com.pulsepass.exception;

import com.pulsepass.dto.response.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 404 -------------------------------------------------------------
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex) {

        return build(HttpStatus.NOT_FOUND, ex.getMessage(), Map.of());
    }

    // 409: unicidad ---------------------------------------------------
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(
            DuplicateResourceException ex) {

        return build(HttpStatus.CONFLICT, ex.getMessage(), Map.of());
    }

    // 409: regla de negocio -------------------------------------------
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(
            BusinessRuleException ex) {

        return build(HttpStatus.CONFLICT, ex.getMessage(), Map.of());
    }

    // 400: Bean Validation (@Valid) falló -----------------------------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> Objects.toString(fe.getDefaultMessage(), "Invalid value"),
                        (first, second) -> first   // 2 errores en un campo: conserva el primero
                ));

        return build(HttpStatus.BAD_REQUEST, "Validation failed", details);
    }

    // 400: JSON mal formado, fecha inválida o enum inválido en el body -
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex) {

        return build(
                HttpStatus.BAD_REQUEST,
                "Malformed or invalid JSON request",
                Map.of("body", "Check JSON syntax, dates and enum values")
        );
    }

    // 400: falta un @RequestParam obligatorio (ej. ?email=) -----------
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException ex) {

        return build(
                HttpStatus.BAD_REQUEST,
                "Missing required parameter",
                Map.of(ex.getParameterName(), "Parameter is required")
        );
    }

    // 400: query param / path variable con tipo inválido (artistId=abc)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {

        Map<String, String> details = Map.of(
                ex.getName(),
                "Invalid value: " + ex.getValue()
        );

        return build(HttpStatus.BAD_REQUEST, "Invalid request parameter", details);
    }

    // 500: cualquier otra cosa ----------------------------------------
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex) {

        // El detalle va al log del servidor, NUNCA al cliente.
        log.error("Unexpected error", ex);

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                Map.of()
        );
    }

    // Helper: garantiza que TODOS los errores tengan la misma estructura
    private ResponseEntity<ErrorResponse> build(HttpStatus status,
                                                String message,
                                                Map<String, String> details) {
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                details
        );
        return ResponseEntity.status(status).body(body);
    }
}
