package com.bridge.facturacion.pdf;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "emisor")
public record EmisorProperties(
        @NotBlank String nombreFantasia,
        @NotBlank String razonSocial,
        @NotBlank String domicilio,
        @NotBlank String condicionFiscal,
        @NotBlank String ingresosBrutos,
        @NotBlank String inicioActividades
) {}
