package com.bridge.facturacion.arca;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "arca")
public record ArcaProperties(
        @NotBlank String cuitEmisor,
        int puntoVenta,
        @NotBlank String certificado,
        @NotBlank String clavePrivada,
        @NotBlank String urlWsaa,
        @NotBlank String urlWsfe,
        Ambiente ambiente
) {}