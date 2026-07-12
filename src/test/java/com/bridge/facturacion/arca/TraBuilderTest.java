package com.bridge.facturacion.arca;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TraBuilderTest {

    // SoapClient pide ArcaProperties por los timeouts; aca solo usamos parse().
    private final SoapClient parser = new SoapClient(new ArcaProperties(
            "20111111112", 1, "c", "k", "u1", "u2", Ambiente.HOMOLOGACION, 15, 45));

    @Test
    void build_generaUnTraValido() {
        String tra = TraBuilder.build();

        // Si el XML esta mal formado, parse() explota y el test falla.
        Document doc = parser.parse(tra);

        assertEquals("wsfe", parser.firstText(doc, "service"));
        assertTrue(parser.firstText(doc, "uniqueId").matches("\\d+"));
    }

    @Test
    void build_retrocedeGenerationTime_yExpiraEnElFuturo() {
        Document doc = parser.parse(TraBuilder.build());

        OffsetDateTime generation = OffsetDateTime.parse(parser.firstText(doc, "generationTime"));
        OffsetDateTime expiration = OffsetDateTime.parse(parser.firstText(doc, "expirationTime"));
        OffsetDateTime ahora = OffsetDateTime.now();

        // generationTime en el pasado (tolerancia de reloj con ARCA)...
        assertTrue(generation.isBefore(ahora));
        // ...y expirationTime bien adelante.
        assertTrue(expiration.isAfter(ahora.plusHours(11)));
    }
}
