package com.bridge.facturacion.pdf;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;


class QrArcaTest {

    @Test
    void buildUrl_generaElPayloadQueExigeLaRg4892() {
        String url = QrArca.buildUrl(
                LocalDate.of(2026, 7, 8), 20463447277L, 1, 11, 42L,
                new BigDecimal("15000"), 96, 12345678L, "86270536276914");

        assertTrue(url.startsWith("https://www.afip.gob.ar/fe/qr/?p="));

        String base64 = url.substring(url.indexOf("?p=") + 3);
        String json = new String(Base64.getUrlDecoder().decode(base64), StandardCharsets.UTF_8);

        assertTrue(json.contains("\"ver\":1"));
        assertTrue(json.contains("\"fecha\":\"2026-07-08\""));
        assertTrue(json.contains("\"cuit\":20463447277"));
        assertTrue(json.contains("\"ptoVta\":1"));
        assertTrue(json.contains("\"tipoCmp\":11"));            // Factura C
        assertTrue(json.contains("\"nroCmp\":42"));
        assertTrue(json.contains("\"importe\":15000.00"));      // siempre 2 decimales
        assertTrue(json.contains("\"tipoDocRec\":96"));
        assertTrue(json.contains("\"nroDocRec\":12345678"));
        assertTrue(json.contains("\"tipoCodAut\":\"E\""));      // E = CAE
        assertTrue(json.contains("\"codAut\":86270536276914"));
    }
}
