package com.bridge.facturacion.alumno;

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
