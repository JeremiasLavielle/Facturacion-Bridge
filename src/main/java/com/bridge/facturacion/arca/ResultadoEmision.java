package com.bridge.facturacion.arca;

import java.time.LocalDate;
import java.util.List;

/**
 * Resultado de FECAESolicitar ya "traducido" a Java.
 *
 * aprobada == true  -> hay CAE, la factura existe fiscalmente.
 * aprobada == false -> ARCA la rechazo; mensajes explica por que.
 */
public record ResultadoEmision(
        boolean aprobada,
        long numeroComprobante,
        String cae,
        LocalDate vencimientoCae,
        List<String> mensajes
) {}
