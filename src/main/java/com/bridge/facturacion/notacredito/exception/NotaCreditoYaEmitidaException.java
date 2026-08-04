package com.bridge.facturacion.notacredito.exception;

import com.bridge.facturacion.common.exception.DuplicateResourceException;

/** Una sola NC por factura: si ya se emitio, no hay segunda anulacion. */
public class NotaCreditoYaEmitidaException extends DuplicateResourceException {
    public NotaCreditoYaEmitidaException(Long facturaId, String cae) {
        super("La factura " + facturaId + " ya fue anulada por una nota de credito (CAE " + cae + ")");
    }
}
