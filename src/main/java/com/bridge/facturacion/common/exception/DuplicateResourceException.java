package com.bridge.facturacion.common.exception;

public abstract class DuplicateResourceException extends RuntimeException {
    protected DuplicateResourceException(String message) {
        super(message);
    }
}
