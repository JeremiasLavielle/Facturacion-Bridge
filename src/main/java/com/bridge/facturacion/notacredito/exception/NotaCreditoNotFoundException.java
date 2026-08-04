package com.bridge.facturacion.notacredito.exception;

import com.bridge.facturacion.common.exception.ResourceNotFoundException;

public class NotaCreditoNotFoundException extends ResourceNotFoundException {
    public NotaCreditoNotFoundException(Long id) {
        super("Nota de credito no encontrada: " + id);
    }

    public static NotaCreditoNotFoundException deFactura(Long facturaId) {
        return new NotaCreditoNotFoundException(
                "La factura " + facturaId + " no tiene nota de credito");
    }

    private NotaCreditoNotFoundException(String message) {
        super(message);
    }
}
