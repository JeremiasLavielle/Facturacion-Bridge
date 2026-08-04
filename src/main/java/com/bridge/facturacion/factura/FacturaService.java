package com.bridge.facturacion.factura;

import com.bridge.facturacion.alumno.Alumno;
import com.bridge.facturacion.alumno.AlumnoRepository;
import com.bridge.facturacion.alumno.exception.AlumnoNotFoundException;
import com.bridge.facturacion.arca.ArcaClient;
import com.bridge.facturacion.arca.ArcaComunicacionException;
import com.bridge.facturacion.arca.ComprobanteEmitido;
import com.bridge.facturacion.arca.ArcaException;
import com.bridge.facturacion.arca.ResultadoEmision;
import com.bridge.facturacion.emisor.Emisor;
import com.bridge.facturacion.emisor.EmisorRepository;
import com.bridge.facturacion.emisor.exception.EmisorNotFoundException;
import com.bridge.facturacion.factura.dto.FacturaRequestDTO;
import com.bridge.facturacion.factura.dto.FacturaResponseDTO;
import com.bridge.facturacion.factura.exception.FacturaAlreadyExistsException;
import com.bridge.facturacion.factura.exception.FacturaNotFoundException;
import com.bridge.facturacion.factura.exception.FacturaYaEmitidaException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FacturaService {

    private static final Logger log = LoggerFactory.getLogger(FacturaService.class);

    private static final int DOC_TIPO_DNI = 96;
    private final AlumnoRepository alumnoRepository;
    private final FacturaRepository facturaRepository;
    private final EmisorRepository emisorRepository;
    private final FacturaMapper facturaMapper;
    private final ArcaClient arcaClient;

    private Alumno findAlumnoById(Long id) {
        return alumnoRepository.findById(id)
                .orElseThrow(() -> new AlumnoNotFoundException(id));
    }

    @Transactional
    public FacturaResponseDTO create(FacturaRequestDTO request) {
        Alumno alumno = findAlumnoById(request.getAlumnoId());
        Emisor emisor = emisorRepository.findById(request.getEmisorId())
                .orElseThrow(() -> new EmisorNotFoundException(request.getEmisorId()));
        LocalDate periodo = request.getPeriodo();

        // La restriccion es GLOBAL (sin emisor): un alumno paga una sola
        // cuota por mes, la facture quien la facture.
        if (facturaRepository.existsByAlumnoAndPeriodo(alumno, periodo)) {
            throw new FacturaAlreadyExistsException(alumno, periodo);
        }

        Factura factura = Factura.pendiente(alumno, emisor, request.getMonto(), periodo);
        Factura facturaGuardada = facturaRepository.save(factura);
        return facturaMapper.toResponse(facturaGuardada);
    }

    public FacturaResponseDTO emitir(Long id) {
        Factura factura = facturaRepository.findById(id)
                .orElseThrow(() -> new FacturaNotFoundException(id));

        if (factura.getEstado() == EstadoFactura.EMITIDA) {
            throw new FacturaYaEmitidaException(id, factura.getCae());
        }

        Emisor emisor = factura.getEmisor();

        if (factura.getEstado() == EstadoFactura.ERROR) {
            ComprobanteEmitido ultimo = arcaClient.consultarUltimoEmitido(emisor);
            if (ultimo != null && coincideCon(factura, ultimo)) {
                log.warn("Factura {} ya existia en ARCA (cbte {}, CAE {}): se recupera sin reemitir",
                        id, ultimo.numero(), ultimo.cae());
                factura.marcarEmitida(ultimo.cae(), ultimo.vencimientoCae(), ultimo.numero());
                return facturaMapper.toResponse(facturaRepository.save(factura));
            }
        }

        Alumno alumno = factura.getAlumno();

        ResultadoEmision resultado;
        try {
            resultado = arcaClient.solicitarCae(
                    emisor,
                    DOC_TIPO_DNI,
                    Long.parseLong(alumno.getDni()),
                    factura.getMonto(),
                    factura.getPeriodo(),
                    alumno.getCondicionIva().getCodigoArca());
        } catch (ArcaException e) {
            factura.marcarError(e.getMessage());
            facturaRepository.save(factura);
            throw e;
        }

        if (resultado.aprobada()) {
            factura.marcarEmitida(resultado.cae(), resultado.vencimientoCae(), resultado.numeroComprobante());
        } else {
            factura.marcarError(String.join(" | ", resultado.mensajes()));
        }
        return facturaMapper.toResponse(facturaRepository.save(factura));
    }

    /**
     * Emision batch del periodo, agrupada por emisor. Un corte de
     * comunicacion (posible timeout fantasma) detiene SOLO el lote del
     * emisor afectado: los otros emisores tienen numeracion propia en
     * ARCA, asi que pueden seguir sin riesgo de duplicados.
     */
    public List<FacturaResponseDTO> emitirPorPeriodo(LocalDate periodo) {
        Map<Long, List<Factura>> lotesPorEmisor = new LinkedHashMap<>();
        for (Factura factura : facturaRepository.findByPeriodo(periodo)) {
            if (factura.getEstado() == EstadoFactura.EMITIDA) {
                continue;
            }
            lotesPorEmisor.computeIfAbsent(factura.getEmisor().getId(), k -> new ArrayList<>())
                    .add(factura);
        }

        List<FacturaResponseDTO> resultados = new ArrayList<>();
        for (List<Factura> lote : lotesPorEmisor.values()) {
            for (Factura factura : lote) {
                try {
                    resultados.add(emitir(factura.getId()));
                } catch (ArcaComunicacionException e) {
                    log.warn("Lote del periodo {} del emisor {} detenido en factura {}: {}. "
                            + "El resto de ESTE emisor queda pendiente hasta resolver esta.",
                            periodo, factura.getEmisor().getCuit(), factura.getId(), e.getMessage());
                    resultados.add(findById(factura.getId()));
                    break; // corta solo el lote de este emisor
                } catch (ArcaException e) {
                    resultados.add(findById(factura.getId()));
                }
            }
        }
        return resultados;
    }

    @Transactional(readOnly = true)
    public FacturaResponseDTO findById(Long id) {
        Factura factura = facturaRepository.findById(id)
                .orElseThrow(() -> new FacturaNotFoundException(id));
        return facturaMapper.toResponse(factura);
    }

    @Transactional(readOnly = true)
    public List<FacturaResponseDTO> findAll() {
        List<Factura> facturas = facturaRepository.findAll();
        return facturas.stream().map(facturaMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<FacturaResponseDTO> findByPeriodo(LocalDate periodo) {
        List<Factura> facturas = facturaRepository.findByPeriodo(periodo);
        return facturas.stream().map(facturaMapper::toResponse).toList();
    }

    private boolean coincideCon(Factura factura, ComprobanteEmitido ultimo) {
        return ultimo.docNro() == Long.parseLong(factura.getAlumno().getDni())
                && ultimo.importeTotal().compareTo(factura.getMonto()) == 0
                && ultimo.servicioDesde().equals(factura.getPeriodo().withDayOfMonth(1));
    }

    @Transactional(readOnly = true)
    public List<FacturaResponseDTO> findByAlumno(Long alumnoId) {
        Alumno alumno = findAlumnoById(alumnoId);
        List<Factura> facturas = facturaRepository.findByAlumno(alumno);
        return facturas.stream().map(facturaMapper::toResponse).toList();
    }
}
