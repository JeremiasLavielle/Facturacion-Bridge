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

    // Los "dobles": dependencias falsas que controlamos nosotros.
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

    // El Service REAL, con los dobles inyectados por su constructor.
    @InjectMocks
    private FacturaService facturaService;

    // Datos de prueba reutilizables, armados antes de cada test.
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

    // ---------- create ----------

    @Test
    void create_guardaEnEstadoPendiente_conElEmisorElegido() {
        // Arrange: el alumno y el emisor existen, y no hay factura previa.
        when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));
        when(emisorRepository.findById(1L)).thenReturn(Optional.of(emisor));
        when(facturaRepository.existsByAlumnoAndPeriodo(alumno, periodo)).thenReturn(false);

        // Act
        facturaService.create(request);

        // Assert: atrapamos la Factura que el Service mandó a guardar
        // y revisamos que la construyó como esperábamos.
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
        // Arrange: el repo no encuentra al alumno.
        when(alumnoRepository.findById(1L)).thenReturn(Optional.empty());

        // Act + Assert: debe cortar con la excepción...
        assertThrows(AlumnoNotFoundException.class,
                () -> facturaService.create(request));

        // ...y nunca haber intentado guardar nada.
        verify(facturaRepository, never()).save(any());
    }

    @Test
    void create_tiraExcepcion_cuandoElEmisorNoExiste() {
        // Arrange: el alumno existe pero el emisor elegido no.
        when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));
        when(emisorRepository.findById(1L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(EmisorNotFoundException.class,
                () -> facturaService.create(request));

        verify(facturaRepository, never()).save(any());
    }

    @Test
    void create_tiraExcepcion_cuandoYaExisteFacturaDelPeriodo() {
        // Arrange: alumno y emisor existen, pero ya hay una factura de ese
        // período (restricción GLOBAL: sin importar qué emisor la hizo).
        when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));
        when(emisorRepository.findById(1L)).thenReturn(Optional.of(emisor));
        when(facturaRepository.existsByAlumnoAndPeriodo(alumno, periodo)).thenReturn(true);

        // Act + Assert
        assertThrows(FacturaAlreadyExistsException.class,
                () -> facturaService.create(request));

        verify(facturaRepository, never()).save(any());
    }

    // ---------- emitir (Fase 4: integración ARCA) ----------

    @Test
    void emitir_marcaEmitidaConCae_cuandoArcaAprueba() {
        // Arrange: factura PENDIENTE en el repo, y ARCA aprueba. La emisión
        // usa el EMISOR DE LA FACTURA (no una config global).
        Factura factura = Factura.pendiente(alumno, emisor, monto, periodo);
        when(facturaRepository.findById(5L)).thenReturn(Optional.of(factura));
        // Verificamos de paso el mapeo de dominio: DNI -> docTipo 96,
        // CONSUMIDOR_FINAL -> código ARCA 5.
        when(arcaClient.solicitarCae(emisor, 96, 12345678L, monto, periodo, 5))
                .thenReturn(new ResultadoEmision(
                        true, 42, "75123456789012", LocalDate.of(2026, 7, 18), List.of()));
        when(facturaRepository.save(factura)).thenReturn(factura);

        // Act
        facturaService.emitir(5L);

        // Assert: la transición de estado dejó todo consistente.
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
        // Arrange: ARCA responde, pero rechaza el comprobante (rechazo de
        // negocio: NO es una excepción, queda registrado en la factura).
        Factura factura = Factura.pendiente(alumno, emisor, monto, periodo);
        when(facturaRepository.findById(5L)).thenReturn(Optional.of(factura));
        when(arcaClient.solicitarCae(emisor, 96, 12345678L, monto, periodo, 5))
                .thenReturn(new ResultadoEmision(
                        false, 42, null, null, List.of("[10048] Campo DocNro invalido")));
        when(facturaRepository.save(factura)).thenReturn(factura);

        // Act
        facturaService.emitir(5L);

        // Assert
        assertEquals(EstadoFactura.ERROR, factura.getEstado());
        assertNull(factura.getCae());
        assertTrue(factura.getMensajeError().contains("10048"));
    }

    @Test
    void emitir_marcaErrorYPropaga_cuandoFallaLaComunicacion() {
        // Arrange: no se pudo hablar con ARCA (timeout, red, etc.).
        Factura factura = Factura.pendiente(alumno, emisor, monto, periodo);
        when(facturaRepository.findById(5L)).thenReturn(Optional.of(factura));
        when(arcaClient.solicitarCae(emisor, 96, 12345678L, monto, periodo, 5))
                .thenThrow(new ArcaException("Fallo la comunicacion con ARCA: timeout"));

        // Act + Assert: propaga (el handler global la traduce a 502)...
        assertThrows(ArcaException.class, () -> facturaService.emitir(5L));

        // ...pero ANTES dejó la factura en ERROR y la guardó (reintentable).
        assertEquals(EstadoFactura.ERROR, factura.getEstado());
        assertTrue(factura.getMensajeError().contains("timeout"));
        verify(facturaRepository).save(factura);
    }

    @Test
    void emitir_tiraExcepcion_cuandoYaTieneCae() {
        // Arrange: la factura ya fue emitida; reemitirla duplicaría un
        // comprobante fiscal.
        Factura factura = Factura.pendiente(alumno, emisor, monto, periodo);
        factura.marcarEmitida("75123456789012", LocalDate.of(2026, 7, 18), 42L);
        when(facturaRepository.findById(5L)).thenReturn(Optional.of(factura));

        // Act + Assert: corta antes de tocar ARCA.
        assertThrows(FacturaYaEmitidaException.class, () -> facturaService.emitir(5L));
        verify(arcaClient, never()).solicitarCae(any(), anyInt(), anyLong(), any(), any(), anyInt());
        verify(facturaRepository, never()).save(any());
    }

    // ---------- reintento y timeout fantasma (FECompConsultar) ----------

    @Test
    void emitir_recuperaElCae_cuandoElReintentoEncuentraElFantasma() {
        // Arrange: la factura quedo en ERROR por un corte de comunicacion,
        // pero ARCA SI la habia emitido. El ultimo comprobante DEL EMISOR
        // coincide en DNI, importe y periodo -> es el fantasma.
        Factura factura = Factura.pendiente(alumno, emisor, monto, periodo);
        factura.marcarError("Fallo la comunicacion con ARCA: timeout");
        when(facturaRepository.findById(5L)).thenReturn(Optional.of(factura));
        when(arcaClient.consultarUltimoEmitido(emisor)).thenReturn(new ComprobanteEmitido(
                42, 12345678L, monto, periodo, "75123456789012", LocalDate.of(2026, 7, 18)));
        when(facturaRepository.save(factura)).thenReturn(factura);

        // Act
        facturaService.emitir(5L);

        // Assert: adopto el CAE existente SIN emitir de nuevo (eso seria
        // duplicar un comprobante fiscal).
        assertEquals(EstadoFactura.EMITIDA, factura.getEstado());
        assertEquals("75123456789012", factura.getCae());
        assertEquals(42L, factura.getNumeroComprobante());
        verify(arcaClient, never()).solicitarCae(any(), anyInt(), anyLong(), any(), any(), anyInt());
    }

    @Test
    void emitir_emiteNormalmente_cuandoElUltimoComprobanteNoEsElFantasma() {
        // Arrange: reintento de una ERROR, pero el ultimo comprobante de ARCA
        // es de OTRA operacion (importe distinto) -> hay que emitir de verdad.
        Factura factura = Factura.pendiente(alumno, emisor, monto, periodo);
        factura.marcarError("rechazo previo");
        when(facturaRepository.findById(5L)).thenReturn(Optional.of(factura));
        when(arcaClient.consultarUltimoEmitido(emisor)).thenReturn(new ComprobanteEmitido(
                42, 12345678L, new BigDecimal("99999.00"), periodo, "70000000000001", LocalDate.of(2026, 7, 18)));
        when(arcaClient.solicitarCae(emisor, 96, 12345678L, monto, periodo, 5))
                .thenReturn(new ResultadoEmision(
                        true, 43, "75123456789013", LocalDate.of(2026, 7, 18), List.of()));
        when(facturaRepository.save(factura)).thenReturn(factura);

        // Act
        facturaService.emitir(5L);

        // Assert: emision nueva, con el CAE nuevo.
        assertEquals(EstadoFactura.EMITIDA, factura.getEstado());
        assertEquals("75123456789013", factura.getCae());
    }

    // ---------- emitirPorPeriodo (halt del lote ante timeout fantasma) ----------

    // Factura no expone setId (el id lo pone la base); en los tests lo
    // seteamos por reflexion para poder simular el lote.
    private Factura facturaPendienteConId(long id, Emisor emisorDeLaFactura) {
        Factura factura = Factura.pendiente(alumno, emisorDeLaFactura, monto, periodo);
        ReflectionTestUtils.setField(factura, "id", id);
        return factura;
    }

    @Test
    void emitirPorPeriodo_cortaElLote_cuandoFallaLaComunicacion() {
        // Arrange: tres pendientes DEL MISMO EMISOR. La 1ra sale bien y la
        // 2da da timeout: no sabemos si ARCA la emitio (posible fantasma).
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

        // Act
        facturaService.emitirPorPeriodo(periodo);

        // Assert: el lote se corto en la 2da; la 3ra NUNCA se intento.
        // Si se emitiera, el "ultimo emitido" de ARCA pisaria al fantasma
        // y el reintento de la 2da podria duplicar el comprobante.
        verify(arcaClient, times(2)).solicitarCae(any(), anyInt(), anyLong(), any(), any(), anyInt());
        verify(facturaRepository, never()).findById(3L);
        assertEquals(EstadoFactura.EMITIDA, f1.getEstado());
        assertEquals(EstadoFactura.ERROR, f2.getEstado());
        assertEquals(EstadoFactura.PENDIENTE, f3.getEstado());
    }

    @Test
    void emitirPorPeriodo_sigueConLaProxima_cuandoArcaRechaza() {
        // Arrange: dos pendientes. La 1ra recibe un rechazo definitivo de
        // ARCA (no un timeout): es seguro seguir con la siguiente.
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

        // Act
        facturaService.emitirPorPeriodo(periodo);

        // Assert: la 1ra quedo en ERROR y la 2da se emitio igual.
        assertEquals(EstadoFactura.ERROR, f1.getEstado());
        assertEquals(EstadoFactura.EMITIDA, f2.getEstado());
    }

    @Test
    void emitirPorPeriodo_unCorteEnUnEmisor_noFrenaElLoteDelOtro() {
        // Arrange (Fase 7): el batch agrupa por emisor. Un timeout en el
        // lote del emisor UNO corta SOLO ese lote: el emisor DOS tiene
        // numeracion propia en ARCA, puede seguir sin riesgo de duplicados.
        Emisor emisorDos = EmisoresDePrueba.emisor(2L, "20222222223", 1);
        Factura f1 = facturaPendienteConId(1L, emisor);      // emisor UNO -> timeout
        Factura f2 = facturaPendienteConId(2L, emisor);      // emisor UNO -> queda pendiente
        Factura f3 = facturaPendienteConId(3L, emisorDos);   // emisor DOS -> se emite igual
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

        // Act
        facturaService.emitirPorPeriodo(periodo);

        // Assert: f1 en ERROR, f2 ni se intento (mismo lote), f3 EMITIDA.
        assertEquals(EstadoFactura.ERROR, f1.getEstado());
        assertEquals(EstadoFactura.PENDIENTE, f2.getEstado());
        assertEquals(EstadoFactura.EMITIDA, f3.getEstado());
        verify(facturaRepository, never()).findById(2L);
    }
}
