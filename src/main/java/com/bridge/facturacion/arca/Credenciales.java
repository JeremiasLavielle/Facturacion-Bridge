package com.bridge.facturacion.arca;

import java.time.Instant;

public record Credenciales(String token, String sign, Instant expiration) {
    public boolean vigente() {
        return Instant.now().isBefore(expiration.minusSeconds(300));
    }
}
