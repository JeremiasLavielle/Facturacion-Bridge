package com.bridge.facturacion.factura.exception;

import com.bridge.facturacion.common.exception.ResourceNotFoundException;

public class FacturaNotFoundException extends ResourceNotFoundException {
    public FacturaNotFoundException(Long id) {
        super("Factura no encontrada: " + id);
    }
}
