package com.bridge.facturacion.alumno;

/**
 * Condicion frente al IVA del alumno.
 * codigoArca es el codigo que WSFE exige como CondicionIVAReceptorId
 * (obligatorio desde RG 5616): es un dato del dominio, por eso vive aca.
 */
public enum CondicionIva {
    RESPONSABLE_INSCRIPTO(1),
    MONOTRIBUTISTA(6),
    CONSUMIDOR_FINAL(5),
    EXENTO(4);

    private final int codigoArca;

    CondicionIva(int codigoArca) {
        this.codigoArca = codigoArca;
    }

    public int getCodigoArca() {
        return codigoArca;
    }
}
