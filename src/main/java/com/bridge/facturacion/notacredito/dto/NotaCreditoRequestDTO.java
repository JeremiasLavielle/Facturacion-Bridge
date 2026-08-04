package com.bridge.facturacion.notacredito.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NotaCreditoRequestDTO {
    @NotBlank
    private String motivo;
}
