package com.bridge.facturacion.factura.exception;

public class FacturaNoEmitidaException extends RuntimeException {

    public FacturaNoEmitidaException(Long id) {
        super("La factura " + id + " no fue emitida todavia: no tiene comprobante para descargar");
    }

    private FacturaNoEmitidaException(String message) {
        super(message);
    }

    public static FacturaNoEmitidaException sinNumeroComprobante(Long id) {
        return new FacturaNoEmitidaException(
                "La factura " + id + " tiene CAE pero no tiene numero de comprobante registrado "
                + "(fue emitida antes de que se persistiera). Completar numero_comprobante en la "
                + "base o reemitir una factura nueva.");
    }
}
