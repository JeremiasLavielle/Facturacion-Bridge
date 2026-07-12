package com.bridge.facturacion.arca;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Detalle de un comprobante YA emitido en ARCA (respuesta de FECompConsultar).
 * Se usa para la recuperacion del "timeout fantasma": comparar estos datos
 * contra una factura en ERROR para saber si en realidad ya fue emitida.
 */
public record ComprobanteEmitido(
        long numero,
        long docNro,
        BigDecimal importeTotal,
        LocalDate servicioDesde,
        String cae,
        LocalDate vencimientoCae
) {}
