package com.bridge.facturacion.pdf;

import com.bridge.facturacion.alumno.Alumno;
import com.bridge.facturacion.alumno.CondicionIva;
import com.bridge.facturacion.arca.Ambiente;
import com.bridge.facturacion.arca.ArcaProperties;
import com.bridge.facturacion.factura.Factura;
import com.bridge.facturacion.factura.FacturaRepository;
import com.bridge.facturacion.factura.exception.FacturaNoEmitidaException;
import com.bridge.facturacion.factura.exception.FacturaNotFoundException;
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

        factura = Factura.pendiente(alumno, new BigDecimal("15000.00"), LocalDate.of(2026, 7, 1));

        facturaRepository = mock(FacturaRepository.class);
        ArcaProperties arca = new ArcaProperties(
                "20463447277", 1, "c", "k", "u1", "u2", Ambiente.HOMOLOGACION, 15, 45);
        EmisorProperties emisor = new EmisorProperties(
                "Instituto Bridge", "Titular Bridge", "Calle 1 - La Plata",
                "Responsable Monotributo", "Exento", "01/01/2020");
        pdfService = new PdfService(facturaRepository, arca, emisor);
    }

    @Test
    void generar_produceUnPdfValido_paraUnaFacturaEmitida() {
        factura.marcarEmitida("86270536276914", LocalDate.of(2026, 7, 18), 42L);

        byte[] pdf = pdfService.generar(factura);

        // Firma del formato: todo PDF empieza con "%PDF".
        assertTrue(pdf.length > 1000, "un comprobante real no puede pesar tan poco");
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    void nombreArchivo_usaPuntoVentaYNumeroConCeros() {
        factura.marcarEmitida("86270536276914", LocalDate.of(2026, 7, 18), 42L);

        assertEquals("factura-0001-00000042.pdf", pdfService.nombreArchivo(factura));
    }

    @Test
    void buscarEmitida_tiraExcepcion_cuandoLaFacturaEstaPendiente() {
        // Sin CAE no hay comprobante que imprimir.
        when(facturaRepository.findById(5L)).thenReturn(Optional.of(factura));

        assertThrows(FacturaNoEmitidaException.class, () -> pdfService.buscarEmitida(5L));
    }

    @Test
    void buscarEmitida_tiraExcepcion_cuandoEsEmitidaHistoricaSinNumero() {
        // Emitida antes de V4: tiene CAE pero numero_comprobante NULL.
        // Antes esto era un NPE criptico al armar el QR; ahora es un 409 claro.
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
}
