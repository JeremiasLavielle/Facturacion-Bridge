package com.bridge.facturacion.arca;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuracion GLOBAL de ARCA: URLs, ambiente y timeouts.
 * Los datos propios de cada emisor (CUIT, punto de venta, certificado,
 * datos fiscales) viven en la tabla {@code emisores} desde la Fase 7.
 *
 * <p>{@code certsDir} es el directorio base donde estan los certificados;
 * cada emisor guarda sus paths relativos a el (ej. "20463447277/certificado.crt").</p>
 */
@Validated
@ConfigurationProperties(prefix = "arca")
public record ArcaProperties(
        @NotBlank String certsDir,
        @NotBlank String urlWsaa,
        @NotBlank String urlWsfe,
        Ambiente ambiente,
        @DefaultValue("15") @Positive int timeoutConexionSegundos,
        @DefaultValue("45") @Positive int timeoutRespuestaSegundos
) {}
