package com.bridge.facturacion.arca;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Construye el TRA (Ticket Request Access): el XML donde le pedimos a WSAA
 * acceso al servicio "wsfe" por un rango de tiempo.
 *
 * generationTime se retrocede 10 minutos para tolerar diferencias de reloj
 * entre nuestro servidor y el de ARCA (causa clasica de rechazo).
 */
final class TraBuilder {

    private static final DateTimeFormatter ISO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private TraBuilder() {}

    static String build() {
        ZonedDateTime now = ZonedDateTime.now();
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <loginTicketRequest version="1.0">
                    <header>
                        <uniqueId>%d</uniqueId>
                        <generationTime>%s</generationTime>
                        <expirationTime>%s</expirationTime>
                    </header>
                    <service>wsfe</service>
                </loginTicketRequest>
                """.formatted(
                now.toEpochSecond(),
                ISO.format(now.minusMinutes(10)),
                ISO.format(now.plusHours(12)));
    }
}
