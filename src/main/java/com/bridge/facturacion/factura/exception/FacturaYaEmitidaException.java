package com.bridge.facturacion.factura.exception;

import com.bridge.facturacion.common.exception.DuplicateResourceException;

/** Reemitir una factura con CAE seria duplicar un comprobante fiscal -> 409. */
public class FacturaYaEmitidaException extends DuplicateResourceException {
    public FacturaYaEmitidaException(Long id, String cae) {
        super("La factura " + id + " ya fue emitida (CAE " + cae + ")");
    }
}
