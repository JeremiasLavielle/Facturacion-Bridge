package com.bridge.facturacion.emisor.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EmisorResponseDTO {
    private Long id;
    private String cuit;
    private String razonSocial;
    private String nombreFantasia;
    private int puntoVenta;
    private boolean activo;
}
