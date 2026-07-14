package com.bridge.facturacion.arca;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ComprobanteEmitido(
        long numero,
        long docNro,
        BigDecimal importeTotal,
        LocalDate servicioDesde,
        String cae,
        LocalDate vencimientoCae
) {}
