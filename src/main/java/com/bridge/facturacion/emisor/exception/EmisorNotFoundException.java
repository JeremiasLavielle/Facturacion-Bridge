package com.bridge.facturacion.emisor.exception;

import com.bridge.facturacion.common.exception.ResourceNotFoundException;

public class EmisorNotFoundException extends ResourceNotFoundException {
    public EmisorNotFoundException(Long id) {
        super("Emisor no encontrado: " + id);
    }
}
