package com.bridge.facturacion.common.exception;

import com.bridge.facturacion.arca.ArcaException;
import com.bridge.facturacion.factura.exception.FacturaNoEmitidaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    @ExceptionHandler(FacturaNoEmitidaException.class)
    public ResponseEntity<ErrorResponse> handleNoEmitida(FacturaNoEmitidaException ex) {
        return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex) {
        return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, "Bad Request", detalle);
    }

    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            org.springframework.security.authentication.BadCredentialsException ex) {
        // Mensaje fijo, no ex.getMessage(): la respuesta debe ser identica para
        // "email inexistente" y "contraseña incorrecta" (evita enumerar usuarios).
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized", "Credenciales inválidas");
    }

    @ExceptionHandler(ArcaException.class)
    public ResponseEntity<ErrorResponse> handleArca(ArcaException ex) {
        log.error("Error contra ARCA: {}", ex.getMessage());
        return build(HttpStatus.BAD_GATEWAY, "Bad Gateway", ex.getMessage());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String message) {
        ErrorResponse body = new ErrorResponse(LocalDateTime.now(), status.value(), error, message);
        return ResponseEntity.status(status).body(body);
    }
}
