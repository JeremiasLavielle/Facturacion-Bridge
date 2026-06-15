package com.bridge.facturacion.factura.exception;

public class FacturaNotFoundException extends RuntimeException {
    public FacturaNotFoundException(Long id) {
        super("Factura no encontrada: " + id);
    }
}
