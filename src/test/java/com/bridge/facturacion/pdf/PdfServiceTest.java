package com.bridge.facturacion.pdf;

import com.bridge.facturacion.EmisoresDePrueba;
import com.bridge.facturacion.alumno.Alumno;
import com.bridge.facturacion.alumno.CondicionIva;
import com.bridge.facturacion.emisor.Emisor;
import com.bridge.facturacion.factura.Factura;
import com.bridge.facturacion.factura.FacturaRepository;
import com.bridge.facturacion.factura.exception.FacturaNoEmitidaException;
import com.bridge.facturacion.factura.exception.FacturaNotFoundException;
import com.bridge.facturacion.notacredito.NotaCredito;
import com.bridge.facturacion.notacredito.NotaCreditoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PdfServiceTest {

    private FacturaRepository facturaRepository;
    private PdfService pdfService;
    private Factura factura;

    @BeforeEach
    void setUp() {
        Alumno alumno = new Alumno();
        alumno.setNombre("Juan Perez");
        alumno.setDni("12345678");
        alumno.setCondicionIva(CondicionIva.CONSUMIDOR_FINAL);

Emisor emisor = EmisoresDePrueba.emisor(1L, "20463447277", 1);
        factura = Factura.pendiente(alumno, emisor, new BigDecimal("15000.00"), LocalDate.of(2026, 7, 1));

        facturaRepository = mock(FacturaRepository.class);
        pdfService = new PdfService(facturaRepository, mock(NotaCreditoRepository.class));
    }

    @Test
    void generar_produceUnPdfValido_paraUnaFacturaEmitida() {
        factura.marcarEmitida("86270536276914", LocalDate.of(2026, 7, 18), 42L);

        byte[] pdf = pdfService.generar(factura);

assertTrue(pdf.length > 1000, "un comprobante real no puede pesar tan poco");
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    void nombreArchivo_usaAlumnoYPeriodo() {
        factura.marcarEmitida("86270536276914", LocalDate.of(2026, 7, 18), 42L);

        assertEquals("Juan Perez - 2026-07.pdf", pdfService.nombreArchivo(factura));
    }

    @Test
    void nombreArchivo_quitaLosCaracteresQueRompenUnNombreDeArchivo() {
        factura.getAlumno().setNombre("Perez / Juan: \"el flaco\"");
        factura.marcarEmitida("86270536276914", LocalDate.of(2026, 7, 18), 42L);

        String nombre = pdfService.nombreArchivo(factura);

        assertEquals("Perez Juan el flaco - 2026-07.pdf", nombre);
        assertFalse(nombre.matches(".*[\\\\/:*?\"<>|].*"),
                "el nombre no puede contener caracteres invalidos para un archivo");
    }

    @Test
    void buscarEmitida_tiraExcepcion_cuandoLaFacturaEstaPendiente() {

        when(facturaRepository.findById(5L)).thenReturn(Optional.of(factura));

        assertThrows(FacturaNoEmitidaException.class, () -> pdfService.buscarEmitida(5L));
    }

    @Test
    void buscarEmitida_tiraExcepcion_cuandoEsEmitidaHistoricaSinNumero() {

factura.marcarEmitida("86270536276914", LocalDate.of(2026, 7, 18), null);
        when(facturaRepository.findById(5L)).thenReturn(Optional.of(factura));

        FacturaNoEmitidaException ex = assertThrows(FacturaNoEmitidaException.class,
                () -> pdfService.buscarEmitida(5L));
        assertTrue(ex.getMessage().contains("numero de comprobante"));
    }

    @Test
    void buscarEmitida_tiraExcepcion_cuandoNoExiste() {
        when(facturaRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(FacturaNotFoundException.class, () -> pdfService.buscarEmitida(9L));
    }

@Test
    void buscarEmitida_permiteFacturasAnuladas_porqueElComprobanteExiste() {
        factura.marcarEmitida("86270536276914", LocalDate.of(2026, 7, 18), 42L);
        factura.marcarAnulada();
        when(facturaRepository.findById(5L)).thenReturn(Optional.of(factura));

        assertSame(factura, pdfService.buscarEmitida(5L));
    }

    @Test
    void generar_produceUnPdfValido_paraUnaNotaDeCredito() {
        factura.marcarEmitida("86270536276914", LocalDate.of(2026, 7, 18), 42L);
        NotaCredito nc = NotaCredito.pendiente(factura, "error en el monto");
        nc.marcarEmitida("86270536276999", LocalDate.of(2026, 8, 30), 5L);

        byte[] pdf = pdfService.generar(nc);

        assertTrue(pdf.length > 1000, "un comprobante real no puede pesar tan poco");
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
        // Prefijo NC: comparte alumno y periodo con su factura, y sin el los dos
        // PDF se pisarian al descargarlos en la misma carpeta.
        assertEquals("NC Juan Perez - 2026-07.pdf", pdfService.nombreArchivo(nc));
    }
}
