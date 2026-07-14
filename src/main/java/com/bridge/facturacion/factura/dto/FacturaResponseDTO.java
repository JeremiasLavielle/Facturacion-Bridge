package com.bridge.facturacion.factura.dto;

import com.bridge.facturacion.alumno.dto.AlumnoResponseDTO;
import com.bridge.facturacion.factura.EstadoFactura;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class FacturaResponseDTO {
    private AlumnoResponseDTO alumno;
    private Long id;
    private BigDecimal monto;
    private LocalDate periodo;
    private EstadoFactura estado;
    private LocalDateTime fechaEmision;
    private String cae;
    private LocalDate vencimientoCae;
    private Long numeroComprobante;
    private String mensajeError;
}
