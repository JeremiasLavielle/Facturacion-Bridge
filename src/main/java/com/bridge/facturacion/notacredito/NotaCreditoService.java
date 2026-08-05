package com.bridge.facturacion.notacredito;

import com.bridge.facturacion.alumno.Alumno;
import com.bridge.facturacion.arca.ArcaClient;
import com.bridge.facturacion.arca.ArcaException;
import com.bridge.facturacion.arca.ComprobanteAsociado;
import com.bridge.facturacion.arca.ComprobanteEmitido;
import com.bridge.facturacion.arca.ResultadoEmision;
import com.bridge.facturacion.emisor.Emisor;
import com.bridge.facturacion.factura.EstadoFactura;
import com.bridge.facturacion.factura.Factura;
import com.bridge.facturacion.factura.FacturaRepository;
import com.bridge.facturacion.factura.exception.FacturaNotFoundException;
import com.bridge.facturacion.notacredito.dto.NotaCreditoResponseDTO;
import com.bridge.facturacion.notacredito.exception.FacturaNoAnulableException;
import com.bridge.facturacion.notacredito.exception.NotaCreditoNotFoundException;
import com.bridge.facturacion.notacredito.exception.NotaCreditoYaEmitidaException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotaCreditoService {

    private static final Logger log = LoggerFactory.getLogger(NotaCreditoService.class);

    private static final int DOC_TIPO_DNI = 96;
    private final FacturaRepository facturaRepository;
    private final NotaCreditoRepository notaCreditoRepository;
    private final NotaCreditoMapper notaCreditoMapper;
    private final ArcaClient arcaClient;

public NotaCreditoResponseDTO crearYEmitir(Long facturaId, String motivo) {
        Factura factura = facturaRepository.findById(facturaId)
                .orElseThrow(() -> new FacturaNotFoundException(facturaId));

        NotaCredito existente = notaCreditoRepository.findByFactura(factura).orElse(null);
        if (existente != null) {
            if (existente.getEstado() == EstadoNotaCredito.EMITIDA) {
                throw new NotaCreditoYaEmitidaException(facturaId, existente.getCae());
            }
            log.info("Reintentando la NC {} de la factura {} (quedo {})",
                    existente.getId(), facturaId, existente.getEstado());
            return emitir(existente.getId());
        }

        if (factura.getEstado() != EstadoFactura.EMITIDA) {
            throw new FacturaNoAnulableException(facturaId, factura.getEstado());
        }

        NotaCredito nc = notaCreditoRepository.save(NotaCredito.pendiente(factura, motivo));
        return emitir(nc.getId());
    }

    public NotaCreditoResponseDTO emitir(Long id) {
        NotaCredito nc = notaCreditoRepository.findById(id)
                .orElseThrow(() -> new NotaCreditoNotFoundException(id));

        if (nc.getEstado() == EstadoNotaCredito.EMITIDA) {
            throw new NotaCreditoYaEmitidaException(nc.getFactura().getId(), nc.getCae());
        }

        Factura factura = nc.getFactura();
        Emisor emisor = nc.getEmisor();
        Alumno alumno = factura.getAlumno();

if (nc.getEstado() == EstadoNotaCredito.ERROR) {
            ComprobanteEmitido ultimo = arcaClient.consultarUltimoEmitido(emisor, ArcaClient.NOTA_CREDITO_C);
            if (ultimo != null && coincideCon(nc, ultimo)) {
                log.warn("NC {} ya existia en ARCA (cbte {}, CAE {}): se recupera sin reemitir",
                        id, ultimo.numero(), ultimo.cae());
                nc.marcarEmitida(ultimo.cae(), ultimo.vencimientoCae(), ultimo.numero());
                anularFactura(factura);
                return notaCreditoMapper.toResponse(notaCreditoRepository.save(nc));
            }
        }

        ComprobanteAsociado asociado = new ComprobanteAsociado(
                ArcaClient.FACTURA_C,
                emisor.getPuntoVenta(),
                factura.getNumeroComprobante(),
                emisor.getCuit(),
                factura.getFechaEmision().toLocalDate());

        ResultadoEmision resultado;
        try {
            resultado = arcaClient.solicitarCae(
                    emisor,
                    ArcaClient.NOTA_CREDITO_C,
                    asociado,
                    DOC_TIPO_DNI,
                    Long.parseLong(alumno.getDni()),
                    nc.getMonto(),
                    factura.getPeriodo(),
                    alumno.getCondicionIva().getCodigoArca());
        } catch (ArcaException e) {
            nc.marcarError(e.getMessage());
            notaCreditoRepository.save(nc);
            throw e;
        }

        if (resultado.aprobada()) {
            nc.marcarEmitida(resultado.cae(), resultado.vencimientoCae(), resultado.numeroComprobante());
            anularFactura(factura);
        } else {
            nc.marcarError(String.join(" | ", resultado.mensajes()));
        }
        return notaCreditoMapper.toResponse(notaCreditoRepository.save(nc));
    }

    @Transactional(readOnly = true)
    public NotaCreditoResponseDTO findByFacturaId(Long facturaId) {
        Factura factura = facturaRepository.findById(facturaId)
                .orElseThrow(() -> new FacturaNotFoundException(facturaId));
        NotaCredito nc = notaCreditoRepository.findByFactura(factura)
                .orElseThrow(() -> NotaCreditoNotFoundException.deFactura(facturaId));
        return notaCreditoMapper.toResponse(nc);
    }

    private void anularFactura(Factura factura) {
        factura.marcarAnulada();
        facturaRepository.save(factura);
    }

    private boolean coincideCon(NotaCredito nc, ComprobanteEmitido ultimo) {
        Factura factura = nc.getFactura();
        return ultimo.docNro() == Long.parseLong(factura.getAlumno().getDni())
                && ultimo.importeTotal().compareTo(nc.getMonto()) == 0
                && ultimo.servicioDesde().equals(factura.getPeriodo().withDayOfMonth(1));
    }
}
