package com.bridge.facturacion.emisor.exception;

/**
 * El emisor existe pero fue dado de baja: no se pueden crear facturas nuevas
 * a su nombre.
 *
 * <p>Deliberadamente NO se valida al emitir. Una factura ya creada mientras el
 * emisor estaba activo corresponde a un servicio realmente prestado, así que
 * debe poder emitirse aunque después se lo desactive. Bloquearla la dejaría
 * varada sin forma de facturarla.
 */
public class EmisorInactivoException extends RuntimeException {
    public EmisorInactivoException(Long id) {
        super("El emisor " + id + " esta dado de baja y no admite facturas nuevas");
    }
}
