package com.bridge.facturacion.pdf;

import com.bridge.facturacion.alumno.Alumno;
import com.bridge.facturacion.alumno.CondicionIva;
import com.bridge.facturacion.arca.Ambiente;
import com.bridge.facturacion.arca.ArcaProperties;
import com.bridge.facturacion.factura.Factura;
import com.bridge.facturacion.factura.FacturaRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class PdfMuestraTest {

    @Test
    void generaUnComprobanteDeMuestra_yLoGuardaEnTarget() throws Exception {
        // Emisor y config como los reales (datos de fantasia).
        ArcaProperties arca = new ArcaProperties(
                "20463447277", 1, "c", "k", "u1", "u2", Ambiente.HOMOLOGACION, 15, 45);
        EmisorProperties emisor = new EmisorProperties(
                "Instituto Bridge",
                "Nombre y Apellido del Titular",
                "Avenida 44 Nro. 1234 - (1900) La Plata, Buenos Aires",
                "Responsable Monotributo",
                "Exento",
                "01/03/2018");
        PdfService pdfService = new PdfService(mock(FacturaRepository.class), arca, emisor);

        Alumno alumno = new Alumno();
        alumno.setNombre("Juan Ignacio Perez");
        alumno.setDni("38456789");
        alumno.setCondicionIva(CondicionIva.CONSUMIDOR_FINAL);

        Factura factura = Factura.pendiente(alumno, new BigDecimal("45000.00"), LocalDate.of(2026, 7, 1));
        factura.marcarEmitida("86270536276914", LocalDate.of(2026, 7, 18), 42L);

        byte[] pdf = pdfService.generar(factura);
        Path destino = Path.of("target", "factura-muestra.pdf");
        Files.createDirectories(destino.getParent());
        Files.write(destino, pdf);

        System.out.println(">>> PDF de muestra generado en: " + destino.toAbsolutePath());
        assertTrue(Files.size(destino) > 1000);
    }
}
