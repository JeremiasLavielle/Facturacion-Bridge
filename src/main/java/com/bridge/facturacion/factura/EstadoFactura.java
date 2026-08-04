package com.bridge.facturacion.factura;

public enum EstadoFactura {
    PENDIENTE,
    EMITIDA,
    ERROR,
    /** Emitida y luego anulada por su nota de credito (Fase 8). */
    ANULADA
}
