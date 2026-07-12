package com.bridge.facturacion.arca;

import java.time.Instant;

/**
 * Token + Sign que devuelve WSAA, validos ~12 horas.
 * Van en el bloque Auth de cada llamada a WSFE.
 */
public record Credenciales(String token, String sign, Instant expiration) {

    /** Margen de 5 minutos: renovamos antes del vencimiento para no usar un token muerto. */
    public boolean vigente() {
        return Instant.now().isBefore(expiration.minusSeconds(300));
    }
}
