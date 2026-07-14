package com.bridge.facturacion.arca;

import java.time.LocalDate;
import java.util.List;

public record ResultadoEmision(
        boolean aprobada,
        long numeroComprobante,
        String cae,
        LocalDate vencimientoCae,
        List<String> mensajes
) {}
