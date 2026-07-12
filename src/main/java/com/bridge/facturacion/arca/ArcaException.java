package com.bridge.facturacion.arca;

/**
 * Error de comunicacion o error estructural devuelto por ARCA.
 * El GlobalExceptionHandler la traduce a HTTP 502: el problema esta
 * "del otro lado" (upstream), no en el request del cliente.
 */
public class ArcaException extends RuntimeException {

    public ArcaException(String message) {
        super(message);
    }

    public ArcaException(String message, Throwable cause) {
        super(message, cause);
    }
}
