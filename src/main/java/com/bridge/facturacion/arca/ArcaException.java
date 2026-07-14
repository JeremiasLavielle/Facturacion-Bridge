package com.bridge.facturacion.arca;

public class ArcaException extends RuntimeException {

    public ArcaException(String message) {
        super(message);
    }

    public ArcaException(String message, Throwable cause) {
        super(message, cause);
    }
}
