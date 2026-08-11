package com.bridge.facturacion.arca;

import java.time.LocalDate;

public record ComprobanteAsociado(
        int tipo,
        int puntoVenta,
        long numero,
        String cuitEmisor,
        LocalDate fecha
) {}
