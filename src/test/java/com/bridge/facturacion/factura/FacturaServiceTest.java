package com.bridge.facturacion.factura;

import com.bridge.facturacion.alumno.Alumno;
import com.bridge.facturacion.alumno.AlumnoRepository;
import com.bridge.facturacion.alumno.CondicionIva;
import com.bridge.facturacion.alumno.exception.AlumnoNotFoundException;
import com.bridge.facturacion.arca.ArcaClient;
import com.bridge.facturacion.arca.ArcaException;
import com.bridge.facturacion.arca.ResultadoEmision;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
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
    private FacturaMapper facturaMapper;
    @Mock
    private ArcaClient arcaClient;

    // El Service REAL, con los dobles inyectados por su constructor.
    @InjectMocks
    private FacturaService facturaService;

    // Datos de prueba reutilizables, armados antes de cada test.
    private Alumno alumno;
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

        request = new FacturaRequestDTO();
        request.setAlumnoId(1L);
        request.setMonto(monto);
        request.setPeriodo(periodo);
    }

    // ---------- create ----------

    @Test
    void create_guardaEnEstadoPendiente_cuandoNoHayDuplicado() {
        // Arrange: el alumno existe y no hay factura previa de ese período.
        when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));
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
    void create_tiraExcepcion_cuandoYaExisteFacturaDelPeriodo() {
        // Arrange: el alumno existe, pero ya hay una factura de ese período.
        when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));
        when(facturaRepository.existsByAlumnoAndPeriodo(alumno, periodo)).thenReturn(true);

        // Act + Assert
        assertThrows(FacturaAlreadyExistsException.class,
                () -> facturaService.create(request));

        verify(facturaRepository, never()).save(any());
    }

    // ---------- emitir (Fase 4: integración ARCA) ----------

    @Test
    void emitir_marcaEmitidaConCae_cuandoArcaAprueba() {
        // Arrange: factura PENDIENTE en el repo, y ARCA aprueba.
        Factura factura = Factura.pendiente(alumno, monto, periodo);
        when(facturaRepository.findById(5L)).thenReturn(Optional.of(factura));
        // Verificamos de paso el mapeo de dominio: DNI -> docTipo 96,
        // CONSUMIDOR_FINAL -> código ARCA 5.
        when(arcaClient.solicitarCae(96, 12345678L, monto, periodo, 5))
                .thenReturn(new ResultadoEmision(
                        true, 42, "75123456789012", LocalDate.of(2026, 7, 18), List.of()));
        when(facturaRepository.save(factura)).thenReturn(factura);

        // Act
        facturaService.emitir(5L);

        // Assert: la transición de estado dejó todo consistente.
        assertEquals(EstadoFactura.EMITIDA, factura.getEstado());
        assertEquals("75123456789012", factura.getCae());
        assertEquals(LocalDate.of(2026, 7, 18), factura.getVencimientoCae());
        assertNotNull(factura.getFechaEmision());
        assertNull(factura.getMensajeError());
        verify(facturaRepository).save(factura);
    }

    @Test
    void emitir_marcaError_cuandoArcaRechaza() {
        // Arrange: ARCA responde, pero rechaza el comprobante (rechazo de
        // negocio: NO es una excepción, queda registrado en la factura).
        Factura factura = Factura.pendiente(alumno, monto, periodo);
        when(facturaRepository.findById(5L)).thenReturn(Optional.of(factura));
        when(arcaClient.solicitarCae(96, 12345678L, monto, periodo, 5))
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
        Factura factura = Factura.pendiente(alumno, monto, periodo);
        when(facturaRepository.findById(5L)).thenReturn(Optional.of(factura));
        when(arcaClient.solicitarCae(96, 12345678L, monto, periodo, 5))
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
        Factura factura = Factura.pendiente(alumno, monto, periodo);
        factura.marcarEmitida("75123456789012", LocalDate.of(2026, 7, 18));
        when(facturaRepository.findById(5L)).thenReturn(Optional.of(factura));

        // Act + Assert: corta antes de tocar ARCA.
        assertThrows(FacturaYaEmitidaException.class, () -> facturaService.emitir(5L));
        verify(arcaClient, never()).solicitarCae(anyInt(), anyLong(), any(), any(), anyInt());
        verify(facturaRepository, never()).save(any());
    }
}
