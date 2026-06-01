package com.bridge.facturacion.factura.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class FacturaRequestDTO {
    @NotNull
    private Long alumnoId;
    @NotNull @Positive
    private BigDecimal monto;
    @NotNull
    private LocalDate periodo;
}
