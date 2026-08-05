package com.bridge.facturacion.factura;

import com.bridge.facturacion.EmisoresDePrueba;
import com.bridge.facturacion.alumno.Alumno;
import com.bridge.facturacion.alumno.AlumnoRepository;
import com.bridge.facturacion.alumno.CondicionIva;
import com.bridge.facturacion.alumno.exception.AlumnoNotFoundException;
import com.bridge.facturacion.arca.ArcaClient;
import com.bridge.facturacion.arca.ArcaComunicacionException;
import com.bridge.facturacion.arca.ArcaException;
import com.bridge.facturacion.arca.ComprobanteEmitido;
import com.bridge.facturacion.arca.ResultadoEmision;
import com.bridge.facturacion.emisor.Emisor;
import com.bridge.facturacion.emisor.EmisorRepository;
import com.bridge.facturacion.emisor.exception.EmisorNotFoundException;
import com.bridge.facturacion.factura.dto.FacturaRequestDTO;
import com.bridge.facturacion.factura.exception.FacturaAlreadyExistsException;
import com.bridge.facturacion.factura.exception.FacturaYaEmitidaException;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacturaServiceTest {

@Mock
    private AlumnoRepository alumnoRepository;
    @Mock
    private FacturaRepository facturaRepository;
    @Mock
    private EmisorRepository emisorRepository;
    @Mock
    private FacturaMapper facturaMapper;
    @Mock
    private ArcaClient arcaClient;

@InjectMocks
    private FacturaService facturaService;

private Alumno alumno;
    private Emisor emisor;
    private FacturaRequestDTO request;
    private final LocalDate periodo = LocalDate.of(2026, 5, 1);
    private final BigDecimal monto = new BigDecimal("15000.00");

    @BeforeEach
    void setUp() {
        alumno = new Alumno();
        alumno.setId(1L);
        alumno.setNombre("Juan Perez");
        alumno.setDni("12345678");
        alumno.setCondicionIva(CondicionIva.CONSUMIDOR_FINAL);

        emisor = EmisoresDePrueba.emisor(1L, "20111111112", 1);

        request = new FacturaRequestDTO();
        request.setAlumnoId(1L);
        request.setEmisorId(1L);
        request.setMonto(monto);
        request.setPeriodo(periodo);
    }

@Test
    void create_guardaEnEstadoPendiente_conElEmisorElegido() {

        when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));
        when(emisorRepository.findById(1L)).thenReturn(Optional.of(emisor));
        when(facturaRepository.existsByAlumnoAndPeriodo(alumno, periodo)).thenReturn(false);

facturaService.create(request);

ArgumentCaptor<Factura> captor = ArgumentCaptor.forClass(Factura.class);
        verify(facturaRepository).save(captor.capture());
        Factura guardada = captor.getValue();

        assertEquals(EstadoFactura.PENDIENTE, guardada.getEstado());
        assertSame(alumno, guardada.getAlumno());
        assertSame(emisor, guardada.getEmisor());
        assertEquals(monto, guardada.getMonto());
        assertEquals(periodo, guardada.getPeriodo());
    }

    @Test
    void create_tiraExcepcion_cuandoElAlumnoNoExiste() {

        when(alumnoRepository.findById(1L)).thenReturn(Optional.empty());

assertThrows(AlumnoNotFoundException.class,
                () -> facturaService.create(request));

verify(facturaRepository, never()).save(any());
    }

    @Test
    void create_tiraExcepcion_cuandoElEmisorNoExiste() {

        when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));
        when(emisorRepository.findById(1L)).thenReturn(Optional.empty());

assertThrows(EmisorNotFoundException.class,
                () -> facturaService.create(request));

        verify(facturaRepository, never()).save(any());
    }

    @Test
    void create_tiraExcepcion_cuandoYaExisteFacturaDelPeriodo() {

when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));
        when(emisorRepository.findById(1L)).thenReturn(Optional.of(emisor));
        when(facturaRepository.existsByAlumnoAndPeriodo(alumno, periodo)).thenReturn(true);

assertThrows(FacturaAlreadyExistsException.class,
                () -> facturaService.create(request));

        verify(facturaRepository, never()).save(any());
    }

@Test
    void emitir_marcaEmitidaConCae_cuandoArcaAprueba() {

Factura factura = Factura.pendiente(alumno, emisor, monto, periodo);
        when(facturaRepository.findById(5L)).thenReturn(Optional.of(factura));

when(arcaClient.solicitarCae(emisor, 96, 12345678L, monto, periodo, 5))
                .thenReturn(new ResultadoEmision(
                        true, 42, "75123456789012", LocalDate.of(2026, 7, 18), List.of()));
        when(facturaRepository.save(factura)).thenReturn(factura);

facturaService.emitir(5L);

assertEquals(EstadoFactura.EMITIDA, factura.getEstado());
        assertEquals("75123456789012", factura.getCae());
        assertEquals(LocalDate.of(2026, 7, 18), factura.getVencimientoCae());
        assertEquals(42L, factura.getNumeroComprobante());
        assertNotNull(factura.getFechaEmision());
        assertNull(factura.getMensajeError());
        verify(facturaRepository).save(factura);
    }

    @Test
    void emitir_marcaError_cuandoArcaRechaza() {

Factura factura = Factura.pendiente(alumno, emisor, monto, periodo);
        when(facturaRepository.findById(5L)).thenReturn(Optional.of(factura));
        when(arcaClient.solicitarCae(emisor, 96, 12345678L, monto, periodo, 5))
                .thenReturn(new ResultadoEmision(
                        false, 42, null, null, List.of("[10048] Campo DocNro invalido")));
        when(facturaRepository.save(factura)).thenReturn(factura);

facturaService.emitir(5L);

assertEquals(EstadoFactura.ERROR, factura.getEstado());
        assertNull(factura.getCae());
        assertTrue(factura.getMensajeError().contains("10048"));
    }

    @Test
    void emitir_marcaErrorYPropaga_cuandoFallaLaComunicacion() {

        Factura factura = Factura.pendiente(alumno, emisor, monto, periodo);
        when(facturaRepository.findById(5L)).thenReturn(Optional.of(factura));
        when(arcaClient.solicitarCae(emisor, 96, 12345678L, monto, periodo, 5))
                .thenThrow(new ArcaException("Fallo la comunicacion con ARCA: timeout"));

assertThrows(ArcaException.class, () -> facturaService.emitir(5L));

assertEquals(EstadoFactura.ERROR, factura.getEstado());
        assertTrue(factura.getMensajeError().contains("timeout"));
        verify(facturaRepository).save(factura);
    }

    @Test
    void emitir_tiraExcepcion_cuandoYaTieneCae() {

Factura factura = Factura.pendiente(alumno, emisor, monto, periodo);
        factura.marcarEmitida("75123456789012", LocalDate.of(2026, 7, 18), 42L);
        when(facturaRepository.findById(5L)).thenReturn(Optional.of(factura));

assertThrows(FacturaYaEmitidaException.class, () -> facturaService.emitir(5L));
        verify(arcaClient, never()).solicitarCae(any(), anyInt(), anyLong(), any(), any(), anyInt());
        verify(facturaRepository, never()).save(any());
    }

@Test
    void emitir_recuperaElCae_cuandoElReintentoEncuentraElFantasma() {

Factura factura = Factura.pendiente(alumno, emisor, monto, periodo);
        factura.marcarError("Fallo la comunicacion con ARCA: timeout");
        when(facturaRepository.findById(5L)).thenReturn(Optional.of(factura));
        when(arcaClient.consultarUltimoEmitido(emisor)).thenReturn(new ComprobanteEmitido(
                42, 12345678L, monto, periodo, "75123456789012", LocalDate.of(2026, 7, 18)));
        when(facturaRepository.save(factura)).thenReturn(factura);

facturaService.emitir(5L);

assertEquals(EstadoFactura.EMITIDA, factura.getEstado());
        assertEquals("75123456789012", factura.getCae());
        assertEquals(42L, factura.getNumeroComprobante());
        verify(arcaClient, never()).solicitarCae(any(), anyInt(), anyLong(), any(), any(), anyInt());
    }

    @Test
    void emitir_emiteNormalmente_cuandoElUltimoComprobanteNoEsElFantasma() {

Factura factura = Factura.pendiente(alumno, emisor, monto, periodo);
        factura.marcarError("rechazo previo");
        when(facturaRepository.findById(5L)).thenReturn(Optional.of(factura));
        when(arcaClient.consultarUltimoEmitido(emisor)).thenReturn(new ComprobanteEmitido(
                42, 12345678L, new BigDecimal("99999.00"), periodo, "70000000000001", LocalDate.of(2026, 7, 18)));
        when(arcaClient.solicitarCae(emisor, 96, 12345678L, monto, periodo, 5))
                .thenReturn(new ResultadoEmision(
                        true, 43, "75123456789013", LocalDate.of(2026, 7, 18), List.of()));
        when(facturaRepository.save(factura)).thenReturn(factura);

facturaService.emitir(5L);

assertEquals(EstadoFactura.EMITIDA, factura.getEstado());
        assertEquals("75123456789013", factura.getCae());
    }

private Factura facturaPendienteConId(long id, Emisor emisorDeLaFactura) {
        Factura factura = Factura.pendiente(alumno, emisorDeLaFactura, monto, periodo);
        ReflectionTestUtils.setField(factura, "id", id);
        return factura;
    }

    @Test
    void emitirPorPeriodo_cortaElLote_cuandoFallaLaComunicacion() {

Factura f1 = facturaPendienteConId(1L, emisor);
        Factura f2 = facturaPendienteConId(2L, emisor);
        Factura f3 = facturaPendienteConId(3L, emisor);
        when(facturaRepository.findByPeriodo(periodo)).thenReturn(List.of(f1, f2, f3));
        when(facturaRepository.findById(1L)).thenReturn(Optional.of(f1));
        when(facturaRepository.findById(2L)).thenReturn(Optional.of(f2));
        when(arcaClient.solicitarCae(emisor, 96, 12345678L, monto, periodo, 5))
                .thenReturn(new ResultadoEmision(
                        true, 42, "75123456789012", LocalDate.of(2026, 7, 18), List.of()))
                .thenThrow(new ArcaComunicacionException(
                        "Fallo la comunicacion con ARCA: timeout", new RuntimeException()));
        when(facturaRepository.save(any(Factura.class))).thenAnswer(inv -> inv.getArgument(0));

facturaService.emitirPorPeriodo(periodo);

verify(arcaClient, times(2)).solicitarCae(any(), anyInt(), anyLong(), any(), any(), anyInt());
        verify(facturaRepository, never()).findById(3L);
        assertEquals(EstadoFactura.EMITIDA, f1.getEstado());
        assertEquals(EstadoFactura.ERROR, f2.getEstado());
        assertEquals(EstadoFactura.PENDIENTE, f3.getEstado());
    }

    @Test
    void emitirPorPeriodo_sigueConLaProxima_cuandoArcaRechaza() {

Factura f1 = facturaPendienteConId(1L, emisor);
        Factura f2 = facturaPendienteConId(2L, emisor);
        when(facturaRepository.findByPeriodo(periodo)).thenReturn(List.of(f1, f2));
        when(facturaRepository.findById(1L)).thenReturn(Optional.of(f1));
        when(facturaRepository.findById(2L)).thenReturn(Optional.of(f2));
        when(arcaClient.solicitarCae(emisor, 96, 12345678L, monto, periodo, 5))
                .thenThrow(new ArcaException("SOAP Fault de ARCA: token invalido"))
                .thenReturn(new ResultadoEmision(
                        true, 42, "75123456789012", LocalDate.of(2026, 7, 18), List.of()));
        when(facturaRepository.save(any(Factura.class))).thenAnswer(inv -> inv.getArgument(0));

facturaService.emitirPorPeriodo(periodo);

assertEquals(EstadoFactura.ERROR, f1.getEstado());
        assertEquals(EstadoFactura.EMITIDA, f2.getEstado());
    }

    @Test
    void emitirPorPeriodo_unCorteEnUnEmisor_noFrenaElLoteDelOtro() {

Emisor emisorDos = EmisoresDePrueba.emisor(2L, "20222222223", 1);
        Factura f1 = facturaPendienteConId(1L, emisor);
        Factura f2 = facturaPendienteConId(2L, emisor);
        Factura f3 = facturaPendienteConId(3L, emisorDos);
        when(facturaRepository.findByPeriodo(periodo)).thenReturn(List.of(f1, f2, f3));
        when(facturaRepository.findById(1L)).thenReturn(Optional.of(f1));
        when(facturaRepository.findById(3L)).thenReturn(Optional.of(f3));
        when(arcaClient.solicitarCae(eq(emisor), anyInt(), anyLong(), any(), any(), anyInt()))
                .thenThrow(new ArcaComunicacionException(
                        "Fallo la comunicacion con ARCA: timeout", new RuntimeException()));
        when(arcaClient.solicitarCae(eq(emisorDos), anyInt(), anyLong(), any(), any(), anyInt()))
                .thenReturn(new ResultadoEmision(
                        true, 42, "75123456789012", LocalDate.of(2026, 7, 18), List.of()));
        when(facturaRepository.save(any(Factura.class))).thenAnswer(inv -> inv.getArgument(0));

facturaService.emitirPorPeriodo(periodo);

assertEquals(EstadoFactura.ERROR, f1.getEstado());
        assertEquals(EstadoFactura.PENDIENTE, f2.getEstado());
        assertEquals(EstadoFactura.EMITIDA, f3.getEstado());
        verify(facturaRepository, never()).findById(2L);
    }
}
