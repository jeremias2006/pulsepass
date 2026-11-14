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
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Traductor central de excepciones a respuestas HTTP. Convierte cada excepción en un
 * {@link ErrorResponse} con la misma estructura, de modo que ningún controlador necesita
 * {@code try/catch}.
 *
 * <p>{@code @RestControllerAdvice} hace que estos métodos apliquen a TODOS los controladores. Cuando
 * se lanza una excepción, Spring elige el {@code @ExceptionHandler} cuyo tipo sea el más específico
 * para esa excepción; el orden en el archivo no importa, y por eso {@code Exception.class} funciona
 * como red de seguridad final.</p>
 *
 * <p>Resumen de códigos:</p>
 * <ul>
 *   <li>400: validación fallida, JSON ilegible, parámetro faltante o de tipo incorrecto.</li>
 *   <li>401: autenticación requerida o credenciales inválidas. 403: sin permiso.</li>
 *   <li>404: recurso inexistente o ruta inexistente. 405: verbo no soportado. 415: Content-Type no soportado.</li>
 *   <li>409: recurso duplicado o regla de negocio violada.</li>
 *   <li>500: cualquier error inesperado (el detalle solo se registra en el log, nunca se envía al cliente).</li>
 * </ul>
 * <p>Los errores 401/403 que ocurren en los filtros (antes de llegar a un controlador) no pasan por aquí:
 * los resuelven {@code RestAuthenticationEntryPoint} y {@code RestAccessDeniedHandler}.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Logger SLF4J: solo se usa para errores inesperados (500) y accesos denegados.
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

        // Arma un mapa campo -> mensaje con cada error de validación (p. ej. "email" -> "Email format is invalid").
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
    // El mensaje es fijo y genérico: no se devuelve el texto interno del parser de JSON.
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

    // 404: ruta inexistente (sin esto caería en el handler de Exception => 500) ----
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex) {

        return build(HttpStatus.NOT_FOUND, "Resource not found", Map.of());
    }

    // 405: verbo no soportado en una ruta existente ---------------------------------
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex) {

        // Aquí no se usa build() porque además hay que reenviar cabeceras (Allow) en la respuesta.
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase(),
                "HTTP method not supported for this resource",
                Map.of());
        // ex.getHeaders() incluye el header "Allow" con los verbos válidos
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .headers(ex.getHeaders())
                .body(body);
    }

    // 415: Content-Type distinto de application/json en un endpoint con body -------
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex) {

        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Unsupported media type. Use application/json", Map.of());
    }

    // 500: cualquier otra cosa ----------------------------------------
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex) {

        // El detalle va al log del servidor, NUNCA al cliente.
        // (Un mensaje de excepción interno podría revelar rutas, SQL o datos sensibles.)
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

    // 403: @PreAuthorize denegó (incluye AuthorizationDeniedException) -----
    // El orden de los métodos no afecta: Spring elige el handler más específico para cada excepción.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {

        log.warn("access_denied reason={}", ex.getClass().getSimpleName());
        return build(HttpStatus.FORBIDDEN,
                "No tiene permisos para realizar esta operación", Map.of());
    }

    // 401: login fallido (BadCredentialsException) y cualquier AuthenticationException
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {

        return build(HttpStatus.UNAUTHORIZED,
                "Autenticación requerida o credenciales inválidas", Map.of());
    }

}
