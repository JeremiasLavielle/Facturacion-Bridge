package com.bridge.facturacion.pdf;

import com.bridge.facturacion.alumno.Alumno;
import com.bridge.facturacion.alumno.CondicionIva;
import com.bridge.facturacion.emisor.Emisor;
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
        // Emisor como los reales (datos de fantasia). Fase 7: el PDF sale
        // del emisor de la factura, no de una config global.
        Emisor emisor = new Emisor();
        emisor.setId(1L);
        emisor.setCuit("20463447277");
        emisor.setNombreFantasia("Instituto Bridge");
        emisor.setRazonSocial("Nombre y Apellido del Titular");
        emisor.setDomicilio("Avenida 44 Nro. 1234 - (1900) La Plata, Buenos Aires");
        emisor.setCondicionFiscal("Responsable Monotributo");
        emisor.setIngresosBrutos("Exento");
        emisor.setInicioActividades("01/03/2018");
        emisor.setPuntoVenta(1);
        emisor.setCertPath("20463447277/certificado.crt");
        emisor.setKeyPath("20463447277/clave-privada.key");
        PdfService pdfService = new PdfService(
                mock(FacturaRepository.class),
                mock(com.bridge.facturacion.notacredito.NotaCreditoRepository.class));

        Alumno alumno = new Alumno();
        alumno.setNombre("Juan Ignacio Perez");
        alumno.setDni("38456789");
        alumno.setCondicionIva(CondicionIva.CONSUMIDOR_FINAL);

        Factura factura = Factura.pendiente(alumno, emisor, new BigDecimal("45000.00"), LocalDate.of(2026, 7, 1));
        factura.marcarEmitida("86270536276914", LocalDate.of(2026, 7, 18), 42L);

        byte[] pdf = pdfService.generar(factura);
        Path destino = Path.of("target", "factura-muestra.pdf");
        Files.createDirectories(destino.getParent());
        Files.write(destino, pdf);

        System.out.println(">>> PDF de muestra generado en: " + destino.toAbsolutePath());
        assertTrue(Files.size(destino) > 1000);
    }
}
