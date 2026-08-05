package com.bridge.facturacion.notacredito;

import com.bridge.facturacion.EmisoresDePrueba;
import com.bridge.facturacion.alumno.Alumno;
import com.bridge.facturacion.alumno.CondicionIva;
import com.bridge.facturacion.arca.ArcaClient;
import com.bridge.facturacion.arca.ArcaException;
import com.bridge.facturacion.arca.ComprobanteAsociado;
import com.bridge.facturacion.arca.ComprobanteEmitido;
import com.bridge.facturacion.arca.ResultadoEmision;
import com.bridge.facturacion.emisor.Emisor;
import com.bridge.facturacion.factura.EstadoFactura;
import com.bridge.facturacion.factura.Factura;
import com.bridge.facturacion.factura.FacturaRepository;
import com.bridge.facturacion.notacredito.exception.FacturaNoAnulableException;
import com.bridge.facturacion.notacredito.exception.NotaCreditoYaEmitidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotaCreditoServiceTest {

    @Mock
    private FacturaRepository facturaRepository;
    @Mock
    private NotaCreditoRepository notaCreditoRepository;
    @Mock
    private NotaCreditoMapper notaCreditoMapper;
    @Mock
    private ArcaClient arcaClient;

    @InjectMocks
    private NotaCreditoService notaCreditoService;

    private Emisor emisor;
    private Factura factura;
    private final LocalDate periodo = LocalDate.of(2026, 7, 1);
    private final BigDecimal monto = new BigDecimal("15000.00");

    @BeforeEach
    void setUp() {
        Alumno alumno = new Alumno();
        alumno.setId(1L);
        alumno.setNombre("Juan Perez");
        alumno.setDni("12345678");
        alumno.setCondicionIva(CondicionIva.CONSUMIDOR_FINAL);

        emisor = EmisoresDePrueba.emisor(1L, "20111111112", 1);

factura = Factura.pendiente(alumno, emisor, monto, periodo);
        ReflectionTestUtils.setField(factura, "id", 5L);
        factura.marcarEmitida("75123456789012", LocalDate.of(2026, 7, 18), 42L);
    }

private void stubPersistenciaDeNc() {
        AtomicReference<NotaCredito> guardada = new AtomicReference<>();
        when(notaCreditoRepository.save(any(NotaCredito.class))).thenAnswer(inv -> {
            NotaCredito nc = inv.getArgument(0);
            if (nc.getId() == null) {
                ReflectionTestUtils.setField(nc, "id", 10L);
            }
            guardada.set(nc);
            return nc;
        });
        when(notaCreditoRepository.findById(10L)).thenAnswer(inv -> Optional.ofNullable(guardada.get()));
    }

    @Test
    void crearYEmitir_emiteTipo13ConLaFacturaAsociada_yAnulaLaFactura() {
        when(facturaRepository.findById(5L)).thenReturn(Optional.of(factura));
        when(notaCreditoRepository.findByFactura(factura)).thenReturn(Optional.empty());
        stubPersistenciaDeNc();
        when(arcaClient.solicitarCae(eq(emisor), eq(ArcaClient.NOTA_CREDITO_C), any(),
                eq(96), eq(12345678L), eq(monto), eq(periodo), eq(5)))
                .thenReturn(new ResultadoEmision(
                        true, 8, "75123456789099", LocalDate.of(2026, 8, 30), List.of()));

        notaCreditoService.crearYEmitir(5L, "error en el monto");

ArgumentCaptor<ComprobanteAsociado> asociado = ArgumentCaptor.forClass(ComprobanteAsociado.class);
        verify(arcaClient).solicitarCae(eq(emisor), eq(ArcaClient.NOTA_CREDITO_C), asociado.capture(),
                eq(96), eq(12345678L), eq(monto), eq(periodo), eq(5));
        assertEquals(ArcaClient.FACTURA_C, asociado.getValue().tipo());
        assertEquals(42L, asociado.getValue().numero());
        assertEquals("20111111112", asociado.getValue().cuitEmisor());

assertEquals(EstadoFactura.ANULADA, factura.getEstado());
        assertEquals("75123456789012", factura.getCae());
        verify(facturaRepository).save(factura);
    }

    @Test
    void crearYEmitir_rechazaFacturasNoEmitidas() {
        Factura pendiente = Factura.pendiente(factura.getAlumno(), emisor, monto, periodo);
        ReflectionTestUtils.setField(pendiente, "id", 6L);
        when(facturaRepository.findById(6L)).thenReturn(Optional.of(pendiente));
        when(notaCreditoRepository.findByFactura(pendiente)).thenReturn(Optional.empty());

        assertThrows(FacturaNoAnulableException.class,
                () -> notaCreditoService.crearYEmitir(6L, "motivo"));

        verify(notaCreditoRepository, never()).save(any());
        verify(arcaClient, never()).solicitarCae(any(), anyInt(), any(), anyInt(), anyLong(), any(), any(), anyInt());
    }

    @Test
    void crearYEmitir_rechazaLaSegundaNc_cuandoYaHayUnaEmitida() {
        NotaCredito emitida = NotaCredito.pendiente(factura, "primera");
        emitida.marcarEmitida("75123456789099", LocalDate.of(2026, 8, 30), 8L);
        when(facturaRepository.findById(5L)).thenReturn(Optional.of(factura));
        when(notaCreditoRepository.findByFactura(factura)).thenReturn(Optional.of(emitida));

        assertThrows(NotaCreditoYaEmitidaException.class,
                () -> notaCreditoService.crearYEmitir(5L, "segunda"));

        verify(notaCreditoRepository, never()).save(any());
    }

    @Test
    void crearYEmitir_reintentaLaMismaNc_cuandoQuedoEnError() {

NotaCredito enError = NotaCredito.pendiente(factura, "motivo original");
        ReflectionTestUtils.setField(enError, "id", 10L);
        enError.marcarError("Fallo la comunicacion con ARCA: timeout");
        when(facturaRepository.findById(5L)).thenReturn(Optional.of(factura));
        when(notaCreditoRepository.findByFactura(factura)).thenReturn(Optional.of(enError));
        when(notaCreditoRepository.findById(10L)).thenReturn(Optional.of(enError));
        when(arcaClient.consultarUltimoEmitido(emisor, ArcaClient.NOTA_CREDITO_C))
                .thenReturn(new ComprobanteEmitido(
                        8, 12345678L, monto, periodo, "75123456789099", LocalDate.of(2026, 8, 30)));
        when(notaCreditoRepository.save(enError)).thenReturn(enError);

        notaCreditoService.crearYEmitir(5L, "da igual: reintenta la existente");

        assertEquals(EstadoNotaCredito.EMITIDA, enError.getEstado());
        assertEquals("75123456789099", enError.getCae());
        assertEquals(8L, enError.getNumeroComprobante());
        assertEquals(EstadoFactura.ANULADA, factura.getEstado());
        verify(arcaClient, never()).solicitarCae(any(), anyInt(), any(), anyInt(), anyLong(), any(), any(), anyInt());
    }

    @Test
    void emitir_marcaError_cuandoArcaRechaza_yLaFacturaSigueEmitida() {
        NotaCredito nc = NotaCredito.pendiente(factura, "motivo");
        ReflectionTestUtils.setField(nc, "id", 10L);
        when(notaCreditoRepository.findById(10L)).thenReturn(Optional.of(nc));
        when(arcaClient.solicitarCae(eq(emisor), eq(ArcaClient.NOTA_CREDITO_C), any(),
                eq(96), eq(12345678L), eq(monto), eq(periodo), eq(5)))
                .thenReturn(new ResultadoEmision(
                        false, 8, null, null, List.of("[10048] Campo invalido")));
        when(notaCreditoRepository.save(nc)).thenReturn(nc);

        notaCreditoService.emitir(10L);

        assertEquals(EstadoNotaCredito.ERROR, nc.getEstado());
        assertTrue(nc.getMensajeError().contains("10048"));

        assertEquals(EstadoFactura.EMITIDA, factura.getEstado());
        verify(facturaRepository, never()).save(any());
    }

    @Test
    void emitir_marcaErrorYPropaga_cuandoFallaLaComunicacion() {
        NotaCredito nc = NotaCredito.pendiente(factura, "motivo");
        ReflectionTestUtils.setField(nc, "id", 10L);
        when(notaCreditoRepository.findById(10L)).thenReturn(Optional.of(nc));
        when(arcaClient.solicitarCae(eq(emisor), eq(ArcaClient.NOTA_CREDITO_C), any(),
                eq(96), eq(12345678L), eq(monto), eq(periodo), eq(5)))
                .thenThrow(new ArcaException("Fallo la comunicacion con ARCA: timeout"));

        assertThrows(ArcaException.class, () -> notaCreditoService.emitir(10L));

assertEquals(EstadoNotaCredito.ERROR, nc.getEstado());
        assertTrue(nc.getMensajeError().contains("timeout"));
        verify(notaCreditoRepository).save(nc);
        assertEquals(EstadoFactura.EMITIDA, factura.getEstado());
    }
}
