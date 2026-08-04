package com.bridge.facturacion.notacredito.dto;

import com.bridge.facturacion.emisor.dto.EmisorResponseDTO;
import com.bridge.facturacion.notacredito.EstadoNotaCredito;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class NotaCreditoResponseDTO {
    private Long id;
    private Long facturaId;
    private EmisorResponseDTO emisor;
    private BigDecimal monto;
    private String motivo;
    private EstadoNotaCredito estado;
    private LocalDateTime fechaEmision;
    private String cae;
    private LocalDate vencimientoCae;
    private Long numeroComprobante;
    private String mensajeError;
}
