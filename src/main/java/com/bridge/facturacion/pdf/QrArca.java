package com.bridge.facturacion.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;

final class QrArca {

    private static final String BASE_URL = "https://www.afip.gob.ar/fe/qr/?p=";

    private QrArca() {}

    static String buildUrl(LocalDate fechaEmision, long cuitEmisor, int puntoVenta,
                           int tipoComprobante, long numeroComprobante, BigDecimal importe,
                           int tipoDocReceptor, long docReceptor, String cae) {
        // JSON armado a mano: son 13 campos fijos, no justifica traer un mapper.
        String json = """
                {"ver":1,"fecha":"%s","cuit":%d,"ptoVta":%d,"tipoCmp":%d,"nroCmp":%d,\
                "importe":%s,"moneda":"PES","ctz":1,"tipoDocRec":%d,"nroDocRec":%d,\
                "tipoCodAut":"E","codAut":%s}"""
                .formatted(fechaEmision, cuitEmisor, puntoVenta, tipoComprobante,
                        numeroComprobante,
                        importe.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                        tipoDocReceptor, docReceptor, cae);
        return BASE_URL + Base64.getUrlEncoder()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}

