package com.bridge.facturacion.notacredito.exception;

import com.bridge.facturacion.factura.EstadoFactura;

/** Solo se anulan facturas EMITIDAS: sin CAE no hay nada que anular. */
public class FacturaNoAnulableException extends RuntimeException {
    public FacturaNoAnulableException(Long facturaId, EstadoFactura estado) {
        super("La factura " + facturaId + " esta " + estado
                + ": solo una factura EMITIDA puede anularse con nota de credito");
    }
}
